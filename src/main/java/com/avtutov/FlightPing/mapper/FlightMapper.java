package com.avtutov.FlightPing.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ValueMapping;

import com.avtutov.FlightPing.dto.external.AeroDataBoxResponse;
import com.avtutov.FlightPing.dto.external.FlightStatus;
import com.avtutov.FlightPing.model.Flight;
import com.avtutov.FlightPing.model.InternalFlightStatus;

@Mapper(componentModel = "spring")
public interface FlightMapper {

	DateTimeFormatter AERODATA_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmXXX");

	DateTimeFormatter UTC_FORMATTER = new DateTimeFormatterBuilder().append(DateTimeFormatter.ISO_LOCAL_DATE)
			.optionalStart().appendLiteral('T').optionalEnd().optionalStart().appendLiteral(' ').optionalEnd()
			.append(DateTimeFormatter.ISO_LOCAL_TIME).optionalStart().appendOffsetId().optionalEnd().toFormatter()
			.withZone(ZoneOffset.UTC);

	DateTimeFormatter FLEXIBLE_OFFSET_FORMATTER = new DateTimeFormatterBuilder()
			.append(DateTimeFormatter.ISO_LOCAL_DATE).optionalStart().appendLiteral('T').optionalEnd().optionalStart()
			.appendLiteral(' ').optionalEnd().appendValue(ChronoField.HOUR_OF_DAY, 2).appendLiteral(':')
			.appendValue(ChronoField.MINUTE_OF_HOUR, 2).optionalStart().appendLiteral(':')
			.appendValue(ChronoField.SECOND_OF_MINUTE, 2).optionalEnd().appendOffsetId().toFormatter();

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "fullFlightNumber", source = "number")
	@Mapping(target = "flightDate", source = "departure.scheduledTime.local", qualifiedByName = "toLocalDate")

	@Mapping(target = "aliases", ignore = true)

	@Mapping(target = "aircraftReg", source = "aircraft.reg")
	@Mapping(target = "aircraftModel", source = "aircraft.model")

	@Mapping(target = "airlineName", source = "airline.name")

	@Mapping(target = "lastUpdated", source = "lastUpdatedUtc", qualifiedByName = "toInstant")

	// DEPARTURE
	@Mapping(target = "gate", source = "departure.gate")
	@Mapping(target = "checkInDesk", source = "departure.checkInDesk")
	@Mapping(target = "terminal", source = "departure.terminal")

	@Mapping(target = "departureAirportIata", source = "departure.airport.iata")
	@Mapping(target = "departureAirportCity", source = "departure.airport.municipalityName")

	@Mapping(target = "departureScheduledTimeUtc", source = "departure.scheduledTime.utc", qualifiedByName = "toInstant")
	@Mapping(target = "departureScheduledTimeLocal", source = "departure.scheduledTime.local", qualifiedByName = "toLocalDateTime")

	@Mapping(target = "departureRevisedTimeUtc", source = "departure.revisedTime.utc", qualifiedByName = "toInstant")
	@Mapping(target = "departureRevisedTimeLocal", source = "departure.revisedTime.local", qualifiedByName = "toLocalDateTime")

	// ARRIVAL
	@Mapping(target = "arrivalAirportIata", source = "arrival.airport.iata")
	@Mapping(target = "arrivalAirportCity", source = "arrival.airport.municipalityName")

	@Mapping(target = "arrivalScheduledTimeUtc", source = "arrival.scheduledTime.utc", qualifiedByName = "toInstant")
	@Mapping(target = "arrivalScheduledTimeLocal", source = "arrival.scheduledTime.local", qualifiedByName = "toLocalDateTime")

	@Mapping(target = "arrivalRevisedTimeUtc", source = "arrival.revisedTime.utc", qualifiedByName = "toInstant")
	@Mapping(target = "arrivalRevisedTimeLocal", source = "arrival.revisedTime.local", qualifiedByName = "toLocalDateTime")
	Flight toEntity(AeroDataBoxResponse response);

	@ValueMapping(source = "Expected", target = "EXPECTED")
	@ValueMapping(source = "EnRoute", target = "ENROUTE")
	@ValueMapping(source = "CheckIn", target = "CHECK_IN")
	@ValueMapping(source = "Boarding", target = "BOARDING")
	@ValueMapping(source = "GateClosed", target = "GATECLOSED")
	@ValueMapping(source = "Departed", target = "DEPARTED")
	@ValueMapping(source = "Delayed", target = "DELAYED")
	@ValueMapping(source = "Approaching", target = "APPROACHING")
	@ValueMapping(source = "Arrived", target = "ARRIVED")
	@ValueMapping(source = "Canceled", target = "CANCELLED")
	@ValueMapping(source = "Diverted", target = "DIVERTED")
	@ValueMapping(source = "CanceledUncertain", target = "CANCELLED_UNCERTAIN")
	@ValueMapping(source = "Unknown", target = "UNKNOWN")
	InternalFlightStatus toInternalStatus(FlightStatus apiStatus);

	@Named("toLocalDate")
	default LocalDate toLocalDate(String localStr) {
		if (localStr == null || localStr.isBlank())
			return null;
		try {
			return LocalDate.parse(localStr.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (Exception e) {
			return null;
		}
	}

	@Named("toLocalDateTime")
	default LocalDateTime toLocalDateTime(String localStr) {
		if (localStr == null || localStr.isBlank())
			return null;
		try {
			return OffsetDateTime.parse(localStr.trim(), FLEXIBLE_OFFSET_FORMATTER).toLocalDateTime();
		} catch (DateTimeParseException e) {
			return null;
		}
	}

	@Named("toInstant")
	default Instant toInstant(String utcStr) {
		if (utcStr == null || utcStr.isBlank())
			return null;
		try {
			return Instant.from(UTC_FORMATTER.parse(utcStr.trim()));
		} catch (Exception e) {
			return null;
		}
	}

}
