package com.avtutov.FlightPing.service.external;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.avtutov.FlightPing.config.WebClientConfig.FlightWebClient;
import com.avtutov.FlightPing.dto.external.AeroDataFlightResponse;
import com.avtutov.FlightPing.dto.external.AeroDataSubscriptionResponse;
import com.avtutov.FlightPing.dto.external.SubscriptionRequest;
import com.avtutov.FlightPing.service.util.HashService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@Slf4j
public class AeroDataFlightApiService implements ExternalFlightApiService {

	private final WebClient flightWebClient;
	private final String callBackUrl;
	
	public AeroDataFlightApiService(
			
			FlightWebClient flightWebClient,
            HashService hashService,
            @Value("${app.base.url}") String appBaseUrl) {
		
		this.flightWebClient = flightWebClient.webClient();
		this.callBackUrl = String.format("%s/callback/flight-update/%s", appBaseUrl, hashService.getAerodataHashToken());
	}
	
	@Override
	public Mono<List<AeroDataFlightResponse>> getFlightInfo(String flightNumber, LocalDate date) {
        
		String urlPath = String.format("/flights/number/%s/%s", flightNumber, date.toString());
		
        log.info("Requesting flight info from AeroDataBox: {}", urlPath);
        return flightWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(urlPath)
                        .queryParam("withAircraftImage", "false")
                        .queryParam("withLocation", "false")
                        .queryParam("withFlightPlan", "false")
                        .queryParam("dateLocalRole", "Both")
                        .build())
                .retrieve()
                .bodyToFlux(AeroDataFlightResponse.class) 
                .collectList()
                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1))
                        .filter(e -> !(e instanceof WebClientResponseException)))
                .doOnError(e -> log.error("❌ AeroDataBox API error for {}: {}", flightNumber, e.getMessage()))
                .onErrorResume(e -> Mono.just(Collections.emptyList()));
    }

	@Override
	public Mono<AeroDataSubscriptionResponse> subscribeToFlightAlerts(String flightNumber) {
		
		var requestBody = new SubscriptionRequest(callBackUrl, 0);
		
		log.info("Subscribing to AeroDataBox: {}", flightNumber);
		return flightWebClient.post()
				.uri("/subscriptions/webhook/FlightByNumber/" + flightNumber)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(requestBody)
				.retrieve()
		        .bodyToMono(AeroDataSubscriptionResponse.class)
		        .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
		        		.filter(this::is429Error))
		        .doOnSuccess(res -> log.debug("API: Subscribed to flightNumber {}", flightNumber))
		        .doOnError(e -> log.error("❌ API: Failed to subscribe flightNumber {}: {}", flightNumber, e.getMessage()));
	}

	@Override
	public Mono<Void> unsubscribeFromFlightAlerts(String subscriptionId) {
		
		log.info("Unsubscribing from AeroDataBox: ID={}", subscriptionId);
		return flightWebClient.delete()
				.uri("/subscriptions/webhook/" + subscriptionId)
				.retrieve()
				.bodyToMono(Void.class)
				.retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
						.filter(this::is429Error))
				.doOnSuccess(v -> log.debug("API: Unsubscribed ID {}", subscriptionId))
				.doOnError(e -> log.error("❌ API: Failed to unsubscribe ID {}: {}", subscriptionId, e.getMessage()));
	}
	
	private boolean is429Error(Throwable throwable) {
	    return throwable instanceof WebClientResponseException e && 
	           e.getStatusCode().value() == 429;
	}

}
