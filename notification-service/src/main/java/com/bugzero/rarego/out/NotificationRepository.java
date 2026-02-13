package com.bugzero.rarego.out;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.bugzero.rarego.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	Page<Notification> findAllByMemberIdAndIsReadFalse(Long memberId, Pageable pageable);

	Page<Notification> findAllByMemberId(Long memberId, Pageable pageable);

	long countAllByMemberIdAndIsReadFalse(Long id);

	// 일괄 삭제 쿼리
	@Modifying(clearAutomatically = true)
	@Query("DELETE FROM Notification n WHERE n.createdAt < :threshold")
	void deleteByCreatedAtBefore(LocalDateTime threshold);
}
