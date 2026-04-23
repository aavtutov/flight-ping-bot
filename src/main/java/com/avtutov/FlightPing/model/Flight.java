package com.avtutov.FlightPing.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "flights", uniqueConstraints = {
		@UniqueConstraint(name = "uc_flight_departure", columnNames = { "fullFlightNumber", "departureScheduledTimeUtc" }) }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Flight {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(nullable = false)
	private String fullFlightNumber;
	
	@Column(nullable = false)
	private LocalDate flightDate;
	
    @OneToMany(mappedBy = "flight", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FlightAlias> aliases = new ArrayList<>();
	
	@Enumerated(EnumType.STRING)
	private InternalFlightStatus status;
	
	private String gate;
	private String checkInDesk;
	private String terminal;
    
    private String departureAirportIata;
    private String departureAirportCity;
    
    private Instant departureScheduledTimeUtc;
    private LocalDateTime departureScheduledTimeLocal;
    
    private Instant departureRevisedTimeUtc;
    private LocalDateTime departureRevisedTimeLocal;
    
    private String arrivalAirportIata;
    private String arrivalAirportCity;
    
    private Instant arrivalScheduledTimeUtc;
    private LocalDateTime arrivalScheduledTimeLocal;
    
    private Instant arrivalRevisedTimeUtc;
    private LocalDateTime arrivalRevisedTimeLocal;

    private String aircraftReg;
    private String aircraftModel;
    
    private String airlineName;
    
    @UpdateTimestamp
    private Instant lastUpdated;
    
    public void addAlias(String aliasCode) {
        FlightAlias alias = FlightAlias.builder()
                .aliasCode(aliasCode)
                .flight(this)
                .build();
        this.aliases.add(alias);
    }
    
}
