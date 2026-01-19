package com.bugzero.rarego.boundedContext.member.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import com.bugzero.rarego.shared.member.event.MemberJoinedEvent;

@ExtendWith(MockitoExtension.class)
class MemberJoinMemberUseCaseTest {
	@Mock
	private MemberRepository memberRepository;

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private MemberJoinMemberUseCase memberJoinMemberUseCase;

	@Test
	@DisplayName("이메일이 비어있으면 MEMBER_EMAIL_EMPTY 예외를 발생시킨다.")
	void joinRejectsBlankEmail() {
		assertThatThrownBy(() -> memberJoinMemberUseCase.join("  "))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_EMAIL_EMPTY);
	}

	@Test
	@DisplayName("이미 존재하는 이메일이면 기존 회원 정보를 반환한다.")
	void joinReturnsExistingMember() {
		Member existing = Member.builder()
			.publicId("public-id")
			.nickname("tester")
			.email("test@example.com")
			.build();

		when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existing));

		MemberJoinResponseDto result = memberJoinMemberUseCase.join("test@example.com");

		assertThat(result.memberPublicId()).isEqualTo("public-id");
		assertThat(result.nickname()).isEqualTo("tester");
		verify(memberRepository, never()).save(any(Member.class));
		verify(eventPublisher, never()).publish(any());
	}

	@Test
	@DisplayName("신규 이메일이면 회원을 생성하고 이벤트를 발행한다.")
	void joinCreatesMemberAndPublishesEvent() {
		Member saved = Member.builder()
			.publicId("new-public-id")
			.nickname("newbie")
			.email("new@example.com")
			.build();

		when(memberRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
		when(memberRepository.save(any(Member.class))).thenReturn(saved);

		MemberJoinResponseDto result = memberJoinMemberUseCase.join("new@example.com");

		assertThat(result.memberPublicId()).isEqualTo("new-public-id");
		assertThat(result.nickname()).isEqualTo("newbie");
		verify(eventPublisher).publish(any(MemberJoinedEvent.class));
	}

	@Test
	@DisplayName("저장 중 중복이 발생하면 기존 회원 정보를 반환한다.")
	void joinReturnsExistingWhenDuplicateOnSave() {
		Member existing = Member.builder()
			.publicId("dup-public-id")
			.nickname("dup")
			.email("dup@example.com")
			.build();

		when(memberRepository.findByEmail("dup@example.com"))
			.thenReturn(Optional.empty(), Optional.of(existing));
		when(memberRepository.save(any(Member.class)))
			.thenThrow(new DataIntegrityViolationException("duplicate"));

		MemberJoinResponseDto result = memberJoinMemberUseCase.join("dup@example.com");

		assertThat(result.memberPublicId()).isEqualTo("dup-public-id");
		verify(eventPublisher, never()).publish(any());
	}
}
