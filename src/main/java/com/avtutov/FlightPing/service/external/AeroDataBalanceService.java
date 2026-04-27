package com.avtutov.FlightPing.service.external;

import java.time.Duration;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.avtutov.FlightPing.dto.external.AeroDataBalanceResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@Slf4j
public class AeroDataBalanceService {
	
	private String apiKey;
	private String apiHost;
	private final WebClient webClient;
	
	public AeroDataBalanceService(
			
			@Value("${aerodatabox.api.base-url}") String baseUrl,
			@Value("${aerodatabox.api.key}") String apiKey,
			@Value("${aerodatabox.api.host}") String apiHost,
			
			WebClient.Builder webClientBuilder) {
		
		this.apiKey = apiKey;
		this.apiHost = apiHost;
		this.webClient = webClientBuilder.baseUrl(baseUrl).build();
	}
	
	
	public Mono<AeroDataBalanceResponse> getBalance() {
		
		return webClient.get()
				.uri("/subscriptions/balance")
				.header("X-RapidAPI-Key", apiKey)
				.header("X-RapidAPI-Host", apiHost)
				.retrieve()
				.bodyToMono(AeroDataBalanceResponse.class)
				.timeout(Duration.ofSeconds(5))
				.retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
				.doOnSuccess(response -> log.info("Actual AeroData Balance: {} credits", response.creditsRemaining()))
				.doOnError(error -> log.error("Error getting balance: {}", error.getMessage()));
	}
	
	public Mono<AeroDataBalanceResponse> refill(int credits) {
		
		Map<String, Integer> body = Map.of("credits", credits);
		
		return webClient.post()
				.uri("/subscriptions/balance/refill")
				.header("X-RapidAPI-Key", apiKey)
				.header("X-RapidAPI-Host", apiHost)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(AeroDataBalanceResponse.class)
				.timeout(Duration.ofSeconds(5))
				.doOnSuccess(response -> log.info("Successfully refilled {} credits. New balance: {}", 
			            credits, response.creditsRemaining()))
				.doOnError(error -> log.error("Failed to refill credits: {}", error.getMessage()));
	}

}
