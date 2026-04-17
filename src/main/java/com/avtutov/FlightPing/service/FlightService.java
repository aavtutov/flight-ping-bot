package com.avtutov.FlightPing.service;

import com.avtutov.FlightPing.dto.FlightParsedData;
import com.avtutov.FlightPing.dto.FlightResponseData;
import com.avtutov.FlightPing.model.Flight;

public interface FlightService {
	
	Flight getOrCreateFlight(FlightParsedData data);
	
	void updateFlightInfo(Flight flight, FlightResponseData freshData);

}
