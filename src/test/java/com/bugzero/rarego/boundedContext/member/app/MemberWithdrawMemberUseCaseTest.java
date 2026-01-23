package com.bugzero.rarego.boundedContext.member.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;

@ExtendWith(MockitoExtension.class)
class MemberWithdrawMemberUseCaseTest {
	@Mock
	private MemberSupport memberSupport;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private EventPublisher eventPublisher;

	@InjectMocks
	private MemberWithdrawMemberUseCase memberWithdrawMemberUseCase;

	@Test
	@DisplayName("탈퇴 대상 회원이 없으면 MEMBER_NOT_FOUND를 던진다.")
	void withdrawThrowsWhenMemberMissing() {
		when(memberSupport.findByPublicId("public-id"))
			.thenThrow(new CustomException(ErrorType.MEMBER_NOT_FOUND));

		assertThatThrownBy(() -> memberWithdrawMemberUseCase.withdraw("public-id"))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_NOT_FOUND);

		verifyNoInteractions(memberRepository, eventPublisher);
	}

	@Test
	@DisplayName("이미 탈퇴한 회원이면 MEMBER_MEMBER_DELETED을 던진다.")
	void withdrawThrowsWhenMemberDeleted() {
		Member member = Member.builder()
			.publicId("public-id")
			.email("user@example.com")
			.nickname("tester")
			.build();
		member.softDelete();

		when(memberSupport.findByPublicId("public-id")).thenReturn(member);

		assertThatThrownBy(() -> memberWithdrawMemberUseCase.withdraw("public-id"))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_MEMBER_DELETED);

		verifyNoInteractions(memberRepository, eventPublisher);
	}

	@Test
	@DisplayName("정상 요청이면 회원을 소프트 삭제하고 이벤트를 발행한다.")
	void withdrawSoftDeletesMemberAndPublishesEvent() {
		Member member = Member.builder()
			.publicId("public-id")
			.email("user@example.com")
			.nickname("tester")
			.build();

		when(memberSupport.findByPublicId("public-id")).thenReturn(member);

		String result = memberWithdrawMemberUseCase.withdraw("public-id");

		assertThat(result).isEqualTo("public-id");
		assertThat(member.isDeleted()).isTrue();
		verify(memberRepository).save(member);

		ArgumentCaptor<MemberUpdatedEvent> captor = ArgumentCaptor.forClass(MemberUpdatedEvent.class);
		verify(eventPublisher).publish(captor.capture());
		assertThat(captor.getValue().memberDto().publicId()).isEqualTo("public-id");
	}
}
