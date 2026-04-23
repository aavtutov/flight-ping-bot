package com.avtutov.FlightPing.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.avtutov.FlightPing.dto.FlightRequestDto;
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
	
	private static final int MAX_SUBSCRIPTIONS = 5;
	
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
	
	@Transactional
    @Override
    public void subscribe(Message message) {
        String messageText = message.getText();
        Long chatId = message.getChatId();

        if ("/start".equalsIgnoreCase(messageText)) {
            sendWelcomeMessage(message);
            return;
        }
        
        if ("/list".equalsIgnoreCase(messageText)) {
            showSubscriptions(chatId);
            return;
        }
        
        if (messageText.startsWith("/subscribe_")) {
        	
        	String flightIdStr = messageText.replaceAll("/subscribe_", "");
        	
        	try {
        		Long flightId = Long.valueOf(flightIdStr);
        		Optional<Flight> flightOpt = flightService.findById(flightId);
        		
        		if(flightOpt.isEmpty()) {
        			telegramBotService.sendMessage(chatId, "You're trying to subscribe to non-existent flight.");
        		}
        		
        		Flight flight = flightOpt.get();
        		handleSingleFlight(message, flight);
        		
        	} catch(Exception exception) {
        		telegramBotService.sendMessage(chatId, "I didn't recognize your /subscribe_... command.");
        		return;
        	}
        }
        
        if (messageText.startsWith("/cancel")) {
        	
        	List<Subscription> subscriptions = subscriptionRepository.findAllByUser_ChatIdAndActiveTrue(chatId);

            if (subscriptions.isEmpty()) {
                telegramBotService.sendMessage(chatId, "📭 <b>You don't have any active subscriptions.</b>\n" +
                        "\nTo subscribe, send me a flight number and date, in the following format:\n\n"
                        + "<code>kl1521, 22.04</code>");
                return;
            }
        	
            String[] parts = messageText.split(" ");
            if (parts.length > 1) {
                cancelSubscription(chatId, parts[1]);
            } else {
                telegramBotService.sendMessage(chatId, "⚠️ Specify number of subscription or cancell all of them at once.\n\n"
                		+ "<code>/cancel 1</code>\n"
                		+ "or\n"
                		+ "<code>/cancel all</code>\n\n"
                		+ "Use /list to see your active subscriptions.");
            }
            return;
        }
        
        long activeCount = subscriptionRepository.countByUser_ChatIdAndActiveTrue(chatId);
        if(activeCount >= MAX_SUBSCRIPTIONS) {
        	telegramBotService.sendMessage(chatId, 
                    "🚫 <b>Limit reached!</b>\n" +
                    "You can only have " + MAX_SUBSCRIPTIONS + " active subscriptions at a time.\n" +
                    "Use /list to manage them.");
                return;
        }

        Optional<FlightRequestDto> parsedDataOpt = parserService.processUserInput(messageText);
        if (parsedDataOpt.isEmpty()) {
            telegramBotService.sendMessage(chatId, "🚫 I don't recognize this format. Try: <code>LH155, 29.03</code>");
            return;
        }
        
        FlightRequestDto requestDto = parsedDataOpt.get();
        List<Flight> flights = flightService.findOrCreateFlights(requestDto);
        
        if(flights == null || flights.isEmpty()) {
        	telegramBotService.sendMessage(chatId, "🚫 Flight not found in system for this date.");
        	return;
        }
        
        if(flights.size() == 1) {
        	handleSingleFlight(message, flights.get(0));
        	return;
        }
        
        showSuggestedFlights(chatId, flights);
    }
	
	private void handleSingleFlight(Message message, Flight flight) {
        Long chatId = message.getChatId();
        User user = userService.getOrCreateUser(message);
        
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

        sendSuccessMessage(chatId, flight);
    }
	
	private void showSuggestedFlights(Long chatId, List<Flight> flights) {
		StringBuilder message = new StringBuilder("We've found a few flights on selected date:\n\n");
		
		for(int i = 0; i < flights.size(); i++) {
			Flight flight = flights.get(i);
			message.append(String.format("%d. ✈️ %s - %s %s /subscribe_%d \n", 
					i + 1,
					flight.getDepartureAirportIata(),
					flight.getArrivalAirportIata(),
					flight.getDepartureScheduledTimeLocal().format(DateTimeFormatter.ofPattern("HH:mm")),
					flight.getId()
					));
		}
		message.append("Tap <code>/subscribe_...</code> to select desired one.");
		telegramBotService.sendMessage(chatId, message.toString());
	}
	
    private void sendWelcomeMessage(Message message) {
        User user = userService.getOrCreateUser(message);
        String welcome = String.format("👋 <b>Hello, %s!</b>\n\nTo subscribe, send flight and date.\nExample: <code>LH155, 29.03</code>", user.getFirstName());
        telegramBotService.sendMessage(message.getChatId(), welcome);
    }

    private void sendSuccessMessage(Long chatId, Flight flight) {
    	
    	LocalDateTime departureScheduledTime = flight.getDepartureScheduledTimeLocal();
    	LocalDateTime departureRevisedTime = flight.getDepartureRevisedTimeLocal();
    	
        String departureDate = formatDate(departureScheduledTime);
    	String depSchedTime = formatTime(departureScheduledTime);
    	String depRevisTime = formatTime(departureRevisedTime);
    	String arrivalTime = formatTime(flight.getArrivalScheduledTimeLocal());
    	
    	String departureTime = depSchedTime;

		if (departureRevisedTime != null && !departureRevisedTime.equals(departureScheduledTime)) {
			long diff = Math.abs(Duration.between(departureScheduledTime, departureRevisedTime).toMinutes());
			if(diff > 5) {
				departureTime = String.format("<s>%s</s> <b>%s</b>", depSchedTime, depRevisTime);
			}
		}

        String response = String.format(
            "🎉 <b>Subscribed!</b>\n\n" +
            "<code>%s - %s (%s)</code>\n\n" +
            "---------------\n\n" +
            "<code>Status:</code> <b>%s</b>\n\n" + 
            "<code>Time:</code> <b>%s</b> \n" +
            "<code>Gate:</code> <b>%s</b>\n" +
            "<code>Terminal:</code> <b>%s</b>\n" +
            "<code>Check-in Desks:</code> <b>%s</b>\n\n" +
            "<code>Aircraft:</code> <b>%s</b>\n" +
            "<code>Arriving time:</code> <b>%s</b>\n\n" +
            "---------------\n\n" +
            "We’ll keep you posted with any changes.",
            flight.getDepartureAirportCity() != null ? flight.getDepartureAirportCity().toUpperCase() : "N/A",
            flight.getArrivalAirportCity() != null ? flight.getArrivalAirportCity().toUpperCase() : "N/A",
            departureDate, 
            flight.getStatus() != null ? flight.getStatus().getDescription() : "Unknown",
            departureTime,
            flight.getGate() != null ? flight.getGate() : "TBD",
            flight.getTerminal() != null ? flight.getTerminal() : "TBD",
            flight.getCheckInDesk() != null ? flight.getCheckInDesk() : "TBD",
            flight.getAircraftModel() != null ? flight.getAircraftModel() : "TBD",
            arrivalTime
        );

        telegramBotService.sendMessage(chatId, response);
    }

    @Override
    public void showSubscriptions(Long chatId) {
        List<Subscription> subscriptions = subscriptionRepository.findAllByUser_ChatIdAndActiveTrue(chatId);

        if (subscriptions.isEmpty()) {
            telegramBotService.sendMessage(chatId, "📭 <b>You don't have any active subscriptions.</b>\n" +
                    "\nTo subscribe, send me a flight number and date, in the following format:\n\n"
                    + "<code>kl1521, 22.04</code>");
            return;
        }

        StringBuilder message = new StringBuilder("<b>Your Active Subscriptions:</b>\n\n");
        
        for (int i = 0; i < subscriptions.size(); i++) {
            Flight flight = subscriptions.get(i).getFlight();
            
            String date = formatDate(flight.getDepartureScheduledTimeLocal());
            String time = formatTime(flight.getDepartureScheduledTimeLocal());
        	
            message.append(String.format("%d. ✈️ <b>%s ➔ %s</b> (%s %s) \n\n",
                    i + 1,
                    flight.getDepartureAirportIata(),
                    flight.getArrivalAirportIata(),
                    date,
                    time
            ));
        }
        message.append("Use /cancel to cancel subscription.");

        telegramBotService.sendMessage(chatId, message.toString());
    }

    @Override
    @Transactional
    public void cancelSubscription(Long chatId, String indexPart) {
    	
        if ("all".equalsIgnoreCase(indexPart)) {
            subscriptionRepository.deactivateAllByChatId(chatId);
            telegramBotService.sendMessage(chatId, "✅ All subscriptions were cancelled.");
            return;
        }

        try {
            int index = Integer.parseInt(indexPart) - 1;
            List<Subscription> activeSubs = subscriptionRepository.findAllByUser_ChatIdAndActiveTrue(chatId);

            if (index >= 0 && index < activeSubs.size()) {
                Subscription subToCancel = activeSubs.get(index);
                subToCancel.setActive(false);
                subscriptionRepository.save(subToCancel);
                
                telegramBotService.sendMessage(chatId,String.format(
                		"✅ Subscription to flight <b>%s</b> cancelled.\n"
                		+ "Use /list to see your active subscriptions.", 
                    subToCancel.getFlight().getFullFlightNumber()));
            } else {
                telegramBotService.sendMessage(chatId, "⚠️ Wrong number.\n"
                		+ "Use /list to see current list of active subscriptions.");
            }
        } catch (NumberFormatException e) {
            telegramBotService.sendMessage(chatId, "⚠️ Please, specify subscription number (example: /cancel 1 or /cancel all)");
        }
    }    
    
    private String formatDate(LocalDateTime ldt) {
    	if(ldt == null) return "TBD";
    	return ldt.format(DATE_FORMATTER);
    }
    
    private String formatTime(LocalDateTime ldt) {
    	if (ldt == null) return "TBD";
    	return ldt.format(TIME_FORMATTER);
    }
    
}
