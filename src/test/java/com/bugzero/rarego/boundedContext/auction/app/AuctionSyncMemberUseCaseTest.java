package com.bugzero.rarego.boundedContext.auction.app;

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

import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.shared.member.domain.MemberDto;

@ExtendWith(MockitoExtension.class)
class AuctionSyncMemberUseCaseTest {

	@Mock
	private AuctionMemberRepository auctionMemberRepository;

	@InjectMocks
	private AuctionSyncMemberUseCase auctionSyncMemberUseCase;

	@Test
	@DisplayName("존재하는 것보다 늦은 변경은 무시")
	void syncMember_SkipDelayedEvent() {
		// given
		LocalDateTime existedUpdatedAt = LocalDateTime.now();
		AuctionMember existed = AuctionMember.builder()
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

		given(auctionMemberRepository.findById(1L)).willReturn(Optional.of(existed));

		// when
		AuctionMember result = auctionSyncMemberUseCase.syncMember(member);

		// then
		assertThat(result).isSameAs(existed);
		verify(auctionMemberRepository, never()).save(any(AuctionMember.class));
	}

	@Test
	@DisplayName("존재하는 것보다 새로운 업데이트는 반영")
	void syncMember_UpdateWhenNewerEvent() {
		// given
		LocalDateTime existedUpdatedAt = LocalDateTime.now().minusHours(2);
		AuctionMember existed = AuctionMember.builder()
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

		given(auctionMemberRepository.findById(1L)).willReturn(Optional.of(existed));
		// when
		AuctionMember result = auctionSyncMemberUseCase.syncMember(member);

		// then
		assertThat(result).isSameAs(existed);
		assertThat(result.getUpdatedAt()).isEqualTo(eventUpdatedAt);
		assertThat(result.getEmail()).isEqualTo("user@example.com");
		verify(auctionMemberRepository, never()).save(any(AuctionMember.class));
	}
}
