package com.avtutov.FlightPing.service;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.avtutov.FlightPing.dto.FlightRequestDto;

@Service
public class ParserServiceImpl implements ParserService {

	@Override
	public Optional<FlightRequestDto> processUserInput(String messageText) {

		if (messageText == null || !messageText.contains(",")) {
			return Optional.empty();
		}

		String[] parts = messageText.split(",", 2);

		String flight = parts[0].toUpperCase().replaceAll("[^0-9A-Z]+", "");
		if (!isValidFlight(flight)) {
			return Optional.empty();
		}

		return parseDate(parts[1]).map(date -> new FlightRequestDto(flight, date));
	}

	private boolean isValidFlight(String flight) {
		return flight.length() >= 3 
				&& flight.matches(".*\\d.*");
	}

	private Optional<LocalDate> parseDate(String rawDate) {
		try {
			String cleaned = rawDate.replaceAll("[^0-9]+", " ").trim();
			String[] parts = cleaned.split("\\s+");

			if (parts.length < 2) {
				return Optional.empty();
			}

			int day = Integer.parseInt(parts[0]);
			int month = Integer.parseInt(parts[1]);
			int year = LocalDate.now().getYear();

			LocalDate date = LocalDate.of(year, month, day);

			if (date.isBefore(LocalDate.now())) {
				date = date.plusYears(1);
			}

			return Optional.of(date);

		} catch (Exception e) {
			return Optional.empty();
		}
	}

}
