package com.avtutov.FlightPing.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.avtutov.FlightPing.model.Flight;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {
	
	List<Flight> findAllByFullFlightNumberAndFlightDate(String fullFlightNumber, LocalDate flightDate);
	
}
