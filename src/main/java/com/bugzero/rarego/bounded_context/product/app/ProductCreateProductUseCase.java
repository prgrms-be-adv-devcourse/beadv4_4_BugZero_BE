package com.bugzero.rarego.bounded_context.product.app;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.bounded_context.product.domain.Product;
import com.bugzero.rarego.bounded_context.product.domain.ProductMember;
import com.bugzero.rarego.bounded_context.product.out.ProductRepository;
import com.bugzero.rarego.shared.auction.out.AuctionApiClient;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductCreateProductUseCase {
	private final ProductRepository productRepository;
	private final AuctionApiClient auctionApiClient;
	private final ProductSupport productSupport;

    @Transactional
    public ProductResponseDto createProduct(String memberUUID, ProductRequestDto productRequestDto) {

		ProductMember seller = productSupport.verifyValidateMember(memberUUID);

		Product product = productRequestDto.toEntity(seller.getId());

		//상품 이미지 순서 보장 정렬 후 저장
		List<ProductImageRequestDto> images = productSupport.normalizeCreateImageOrder(
			productRequestDto.productImageRequestDto());

		images.forEach(imageRequestDto -> {
			product.addImage(imageRequestDto.toEntity(product));
		});

        // 부모만 저장 (CascadeType.PERSIST에 의해 자식인 ProductImage들도 자동으로 INSERT됨)
        Product savedProduct = productRepository.save(product);

        //경매정보 생성 요청하는 api
        Long auctionId = auctionApiClient.createAuction(savedProduct.getId(), memberUUID,
                productRequestDto.productAuctionRequestDto());

		return ProductResponseDto.builder()
			.productId(savedProduct.getId())
			.auctionId(auctionId)
			.inspectionStatus(savedProduct.getInspectionStatus())
			.build();
	}
}
