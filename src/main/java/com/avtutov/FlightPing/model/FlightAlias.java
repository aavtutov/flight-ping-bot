package com.avtutov.FlightPing.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "flight_aliases",
		uniqueConstraints = {
				@UniqueConstraint(columnNames = { "flight_id", "aliasCode" })},
		indexes = {
				@Index(name = "idx_alias_code", columnList = "aliasCode") }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FlightAlias {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "flight_id", nullable = false)
	Flight flight;
	
	@Column(nullable = false)
	String aliasCode;

}
