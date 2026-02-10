package com.bugzero.rarego.app;

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

import com.bugzero.rarego.domain.AuctionMember;
import com.bugzero.rarego.out.AuctionMemberRepository;
import com.bugzero.rarego.shared.member.domain.MemberDto;

@ExtendWith(MockitoExtension.class)
class AuctionSyncMemberUseCaseTest {

	@Mock
	private AuctionMemberRepository auctionMemberRepository;

	@InjectMocks
	private AuctionSyncMemberUseCase auctionSyncMemberUseCase;

	@Test
	@DisplayName("replica에서 이미 업데이트 된 날짜보다 과거 이벤트도 반영")
	void syncMember_UpdateWhenDelayedEvent() {
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
		assertThat(result.getUpdatedAt()).isEqualTo(existedUpdatedAt.minusMinutes(1));
		assertThat(result.getEmail()).isEqualTo("user@example.com");
		verify(auctionMemberRepository, never()).save(any(AuctionMember.class));
	}

	@Test
	@DisplayName("replica에서 업데이트 된 날짜보다 새로운 업데이트는 반영")
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

	@Test
	@DisplayName("같은 updatedAt 이벤트도 최신 상태로 반영")
	void syncMember_UpdateWhenEqualUpdatedAt() {
		// given
		LocalDateTime sameUpdatedAt = LocalDateTime.now();
		AuctionMember existed = AuctionMember.builder()
			.id(1L)
			.email("before@example.com")
			.updatedAt(sameUpdatedAt)
			.build();

		MemberDto member = new MemberDto(
			1L,
			"public-id",
			"after@example.com",
			"nick",
			"intro",
			"address",
			"address detail",
			"12345",
			"01000000000",
			"real name",
			sameUpdatedAt.minusDays(1),
			sameUpdatedAt,
			false
		);

		given(auctionMemberRepository.findById(1L)).willReturn(Optional.of(existed));

		// when
		AuctionMember result = auctionSyncMemberUseCase.syncMember(member);

		// then
		assertThat(result).isSameAs(existed);
		assertThat(result.getEmail()).isEqualTo("after@example.com");
		assertThat(result.getUpdatedAt()).isEqualTo(sameUpdatedAt);
		verify(auctionMemberRepository, never()).save(any(AuctionMember.class));
	}

	@Test
	@DisplayName("이미 삭제된 회원은 활성 이벤트로 복구하지 않는다")
	void syncMember_DoNotRestoreDeletedMember() {
		// given
		LocalDateTime existedUpdatedAt = LocalDateTime.now().minusHours(2);
		AuctionMember existed = AuctionMember.builder()
			.id(1L)
			.email("deleted@example.com")
			.updatedAt(existedUpdatedAt)
			.deleted(true)
			.build();

		LocalDateTime eventUpdatedAt = LocalDateTime.now();
		MemberDto member = new MemberDto(
			1L,
			"public-id",
			"restored@example.com",
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
		assertThat(result.isDeleted()).isTrue();
		assertThat(result.getEmail()).isEqualTo("deleted@example.com");
		verify(auctionMemberRepository, never()).save(any(AuctionMember.class));
	}

	@Test
	@DisplayName("지연 이벤트여도 삭제 이벤트는 반영한다")
	void syncMember_ApplyDeleteEvenWhenDelayedEvent() {
		// given
		LocalDateTime existedUpdatedAt = LocalDateTime.now();
		AuctionMember existed = AuctionMember.builder()
			.id(1L)
			.updatedAt(existedUpdatedAt)
			.deleted(false)
			.build();

		LocalDateTime eventUpdatedAt = existedUpdatedAt.minusMinutes(1);
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
			eventUpdatedAt,
			true
		);

		given(auctionMemberRepository.findById(1L)).willReturn(Optional.of(existed));

		// when
		AuctionMember result = auctionSyncMemberUseCase.syncMember(member);

		// then
		assertThat(result).isSameAs(existed);
		assertThat(result.isDeleted()).isTrue();
		assertThat(result.getUpdatedAt()).isEqualTo(eventUpdatedAt);
		verify(auctionMemberRepository, never()).save(any(AuctionMember.class));
	}
}
