package com.bugzero.rarego.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.domain.NotificationMember;

public interface NotificationMemberRepository extends JpaRepository<NotificationMember, Long> {
	Optional<NotificationMember> findByPublicId(String publicId);
}
