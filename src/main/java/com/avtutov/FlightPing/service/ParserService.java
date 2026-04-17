package com.avtutov.FlightPing.service;

import java.util.Optional;

import com.avtutov.FlightPing.dto.FlightParsedData;

public interface ParserService {

	Optional<FlightParsedData> parse(String messageText);
	
}
