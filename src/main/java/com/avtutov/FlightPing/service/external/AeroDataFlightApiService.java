package com.avtutov.FlightPing.service.external;

import java.time.Duration;
import java.time.LocalDate;
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
	public List<AeroDataFlightResponse> getFlightInfo(String flightNumber, LocalDate date) {
        
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
                .doOnError(e -> log.error("AeroDataBox API error: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .block();
    }

	@Override
	public Mono<AeroDataSubscriptionResponse> subscribeToFlightAlerts(String flightNumber) {
		
		var requestBody = new SubscriptionRequest(callBackUrl, 0);
		
		return flightWebClient.post()
				.uri("/subscriptions/webhook/FlightByNumber/" + flightNumber)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(requestBody)
				.retrieve()
		        .bodyToMono(AeroDataSubscriptionResponse.class)
		        .retryWhen(Retry.backoff(2, Duration.ofSeconds(2))
		        		.filter(e -> e instanceof WebClientResponseException && 
                                ((WebClientResponseException) e).getStatusCode().value() == 429))
		        .doOnSuccess(res -> log.info("✅ Subscribed to {}", flightNumber))
		        .doOnError(e -> log.error("❌ Failed to subscribe to {}: {}", flightNumber, e.getMessage()));
		}

	@Override
	public Mono<Void> unsubscribeFromFlightAlerts(String subscriptionId) {
		return flightWebClient.delete()
				.uri("/subscriptions/webhook/" + subscriptionId)
				.retrieve()
				.bodyToMono(Void.class)
				.doOnSuccess(v -> log.info("✅ Successfully unsubscribed from ID: {}", subscriptionId))
				.doOnError(e -> log.error("❌ Failed to unsubscribe from ID {}: {}", subscriptionId, e.getMessage()));
	}

}
