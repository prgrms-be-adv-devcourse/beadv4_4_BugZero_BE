package com.bugzero.rarego.boundedContext.product.app;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.shared.auction.out.AuctionApiClient;
import com.bugzero.rarego.shared.product.dto.ProductCreateRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductCreateResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductCreateProductUseCase {
	private final ProductRepository productRepository;
	private final AuctionApiClient auctionApiClient;
	private final ProductSupport productSupport;

    @Transactional
    public ProductCreateResponseDto createProduct(String memberUUID, ProductCreateRequestDto productCreateRequestDto) {

		ProductMember seller = productSupport.verifyValidateMember(memberUUID);

		Product product = productCreateRequestDto.toEntity(seller);

		//상품 이미지 순서 보장 정렬 후 저장
		List<ProductImageRequestDto> images = productSupport.normalizeCreateImageOrder(
			productCreateRequestDto.productImageRequestDto());

		images.forEach(imageRequestDto -> {
			product.addImage(imageRequestDto.toEntity(product));
		});

        // 부모만 저장 (CascadeType.PERSIST에 의해 자식인 ProductImage들도 자동으로 INSERT됨)
        Product savedProduct = productRepository.save(product);

        //경매정보 생성 요청하는 api
        Long auctionId = auctionApiClient.createAuction(savedProduct.getId(), memberUUID,
                productCreateRequestDto.productAuctionRequestDto());

		return ProductCreateResponseDto.builder()
			.productId(savedProduct.getId())
			.auctionId(auctionId)
			.inspectionStatus(savedProduct.getInspectionStatus())
			.build();
	}
}
