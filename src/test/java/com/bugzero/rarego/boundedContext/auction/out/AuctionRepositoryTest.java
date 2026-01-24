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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AuctionRepositoryTest {

	@Autowired
	private AuctionRepository auctionRepository;

	@Autowired
	private TestEntityManager em;

	@Test
	@DisplayName("경매 조회 - SCHEDULED 상태도 포함하여 검수 승인된 모든 경매 조회")
	void findAllBySearchConditions() {
		// 1. [Given] 데이터 준비 (기존과 동일)
		ProductMember seller = ProductMember.builder()
			.id(1L)                     // ✅ ID 수동 할당 (필수)
			.publicId("seller_pub_id")  // ✅ Public ID (필수)
			.email("test@example.com")  // ✅ Email (필수, 로그상 NOT NULL 가능성 높음)
			.nickname("테스트판매자")     // ✅ [핵심] 닉네임 (필수 - 에러 원인 해결)
			.realName("홍길동")          // ✅ 실명 (필수 가능성 있음)
			.contactPhone("01012345678") // ✅ 연락처
			.deleted(false)             // ✅ 삭제 여부
			.build();
		em.persist(seller);

		Product approvedProduct = Product.builder()
			.seller(seller).name("승인상품").inspectionStatus(InspectionStatus.APPROVED)
			.productCondition(ProductCondition.MISB).category(Category.스타워즈).build();
		em.persist(approvedProduct);

		Inspection inspection = Inspection.builder()
			.product(approvedProduct).seller(seller).inspectorId(999L)
			.inspectionStatus(InspectionStatus.APPROVED).productCondition(ProductCondition.MISB).build();
		em.persist(inspection);

		// A: IN_PROGRESS (당연히 조회됨)
		Auction target1 = Auction.builder()
			.sellerId(seller.getId()).productId(approvedProduct.getId())
			.startTime(LocalDateTime.now().minusHours(1))
			.endTime(LocalDateTime.now().plusDays(3))
			.startPrice(10000).durationDays(3).build();
		ReflectionTestUtils.setField(target1, "status", AuctionStatus.IN_PROGRESS);
		em.persist(target1);

		// B: SCHEDULED (이제 조회되어야 함!)
		Auction target2 = Auction.builder()
			.sellerId(seller.getId()).productId(approvedProduct.getId())
			.startTime(LocalDateTime.now().plusHours(1)) // 미래 시작
			.endTime(LocalDateTime.now().plusDays(3))
			.startPrice(30000).durationDays(3).build();
		ReflectionTestUtils.setField(target2, "status", AuctionStatus.SCHEDULED);
		em.persist(target2);

		em.flush();
		em.clear();

		// 2. [When] 조건 없이 전체 조회 (status = null)
		Page<Auction> result = auctionRepository.findAllBySearchConditions(
			null,
			null, // status 필터 없음 -> SCHEDULED도 나와야 함
			null,
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
