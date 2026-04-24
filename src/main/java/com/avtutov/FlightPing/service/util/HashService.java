package com.avtutov.FlightPing.service.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class HashService {

	private final String telegramHashToken;
	private final String aerodataHashToken;
	
	public HashService(@Value("${telegram.bot.token}") String telegramToken,
			@Value("${aerodatabox.api.key}") String aerodataToken) {
		
		this.telegramHashToken = generateSha256(telegramToken);
		this.aerodataHashToken = generateSha256(aerodataToken);
		
		log.info("Telegram webhook url: /callback/telegram-update/{}", telegramHashToken);
    	log.info("Aerodata webhook url: /callback/flight-update/{}", aerodataHashToken);
	}

    public boolean isTelegramHashValid(String hash) {
    	return hash.equals(telegramHashToken);
    }
    
    public boolean isAerodataHashValid(String hash) {
    	return hash.equals(aerodataHashToken);
    }

    public String getTelegramHashToken() {
		return telegramHashToken;
	}

	public String getAerodataHashToken() {
		return aerodataHashToken;
	}
	
    private static String generateSha256(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hashBytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashBytes);
		} catch (NoSuchAlgorithmException e) {
			log.error("Critical error: SHA-256 algorithm not found in JVM", e);
			throw new IllegalStateException("SHA-256 algorithm not found", e);
		}
    }
}
