package com.avtutov.FlightPing.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.avtutov.FlightPing.dto.FlightParsedData;
import com.avtutov.FlightPing.model.Flight;
import com.avtutov.FlightPing.model.Subscription;
import com.avtutov.FlightPing.model.User;
import com.avtutov.FlightPing.repository.SubscriptionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
	
	private final SubscriptionRepository subscriptionRepository;
	private final UserService userService;
	private final FlightService flightService;
	private final ParserService parserService;
	
	@Transactional
	@Override
	public void subscribe(Message message) {
		
		String messageText = message.getText();
		Long telegramId = message.getFrom().getId();
		
		Optional<FlightParsedData> parsedDataOpt = parserService.parse(messageText);
		
		if(parsedDataOpt.isEmpty()) {
			log.warn("Message from {} could not be parsed: {}", telegramId, messageText);
			return;
		}
		
		FlightParsedData data = parsedDataOpt.get();
		
		User user = userService.getOrCreateUser(message);
		
		Flight flight = flightService.getOrCreateFlight(data);
		
		if (subscriptionRepository.existsByUserAndFlightAndActiveTrue(user, flight)) {
            log.info("User {} already subscribed to {}", telegramId, flight.getAirlineCode() + flight.getFlightNumber());
            return;
        }
		
		Subscription newSubscription = Subscription.builder()
				.user(user)
				.flight(flight)
				.active(true)
				.build();
		subscriptionRepository.save(newSubscription);
				
		log.info("Successfull subscription: User {} to flight {}", 
                user.getFirstName(), flight.getAirlineCode() + flight.getFlightNumber());
		
	}
	
	

}
