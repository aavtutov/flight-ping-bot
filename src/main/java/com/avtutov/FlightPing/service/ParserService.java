package com.avtutov.FlightPing.service;

import com.avtutov.FlightPing.dto.FlightParsedData;

public interface ParserService {

	FlightParsedData parse(String message);
	
}
