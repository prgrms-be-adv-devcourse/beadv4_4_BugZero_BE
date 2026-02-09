package com.bugzero.rarego.out;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.domain.NotificationMember;

public interface NotificationMemberRepository extends JpaRepository<NotificationMember, Long> {
}
