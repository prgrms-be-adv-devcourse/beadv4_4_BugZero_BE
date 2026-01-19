package com.bugzero.rarego.boundedContext.member.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.domain.MemberClearField;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateRequestDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateResponseDto;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class MemberUpdateMemberUseCaseTest {

	@Mock
	private MemberSupport memberSupport;

	@Mock
	private MemberRepository memberRepository;

	@InjectMocks
	private MemberUpdateMemberUseCase memberUpdateMemberUseCase;

	@Test
	@DisplayName("USER가 유효한 요청을 보내면 값이 갱신되고 공백/하이픈이 정규화된다")
	void updateMe_updatesFieldsForUser() {
		// given
		Member member = baseMember();
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			" newNick ",
			" intro ",
			"12345",
			" new address ",
			" detail ",
			"Alice",
			"010-1234-5678",
			null
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		// when
		MemberUpdateResponseDto response = memberUpdateMemberUseCase.updateMe("public-id", "USER", requestDto);

		// then
		assertThat(response.nickname()).isEqualTo("newNick");
		assertThat(response.intro()).isEqualTo("intro");
		assertThat(response.zipCode()).isEqualTo("12345");
		assertThat(response.address()).isEqualTo("new address");
		assertThat(response.addressDetail()).isEqualTo("detail");
		assertThat(response.contactPhone()).isEqualTo("01012345678");
		assertThat(response.realName()).isEqualTo("Alice");
		verify(memberRepository).save(member);
	}

	@Test
	@DisplayName("clearFields가 전달되면 해당 값이 null로 초기화된다")
	void updateMe_appliesClearFields() {
		// given
		Member member = baseMember();
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			null,
			null,
			null,
			null,
			null,
			null,
			null,
			Set.of(MemberClearField.INTRO, MemberClearField.CONTACT_PHONE)
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		// when
		MemberUpdateResponseDto response = memberUpdateMemberUseCase.updateMe("public-id", "USER", requestDto);

		// then
		assertThat(response.intro()).isNull();
		assertThat(response.contactPhone()).isNull();
		verify(memberRepository).save(member);
	}

	@Test
	@DisplayName("clearFields와 patch가 동시에 오면 MEMBER_UPDATED_FAILED 예외를 발생시킨다")
	void updateMe_rejectsClearAndPatchConflict() {
		// given
		Member member = baseMember();
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			" newNick ",
			" new intro ",
			"54321",
			" new address ",
			" new detail ",
			"Alice",
			"010-9999-8888",
			Set.of(
				MemberClearField.INTRO,
				MemberClearField.CONTACT_PHONE,
				MemberClearField.ADDRESS_DETAIL
			)
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		// when
		Throwable thrown = catchThrowable(
			() -> memberUpdateMemberUseCase.updateMe("public-id", "USER", requestDto)
		);

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_UPDATED_FAILED);
		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("SELLER는 닉네임이 없으면 MEMBER_NICKNAME_REQUIRED 예외를 발생시킨다")
	void updateMe_requiresNicknameForSeller() {
		// given
		Member member = baseMember();
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			null,
			"intro",
			"12345",
			"Seoul",
			"Detail",
			"Alice",
			"01012345678",
			null
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		// when
		Throwable thrown = catchThrowable(
			() -> memberUpdateMemberUseCase.updateMe("public-id", "SELLER", requestDto)
		);

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_NICKNAME_REQUIRED);
		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("SELLER가 필수 값을 clear 하면 MEMBER_SELLER_REQUIRED_FIELD_CANNOT_BE_CLEARED 예외를 발생시킨다")
	void updateMe_sellerCannotClearRequiredFields() {
		// given
		Member member = baseMember();
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			"nickname",
			null,
			"12345",
			"Seoul",
			"Detail",
			"Alice",
			"01012345678",
			Set.of(MemberClearField.ZIPCODE)
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		// when
		Throwable thrown = catchThrowable(
			() -> memberUpdateMemberUseCase.updateMe("public-id", "SELLER", requestDto)
		);

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_SELLER_REQUIRED_FIELD_CANNOT_BE_CLEARED);
		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("연락처 형식이 올바르지 않으면 MEMBER_INVALID_PHONE_NUMBER 예외를 발생시킨다")
	void updateMe_rejectsInvalidPhone() {
		// given
		Member member = baseMember();
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			null,
			null,
			null,
			null,
			null,
			null,
			"010-12",
			null
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		// when
		Throwable thrown = catchThrowable(
			() -> memberUpdateMemberUseCase.updateMe("public-id", "USER", requestDto)
		);

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_INVALID_PHONE_NUMBER);
		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("실명에 숫자가 포함되면 MEMBER_INVALID_REALNAME 예외를 발생시킨다")
	void updateMe_rejectsInvalidRealName() {
		// given
		Member member = baseMember();
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			null,
			null,
			null,
			null,
			null,
			"홍길동1",
			null,
			null
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		// when
		Throwable thrown = catchThrowable(
			() -> memberUpdateMemberUseCase.updateMe("public-id", "USER", requestDto)
		);

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_INVALID_REALNAME);
		verify(memberRepository, never()).save(any(Member.class));
	}

	private Member baseMember() {
		return Member.builder()
			.publicId("public-id")
			.email("test@example.com")
			.nickname("old")
			.intro("old intro")
			.address("Old Address")
			.addressDetail("Old Detail")
			.zipCode("00000")
			.contactPhone("01011112222")
			.realName("OldName")
			.build();
	}
}
