package com.avtutov.FlightPing.dto.external;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AeroDataBalanceResponse(
		
		@JsonProperty("creditsRemaining") long creditsRemaining,
		@JsonProperty("lastRefilledUtc") Instant lastRefilledUtc,
		@JsonProperty("lastDeductedUtc") Instant lastDeductedUtc
		
		) {}
