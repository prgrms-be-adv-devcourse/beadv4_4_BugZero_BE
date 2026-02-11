package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationSubscribeUseCase {
	private final NotificationSseSupport notificationSseSupport;

	public SseEmitter subscribe(String publicId) {
		return notificationSseSupport.subscribe(publicId);
	}
}
