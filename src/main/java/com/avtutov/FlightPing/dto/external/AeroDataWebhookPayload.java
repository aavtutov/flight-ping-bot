package com.avtutov.FlightPing.dto.external;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AeroDataWebhookPayload(
		
		List<FlightNotification> flights,
	    SubscriptionInfo subscription,
	    BalanceInfo balance

		) {
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record FlightNotification(
	    String number,
	    FlightStatus status,
	    String notificationSummary,
	    String notificationRemark,
	    FlightPoint departure,
	    FlightPoint arrival,
	    Aircraft aircraft
	) {}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record FlightPoint(
		Airport airport,
		TimeInfo scheduledTime,
		TimeInfo revisedTime,
	    String terminal,
	    String gate
	) {}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Airport(
			String icao,
			String iata,
			String name,
			String municipalityName) {}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Aircraft(
			String reg,
			String model) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TimeInfo(String local, String utc) {}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record BalanceInfo(int creditsRemaining) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record SubscriptionInfo(String id) {}
	
}
