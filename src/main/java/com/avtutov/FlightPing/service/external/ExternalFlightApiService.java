package com.avtutov.FlightPing.service.external;

import java.time.LocalDate;
import java.util.List;

import com.avtutov.FlightPing.dto.external.AeroDataFlightResponse;
import com.avtutov.FlightPing.dto.external.AeroDataSubscriptionResponse;

import reactor.core.publisher.Mono;

public interface ExternalFlightApiService {
	
	List<AeroDataFlightResponse> getFlightInfo(String flightNumber, LocalDate date);
	
	Mono<AeroDataSubscriptionResponse> subscribeToFlightAlerts(String flightNumber);
	
	Mono<Void> unsubscribeFromFlightAlerts(String subscriptionId);

}
