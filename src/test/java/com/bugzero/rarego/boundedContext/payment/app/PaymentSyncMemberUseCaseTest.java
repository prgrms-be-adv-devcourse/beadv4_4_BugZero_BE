package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.out.PaymentMemberRepository;
import com.bugzero.rarego.shared.member.domain.MemberDto;

@ExtendWith(MockitoExtension.class)
class PaymentSyncMemberUseCaseTest {

	@Mock
	private PaymentMemberRepository paymentMemberRepository;

	@InjectMocks
	private PaymentSyncMemberUseCase paymentSyncMemberUseCase;

	@Test
	@DisplayName("존재하는것보다 늦은 변경은 무시")
	void syncMember_SkipDelayedEvent() {
		// given
		LocalDateTime existedUpdatedAt = LocalDateTime.now();
		PaymentMember existed = PaymentMember.builder()
			.id(1L)
			.updatedAt(existedUpdatedAt)
			.build();

		MemberDto member = new MemberDto(
			1L,
			"public-id",
			"user@example.com",
			"nick",
			"intro",
			"address",
			"address detail",
			"12345",
			"01000000000",
			"real name",
			existedUpdatedAt.minusDays(1),
			existedUpdatedAt.minusMinutes(1),
			false
		);

		given(paymentMemberRepository.findById(1L)).willReturn(Optional.of(existed));

		// when
		PaymentMember result = paymentSyncMemberUseCase.syncMember(member);

		// then
		assertThat(result).isSameAs(existed);
		verify(paymentMemberRepository, never()).save(any(PaymentMember.class));
	}

	@Test
	@DisplayName("존재하는 것보다 빠른 변경은 추가")
	void syncMember_SaveWhenNewerEvent() {
		// given
		LocalDateTime existedUpdatedAt = LocalDateTime.now().minusHours(2);
		PaymentMember existed = PaymentMember.builder()
			.id(1L)
			.updatedAt(existedUpdatedAt)
			.build();

		LocalDateTime eventUpdatedAt = LocalDateTime.now();
		MemberDto member = new MemberDto(
			1L,
			"public-id",
			"user@example.com",
			"nick",
			"intro",
			"address",
			"address detail",
			"12345",
			"01000000000",
			"real name",
			eventUpdatedAt.minusDays(1),
			eventUpdatedAt,
			false
		);

		given(paymentMemberRepository.findById(1L)).willReturn(Optional.of(existed));
		given(paymentMemberRepository.save(any(PaymentMember.class)))
			.willAnswer(invocation -> invocation.getArgument(0));

		// when
		PaymentMember result = paymentSyncMemberUseCase.syncMember(member);

		// then
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getUpdatedAt()).isEqualTo(eventUpdatedAt);
		assertThat(result.getEmail()).isEqualTo("user@example.com");
		verify(paymentMemberRepository).save(any(PaymentMember.class));
	}
}
