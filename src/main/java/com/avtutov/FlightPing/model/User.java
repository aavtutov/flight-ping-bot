package com.avtutov.FlightPing.model;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
	
	@Id
    private Long telegramId;

    private Long chatId;
    private String username;
    private String firstName;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private Instant registeredAt;

}
