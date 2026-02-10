package com.bugzero.rarego.app;

import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
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

		List<Notification> notifications = mapperOptional.get().map(event);

		if (notifications.isEmpty()) {
			log.warn("[알림] 생성된 알림이 없습니다. Event: {}", event.getClass().getSimpleName());
			return;
		}

		for (Notification notification : notifications) {
			saveWithIdempotency(notification);
		}

		log.info("[알림] 저장 완료. 타입: {}, 개수: {}건", event.getClass().getSimpleName(), notifications.size());
	}

	private void saveWithIdempotency(Notification notification) {
		try {
			notificationRepository.saveAndFlush(notification);
		} catch (DataIntegrityViolationException e) {
			// 이미 DB에 존재하는 경우 (Unique Constraint 위배)
			if (isDuplicateEntryException(e)) {

				log.warn("[알림 중복 무시] 이미 존재하는 알림입니다. MemberId: {}, Type: {}, RefId: {}",
					notification.getMember().getId(),
					notification.getType(),
					notification.getReferenceId());
				return;
			}

			log.error("중복이 아닌 심각한 오류 발생, 알림 저장 실패.", e);
			throw e;
		}
	}

	private boolean isDuplicateEntryException(DataIntegrityViolationException e) {
		Throwable cause = e.getMostSpecificCause();

		// 우리가 Entity에 설정한 제약조건 이름: "uk_notification_dedup"
		String message = cause.getMessage();
		return message != null && (
			message.contains("uk_notification_dedup") || // 우리가 지정한 제약조건명
				message.contains("Duplicate entry")          // MySQL 메시지 패턴
		);
	}
}
