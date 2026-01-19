package com.bugzero.rarego.boundedContext.member.app;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MemberSupport {
	private final MemberRepository memberRepository;

	public long count() {
		return memberRepository.count();
	}

	public Optional<Member> findByEmail(String email) {
		return memberRepository.findByEmail(email);
	}

	public Optional<Member> findById(Long id) {
		return memberRepository.findById(id);
	}
}