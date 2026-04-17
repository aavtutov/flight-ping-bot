package com.avtutov.FlightPing.service;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
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
	private final TelegramBotService telegramBotService;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);
	
	@Transactional
	@Override
	public void subscribe(Message message) {
		
		String messageText = message.getText();
		Long chatId = message.getChatId();
		
		if ("/start".equalsIgnoreCase(messageText)) {
            User user = userService.getOrCreateUser(message);
            String welcome = String.format(
                "👋 <b>Hello, %s!</b>\n\n"
                + "To subscribe to flight notifications, please send me the flight number and date.\n\n"
                + "Example:\n<code>LH155 29.03</code>",
                user.getFirstName()
            );
            telegramBotService.sendMessage(chatId, welcome);
            return;
        }
		
		Optional<FlightParsedData> parsedDataOpt = parserService.parse(messageText);
		
		if(parsedDataOpt.isEmpty()) {
			telegramBotService.sendMessage(chatId, "❌ I don't recognize this format. Try again:\n\n"
					+ "Example:\n<code>LH155 29.03</code>");
			return;
		}
		
		FlightParsedData data = parsedDataOpt.get();
		
		User user = userService.getOrCreateUser(message);
		
		Flight flight = flightService.getOrCreateFlight(data);
		
		if (subscriptionRepository.existsByUserAndFlightAndActiveTrue(user, flight)) {
			telegramBotService.sendMessage(chatId, "⚠️ You are already subscribed to this flight.");
            return;
        }
		
		Subscription newSubscription = Subscription.builder()
				.user(user)
				.flight(flight)
				.active(true)
				.build();
		subscriptionRepository.save(newSubscription);
		
		String formattedDate = flight.getFlightDate().format(DATE_FORMATTER);
		String successMsg = String.format("🎉 Success!\n\n"
				+ "I am tracking your flight:\n\n"
				+ "<b>%s%s on %s</b>\n\n"
				+ "If any changes occur you'll receive an instant notification!", 
                flight.getAirlineCode(), flight.getFlightNumber(), formattedDate);
		telegramBotService.sendMessage(chatId, successMsg);
				
	}

}
