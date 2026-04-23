package com.avtutov.FlightPing.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.avtutov.FlightPing.model.FlightAlias;

public interface FlightAliasRepository extends JpaRepository<FlightAlias, Long> {
	
	Optional<FlightAlias> findByAliasCodeAndFlight_FlightDate(String aliasCode, LocalDate date);

}
