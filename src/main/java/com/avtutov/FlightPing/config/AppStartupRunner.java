package com.avtutov.FlightPing.config;

import java.time.Duration;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.avtutov.FlightPing.service.TelegramBotService;
import com.avtutov.FlightPing.service.external.AeroDataBalanceService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppStartupRunner {

	private final TelegramBotService telegramBotService;
	private final AeroDataBalanceService aeroDataBalanceService;
	
	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		
		telegramBotService.setWebhook()
		.doOnSuccess(res -> log.info("Telegram webhook set successfully"))
		.subscribe();
		
		aeroDataBalanceService.getBalance()
	    .delaySubscription(Duration.ofSeconds(2))
	    .subscribe(
	        res -> {
	            long credits = (res != null) ? res.creditsRemaining() : 0;
	            log.info("✅ AeroData balance checked. Credits: {}", credits);
	        },
	        err -> log.warn("⚠️ Balance check skipped: {}", err.getMessage())
	    );
		
	}
	
}
