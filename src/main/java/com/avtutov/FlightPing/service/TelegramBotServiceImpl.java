package com.avtutov.FlightPing.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class TelegramBotServiceImpl implements TelegramBotService {

	private final WebClient webClient;
	private final String botToken;

	public TelegramBotServiceImpl(
			WebClient.Builder webClientBuilder,
			@Value("${telegram.bot.token}") String botToken) {
		this.webClient = webClientBuilder.baseUrl("https://api.telegram.org").build();
		this.botToken = botToken;
	}
	
	@Override
    public void sendMessage(Long chatId, String message) {
        sendMessage(chatId, message, null);
    }

	@Override
	public void sendMessage(Long chatId, String message, InlineKeyboardMarkup keyboard) {
		String urlPath = String.format("/bot%s/sendMessage", botToken);
		
		Map<String, Object> body = new HashMap<>();
		body.put("chat_id", chatId);
		body.put("text", message);
		body.put("parse_mode", "HTML");
		if (keyboard != null) {
	        body.put("reply_markup", keyboard);
	    }

		webClient.post()
				.uri(urlPath)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(String.class)
				.timeout(Duration.ofSeconds(5))
				.doOnError(error -> {log.error("Telegram API error [chatId: {}]: {}", chatId, error.getMessage());})
				.onErrorResume(e -> Mono.empty())
				.subscribe();
	}
}
