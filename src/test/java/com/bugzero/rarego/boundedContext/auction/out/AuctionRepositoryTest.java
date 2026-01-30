package com.bugzero.rarego.boundedContext.auction.out;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;

@DataJpaTest
class AuctionRepositoryTest {

	@Autowired
	private AuctionRepository auctionRepository;

	@Autowired
	private TestEntityManager em;

	@Test
	@DisplayName("경매 조회 - SCHEDULED 상태도 포함하여 조회되는지 확인")
	void findAllBySearchConditions() {
		// 1. [Given] 데이터 준비 - 타 도메인 엔티티(Product, Member) 제거

		Long sellerId = 1L;
		Long productId = 100L;

		// A: IN_PROGRESS 상태의 경매
		Auction target1 = Auction.builder()
			.sellerId(sellerId)
			.productId(productId)
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusDays(3))
			.startPrice(10000)
			.durationDays(3)
			.build();

		// status는 @BuilderDefault나 로직에 의해 결정되므로 테스트를 위해 강제 주입
		ReflectionTestUtils.setField(target1, "status", AuctionStatus.IN_PROGRESS);
		em.persist(target1);

		// B: SCHEDULED 상태의 경매 (미래 시작)
		Auction target2 = Auction.builder()
			.sellerId(sellerId)
			.productId(productId)
			.startTime(LocalDateTime.now().plusHours(1))
			.endTime(LocalDateTime.now().plusDays(3))
			.startPrice(30000)
			.durationDays(3)
			.build();

		ReflectionTestUtils.setField(target2, "status", AuctionStatus.SCHEDULED);
		em.persist(target2);

		em.flush();
		em.clear();

		// 2. [When] 조건 없이 전체 조회 (status = null)
		Page<Auction> result = auctionRepository.findAllBySearchConditions(
			null, // auctionIds
			null, // status 필터 없음 -> SCHEDULED도 나와야 함
			null, // productIds
			List.of(productId), // approvedProductIds - 테스트용으로 productId 포함
			PageRequest.of(0, 10)
		);

		// 3. [Then] 검증
		// IN_PROGRESS(1개) + SCHEDULED(1개) = 총 2개가 나와야 정상
		assertThat(result.getContent()).hasSize(2);

		// ID 검증
		List<Long> resultIds = result.getContent().stream().map(Auction::getId).toList();
		assertThat(resultIds).contains(target1.getId(), target2.getId());
	}
}