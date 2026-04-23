package com.avtutov.FlightPing.service;

import java.util.Optional;

import com.avtutov.FlightPing.dto.FlightRequestDto;

public interface ParserService {

	Optional<FlightRequestDto> processUserInput(String messageText);
	
}
