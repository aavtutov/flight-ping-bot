package com.avtutov.FlightPing.config;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Configuration
public class WebClientConfig {
	
	private final String telegramBaseUrl;
	
	private final String aeroDataBaseUrl;
	private final String aeroDataApiKey;
	private final String aeroDataApiHost;
	
	WebClientConfig(
			
			@Value("${telegram.api.base-url}") String telegramBaseUrl,
			@Value("${aerodatabox.api.base-url}") String aeroDataBaseUrl,
			@Value("${aerodatabox.api.key}") String aeroDataApiKey,
			@Value("${aerodatabox.api.host}") String aeroDataApiHost) {
		
		this.telegramBaseUrl = telegramBaseUrl;
		this.aeroDataBaseUrl = aeroDataBaseUrl;
		this.aeroDataApiKey = aeroDataApiKey;
		this.aeroDataApiHost = aeroDataApiHost;
	}
	
	@Bean
	FlightWebClient flightWebClient(WebClient.Builder builder) {
	    WebClient client = builder.baseUrl(aeroDataBaseUrl)
	    		.defaultHeader("X-RapidAPI-Key", aeroDataApiKey)
	    		.defaultHeader("X-RapidAPI-Host", aeroDataApiHost)
	    		.filter(rateLimitFilter(1100))
	    		.build();
	    return new FlightWebClient(client);
	}
	
	@Bean
	TelegramWebClient telegramWebClient(WebClient.Builder builder) {
	    WebClient client = builder.baseUrl(telegramBaseUrl).build();
	    return new TelegramWebClient(client);
	}
	
	private ExchangeFilterFunction rateLimitFilter(long minDelayMs) {
        AtomicLong nextAllowedTime = new AtomicLong(System.currentTimeMillis());

        return (request, next) -> Mono.defer(() -> {
            long now = System.currentTimeMillis();
            long targetTime = nextAllowedTime.getAndUpdate(t -> Math.max(t, now) + minDelayMs);
            long delay = Math.max(0, targetTime - now);

            return Mono.delay(Duration.ofMillis(delay))
                       .flatMap(d -> next.exchange(request));
        });
    }
	
	public record FlightWebClient(WebClient webClient) {}
	public record TelegramWebClient(WebClient webClient) {}
}
