package com.bugzero.rarego.boundedContext.member.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.event.MemberBecameSellerEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberPromoteSellerUseCase {
	private final MemberSupport memberSupport;
	private final EventPublisher eventPublisher;

	@Transactional
	public void promoteSeller(String publicId, String role) {

		// 이미 seller나 admin이라면 로직 불필요
		if (role.equals("SELLER") || role.equals("ADMIN")) {
			return;
		}

		Member member = memberSupport.findByPublicId(publicId);
		validateSellerField(member);
		eventPublisher.publish(new MemberBecameSellerEvent(publicId));
	}

	private void validateSellerField(Member member) {
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
