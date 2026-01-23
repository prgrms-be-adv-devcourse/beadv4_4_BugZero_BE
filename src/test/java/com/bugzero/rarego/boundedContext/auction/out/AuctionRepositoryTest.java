package com.bugzero.rarego.boundedContext.auction.out;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
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

	@Test
	@DisplayName("경매 조회 - 검수 승인됨(APPROVED) + 시간 확정된 경매만 조회된다")
	void findAllApproved_success() {
		// given
		ProductMember seller = ProductMember.builder()
			.publicId("seller_123")
			.email("test@example.com")
			.nickname("판매자1")
			.realName("홍길동")
			.contactPhone("01012345678")
			.deleted(false)
			.build();

		// ID 수동 할당 (이전 해결책 유지)
		ReflectionTestUtils.setField(seller, "id", 1L);
		em.persist(seller);

		// 1. 승인된 상품
		Product approvedProduct = Product.builder()
			.seller(seller)
			.name("승인상품")
			.build();
		em.persist(approvedProduct);

		Inspection approvedInspection = Inspection.builder()
			.product(approvedProduct)
			.inspectionStatus(InspectionStatus.APPROVED) // 승인!
			.inspectorId(999L)
			.seller(seller)
			.productCondition(ProductCondition.MISB)
			.build();
		em.persist(approvedInspection);

		// 2. 미승인 상품 (PENDING)
		Product pendingProduct = Product.builder()
			.seller(seller)
			.name("대기상품")
			.build();
		em.persist(pendingProduct);

		Inspection pendingInspection = Inspection.builder()
			.product(pendingProduct)
			.inspectionStatus(InspectionStatus.PENDING) // 대기
			.inspectorId(999L)
			.seller(seller)
			.productCondition(ProductCondition.INSPECTION)
			.build();
		em.persist(pendingInspection);

		// 3. 경매 생성
		// A: 노출 대상 (승인됨 + 시간 있음)
		Auction target = Auction.builder()
			.sellerId(seller.getId())
			.productId(approvedProduct.getId())
			.startTime(LocalDateTime.now())
			.endTime(LocalDateTime.now().plusDays(3))
			.startPrice(10000)
			.durationDays(3)
			.build();
		// status 설정 (빌더에 없으면 Reflection 사용)
		ReflectionTestUtils.setField(target, "status", AuctionStatus.IN_PROGRESS);
		em.persist(target);

		// B: 미노출 대상 (미승인 상품)
		Auction hidden1 = Auction.builder()
			.sellerId(seller.getId())
			.productId(pendingProduct.getId())
			.startTime(LocalDateTime.now())
			.endTime(LocalDateTime.now().plusDays(3))
			.startPrice(20000)
			.durationDays(3)
			.build();
		ReflectionTestUtils.setField(hidden1, "status", AuctionStatus.IN_PROGRESS);
		em.persist(hidden1);

		// C: 미노출 대상 (시간 없음)
		Auction hidden2 = Auction.builder()
			.sellerId(seller.getId())
			.productId(approvedProduct.getId())
			.startTime(null).endTime(null) // 시간 없음
			.startPrice(30000)
			.durationDays(3)
			.build();
		ReflectionTestUtils.setField(hidden2, "status", AuctionStatus.SCHEDULED);
		em.persist(hidden2);

		em.flush();
		em.clear();

		// when
		Page<Auction> result = auctionRepository.findAllApproved(null, null, PageRequest.of(0, 10));

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getProductId()).isEqualTo(approvedProduct.getId());
	}
}
