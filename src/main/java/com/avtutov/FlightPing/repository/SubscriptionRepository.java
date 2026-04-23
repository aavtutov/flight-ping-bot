package com.avtutov.FlightPing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.avtutov.FlightPing.model.Flight;
import com.avtutov.FlightPing.model.Subscription;
import com.avtutov.FlightPing.model.User;

import jakarta.transaction.Transactional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

	boolean existsByUserAndFlightAndActiveTrue(User user, Flight flight);
	
	List<Subscription> findAllByUserAndActiveTrue(User user);
	
	List<Subscription> findAllByFlightAndActiveTrue(Flight flight);
	
	long countByUser_ChatIdAndActiveTrue(Long chatId);
	
	List<Subscription> findAllByUser_ChatIdAndActiveTrue(Long chatId);
	
	@Modifying
	@Transactional
	@Query("UPDATE Subscription s SET s.active = false WHERE s.user.chatId = :chatId AND s.active = true")
	void deactivateAllByChatId(Long chatId);
}
