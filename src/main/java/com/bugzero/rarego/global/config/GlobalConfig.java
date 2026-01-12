package com.bugzero.rarego.global.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import com.bugzero.rarego.global.event.EventPublisher;

import lombok.Getter;

@Configuration
public class GlobalConfig {

	@Getter
	private static EventPublisher eventPublisher;

	@Autowired
	public void setEventPublisher(EventPublisher eventPublisher){
		GlobalConfig.eventPublisher = eventPublisher;
	}

	public static String INTERNAL_CALL_BACK_URL;
}
