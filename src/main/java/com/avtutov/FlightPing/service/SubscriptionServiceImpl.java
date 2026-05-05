package com.avtutov.FlightPing.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;

import com.avtutov.FlightPing.bot.FlightMessageFormatter;
import com.avtutov.FlightPing.bot.TelegramBotService;
import com.avtutov.FlightPing.dto.FlightRequestDto;
import com.avtutov.FlightPing.dto.external.AeroDataSubscriptionResponse;
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
import com.avtutov.FlightPing.service.util.TimeUtils;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {
	
	private final SubscriptionRepository subscriptionRepository;
	private final FlightService flightService;
	private final TelegramBotService telegramBotService;
	private final ExternalFlightApiService flightApi;
	private final FlightMapper mapper;
	private final AeroDataBalanceService balanceService;
	private final FlightMessageFormatter ui;
	private final TimeUtils timeUtils;
	
	private static final int MAX_SUBSCRIPTIONS = 3;

    @Override
    public void showSubscriptions(Long chatId) {
    	List<Subscription> subscriptions = subscriptionRepository.findAllByUser_ChatIdAndActiveTrue(chatId);
        telegramBotService.sendMessage(chatId, ui.formatSubscriptionList(subscriptions));
    }

    @Override
    @Transactional
    public void cancelSubscription(Long chatId, String indexPart) {
    	
    	List<Subscription> activeSubs = subscriptionRepository.findAllByUser_ChatIdAndActiveTrue(chatId);
    	
        if ("all".equalsIgnoreCase(indexPart)) {
        	
        	for(Subscription sub : activeSubs) {
        		processSingleCancellation(sub);
        	}
            telegramBotService.sendMessage(chatId, ui.ALL_SUBS_CANCELLED);
            return;
        }

        try {
            int index = Integer.parseInt(indexPart) - 1;

            if (index >= 0 && index < activeSubs.size()) {
                Subscription subToCancel = activeSubs.get(index);
                
                processSingleCancellation(subToCancel);
                
                String arrivalCity = subToCancel.getFlight().getDepartureAirportCity();
                telegramBotService.sendMessage(chatId, ui.formatSubscriptionCancelled(arrivalCity));
            } else {
                telegramBotService.sendMessage(chatId, ui.WRONG_NUMBER_TO_CANCEL);
            }
        } catch (NumberFormatException e) {
            telegramBotService.sendMessage(chatId, ui.CANCEL_FORMAT);
        }
    }
    
    @Override
    public void subscribeById(User user, Long flightId) {
        flightService.findById(flightId).ifPresentOrElse(
            flight -> handleSingleFlight(user, flight),
            () -> telegramBotService.sendMessage(user.getChatId(), ui.FLIGHT_NOT_FOUND));
    }

    @Override
    public void processFlightRequest(User user, FlightRequestDto requestDto) {
        List<Flight> flights = flightService.findOrCreateFlights(requestDto);
        
        if (flights == null || flights.isEmpty()) {
            telegramBotService.sendMessage(user.getChatId(), ui.FLIGHT_NOT_FOUND);
            return;
        }
        
        if (flights.size() == 1) {
            handleSingleFlight(user, flights.get(0));
        } else {
            showSuggestedFlights(user.getChatId(), flights);
        }
    }
    
    @Override
    public void initiateCancellation(User user, String text) {
    	Long chatId = user.getChatId();
    	List<Subscription> activeSubs = subscriptionRepository.findAllByUserAndActiveTrue(user);
    	
    	if (activeSubs.isEmpty()) {
            telegramBotService.sendMessage(chatId, ui.NO_ACTIVE_SUBS);
            return;
        }
    	
    	String[] parts = text.split(" ");
        if (parts.length > 1) {
        	cancelSubscription(chatId, parts[1]);
        } else {
        	telegramBotService.sendMessage(chatId, ui.formatCancelSubscriptionGuide(activeSubs));
        }
    }

    @Transactional
    @Override
    public void processApiUpdate(AeroDataWebhookPayload payload) {
        String extId = payload.subscription().id();
        
        List<Subscription> candidates = subscriptionRepository.findAllByApiSubscriptionIdAndActiveTrue(extId);

        if (candidates.isEmpty()) {
            log.warn("Ghost subscription {} detected. Cleaning up...", extId);
            try {
                flightApi.unsubscribeFromFlightAlerts(extId).block(Duration.ofSeconds(10));
            } catch (Exception e) {
                log.error("Termination of ghost subscription failed: {}", e.getMessage());
            }
            return;
        }
        
        if (payload.flights() == null || payload.flights().isEmpty()) {
            return;
        }

        handleBalance(payload.balance());
        
        var flightData = payload.flights().get(0);
        
        Instant alertScheduledUtc = timeUtils.parseToInstant(flightData.departure().scheduledTime().utc());
        
        if (alertScheduledUtc == null) {
            log.error("Failed to parse UTC time from payload for subscription {}", extId);
            return;
        }

        for (Subscription sub : candidates) {
            Flight flight = sub.getFlight();
            
            Instant flightTime = flight.getDepartureScheduledTimeUtc().truncatedTo(ChronoUnit.MINUTES);
            Instant incomingTime = alertScheduledUtc.truncatedTo(ChronoUnit.MINUTES);
            
            if (flightTime.equals(incomingTime)) {
                
                updateFlightInfo(flight, flightData);
                
                String summary = flightData.notificationSummary();
                if (summary != null && !summary.isBlank()) {
                	
                	Long chatId = sub.getUser().getChatId();
                    telegramBotService.sendMessage(chatId,
                        ui.formatUpdatedFlightSummary(flight.getArrivalAirportCity(), summary, flight)
                    );
                }

                if (isFinalStatus(flightData.status())) {
                    sub.setActive(false);
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
            	LocalDateTime revised = timeUtils.parseToLocal(newData.departure().revisedTime().local());
            	if (revised != null) {
                    flight.setDepartureRevisedTimeLocal(revised);
                }
            }
        }
        
        if (newData.arrival() != null && newData.arrival().revisedTime() != null) {
        	LocalDateTime revised = timeUtils.parseToLocal(newData.arrival().revisedTime().local());
        	if (revised != null) {
        		flight.setArrivalRevisedTimeLocal(revised);
            }
        }
    }
    
    private void handleBalance(AeroDataWebhookPayload.BalanceInfo balanceInfo) {
        int remaining = balanceInfo.creditsRemaining();
        log.info("Logic: AeroData balance update: {} credits remaining.", remaining);

        balanceService.updateRedisFromCallback(remaining);

        if (remaining < 30) {
            log.warn("Low balance detected! Triggering auto-refill...");
            balanceService.refill(30)
            .timeout(Duration.ofSeconds(10))
            .subscribe(
                    res -> log.info("Logic: Auto-refill successful. New balance: {}", res.creditsRemaining()),
                    err -> log.error("❌ Auto-refill background error: {}", err.getMessage())
                );
        }
    }
    
	private void handleSingleFlight(User user, Flight flight) {
        
		Long chatId = user.getChatId();
        
        long activeCount = subscriptionRepository.countByUser_ChatIdAndActiveTrue(chatId);
        if(activeCount >= MAX_SUBSCRIPTIONS) {
        	telegramBotService.sendMessage(chatId, ui.formatLimitReached(MAX_SUBSCRIPTIONS));
                return;
        }
        
        if (subscriptionRepository.existsByUserAndFlightAndActiveTrue(user, flight)) {
            telegramBotService.sendMessage(chatId, ui.ALREADY_SUBSCRIBED);
            return;
        }
        manageSubscription(user, flight);
    }
	
	private void showSuggestedFlights(Long chatId, List<Flight> flights) {
		telegramBotService.sendMessage(chatId, ui.formatSuggestedFlights(flights));
	}

    private void sendSuccessMessage(Long chatId, Flight flight) {
    	telegramBotService.sendMessage(chatId, ui.formatSuccessSubscription(flight));
    }
    
    private void processSingleCancellation(Subscription sub) {
        sub.setActive(false);
        subscriptionRepository.save(sub);
        
        String extId = sub.getApiSubscriptionId();
        if (extId == null) return;
        
        boolean isUsedByOthers = subscriptionRepository.existsByApiSubscriptionIdAndActiveTrue(extId);
        
        if (!isUsedByOthers && extId != null) {
            log.info("Logic: No more observers for {}. Terminating API subscription...", extId);
            
            try {
            	flightApi.unsubscribeFromFlightAlerts(extId).block(Duration.ofSeconds(10));
            } catch (Exception e) {
            	log.error("❌ Logic: API termination call failed for {}", extId);
            }
        } else {
        	log.info("Logic: API ID {} is still shared by other users. Local skip.", extId);
        }
    }
    
    private void manageSubscription(User user, Flight flight) {
        Long chatId = user.getChatId();
        String flightNumber = flight.getFullFlightNumber();

        List<Subscription> existingSubs = subscriptionRepository.findAllByFlightAndActiveTrue(flight);
        
        if (!existingSubs.isEmpty()) {
            String reuseId = existingSubs.get(0).getApiSubscriptionId();
            log.info("Logic: ♻️ Reusing existing API ID: {} for flight: {}", reuseId, flightNumber);
            saveLocalSubscription(user, flight, reuseId);
            sendSuccessMessage(chatId, flight);
            return;
        }

        long currentBalance = balanceService.getVirtualBalance();
        
        Mono<AeroDataSubscriptionResponse> subscriptionFlow = Mono.defer(() -> {
            if (currentBalance < 30) {
                return balanceService.refill(30).then(flightApi.subscribeToFlightAlerts(flightNumber));
            } else {
                return flightApi.subscribeToFlightAlerts(flightNumber);
            }
        });

        try {
            AeroDataSubscriptionResponse apiResponse = subscriptionFlow.block(Duration.ofSeconds(10));

            if (apiResponse != null && apiResponse.id() != null) {
                saveLocalSubscription(user, flight, apiResponse.id());
                sendSuccessMessage(chatId, flight);
                log.info("Logic:  New API subscription created: {}", apiResponse.id());
            }
        } catch (Exception error) {
            log.error("❌ Subscription flow failed: {}", error.getMessage());
            
            String errorMsg = ui.SUB_CREATEING_ERROR;
            if (error.getMessage() != null && error.getMessage().contains("429")) {
                errorMsg = ui.API_LIMIT;
            }
            telegramBotService.sendMessage(chatId, errorMsg);
        }
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
    
}
