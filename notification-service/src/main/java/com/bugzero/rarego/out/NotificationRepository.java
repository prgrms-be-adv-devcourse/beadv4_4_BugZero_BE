package com.bugzero.rarego.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.domain.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
