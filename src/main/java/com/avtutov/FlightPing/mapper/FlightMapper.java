package com.avtutov.FlightPing.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ValueMapping;

import com.avtutov.FlightPing.dto.external.AeroDataFlightResponse;
import com.avtutov.FlightPing.dto.external.FlightStatus;
import com.avtutov.FlightPing.model.Flight;
import com.avtutov.FlightPing.model.InternalFlightStatus;
import com.avtutov.FlightPing.service.util.TimeUtils;

@Mapper(componentModel = "spring", uses = TimeUtils.class)
public interface FlightMapper {

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
	Flight toEntity(AeroDataFlightResponse response);

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

}
