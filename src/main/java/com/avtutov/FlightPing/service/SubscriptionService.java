package com.avtutov.FlightPing.service;

import com.avtutov.FlightPing.dto.FlightRequestDto;
import com.avtutov.FlightPing.dto.external.AeroDataWebhookPayload;
import com.avtutov.FlightPing.model.User;

public interface SubscriptionService {

	void processApiUpdate(AeroDataWebhookPayload notification);
	
	void showSubscriptions(Long chatId);
	
	void cancelSubscription(Long chatId, String indexPart);
	
	void subscribeById(User user, Long flightId);
	
	void processFlightRequest(User user, FlightRequestDto requestDto);
	
	void initiateCancellation(User user, String text);
	
}
