package com.avtutov.FlightPing.service;

import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.avtutov.FlightPing.model.User;

public interface UserService {
	
	User getOrCreateUser(Message message);

}
