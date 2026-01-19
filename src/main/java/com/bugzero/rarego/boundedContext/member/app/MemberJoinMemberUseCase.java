package com.bugzero.rarego.boundedContext.member.app;

import java.security.SecureRandom;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.domain.MemberDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import com.bugzero.rarego.shared.member.event.MemberJoinedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberJoinMemberUseCase {
	private final MemberRepository memberRepository;
	private final EventPublisher eventPublisher;
	private static final SecureRandom RND = new SecureRandom();

	public MemberJoinResponseDto join(String email) {
		if (email == null || email.isBlank()) {
			throw new CustomException(ErrorType.MEMBER_EMAIL_EMPTY);
		}
		return memberRepository.findByEmail(email)
			.map(existing -> new MemberJoinResponseDto(existing.getNickname(), existing.getPublicId()))
			.orElseGet(() -> createAndPublishMember(email));
	}

	private MemberJoinResponseDto createAndPublishMember(String email) {
		UUID newPublicId = UUID.randomUUID();

		try {
			Member member = Member.builder()
				.publicId(newPublicId.toString())
				.nickname(randomUserNickname())
				.email(email)
				.build();
			Member saved = memberRepository.save(member);
			MemberJoinResponseDto responseDto = new MemberJoinResponseDto(saved.getNickname(), saved.getPublicId());
			eventPublisher.publish(new MemberJoinedEvent(MemberDto.from(saved)));
			return responseDto;
		} catch (DataIntegrityViolationException e) {
			Member existing = memberRepository.findByEmail(email).orElseThrow(() -> e);
			return new MemberJoinResponseDto(existing.getNickname(), existing.getPublicId());
		} catch (Exception e) {
			throw new CustomException(ErrorType.MEMBER_JOIN_FAILED);
		}
	}

	public static String randomUserNickname() {
		int num = RND.nextInt(10000); // 0~9999
		return "사용자" + String.format("%04d", num);
	}

}
