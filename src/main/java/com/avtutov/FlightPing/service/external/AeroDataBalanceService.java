package com.avtutov.FlightPing.service.external;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.avtutov.FlightPing.config.WebClientConfig.FlightWebClient;
import com.avtutov.FlightPing.dto.external.AeroDataBalanceResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Service
@Slf4j
public class AeroDataBalanceService {
	
	private final WebClient flightWebClient;
	private final StringRedisTemplate redisTemplate;
	
	private final String BALANCE_KEY = "aerodata:balance";
    private final String LAST_CHECK_KEY = "aerodata:last_check";
	
	public AeroDataBalanceService(
			
			FlightWebClient flightWebClient,
			StringRedisTemplate redisTemplate) {
		
		this.flightWebClient = flightWebClient.webClient();
		this.redisTemplate = redisTemplate;
	}
	
	public Mono<AeroDataBalanceResponse> getBalance() {
	    return flightWebClient.get()
	            .uri("/subscriptions/balance")
	            .accept(MediaType.APPLICATION_JSON)
	            .retrieve()
	            .bodyToMono(AeroDataBalanceResponse.class)
	            .timeout(Duration.ofSeconds(5))
	            .defaultIfEmpty(new AeroDataBalanceResponse(0, null, null))
	            .publishOn(Schedulers.boundedElastic())
	            .doOnSuccess(response -> {
	                updateRedis(response);
	            })
	            .doOnError(error -> log.error("❌ Failed to fetch balance: {}", error.getMessage()));
	}
	
	public Mono<AeroDataBalanceResponse> refill(int credits) {
	    return flightWebClient.post()
	            .uri("/subscriptions/balance/refill")
	            .contentType(MediaType.APPLICATION_JSON)
	            .bodyValue(Map.of("credits", credits))
	            .retrieve()
	            .bodyToMono(AeroDataBalanceResponse.class)
	            .timeout(Duration.ofSeconds(10))
	            .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
	            		.filter(e -> e instanceof WebClientResponseException && 
                                ((WebClientResponseException) e).getStatusCode().value() == 429))
	            .publishOn(Schedulers.boundedElastic())
	            .doOnSuccess(this::updateRedis)
	            .doOnError(error -> log.error("❌ Refill failed after retries: {}", error.getMessage()));
	}
	
	public long getVirtualBalance() {
		String val = redisTemplate.opsForValue().get(BALANCE_KEY);
		return val != null ? Long.parseLong(val) : 0;
	}
	
	public long getVirtualLastChecked() {
		String val = redisTemplate.opsForValue().get(LAST_CHECK_KEY);
		return val != null ? Long.parseLong(val) : 0;
	}
	
	public void virtualDecrement() {
	    redisTemplate.opsForValue().decrement(BALANCE_KEY);
	    log.debug("Virtual balance decrement in Redis");
	}

	public void updateRedisFromCallback(int credits) {
	    redisTemplate.opsForValue().set(BALANCE_KEY, String.valueOf(credits));
	    redisTemplate.opsForValue().set(LAST_CHECK_KEY, String.valueOf(Instant.now().getEpochSecond()));
	}
	
	private void updateRedis(AeroDataBalanceResponse response) {
        redisTemplate.opsForValue().set(BALANCE_KEY, String.valueOf(response.creditsRemaining()));
        redisTemplate.opsForValue().set(LAST_CHECK_KEY, String.valueOf(Instant.now().getEpochSecond()));
    }
}
