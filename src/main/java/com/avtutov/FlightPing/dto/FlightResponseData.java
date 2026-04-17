package com.avtutov.FlightPing.dto;

import java.time.Instant;
import java.time.LocalDate;

public record FlightResponseData(
		
		String airlineCode,
		String flightNumber,
		LocalDate flightDate,
		
		String status,
		String gate,
		
		String departureCity,
	    String arrivalCity,
	    
	    String departureAirportCode,
	    String arrivalAirportCode,
	    
	    Instant departureTime,
	    Instant arrivalTime) {}
