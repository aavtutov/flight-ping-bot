package com.avtutov.FlightPing.service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import com.avtutov.FlightPing.service.util.HashService;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@Slf4j
public class TelegramBotServiceImpl implements TelegramBotService {

	private final WebClient webClient;
	private final HashService hashService;
	
	private final String botToken;
	private final String appBaseUrl;

	public TelegramBotServiceImpl(
			
			WebClient.Builder webClientBuilder,
			HashService hashService,
			
			@Value("${telegram.bot.token}") String botToken,
			@Value("${app.base.url}") String appBaseUrl) {
		
		this.webClient = webClientBuilder.baseUrl("https://api.telegram.org").build();
		this.hashService = hashService;
		this.botToken = botToken;
		this.appBaseUrl = appBaseUrl;
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

		if (message == null || message.isBlank()) {
		    log.warn("Attempted to send empty message to chatId: {}", chatId);
		    return;
		}
		
		webClient.post()
			.uri(urlPath)
			.bodyValue(body)
			.retrieve()
			.bodyToMono(String.class)
			.timeout(Duration.ofSeconds(5))
			.retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
			.doOnError(error -> {log.error("Telegram API error [chatId: {}]: {}", chatId, error.getMessage());})
			.onErrorResume(e -> Mono.empty())
			.subscribe();
	}
	
	@EventListener(ApplicationReadyEvent.class)
	public void setWebhook() {
		
		String hash = hashService.getTelegramHashToken();
		String webhookUrl = String.format("%s/callback/telegram-update/%s", appBaseUrl, hash);
		
		String telegramUrl = String.format("/bot%s/setWebhook", botToken);
		
		log.info("Attempting to register Telegram webhook: {}", webhookUrl);
		
		webClient.post()
			.uri(uriBuilder -> uriBuilder
				.path(telegramUrl)
				.queryParam("url", webhookUrl)
				.build())
			.retrieve()
			.onStatus(status -> !status.is2xxSuccessful(),
				response -> response.bodyToMono(String.class)
				.map(errorBody -> new RuntimeException("Telegram API Error: " + errorBody)))
			.bodyToMono(String.class)
			.timeout(Duration.ofSeconds(10))
			.retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
	            .filter(throwable -> !(throwable instanceof RuntimeException)))
			.onErrorResume(e -> {
				log.error("Final attempt failed. Webhook NOT registered: {}", e.getMessage());
				return Mono.empty(); 
			})
			.subscribe(result -> log.info("Telegram webhook registered successfully: {}", result),
				error -> {});
		
	}
	
}
