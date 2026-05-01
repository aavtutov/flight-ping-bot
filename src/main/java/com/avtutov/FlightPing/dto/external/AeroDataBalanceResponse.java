package com.avtutov.FlightPing.dto.external;

public record AeroDataBalanceResponse(
		
	    int creditsRemaining,
	    String lastRefillAt,
	    String expiresAt) {}
