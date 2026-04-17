package com.avtutov.FlightPing.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import com.avtutov.FlightPing.model.User;
import com.avtutov.FlightPing.repository.UserRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
	
	private final UserRepository userRepository;

	@Transactional
	@Override
	public User getOrCreateUser(Message message) {
		
		Long telegramId = message.getFrom().getId();
		
		return userRepository.findByTelegramId(telegramId)
				.map(user -> updateExistingUser(user, message))
				.orElseGet(() -> registerNewUser(message));
	}
	
	private User updateExistingUser(User user, Message message) {
		
		user.setChatId(message.getChatId());
		user.setUsername(message.getFrom().getUserName());
		user.setFirstName(message.getFrom().getFirstName());
		
		return user;
	}
	
	private User registerNewUser(Message message) {
		
		var from = message.getFrom();
		
		User newUser = User.builder()
				.telegramId(from.getId())
				.chatId(message.getChatId())
				.username(from.getUserName())
				.firstName(from.getFirstName())
				.build();
		
		return userRepository.save(newUser);
	}

}
