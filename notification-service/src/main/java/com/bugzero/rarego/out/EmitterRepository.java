package com.bugzero.rarego.out;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Repository
public class EmitterRepository {
	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	public SseEmitter save(String id, SseEmitter sseEmitter) {
		emitters.put(id, sseEmitter);
		return sseEmitter;
	}

	public void deleteById(String id) {
		emitters.remove(id);
	}

	// 한 회원이 여러 연결를 가질 수 있으므로 map으로 반환
	public Map<String, SseEmitter> findAllByMemberPublicId(String memberPublicId) {
		return emitters.entrySet().stream()
			.filter(entry -> entry.getKey().startsWith(memberPublicId))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}
}
