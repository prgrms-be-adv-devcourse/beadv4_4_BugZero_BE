package com.bugzero.rarego.out;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	Page<Notification> findAllByMemberIdAndIsReadFalse(Long memberId, Pageable pageable);

	Page<Notification> findAllByMemberId(Long memberId, Pageable pageable);

	long countAllByMemberIdAndIsReadFalse(Long id);
}
