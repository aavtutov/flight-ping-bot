package com.avtutov.FlightPing.service;

import java.util.Objects;

import org.springframework.stereotype.Service;

import com.avtutov.FlightPing.dto.FlightParsedData;
import com.avtutov.FlightPing.dto.FlightResponseData;
import com.avtutov.FlightPing.model.Flight;
import com.avtutov.FlightPing.repository.FlightRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {
	
	private final FlightRepository flightRepository;

	@Transactional
	@Override
	public Flight getOrCreateFlight(FlightParsedData data) {
		
		return flightRepository.findByAirlineCodeAndFlightNumberAndFlightDate(
				data.airlineCode(),
				data.flightNumber(),
				data.flightDate())
		.orElseGet(() -> registerNewFlight(data));
	}

	@Transactional
	@Override
	public void updateFlightInfo(Flight flight, FlightResponseData apiData) {
		
		String newHash = generateHash(apiData);
		
		if (Objects.equals(flight.getDataHash(), newHash)) {
            return;
        }
		
		flight.setStatus(apiData.status());
        flight.setGate(apiData.gate());
        flight.setDepartureCity(apiData.departureCity());
        flight.setArrivalCity(apiData.arrivalCity());
        flight.setDepartureAirportCode(apiData.departureAirportCode());
        flight.setArrivalAirportCode(apiData.arrivalAirportCode());
        flight.setDepartureTime(apiData.departureTime());
        flight.setArrivalTime(apiData.arrivalTime());
        flight.setDataHash(newHash);
	}
	
	private Flight registerNewFlight(FlightParsedData data) {
		
		Flight newFlight = Flight.builder()
				.airlineCode(data.airlineCode())
				.flightNumber(data.flightNumber())
				.flightDate(data.flightDate())
				.build();
		
		return flightRepository.save(newFlight);
	}
	
	private String generateHash(FlightResponseData data) {
        return String.valueOf(Objects.hash(
        		data.flightDate(),
                data.status(), 
                data.gate(), 
                data.departureTime(), 
                data.arrivalTime()
        ));
    }

}
