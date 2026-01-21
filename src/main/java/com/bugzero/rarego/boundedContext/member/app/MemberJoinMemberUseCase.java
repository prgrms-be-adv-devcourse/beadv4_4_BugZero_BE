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
	private static final SecureRandom RND = new SecureRandom();
	private static final String[] ADJECTIVES = {
		"귀여운", "멋진", "용감한", "똑똑한", "행복한", "차분한", "활발한", "신비한",
		"따뜻한", "성실한", "엉뚱한", "재빠른", "고요한", "빛나는", "상냥한", "든든한"
	};
	private static final String[] NOUNS = {
		"고양이", "강아지", "여우", "호랑이", "토끼", "곰", "펭귄", "돌고래",
		"사자", "다람쥐", "부엉이", "거북이", "햄스터", "수달", "늑대", "치타"
	};
	private final MemberRepository memberRepository;
	private final EventPublisher eventPublisher;

	// 랜덤으로 닉네임 제공
	public static String randomUserNickname() {
		int num = RND.nextInt(10000); // 0000~9999
		String adj = ADJECTIVES[RND.nextInt(ADJECTIVES.length)];
		String noun = NOUNS[RND.nextInt(NOUNS.length)];
		return adj + noun + String.format("%04d", num);
	}

	// 존재하는 닉네임인지 확인하고, 존재하지 않을때까지 최대 5번 생성
	// 겹칠 확률: 1 / 2백만
	public String uniqueMemberNickname() {
		String checkedNickname;
		int maxAttempts = 5;
		int attempt = 0;
		do {
			checkedNickname = randomUserNickname();
			attempt++;
			if (attempt >= maxAttempts) {
				throw new CustomException(ErrorType.MEMBER_JOIN_FAILED);	// 닉네임 생성 실패
			}
		} while (memberRepository.existsByNickname(checkedNickname));
		return checkedNickname;
	}

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
				.nickname(uniqueMemberNickname())
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

}
