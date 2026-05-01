package com.avtutov.FlightPing.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AeroDataSubscriptionResponse(String id) {}
