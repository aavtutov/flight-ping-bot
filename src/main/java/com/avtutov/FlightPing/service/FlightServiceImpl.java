package com.avtutov.FlightPing.service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.avtutov.FlightPing.dto.FlightRequestDto;
import com.avtutov.FlightPing.dto.external.AeroDataBoxResponse;
import com.avtutov.FlightPing.mapper.FlightMapper;
import com.avtutov.FlightPing.model.Flight;
import com.avtutov.FlightPing.model.FlightAlias;
import com.avtutov.FlightPing.repository.FlightAliasRepository;
import com.avtutov.FlightPing.repository.FlightRepository;
import com.avtutov.FlightPing.service.external.ExternalFlightApiService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class FlightServiceImpl implements FlightService {
	
	private final FlightRepository flightRepository;
	private final FlightAliasRepository aliasRepository;
	private final ExternalFlightApiService apiService;
	private final FlightMapper flightMapper;

	@Transactional
	@Override
	public List<Flight> findOrCreateFlights(FlightRequestDto requestDto) {
		
		String flightCode = requestDto.flight();
        LocalDate date = requestDto.date();
		
//		if known alias
		Optional<FlightAlias> existingAlias = aliasRepository
                .findByAliasCodeAndFlight_FlightDate(flightCode, date);
		
		if (existingAlias.isPresent()) {
			return List.of(existingAlias.get().getFlight());
		}
		
		List<AeroDataBoxResponse> apiFlights = apiService.getFlightInfo(flightCode, date);
		if(apiFlights == null || apiFlights.isEmpty()) {
            return Collections.emptyList();
		}
		
		String fullNumberFromApi = apiFlights.get(0).number();
		
		List<Flight> existingFlights =
				flightRepository.findAllByFullFlightNumberAndFlightDate(fullNumberFromApi, date);
		
//		if unknown alias but we have original flight in db
		if(!existingFlights.isEmpty()) {
			
			// alias can be tied to one flight only
			Flight flight = existingFlights.get(0);
			
			FlightAlias newAlias = FlightAlias.builder()
					.aliasCode(requestDto.flight())
					.flight(flight)
					.build();
			flight.getAliases().add(newAlias);
			
            return List.of(flight);
		}
		
//		in other cases just map and add generated aliases (and request code as alias)		
		List<Flight> newFlights = apiFlights.stream()
				.map(flightMapper::toEntity)
				.filter(flight -> flight.getDepartureScheduledTimeLocal().toLocalDate().equals(date))
				.toList();
		
		if (newFlights.isEmpty()) {
	        return Collections.emptyList();
	    }
		
		Set<String> generatedAliases = provideAliasCodes(apiFlights.get(0), requestDto);
		addAliasesToFlights(newFlights, generatedAliases);
			
		return flightRepository.saveAll(newFlights);
	}
	
	private Set<String> provideAliasCodes(AeroDataBoxResponse apiDto, FlightRequestDto requestDto) {
		
		String fullFlightNumber = apiDto.number();
		String iata = apiDto.airline() != null ? apiDto.airline().iata() : null;
		String icao = apiDto.airline() != null ? apiDto.airline().icao() : null;
		
		String flightNumberOnly = fullFlightNumber;
		if (iata != null) flightNumberOnly = flightNumberOnly.replace(iata, "");
		if (icao != null) flightNumberOnly = flightNumberOnly.replace(icao, "");
		flightNumberOnly = flightNumberOnly.trim().replaceAll("\\s+", "");
		
		Set<String> uniqueCodes = new HashSet<>();
		
		uniqueCodes.add(requestDto.flight());
		if (iata != null) uniqueCodes.add(iata + flightNumberOnly);
		if (icao != null) uniqueCodes.add(icao + flightNumberOnly);
		
		return uniqueCodes;
	}
	
	private static void addAliasesToFlights(List<Flight> flights, Set<String> aliasCodes) {
		for (Flight flight : flights) {
			aliasCodes.forEach(flight::addAlias);
		}
	}

	@Override
	public Optional<Flight> findById(Long id) {
		return flightRepository.findById(id);
	}

}
