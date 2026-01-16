package com.bugzero.rarego.boundedContext.member.out;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bugzero.rarego.boundedContext.member.domain.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
	public Member findByPublicId(String publicId);
}
