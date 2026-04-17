package com.avtutov.FlightPing.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.telegram.telegrambots.meta.api.objects.Update;

import com.avtutov.FlightPing.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final SubscriptionService subscriptionService;

    @PostMapping("/callback/${telegram.bot.token}")
    public void onUpdateReceived(@RequestBody Update update) {
        
    	if (update.hasMessage() && update.getMessage().hasText()) {
            subscriptionService.subscribe(update.getMessage());
        }
    	
    }
}
