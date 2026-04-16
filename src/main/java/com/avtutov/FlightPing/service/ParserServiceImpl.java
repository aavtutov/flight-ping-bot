package com.avtutov.FlightPing.service;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.avtutov.FlightPing.dto.FlightParsedData;

@Service
public class ParserServiceImpl implements ParserService {

	@Override
	public FlightParsedData parse(String message) {

		if (message == null || message.isBlank()) {
			throw new IllegalArgumentException("Message is empty or blank");
		}

		String cleanedMessage = message.toUpperCase().replaceAll("[^A-Z0-9]+", " ").trim();

		Pattern pattern = Pattern.compile("^([A-Z0-9]{2})\\s*([0-9A-Z]+)\\s+([0-9]{1,2})\\s+([0-9]{1,2})$");
		Matcher matcher = pattern.matcher(cleanedMessage);

		if (matcher.find()) {

			String airlineCode = matcher.group(1);
			String flightNumber = normalizeFlightNumber(matcher.group(2));
			LocalDate date = normalizeDate(matcher.group(3), matcher.group(4));

			return new FlightParsedData(airlineCode, flightNumber, date);

		} else {
			throw new IllegalArgumentException("Incorrect message format");
		}
	}

	private String normalizeFlightNumber(String flight) {
		return flight.replaceFirst("^0+(?!$)", "");
	}

	private LocalDate normalizeDate(String dayStr, String monthStr) {

		try {
			int day = Integer.parseInt(dayStr);
			int month = Integer.parseInt(monthStr);
			int year = LocalDate.now().getYear();

			LocalDate date = LocalDate.of(year, month, day);

			// if date in past
			if (date.isBefore(LocalDate.now())) {
				date = date.plusYears(1);
			}

			return date;
		} catch (Exception e) {
			throw new IllegalArgumentException("Incorrect date format");
		}
	}

}
