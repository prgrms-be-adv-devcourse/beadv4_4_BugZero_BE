package com.bugzero.rarego.boundedContext.product.app;

import static org.assertj.core.api.AssertionsForInterfaceTypes.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductImage;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.shared.product.dto.ProductResponseForInspectionDto;

@DataJpaTest
class ProductReadProductsForInspectionUseCaseTest {
	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private TestEntityManager entityManager; // 테스트 데이터 삽입을 위한 매니저

	private ProductMember seller;
	private Product product1;
	private Product product2;

	@BeforeEach
	void setUp() {
		// 1. 판매자 생성 및 저장
		seller = ProductMember.builder()
			.id(1L)
			.email("seller@test.com")
			.publicId("seller-123")
			.nickname("seller")
			.build();
		entityManager.persist(seller);

		product1 = Product.builder()
			.name("레고 스타워즈")
			.seller(seller)
			.category(Category.스타워즈)
			.inspectionStatus(InspectionStatus.PENDING)
			.build();

		product2 = Product.builder()
			.name("해리포터 불의잔")
			.seller(seller)
			.category(Category.해리포터)
			.inspectionStatus(InspectionStatus.APPROVED)
			.build();

		product1.addImage(ProductImage.createConfirmedImage(product1, "url0", 0));
		product1.addImage(ProductImage.createConfirmedImage(product1, "url1", 1));

		entityManager.persist(product1);
		entityManager.persist(product2);
		entityManager.flush();
	}

	@Test
	@DisplayName("성공 : 스타워즈라는 이름을 검색했을 때 해당하는 상품만 나와야 한다.")
	void readProductsForAdmin_Thumbnail_Success() {
		// when
		Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
		Page<ProductResponseForInspectionDto> result = productRepository.readProductsForAdmin(
			"레고", null, null, pageable);

		// then
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).name()).isEqualTo("레고 스타워즈");
		assertThat(result.getContent().get(0).thumbnail()).isEqualTo("url0"); // 0번 이미지가 왔는지 확인
	}

	@Test
	@DisplayName("관리자 목록 조회 - 검수 상태 필터가 적용되어야 한다.")
	void readProductsForAdmin_NoImage_Success() {
		// given
		Product noImgProduct = Product.builder()
			.name("해리포터 아즈카반의 죄수")
			.seller(seller)
			.category(Category.해리포터)
			.inspectionStatus(InspectionStatus.APPROVED)
			.build();
		entityManager.persist(noImgProduct);
		entityManager.flush();

		// when
		Pageable pageable = PageRequest.of(0, 10);
		Page<ProductResponseForInspectionDto> result = productRepository.readProductsForAdmin(
			null, null, InspectionStatus.APPROVED, pageable);

		// then
		assertThat(result.getContent()).hasSize(2);
		assertThat(result.getContent().get(0).name()).isEqualTo("해리포터 불의잔"); //before에서 먼저 생성되었기 때문에
		assertThat(result.getContent().get(1).name()).isEqualTo("해리포터 아즈카반의 죄수");
	}

}