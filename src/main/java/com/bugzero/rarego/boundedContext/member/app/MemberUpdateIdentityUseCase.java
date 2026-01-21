package com.bugzero.rarego.boundedContext.member.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateIdentityRequestDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateResponseDto;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.domain.MemberDto;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberUpdateIdentityUseCase {

	private final MemberSupport memberSupport;
	private final MemberRepository memberRepository;
	private final EventPublisher eventPublisher;

	// 본인인증
	public MemberUpdateResponseDto updateIdentity(String publicId, MemberUpdateIdentityRequestDto requestDto
	) {
		// valid
		if (requestDto == null) {
			throw new CustomException(ErrorType.MEMBER_IDENTITY_REQUIRED);
		}
		// Member 객체 확인
		Member member = memberSupport.findByPublicId(publicId);

		// 내용 valid
		String contactPhone = requestDto.contactPhone();
		String realName = requestDto.realName();
		if (realName == null || contactPhone == null) {
			throw new CustomException(ErrorType.MEMBER_IDENTITY_REQUIRED);
		}

		String normalizedPhone = normalizePhone(contactPhone);
		validatePhone(normalizedPhone);
		String normalizedName = normalizeRealName(realName);
		validateRealName(normalizedName);

		memberSupport.findByContactPhone(contactPhone);	// 이미 같은 번호 존재하면 오류
		member.changeIdentity(normalizedPhone, normalizedName);
		memberRepository.save(member);
		eventPublisher.publish(new MemberUpdatedEvent(MemberDto.from(member)));
		return MemberUpdateResponseDto.from(member);
	}

	// 하이픈 방지
	private String normalizePhone(String contactPhone) {
		return contactPhone.trim().replaceAll("-", "");
	}

	// 숫자 10~11자리만 허용
	private void validatePhone(String normalizedPhone) {
		if (normalizedPhone.isBlank() || !normalizedPhone.matches("\\d{10,11}")) {
			throw new CustomException(ErrorType.MEMBER_INVALID_PHONE_NUMBER);
		}
	}

	// 좌우 공백 없애기
	private String normalizeRealName(String realName) {
		return realName.trim();
	}

	// 한국어, 영어만 허용
	private void validateRealName(String normalizedName) {
		if (normalizedName.isBlank() || normalizedName.length() > 10) {
			throw new CustomException(ErrorType.MEMBER_INVALID_REALNAME);
		}
		if (!normalizedName.matches("^[가-힣a-zA-Z]+$")) {
			throw new CustomException(ErrorType.MEMBER_INVALID_REALNAME);
		}
	}
}
