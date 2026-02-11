package com.bugzero.rarego.product.app;

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

import com.bugzero.rarego.product.domain.ProductMember;
import com.bugzero.rarego.product.out.ProductMemberRepository;
import com.bugzero.rarego.shared.member.domain.MemberDto;

@ExtendWith(MockitoExtension.class)
class ProductSyncMemberUseCaseTest {

	@Mock
	private ProductMemberRepository productMemberRepository;

	@InjectMocks
	private ProductSyncMemberUseCase productSyncMemberUseCase;

	@Test
	@DisplayName("replica에서 이미 업데이트 된 날짜보다 늦은 변경은 무시")
	void syncMember_SkipDelayedEvent() {
		// given
		LocalDateTime existedUpdatedAt = LocalDateTime.now();
		ProductMember existed = ProductMember.builder()
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

		given(productMemberRepository.findById(1L)).willReturn(Optional.of(existed));

		// when
		ProductMember result = productSyncMemberUseCase.syncMember(member);

		// then
		assertThat(result).isSameAs(existed);
		verify(productMemberRepository, never()).save(any(ProductMember.class));
	}

	@Test
	@DisplayName("replica에서 이미 업데이트 된 날짜보다 새로운 업데이트는 반영")
	void syncMember_UpdateWhenNewerEvent() {
		// given
		LocalDateTime existedUpdatedAt = LocalDateTime.now().minusHours(2);
		ProductMember existed = ProductMember.builder()
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

		given(productMemberRepository.findById(1L)).willReturn(Optional.of(existed));

		// when
		ProductMember result = productSyncMemberUseCase.syncMember(member);

		// then
		assertThat(result).isSameAs(existed);
		assertThat(result.getUpdatedAt()).isEqualTo(eventUpdatedAt);
		assertThat(result.getEmail()).isEqualTo("user@example.com");
		verify(productMemberRepository, never()).save(any(ProductMember.class));
	}

	@Test
	@DisplayName("같은 updatedAt 이벤트도 최신 상태로 반영")
	void syncMember_UpdateWhenEqualUpdatedAt() {
		// given
		LocalDateTime sameUpdatedAt = LocalDateTime.now();
		ProductMember existed = ProductMember.builder()
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

		given(productMemberRepository.findById(1L)).willReturn(Optional.of(existed));

		// when
		ProductMember result = productSyncMemberUseCase.syncMember(member);

		// then
		assertThat(result).isSameAs(existed);
		assertThat(result.getEmail()).isEqualTo("after@example.com");
		assertThat(result.getUpdatedAt()).isEqualTo(sameUpdatedAt);
		verify(productMemberRepository, never()).save(any(ProductMember.class));
	}

	@Test
	@DisplayName("기존 회원이 없으면 신규 저장")
	void syncMember_SaveWhenNotExists() {
		// given
		LocalDateTime now = LocalDateTime.now();
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
			now.minusDays(1),
			now,
			false
		);
		given(productMemberRepository.findById(1L)).willReturn(Optional.empty());

		// when
		ProductMember result = productSyncMemberUseCase.syncMember(member);

		// then
		assertThat(result.getId()).isEqualTo(1L);
		assertThat(result.getEmail()).isEqualTo("user@example.com");
		verify(productMemberRepository).save(any(ProductMember.class));
	}
}
