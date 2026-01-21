package com.bugzero.rarego.boundedContext.member.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

// 회원이 입찰이나 확정 시 필수 요소들을 작성했는지 판별하는 검수 로직
@Service
@RequiredArgsConstructor
public class MemberVerifyParticipationUseCase {
	private final MemberSupport memberSupport;

	// 허용시 OK, 공백이 있다면 오류
	public void verifyParticipation(String publicId) {
		Member member = memberSupport.findByPublicId(publicId);
		validateEligibilityField(member);
	}

	// 이후 정책 변경에 대비하기 위해 seller와 분리
	private void validateEligibilityField(Member member) {
		// intro 제외 필수: address, addressDetail, zipCode, contactPhone, realName
		requireText(member.getZipCode(), ErrorType.MEMBER_ZIPCODE_REQUIRED);
		requireText(member.getAddress(), ErrorType.MEMBER_ADDRESS_REQUIRED);
		requireText(member.getAddressDetail(), ErrorType.MEMBER_ADDRESS_DETAIL_REQUIRED);
		requireText(member.getContactPhone(), ErrorType.MEMBER_PHONE_REQUIRED);
		requireText(member.getRealName(), ErrorType.MEMBER_REALNAME_REQUIRED);
	}

	private void requireText(String value, ErrorType errorType) {
		if (value == null || value.isBlank()) {
			throw new CustomException(errorType);
		}
	}
}
