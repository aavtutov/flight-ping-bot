package com.avtutov.FlightPing.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

import com.avtutov.FlightPing.service.SubscriptionService;
import com.avtutov.FlightPing.service.util.HashService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
@RequiredArgsConstructor
public class WebhookController {

    private final SubscriptionService subscriptionService;
    private final HashService hashService;
    
    @PostMapping("/callback/telegram-update/{hash}")
    public ResponseEntity<Void> onTelegramUpdateReceived(
    		
    		@PathVariable String hash,
    		@RequestBody Update update) {
    	
    	if(!hashService.isTelegramHashValid(hash)) {
    		return ResponseEntity.notFound().build();
    	}
        
    	if (update.hasMessage() && update.getMessage().hasText()) {
            subscriptionService.subscribe(update.getMessage());
        }
    	
    	return ResponseEntity.ok().build();
    }
    
    @PostMapping("/callback/flight-update/{hash}")
    public ResponseEntity<Void> onFlightUpdateReceived(
    		
    		@PathVariable String hash,
    		@RequestBody Update update) {
    	
    	if(!hashService.isAerodataHashValid(hash)) {
    		return ResponseEntity.notFound().build();
    	}
    	
    	// process flight updates
    	
    	return ResponseEntity.ok().build();
    }
    
    

    
}
