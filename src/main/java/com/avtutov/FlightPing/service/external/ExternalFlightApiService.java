package com.avtutov.FlightPing.service.external;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.avtutov.FlightPing.dto.external.AeroDataBoxResponse;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class ExternalFlightApiService {

	private final WebClient webClient;
	private final String apiKey;
	private final String apiHost;
	
	public ExternalFlightApiService(
            WebClient.Builder webClientBuilder,
            @Value("${aerodatabox.api.base-url}") String baseUrl,
            @Value("${aerodatabox.api.key}") String apiKey,
            @Value("${aerodatabox.api.host}") String apiHost) {
		
		this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.apiHost = apiHost;
	}
	
	private long lastRequestTime = 0;
	
	public List<AeroDataBoxResponse> getFlightInfo(String flightNumber, LocalDate date) {
		
		long currentTime = System.currentTimeMillis();
		
		if (currentTime - lastRequestTime < 1500) {
	        try {
	            Thread.sleep(1500); 
	        } catch (InterruptedException e) {
	            Thread.currentThread().interrupt();
	        }
	    }
        
		String urlPath = String.format("/flights/number/%s/%s", flightNumber, date.toString());
		
        log.info("Requesting flight info from AeroDataBox: {}", urlPath);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(urlPath)
                        .queryParam("withAircraftImage", "false")
                        .queryParam("withLocation", "false")
                        .queryParam("withFlightPlan", "false")
                        .queryParam("dateLocalRole", "Both")
                        .build())
                .header("X-RapidAPI-Key", apiKey)
                .header("X-RapidAPI-Host", apiHost)
                .retrieve()
                .bodyToFlux(AeroDataBoxResponse.class) 
                .collectList()
                .doOnError(e -> log.error("AeroDataBox API error: {}", e.getMessage()))
                .onErrorResume(e -> Mono.empty())
                .block();
    }
}
