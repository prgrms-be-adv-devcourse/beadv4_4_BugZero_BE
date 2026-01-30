package com.bugzero.rarego.bounded_context.member.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.bounded_context.member.domain.Member;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.event.MemberBecameSellerEvent;

@ExtendWith(MockitoExtension.class)
class MemberPromoteSellerUseCaseTest {

	@Mock
	private MemberSupport memberSupport;

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private MemberPromoteSellerUseCase memberPromoteSellerUseCase;

	@Test
	@DisplayName("이미 SELLER라면 추가 처리 없이 종료한다")
	void promoteSeller_returnsWhenRoleSeller() {
		memberPromoteSellerUseCase.promoteSeller("public-id", "SELLER");

		verifyNoInteractions(memberSupport, eventPublisher);
	}

	@Test
	@DisplayName("이미 ADMIN이라면 추가 처리 없이 종료한다")
	void promoteSeller_returnsWhenRoleAdmin() {
		memberPromoteSellerUseCase.promoteSeller("public-id", "ADMIN");

		verifyNoInteractions(memberSupport, eventPublisher);
	}

	@Test
	@DisplayName("필수 항목이 모두 있으면 판매자 전환 이벤트를 발행한다")
	void promoteSeller_publishesEvent() {
		Member member = baseMember();
		when(memberSupport.findByPublicId("public-id")).thenReturn(member);

		memberPromoteSellerUseCase.promoteSeller("public-id", "USER");

		ArgumentCaptor<MemberBecameSellerEvent> eventCaptor =
			ArgumentCaptor.forClass(MemberBecameSellerEvent.class);
		verify(eventPublisher).publish(eventCaptor.capture());
		assertThat(eventCaptor.getValue().getPublicId()).isEqualTo("public-id");
	}

	@Test
	@DisplayName("우편번호가 비어있으면 MEMBER_ZIPCODE_REQUIRED 예외가 발생한다")
	void promoteSeller_rejectsMissingZipCode() {
		Member member = buildMember("address", "detail", null, "01012345678", "Alice");

		assertRequiredFieldError(member, ErrorType.MEMBER_ZIPCODE_REQUIRED);
	}

	@Test
	@DisplayName("주소가 비어있으면 MEMBER_ADDRESS_REQUIRED 예외가 발생한다")
	void promoteSeller_rejectsMissingAddress() {
		Member member = buildMember(null, "detail", "12345", "01012345678", "Alice");

		assertRequiredFieldError(member, ErrorType.MEMBER_ADDRESS_REQUIRED);
	}

	@Test
	@DisplayName("상세주소가 비어있으면 MEMBER_ADDRESS_DETAIL_REQUIRED 예외가 발생한다")
	void promoteSeller_rejectsMissingAddressDetail() {
		Member member = buildMember("address", null, "12345", "01012345678", "Alice");

		assertRequiredFieldError(member, ErrorType.MEMBER_ADDRESS_DETAIL_REQUIRED);
	}

	@Test
	@DisplayName("연락처가 비어있으면 MEMBER_PHONE_REQUIRED 예외가 발생한다")
	void promoteSeller_rejectsMissingPhone() {
		Member member = buildMember("address", "detail", "12345", null, "Alice");

		assertRequiredFieldError(member, ErrorType.MEMBER_PHONE_REQUIRED);
	}

	@Test
	@DisplayName("실명이 비어있으면 MEMBER_REALNAME_REQUIRED 예외가 발생한다")
	void promoteSeller_rejectsMissingRealName() {
		Member member = buildMember("address", "detail", "12345", "01012345678", null);

		assertRequiredFieldError(member, ErrorType.MEMBER_REALNAME_REQUIRED);
	}

	private void assertRequiredFieldError(Member member, ErrorType errorType) {
		when(memberSupport.findByPublicId("public-id")).thenReturn(member);

		Throwable thrown = catchThrowable(
			() -> memberPromoteSellerUseCase.promoteSeller("public-id", "USER")
		);

		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(errorType);
		verify(eventPublisher, never()).publish(any());
	}

	private Member baseMember() {
		return buildMember("address", "detail", "12345", "01012345678", "Alice");
	}

	private Member buildMember(
		String address,
		String addressDetail,
		String zipCode,
		String contactPhone,
		String realName
	) {
		return Member.builder()
			.publicId("public-id")
			.email("test@example.com")
			.nickname("tester")
			.address(address)
			.addressDetail(addressDetail)
			.zipCode(zipCode)
			.contactPhone(contactPhone)
			.realName(realName)
			.build();
	}
}
