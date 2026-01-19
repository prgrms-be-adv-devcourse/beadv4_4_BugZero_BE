package com.bugzero.rarego.boundedContext.auction.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductImage;
import com.bugzero.rarego.boundedContext.product.out.ProductImageRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderResponseDto;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuctionReadUseCaseTest {

	@InjectMocks
	private AuctionReadUseCase auctionReadUseCase;

	@Mock private AuctionOrderRepository auctionOrderRepository;
	@Mock private AuctionRepository auctionRepository;
	@Mock private ProductRepository productRepository;
	@Mock private AuctionMemberRepository auctionMemberRepository;
	@Mock private ProductImageRepository productImageRepository;

	@Test
	@DisplayName("낙찰 기록 조회 - 구매자(BUYER) 입장")
	void getAuctionOrder_asBuyer() {
		// given
		Long auctionId = 1L;
		Long buyerId = 10L;  // 나 (요청자)
		Long sellerId = 20L; // 판매자

		// Mock Data Setup
		AuctionOrder order = AuctionOrder.builder()
			.auctionId(auctionId).bidderId(buyerId).sellerId(sellerId).finalPrice(10000)
			.build();
		ReflectionTestUtils.setField(order, "id", 777L); // ID 강제 주입

		Auction auction = Auction.builder().productId(50L).build();
		Product product = Product.builder().sellerId(sellerId).build(); // 상품 판매자 = sellerId
		ReflectionTestUtils.setField(product, "id", 50L);
		ReflectionTestUtils.setField(product, "name", "Test Item");

		AuctionMember sellerMember = AuctionMember.builder().id(sellerId).publicId("seller_pub_id").build();

		// 2. 빌더에서 .product() 메서드에 객체를 전달
		ProductImage image = ProductImage.builder()
			.product(product) // O (객체 자체를 주입)
			.imageUrl("thumb.jpg")
			.build();

		// Stubbing
		given(auctionOrderRepository.findByAuctionId(auctionId)).willReturn(Optional.of(order));
		given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));
		given(productRepository.findById(50L)).willReturn(Optional.of(product));
		given(productImageRepository.findAllByProductId(50L)).willReturn(List.of(image));
		given(auctionMemberRepository.findById(sellerId)).willReturn(Optional.of(sellerMember));

		// when
		AuctionOrderResponseDto result = auctionReadUseCase.getAuctionOrder(auctionId, buyerId);

		// then
		assertThat(result.viewerRole()).isEqualTo("BUYER");
		assertThat(result.trader().nickname()).isEqualTo("seller_pub_id"); // 상대방(판매자) 정보
		assertThat(result.productInfo().thumbnailUrl()).isEqualTo("thumb.jpg");
	}

	@Test
	@DisplayName("낙찰 기록 조회 - 권한 없음 (제3자)")
	void getAuctionOrder_forbidden() {
		// given
		Long auctionId = 1L;
		Long strangerId = 99L; // 구매자도 판매자도 아님
		Long buyerId = 10L;
		Long sellerId = 20L;

		AuctionOrder order = AuctionOrder.builder()
			.auctionId(auctionId).bidderId(buyerId).sellerId(sellerId).finalPrice(10000).build();
		Auction auction = Auction.builder().productId(50L).build();
		Product product = Product.builder().sellerId(sellerId).build();

		given(auctionOrderRepository.findByAuctionId(auctionId)).willReturn(Optional.of(order));
		given(auctionRepository.findById(auctionId)).willReturn(Optional.of(auction));
		given(productRepository.findById(50L)).willReturn(Optional.of(product));

		// when & then
		assertThatThrownBy(() -> auctionReadUseCase.getAuctionOrder(auctionId, strangerId))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_ORDER_ACCESS_DENIED);
	}
}