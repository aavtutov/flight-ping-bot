package com.avtutov.FlightPing.dto.external;

public record SubscriptionRequest(String url, Integer maxDeliveryRetries) {}