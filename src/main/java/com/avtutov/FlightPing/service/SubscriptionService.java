package com.avtutov.FlightPing.service;

import org.telegram.telegrambots.meta.api.objects.message.Message;

public interface SubscriptionService {

	void subscribe(Message message);
	
	void showSubscriptions(Long chatId);
	
	void cancelSubscription(Long chatId, String indexPart);
	
}
