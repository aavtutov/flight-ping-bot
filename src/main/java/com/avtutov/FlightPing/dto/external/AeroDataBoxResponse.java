package com.avtutov.FlightPing.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AeroDataBoxResponse(
		
		FlightPoint departure,
		FlightPoint arrival,
		String number,
		FlightStatus status,
		CodeshareStatus codeshareStatus,
		Aircraft aircraft,
		Airline airline,
		String lastUpdatedUtc) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record FlightPoint(
			Airport airport,
			TimeInfo scheduledTime,
			TimeInfo revisedTime,
			String terminal,
			String checkInDesk,
			String gate) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Airport(
			String icao,
			String iata,
			String name,
			String municipalityName) {}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Airline(
			String name,
			String iata,
			String icao) {}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Aircraft(
			String reg,
			String model) {}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record TimeInfo(String local, String utc) {}
}
