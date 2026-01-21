package com.bugzero.rarego.boundedContext.member.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateIdentityRequestDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateResponseDto;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class MemberUpdateIdentityUseCaseTest {

	@Mock
	private MemberSupport memberSupport;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private MemberUpdateIdentityUseCase memberUpdateIdentityUseCase;

	@Test
	@DisplayName("연락처/실명을 함께 전달하면 본인인증 정보가 갱신된다")
	void updateIdentity_updatesIdentity() {
		// given
		Member member = baseMember(null, null);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		MemberUpdateIdentityRequestDto requestDto = new MemberUpdateIdentityRequestDto(
			"Alice",
			"010-1234-5678"
		);

		// when
		MemberUpdateResponseDto response = memberUpdateIdentityUseCase.updateIdentity(
			"public-id",
			requestDto
		);

		// then
		assertThat(response.contactPhone()).isEqualTo("01012345678");
		assertThat(response.realName()).isEqualTo("Alice");
		verify(memberRepository).save(member);
	}

	@Test
	@DisplayName("이미 동일한 연락처가 존재하면 예외가 발생한다")
	void updateIdentity_rejectsWhenContactPhoneExists() {
		// given
		Member member = baseMember("01012345678", "Alice");
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		MemberUpdateIdentityRequestDto requestDto = new MemberUpdateIdentityRequestDto(
			"Bob",
			"010-9999-8888"
		);
		willThrow(new CustomException(ErrorType.MEMBER_IDENTITY_ALREADY_VERIFIED))
			.given(memberSupport)
			.findByContactPhone("010-9999-8888");

		// when
		Throwable thrown = catchThrowable(() -> memberUpdateIdentityUseCase.updateIdentity(
			"public-id",
			requestDto
		));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_IDENTITY_ALREADY_VERIFIED);
		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("연락처/실명 중 하나만 전달되면 MEMBER_IDENTITY_REQUIRED 예외가 발생한다")
	void updateIdentity_rejectsPartialIdentity() {
		// given
		Member member = baseMember(null, null);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		MemberUpdateIdentityRequestDto requestDto = new MemberUpdateIdentityRequestDto(
			"Alice",
			null
		);

		// when
		Throwable thrown = catchThrowable(() -> memberUpdateIdentityUseCase.updateIdentity(
			"public-id",
			requestDto
		));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_IDENTITY_REQUIRED);
		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("연락처 형식이 올바르지 않으면 MEMBER_INVALID_PHONE_NUMBER 예외가 발생한다")
	void updateIdentity_rejectsInvalidPhone() {
		// given
		Member member = baseMember(null, null);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		MemberUpdateIdentityRequestDto requestDto = new MemberUpdateIdentityRequestDto(
			"Alice",
			"010-12"
		);

		// when
		Throwable thrown = catchThrowable(() -> memberUpdateIdentityUseCase.updateIdentity(
			"public-id",
			requestDto
		));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_INVALID_PHONE_NUMBER);
		verify(memberRepository, never()).save(any(Member.class));
	}

	@Test
	@DisplayName("실명에 숫자가 포함되면 MEMBER_INVALID_REALNAME 예외가 발생한다")
	void updateIdentity_rejectsInvalidRealName() {
		// given
		Member member = baseMember(null, null);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		MemberUpdateIdentityRequestDto requestDto = new MemberUpdateIdentityRequestDto(
			"홍길동1",
			"01012345678"
		);

		// when
		Throwable thrown = catchThrowable(() -> memberUpdateIdentityUseCase.updateIdentity(
			"public-id",
			requestDto
		));

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_INVALID_REALNAME);
		verify(memberRepository, never()).save(any(Member.class));
	}

	private Member baseMember(String contactPhone, String realName) {
		return Member.builder()
			.publicId("public-id")
			.email("test@example.com")
			.nickname("old")
			.contactPhone(contactPhone)
			.realName(realName)
			.build();
	}
}
