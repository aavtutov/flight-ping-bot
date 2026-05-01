package com.avtutov.FlightPing.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.avtutov.FlightPing.dto.FlightRequestDto;
import com.avtutov.FlightPing.dto.external.AeroDataWebhookPayload;
import com.avtutov.FlightPing.dto.external.FlightStatus;
import com.avtutov.FlightPing.mapper.FlightMapper;
import com.avtutov.FlightPing.model.Flight;
import com.avtutov.FlightPing.model.InternalFlightStatus;
import com.avtutov.FlightPing.model.Subscription;
import com.avtutov.FlightPing.model.User;
import com.avtutov.FlightPing.repository.SubscriptionRepository;
import com.avtutov.FlightPing.service.external.AeroDataBalanceService;
import com.avtutov.FlightPing.service.external.ExternalFlightApiService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
	
	private final SubscriptionRepository subscriptionRepository;
	private final UserService userService;
	private final FlightService flightService;
	private final ParserService parserService;
	private final TelegramBotService telegramBotService;
	private final ExternalFlightApiService flightApi;
	private final FlightMapper mapper;
	private final AeroDataBalanceService balanceService;
	
	private static final int MAX_SUBSCRIPTIONS = 3;
	
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd MMM", Locale.ENGLISH);
	
	private static final DateTimeFormatter ISO_FORMATTER = new DateTimeFormatterBuilder()
		    .append(DateTimeFormatter.ISO_LOCAL_DATE)
		    .optionalStart()
		    .appendLiteral('T')
		    .optionalEnd()
		    .optionalStart()
		    .appendLiteral(' ')
		    .optionalEnd()
		    .append(DateTimeFormatter.ISO_LOCAL_TIME)
		    .optionalStart()
		    .appendOffsetId()
		    .toFormatter();
	
	private static final DateTimeFormatter FLEXIBLE_FORMATTER = new DateTimeFormatterBuilder()
		    .append(DateTimeFormatter.ISO_LOCAL_DATE)
		    .optionalStart()
		    .appendLiteral('T')
		    .optionalEnd()
		    .optionalStart()
		    .appendLiteral(' ')
		    .optionalEnd()
		    .append(DateTimeFormatter.ofPattern("HH:mm"))
		    .optionalStart()
		    .appendPattern(":ss")
		    .optionalEnd()
		    .optionalStart()
		    .appendOffsetId()
		    .optionalEnd()
		    .toFormatter();
	
	@Transactional
    @Override
    public void subscribe(Message message) {
		
		User user = userService.getOrCreateUser(message);
		Long chatId = user.getChatId();
		String messageText = message.getText();

        if ("/start".equalsIgnoreCase(messageText)) {
            sendWelcomeMessage(message);
            return;
        }
        
        if ("/list".equalsIgnoreCase(messageText)) {
            showSubscriptions(chatId);
            return;
        }
        
        if (messageText.startsWith("/subscribe_")) {
        	handleSubscribeCommand(user, messageText);
        	return;
        }
        
        if (messageText.startsWith("/cancel")) {
        	handleCancelCommand(user, messageText);
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
        	handleSingleFlight(user, flights.get(0));
        	return;
        }
        showSuggestedFlights(chatId, flights);
    }
	
	private void handleSingleFlight(User user, Flight flight) {
        
		Long chatId = user.getChatId();
        
        long activeCount = subscriptionRepository.countByUser_ChatIdAndActiveTrue(chatId);
        if(activeCount >= MAX_SUBSCRIPTIONS) {
        	telegramBotService.sendMessage(chatId, 
                    "🚫 <b>Limit reached!</b>\n" +
                    "You can only have " + MAX_SUBSCRIPTIONS + " active subscriptions at a time.\n" +
                    "Use /list to manage them.");
                return;
        }
        
        if (subscriptionRepository.existsByUserAndFlightAndActiveTrue(user, flight)) {
            telegramBotService.sendMessage(chatId, "⚠️ You are already subscribed to this flight.");
            return;
        }
        manageSubscription(user, flight);
    }
	
	private void showSuggestedFlights(Long chatId, List<Flight> flights) {
		StringBuilder message = new StringBuilder("We've found a few flights on selected date:\n\n");
		
		for(int i = 0; i < flights.size(); i++) {
			Flight flight = flights.get(i);
			message.append(String.format("%d. ✈️ <b>%s ➔ %s</b> %s (/subscribe_%d) \n\n", 
					i + 1,
					flight.getDepartureAirportIata(),
					flight.getArrivalAirportIata(),
					flight.getDepartureScheduledTimeLocal().format(DateTimeFormatter.ofPattern("HH:mm")),
					flight.getId()
					));
		}
		message.append("Tap <code>/subscribe_...</code> to select one.");
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
        	List<Subscription> activeSubs = subscriptionRepository.findAllByUser_ChatIdAndActiveTrue(chatId);
        	
        	for(Subscription sub : activeSubs) {
        		processSingleCancellation(sub);
        	}
            telegramBotService.sendMessage(chatId, "✅ All subscriptions were cancelled.");
            return;
        }

        try {
            int index = Integer.parseInt(indexPart) - 1;
            List<Subscription> activeSubs = subscriptionRepository.findAllByUser_ChatIdAndActiveTrue(chatId);

            if (index >= 0 && index < activeSubs.size()) {
                Subscription subToCancel = activeSubs.get(index);
                
                processSingleCancellation(subToCancel);
               
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
    
    private void processSingleCancellation(Subscription sub) {
        sub.setActive(false);
        subscriptionRepository.save(sub);
        
        String extId = sub.getApiSubscriptionId();
        boolean isUsedByOthers = subscriptionRepository.existsByApiSubscriptionIdAndActiveTrue(extId);
        
        if (!isUsedByOthers && extId != null) {
            log.info("No more active observers for subscription {}. Unsubscribing from API...", extId);
            flightApi.unsubscribeFromFlightAlerts(extId)
                .doOnSuccess(v -> log.info("Successfully unsubscribed from AeroData API"))
                .doOnError(e -> log.error("Failed to unsubscribe from API: {}", e.getMessage()))
                .subscribe();
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
    
    private void handleSubscribeCommand(User user, String messageText) {
    	
    	Long chatId = user.getChatId();
    	String flightIdStr = messageText.replaceAll("/subscribe_", "");
    	
    	try {
    		Long flightId = Long.valueOf(flightIdStr);
    		Optional<Flight> flightOpt = flightService.findById(flightId);
    		
    		if(flightOpt.isEmpty()) {
    			telegramBotService.sendMessage(chatId, "You're trying to subscribe to non-existent flight.");
    			return;
    		}
    		
    		Flight flight = flightOpt.get();
    		handleSingleFlight(user, flight);
    		
    	} catch(Exception exception) {
    		telegramBotService.sendMessage(chatId, "I didn't recognize your /subscribe_... command.");
    		return;
    	}
    	return;
    }
    
    private void handleCancelCommand(User user, String messageText) {
    	
    	Long chatId = user.getChatId();
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
    
    private void manageSubscription(User user, Flight flight) {
        Long chatId = user.getChatId();
        String flightNumber = flight.getFullFlightNumber();

        // 1. Сначала проверяем, нет ли уже активной подписки в API для этого рейса
        List<Subscription> existingSubs = subscriptionRepository.findAllByFlightAndActiveTrue(flight);
        
        if (!existingSubs.isEmpty()) {
            String reuseId = existingSubs.get(0).getApiSubscriptionId();
            log.info("♻️ Reusing existing API ID: {} for flight: {}", reuseId, flightNumber);
            saveLocalSubscription(user, flight, reuseId);
            sendSuccessMessage(chatId, flight);
            return; // Выходим, API дергать не нужно
        }

        // 2. Если подписки нет — идем в API через цепочку (Refill -> Subscribe)
        long currentBalance = balanceService.getVirtualBalance();
        Mono<Void> preparation = (currentBalance < 10) 
            ? balanceService.refill(60).then() 
            : Mono.empty();

        preparation
            .then(flightApi.subscribeToFlightAlerts(flightNumber))
            .subscribe(
                apiResponse -> {
                    // ВАЖНО: saveLocalSubscription вызовется внутри асинхронного потока
                    saveLocalSubscription(user, flight, apiResponse.id());
                    sendSuccessMessage(chatId, flight);
                    log.info("✅ New API subscription created: {}", apiResponse.id());
                },
                error -> {
                    log.error("🔴 Subscription flow failed: {}", error.getMessage());
                    String errorMsg = error.getMessage().contains("429") 
                        ? "⚠️ Лимит запросов API. Попробуйте чуть позже." 
                        : "❌ Ошибка при создании подписки.";
                    telegramBotService.sendMessage(chatId, errorMsg);
                }
            );
    }
    
    private void saveLocalSubscription(User user, Flight flight, String apiSubscriptionId) {
    	Subscription newSubscription = Subscription.builder()
                .user(user)
                .flight(flight)
                .apiSubscriptionId(apiSubscriptionId)
                .active(true)
                .build();
    	subscriptionRepository.save(newSubscription);
    }

    @Transactional
    @Override
    public void processApiUpdate(AeroDataWebhookPayload payload) {
        String extId = payload.subscription().id();
        
        List<Subscription> candidates = subscriptionRepository.findAllByApiSubscriptionIdAndActiveTrue(extId);

        if (candidates.isEmpty()) {
            log.warn("Ghost subscription {} detected. Cleaning up...", extId);
            flightApi.unsubscribeFromFlightAlerts(extId).subscribe();
            return;
        }
        
        if (payload.flights() == null || payload.flights().isEmpty()) {
            return;
        }

        handleBalance(payload.balance());
        
        var flightData = payload.flights().get(0);
        
        String rawUtcTime = flightData.departure().scheduledTime().utc();
        Instant alertScheduledUtc = ZonedDateTime.parse(rawUtcTime, ISO_FORMATTER).toInstant();

        for (Subscription sub : candidates) {
            Flight flight = sub.getFlight();
            
            Instant flightTime = flight.getDepartureScheduledTimeUtc().truncatedTo(ChronoUnit.MINUTES);
            Instant incomingTime = alertScheduledUtc.truncatedTo(ChronoUnit.MINUTES);
            
            if (flightTime.equals(incomingTime)) {
                
                updateFlightInfo(flight, flightData);
                
                String summary = flightData.notificationSummary();
                if (summary != null && !summary.isBlank()) {
                    String message = String.format("📣 <b>Flight to %s:</b>\n\n%s", 
                        flight.getDepartureAirportCity(), summary);
                    telegramBotService.sendMessage(sub.getUser().getChatId(), message);
                }

                if (isFinalStatus(flightData.status())) {
                    sub.setActive(false);
                    telegramBotService.sendMessage(sub.getUser().getChatId(), "🏁 Tracking finished.");
                }
            }
        }
    }

    private boolean isFinalStatus(FlightStatus status) {
        if (status == null) return false;
        return status == FlightStatus.Arrived || 
               status == FlightStatus.CanceledUncertain || 
               status == FlightStatus.Canceled;
    }
	
    private void updateFlightInfo(Flight flight, AeroDataWebhookPayload.FlightNotification newData) {
        
        InternalFlightStatus newStatus = mapper.toInternalStatus(newData.status());
        flight.setStatus(newStatus);
        
        if (newData.departure() != null) {
            flight.setTerminal(newData.departure().terminal());
            flight.setGate(newData.departure().gate());
            
            if (newData.departure().revisedTime() != null) {
            	parseLocalTime(newData.departure().revisedTime().local())
                .ifPresent(flight::setDepartureRevisedTimeLocal);
            }
        }
        
        if (newData.arrival() != null && newData.arrival().revisedTime() != null) {
        	parseLocalTime(newData.arrival().revisedTime().local())
            .ifPresent(flight::setArrivalRevisedTimeLocal);
        }
    }
    
    private Optional<LocalDateTime> parseLocalTime(String rawTime) {
        if (rawTime == null || rawTime.isBlank()) {
            return Optional.empty();
        }
        try {
            // FLEXIBLE_FORMATTER поймет и "2026-05-01 09:15Z", и "2026-05-01T09:15:00"
            TemporalAccessor temporal = FLEXIBLE_FORMATTER.parseBest(rawTime.trim(), 
                    OffsetDateTime::from, LocalDateTime::from, LocalDate::from);

            if (temporal instanceof OffsetDateTime odt) {
                return Optional.of(odt.toLocalDateTime());
            } else if (temporal instanceof LocalDateTime ldt) {
                return Optional.of(ldt);
            } else if (temporal instanceof LocalDate ld) {
                return Optional.of(ld.atStartOfDay());
            }
            return Optional.empty();
        } catch (Exception e) {
            log.error("Failed to parse local time: {}", rawTime, e);
            return Optional.empty();
        }
    }
    
    private void handleBalance(AeroDataWebhookPayload.BalanceInfo balanceInfo) {
        int remaining = balanceInfo.creditsRemaining();
        log.info("AeroData balance update: {} credits remaining.", remaining);

        balanceService.updateRedisFromCallback(remaining);

        if (remaining < 30) {
            log.warn("Low balance detected! Triggering auto-refill...");
            balanceService.refill(60)
            .subscribe(
                    res -> log.info("✅ Auto-refill successful. New balance: {}", res.creditsRemaining()),
                    err -> log.error("❌ Auto-refill background error: {}", err.getMessage())
                );
        }
    }
 
}
