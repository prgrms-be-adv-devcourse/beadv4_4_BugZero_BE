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
		requireText(member.getAddress(), "address");
		requireText(member.getAddressDetail(), "addressDetail");
		requireText(member.getZipCode(), "zipCode");
		requireText(member.getContactPhone(), "contactPhone");
		requireText(member.getRealName(), "realName");
	}

	private void requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			String message = String.format("%s은(는) 판매자 필수 값입니다.", field);
			throw new CustomException(ErrorType.MEMBER_NOT_ELIGIBLE_SELLER, message);
		}
	}
}
