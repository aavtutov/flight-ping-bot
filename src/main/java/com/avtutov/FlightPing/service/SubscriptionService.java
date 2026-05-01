package com.avtutov.FlightPing.service;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.avtutov.FlightPing.dto.external.AeroDataWebhookPayload;

public interface SubscriptionService {

	void subscribe(Message message);
	
	void processApiUpdate(AeroDataWebhookPayload notification);
	
	void showSubscriptions(Long chatId);
	
	void cancelSubscription(Long chatId, String indexPart);
	
}
