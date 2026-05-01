package com.avtutov.FlightPing.bot;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import reactor.core.publisher.Mono;

/**
 * Service for sending asynchronous notifications to users via Telegram.
 */
public interface TelegramBotService {

	void sendMessage(Long chatId, String message);
	
	void sendMessage(Long chatId, String message, InlineKeyboardMarkup keyboard);
	
	Mono<String> setWebhook();
}
