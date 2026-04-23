package com.avtutov.FlightPing.service;

import java.util.List;
import java.util.Optional;

import com.avtutov.FlightPing.dto.FlightRequestDto;
import com.avtutov.FlightPing.model.Flight;

public interface FlightService {
	
	List<Flight> findOrCreateFlights(FlightRequestDto requestDto);
	
	Optional<Flight> findById(Long id);

}
