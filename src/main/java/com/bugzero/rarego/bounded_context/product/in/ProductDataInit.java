package com.bugzero.rarego.bounded_context.product.in;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.bounded_context.product.domain.Category;
import com.bugzero.rarego.bounded_context.product.domain.InspectionStatus;
import com.bugzero.rarego.bounded_context.product.domain.Product;
import com.bugzero.rarego.bounded_context.product.domain.ProductCondition;
import com.bugzero.rarego.bounded_context.product.out.ProductMemberRepository;
import com.bugzero.rarego.bounded_context.product.out.ProductRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ProductDataInit implements CommandLineRunner {
	private final ProductMemberRepository productMemberRepository;
	private final ProductRepository productRepository;

	@Override
	public void run(String... args) throws Exception {

		//ProductMember 초기 데이터
		createProduct(1L, "다크나이트");
		createProduct(1L, "화이트 졸병");

	}

	private Product createProduct(Long sellerId, String name) {
		return productRepository.save(Product.builder()
			.sellerId(sellerId)
			.name(name)
			.description("테스트용 상품 설명")
			.category(Category.스타워즈)
			.productCondition(ProductCondition.INSPECTION)
			.inspectionStatus(InspectionStatus.PENDING)
			.build());
	}
}
