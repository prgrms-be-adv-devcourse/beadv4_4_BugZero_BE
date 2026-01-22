package com.bugzero.rarego.boundedContext.product.in;

import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductCondition;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.boundedContext.product.out.ProductMemberRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;

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
		ProductMember seller = ProductMember.builder()
			.id(1L)
			.publicId(UUID.randomUUID().toString())
			.email("email")
			.nickname("nickname")
			.build();

		//ProductMember 초기 데이터
		createProduct(seller, "다크나이트");
		createProduct(seller, "화이트 졸병");

	}

	private Product createProduct(ProductMember seller, String name) {
		return productRepository.save(Product.builder()
			.seller(seller)
			.name(name)
			.description("테스트용 상품 설명")
			.category(Category.스타워즈)
			.productCondition(ProductCondition.INSPECTION)
			.inspectionStatus(InspectionStatus.PENDING)
			.build());
	}
}
