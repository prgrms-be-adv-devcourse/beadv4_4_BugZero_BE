package com.bugzero.rarego.boundedContext.member.app;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

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

	public Member findByPublicId(String publicId) {
		return memberRepository.findByPublicId(publicId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
	}

	public void findByContactPhone(String contactPhone) {
		if (memberRepository.findByContactPhone(contactPhone).isPresent()) {
			throw new CustomException(ErrorType.MEMBER_IDENTITY_ALREADY_VERIFIED);
		}
	}
}