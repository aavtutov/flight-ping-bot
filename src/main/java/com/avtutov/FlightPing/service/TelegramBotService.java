package com.avtutov.FlightPing.service;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Service for sending asynchronous notifications to users via Telegram.
 */
public interface TelegramBotService {

	/**
     * Sends a text message to the specified chat.
     * @param chatId  Target chat identifier.
     * @param message Text content.
     */
	void sendMessage(Long chatId, String message);
	
	void sendMessage(Long chatId, String message, InlineKeyboardMarkup keyboard);
}
