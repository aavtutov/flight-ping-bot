package com.avtutov.FlightPing.dto;

import java.time.LocalDate;

public record FlightRequestDto(
		
		String flight,
		LocalDate date) {}
