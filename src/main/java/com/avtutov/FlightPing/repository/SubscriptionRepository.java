package com.avtutov.FlightPing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.avtutov.FlightPing.model.Flight;
import com.avtutov.FlightPing.model.Subscription;
import com.avtutov.FlightPing.model.User;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	boolean existsByUserAndFlightAndActiveTrue(User user, Flight flight);
	
	List<Subscription> findAllByUserAndActiveTrue(User user);
	
	List<Subscription> findAllByFlightAndActiveTrue(Flight flight);
	
}
