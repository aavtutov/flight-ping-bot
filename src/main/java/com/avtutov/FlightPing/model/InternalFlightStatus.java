package com.avtutov.FlightPing.model;

public enum InternalFlightStatus {
	
    CHECK_IN("Check-in is open"),
    BOARDING("Boarding in progress ..."),
    GATECLOSED("Gate closed"),
    DEPARTED("Departed"),
    ENROUTE("En route"),
    EXPECTED("Expected"),
    ARRIVED("Arrived"),
    APPROACHING("On approach to destination"),
    DIVERTED("Diverted to another destination"),
    CANCELLED("Cancelled"),
    CANCELLED_UNCERTAIN("is uncertain, may be cancelled"),
    DELAYED("Delayed"),
    UNKNOWN("is not available for this flight");

	private final String description;
	
	InternalFlightStatus(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}
	
}