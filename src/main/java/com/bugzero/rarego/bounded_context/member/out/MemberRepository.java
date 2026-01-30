package com.bugzero.rarego.bounded_context.member.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.bounded_context.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	Optional<Member> findByEmail(String email);

	Optional<Member> findByPublicId(String publicId);

	Optional<Member> findByContactPhone(String contactPhone);

	boolean existsByNickname(String nickname);
}
