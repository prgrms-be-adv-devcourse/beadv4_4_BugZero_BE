package com.bugzero.rarego.boundedContext.member.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class MemberVerifyParticipationUseCaseTest {

	@Mock
	private MemberSupport memberSupport;

	@InjectMocks
	private MemberVerifyParticipationUseCase memberVerifyParticipationUseCase;

	@Test
	@DisplayName("필수 항목이 모두 있으면 검수가 통과한다")
	void verifyParticipation_acceptsWhenAllFieldsPresent() {
		Member member = baseMember();
		when(memberSupport.findByPublicId("public-id")).thenReturn(member);

		memberVerifyParticipationUseCase.verifyParticipation("public-id");

		verify(memberSupport).findByPublicId("public-id");
	}

	@Test
	@DisplayName("우편번호가 비어있으면 MEMBER_ZIPCODE_REQUIRED 예외가 발생한다")
	void verifyParticipation_rejectsMissingZipCode() {
		Member member = buildMember("address", "detail", null, "01012345678", "Alice");

		assertRequiredFieldError(member, ErrorType.MEMBER_ZIPCODE_REQUIRED);
	}

	@Test
	@DisplayName("주소가 비어있으면 MEMBER_ADDRESS_REQUIRED 예외가 발생한다")
	void verifyParticipation_rejectsMissingAddress() {
		Member member = buildMember(null, "detail", "12345", "01012345678", "Alice");

		assertRequiredFieldError(member, ErrorType.MEMBER_ADDRESS_REQUIRED);
	}

	@Test
	@DisplayName("상세주소가 비어있으면 MEMBER_ADDRESS_DETAIL_REQUIRED 예외가 발생한다")
	void verifyParticipation_rejectsMissingAddressDetail() {
		Member member = buildMember("address", null, "12345", "01012345678", "Alice");

		assertRequiredFieldError(member, ErrorType.MEMBER_ADDRESS_DETAIL_REQUIRED);
	}

	@Test
	@DisplayName("연락처가 비어있으면 MEMBER_PHONE_REQUIRED 예외가 발생한다")
	void verifyParticipation_rejectsMissingPhone() {
		Member member = buildMember("address", "detail", "12345", null, "Alice");

		assertRequiredFieldError(member, ErrorType.MEMBER_PHONE_REQUIRED);
	}

	@Test
	@DisplayName("실명이 비어있으면 MEMBER_REALNAME_REQUIRED 예외가 발생한다")
	void verifyParticipation_rejectsMissingRealName() {
		Member member = buildMember("address", "detail", "12345", "01012345678", null);

		assertRequiredFieldError(member, ErrorType.MEMBER_REALNAME_REQUIRED);
	}

	private void assertRequiredFieldError(Member member, ErrorType errorType) {
		when(memberSupport.findByPublicId("public-id")).thenReturn(member);

		Throwable thrown = catchThrowable(
			() -> memberVerifyParticipationUseCase.verifyParticipation("public-id")
		);

		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(errorType);
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
