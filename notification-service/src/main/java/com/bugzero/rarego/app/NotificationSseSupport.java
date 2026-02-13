package com.bugzero.rarego.app;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.bugzero.rarego.in.dto.NotificationResponseDto;
import com.bugzero.rarego.out.EmitterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSseSupport {
	private static final Long DEFAULT_TIMEOUT = 60L * 1000 * 60; // 1시간
	private final EmitterRepository emitterRepository;

	public SseEmitter subscribe(String publicId) {
		String emitterId = publicId + "_" + System.currentTimeMillis();

		SseEmitter sseEmitter = new SseEmitter(DEFAULT_TIMEOUT);

		sseEmitter.onCompletion(() -> emitterRepository.deleteById(emitterId));
		sseEmitter.onTimeout(() -> emitterRepository.deleteById(emitterId));
		sseEmitter.onError((error) -> emitterRepository.deleteById(emitterId));

		emitterRepository.save(emitterId, sseEmitter);

		sendToClient(sseEmitter, emitterId, "connect", "SSE 구독이 완료되었습니다.");

		return sseEmitter;
	}

	public void send(String publicId, NotificationResponseDto response) {
		Map<String, SseEmitter> sseEmitters = emitterRepository.findAllByMemberPublicId(publicId);

		sseEmitters.forEach(
			(emitterId, emitter) -> sendToClient(emitter, emitterId, "notification", response));
	}

	private void sendToClient(SseEmitter emitter, String emitterId, String eventName, Object data) {
		try {
			emitter.send(SseEmitter.event()
				.id(emitterId)
				.name(eventName) // 클라이언트에서 addEventListener로 받을 이름
				.data(data));
		} catch (IOException exception) {
			emitterRepository.deleteById(emitterId);
			log.error("SSE 연결 오류 발생하여 emitter 삭제: {}", emitterId);
		}
	}
}
