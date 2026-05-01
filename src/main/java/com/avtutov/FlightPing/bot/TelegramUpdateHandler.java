package com.avtutov.FlightPing.bot;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.avtutov.FlightPing.model.User;
import com.avtutov.FlightPing.service.ParserService;
import com.avtutov.FlightPing.service.SubscriptionService;
import com.avtutov.FlightPing.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramUpdateHandler {

    private final SubscriptionService subscriptionService;
    private final UserService userService;
    private final ParserService parserService;
    private final TelegramBotService telegramBotService;
    private final FlightMessageFormatter ui;

    public void handleUpdate(Update update) {
    	
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        Message message = update.getMessage();
        String text = message.getText();
        User user = userService.getOrCreateUser(message);
        Long chatId = user.getChatId();

        if (text.equalsIgnoreCase("/start")) {
            telegramBotService.sendMessage(chatId, ui.formatWelcomeMessage(user.getFirstName()));
            return;
        }

        if (text.equalsIgnoreCase("/list")) {
            subscriptionService.showSubscriptions(chatId);
            return;
        }

        if (text.startsWith("/subscribe_")) {
            handleSubscribeCommand(user, text);
            return;
        }

        if (text.startsWith("/cancel")) {
        	subscriptionService.initiateCancellation(user, text);
            return;
        }

        handleTextMessage(user, text);
    }

    private void handleSubscribeCommand(User user, String text) {
        try {
            Long flightId = Long.valueOf(text.replace("/subscribe_", ""));
            subscriptionService.subscribeById(user, flightId);
        } catch (NumberFormatException e) {
            telegramBotService.sendMessage(user.getChatId(), ui.INVALID_FLIGHT_SELECTION);
        }
    }

    private void handleTextMessage(User user, String text) {
        parserService.processUserInput(text).ifPresentOrElse(
            requestDto -> subscriptionService.processFlightRequest(user, requestDto),
            () -> telegramBotService.sendMessage(user.getChatId(), ui.WRONG_FLIGHT_DATA)
        );
    }
}