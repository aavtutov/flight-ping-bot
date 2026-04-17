package com.avtutov.FlightPing.model;

import java.time.Instant;
import java.time.LocalDate;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "flights", uniqueConstraints = {
		@UniqueConstraint(columnNames = { "airlineCode", "flightNumber", "flightDate" }) })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Flight {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String airlineCode;
	
	@Column(nullable = false)
	private String flightNumber;
	
	@Column(nullable = false)
	private LocalDate flightDate;
	
	private String status;
	private String gate;
    
	private String departureCity;
    private String arrivalCity;
    
    private String departureAirportCode;
    private String arrivalAirportCode;
    
    private Instant departureTime;
    private Instant arrivalTime;
    
    private String dataHash;
    
    @UpdateTimestamp
    private Instant lastUpdated;
	
}
