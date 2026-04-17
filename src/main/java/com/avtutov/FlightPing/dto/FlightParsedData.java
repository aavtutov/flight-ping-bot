package com.avtutov.FlightPing.dto;

import java.time.LocalDate;

public record FlightParsedData(
		
		String airlineCode,
		String flightNumber,
		LocalDate flightDate) {}
