package com.bugzero.rarego.app;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.app.mapper.NotificationMapper;
import com.bugzero.rarego.domain.Notification;
import com.bugzero.rarego.out.NotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCreateNotificationUseCase {
	private final NotificationRepository notificationRepository;
	private final List<NotificationMapper<?>> mappers;

	@SuppressWarnings("unchecked")
	@Transactional
	public void createNotification(Object event) {
		Optional<NotificationMapper<Object>> mapperOptional = (Optional)mappers.stream()
			.filter(m -> m.supports(event))
			.findFirst();

		// 에러를 던지지 않고 로그만 찍고 넘어감 -> 에러 던지면 kafka 재시도
		if (mapperOptional.isEmpty()) {
			log.error("지원하지 않는 알림 이벤트가 감지되었습니다. mapper에 등록해주세요. Event: {}", event.getClass().getSimpleName());
			return;
		}

		NotificationMapper<Object> mapper = mapperOptional.get();
		List<Notification> notifications = mapper.map(event);

		if (notifications.isEmpty()) {
			log.warn("[알림] 생성된 알림이 없습니다. Event: {}", event.getClass().getSimpleName());
			return;
		}

		notificationRepository.saveAll(notifications);

		log.info("[알림] 저장 완료. 타입: {}, 개수: {}건",
			event.getClass().getSimpleName(), notifications.size());
	}
}
