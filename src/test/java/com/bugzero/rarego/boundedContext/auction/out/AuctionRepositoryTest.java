package com.bugzero.rarego.boundedContext.auction.out;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.Inspection;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductCondition;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuctionRepositoryTest {

	@Autowired
	private AuctionRepository auctionRepository;

	@Autowired
	private TestEntityManager em;

	// AuctionRepositoryTest.java

	@Test
	@DisplayName("경매 조회 - 검수 승인, 시간 유효, SCHEDULED 아님 조건 및 필터링 조회")
	void findAllBySearchConditions() {
		// 1. [Given] 데이터 준비 (기존 코드와 동일)
		ProductMember seller = ProductMember.builder()
			.id(1L) // ✅ [핵심] Reflection 대신 빌더에서 직접 ID 할당
			.publicId("seller_123")
			.email("test@example.com")
			.nickname("판매자1")
			.realName("홍길동")
			.contactPhone("01012345678")
			.deleted(false)
			.build();

		em.persist(seller);

		// 상품 생성 (승인된 것, 승인 안 된 것)
		Product approvedProduct = Product.builder()
			.seller(seller).name("승인상품").inspectionStatus(InspectionStatus.APPROVED)
			.productCondition(ProductCondition.MISB).category(Category.스타워즈).build();
		em.persist(approvedProduct);

		Product pendingProduct = Product.builder()
			.seller(seller).name("미승인상품").inspectionStatus(InspectionStatus.PENDING)
			.productCondition(ProductCondition.MISB).category(Category.스타워즈).build();
		em.persist(pendingProduct);

		// 검수 승인 데이터 (필수)
		Inspection inspection = Inspection.builder()
			.product(approvedProduct).seller(seller).inspectorId(999L)
			.inspectionStatus(InspectionStatus.APPROVED).productCondition(ProductCondition.MISB).build();
		em.persist(inspection);

		// A: 조회 대상 (승인됨 + 시간 유효 + IN_PROGRESS)
		Auction target = Auction.builder()
			.sellerId(seller.getId()).productId(approvedProduct.getId())
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusDays(3))
			.startPrice(10000).durationDays(3).build();
		ReflectionTestUtils.setField(target, "status", AuctionStatus.IN_PROGRESS);
		em.persist(target);

		// B: 제외 대상 (검수 미승인)
		Auction hidden1 = Auction.builder()
			.sellerId(seller.getId()).productId(pendingProduct.getId())
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusDays(3))
			.startPrice(20000).durationDays(3).build();
		ReflectionTestUtils.setField(hidden1, "status", AuctionStatus.IN_PROGRESS);
		em.persist(hidden1);

		// C: 제외 대상 (SCHEDULED 상태)
		Auction hidden2 = Auction.builder()
			.sellerId(seller.getId()).productId(approvedProduct.getId())
			.startTime(LocalDateTime.now().plusHours(1)) // 미래 시작
			.endTime(LocalDateTime.now().plusDays(3))
			.startPrice(30000).durationDays(3).build();
		// status 기본값은 SCHEDULED
		em.persist(hidden2);

		em.flush();
		em.clear();

		// 2. [When] 메서드 호출
		// 조건 없이 조회 (모든 필터 null) -> 기본 안전장치만 작동해야 함
		Page<Auction> result = auctionRepository.findAllBySearchConditions(
			null, // auctionIds
			null, // status
			null, // productIds
			PageRequest.of(0, 10)
		);

		// 3. [Then] 검증
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getId()).isEqualTo(target.getId());
	}
}
