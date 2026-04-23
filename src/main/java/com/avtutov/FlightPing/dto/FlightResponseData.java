package com.avtutov.FlightPing.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.avtutov.FlightPing.model.InternalFlightStatus;

public record FlightResponseData(
		
		String airlineCode,
		String flightNumber,
		LocalDate flightDate,
		
		InternalFlightStatus status,
		String terminal,
		String checkInDesk,
		String gate,
		
		String departureCity,
	    String arrivalCity,
	    
	    String departureAirportCode,
	    String arrivalAirportCode,
	    
	    Instant departureTime,
	    Instant arrivalTime,
	    
	    String scheduledTime) {}
