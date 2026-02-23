package com.bugzero.rarego.bounded_context.member.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.app.MemberSupport;
import com.bugzero.rarego.app.MemberUpdateMemberUseCase;
import com.bugzero.rarego.domain.Member;
import com.bugzero.rarego.domain.MemberClearField;
import com.bugzero.rarego.domain.MemberUpdateRequestDto;
import com.bugzero.rarego.domain.MemberUpdateResponseDto;
import com.bugzero.rarego.global.outbox.app.OutboxUseCase;
import com.bugzero.rarego.out.MemberRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class MemberUpdateMemberUseCaseTest {

	@Mock
	private MemberSupport memberSupport;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private OutboxUseCase outboxUseCase;

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
			null
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);
		given(memberRepository.saveAndFlush(member)).willReturn(member);

		// when
		MemberUpdateResponseDto response = memberUpdateMemberUseCase.updateMe("public-id", requestDto);

		// then
		assertThat(response.nickname()).isEqualTo("newNick");
		assertThat(response.intro()).isEqualTo("intro");
		assertThat(response.zipCode()).isEqualTo("12345");
		assertThat(response.address()).isEqualTo("new address");
		assertThat(response.addressDetail()).isEqualTo("detail");
		verify(memberRepository).saveAndFlush(member);
		verify(outboxUseCase).saveOutbox(any());
	}

	@Test
	@DisplayName("닉네임이 이미 존재하면 MEMBER_NICKNAME_ALREADY_EXISTS 예외를 발생시킨다")
	void updateMe_rejectsExistingNickname() {
		// given
		Member member = baseMember();
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			" dupNick ",
			null,
			null,
			null,
			null,
			null
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);
		given(memberRepository.existsByNickname("dupNick")).willReturn(true);

		// when
		Throwable thrown = catchThrowable(
			() -> memberUpdateMemberUseCase.updateMe("public-id", requestDto)
		);

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_NICKNAME_ALREADY_EXISTS);
		verify(memberRepository, never()).saveAndFlush(any(Member.class));
		verifyNoInteractions(outboxUseCase);
	}

	@Test
	@DisplayName("동일한 닉네임이면 중복 검사 없이 무시한다")
	void updateMe_ignoresSameNickname() {
		// given
		Member member = baseMember();
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			" old ",
			null,
			null,
			null,
			null,
			null
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);
		given(memberRepository.saveAndFlush(member)).willReturn(member);

		// when
		MemberUpdateResponseDto response = memberUpdateMemberUseCase.updateMe("public-id", requestDto);

		// then
		assertThat(response.nickname()).isEqualTo("old");
		verify(memberRepository, never()).existsByNickname(anyString());
		verify(memberRepository).saveAndFlush(member);
		verify(outboxUseCase).saveOutbox(any());
	}

	@Test
	@DisplayName("clearFields가 전달되면 해당 값이 null로 초기화된다")
	void updateMe_appliesClearFields() {
		// given
		Member member = baseMember();
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			"check",
			null,
			null,
			null,
			null,
			Set.of(MemberClearField.INTRO)
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);
		given(memberRepository.saveAndFlush(member)).willReturn(member);

		// when
		MemberUpdateResponseDto response = memberUpdateMemberUseCase.updateMe("public-id", requestDto);

		// then
		assertThat(response.intro()).isNull();
		verify(memberRepository).saveAndFlush(member);
		verify(outboxUseCase).saveOutbox(any());
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
			Set.of(
				MemberClearField.INTRO,
				MemberClearField.ADDRESS_DETAIL
			)
		);
		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		// when
		Throwable thrown = catchThrowable(
			() -> memberUpdateMemberUseCase.updateMe("public-id", requestDto)
		);

		// then
		assertThat(thrown)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_UPDATED_FAILED);
		verify(memberRepository, never()).saveAndFlush(any(Member.class));
		verifyNoInteractions(outboxUseCase);
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
