package com.bugzero.rarego.boundedContext.auction.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.AuctionRelistRequestDto;
import com.bugzero.rarego.shared.auction.dto.AuctionRelistResponseDto;

@ExtendWith(MockitoExtension.class)
class AuctionRelistUseCaseTest {

	@InjectMocks
	private AuctionRelistUseCase auctionRelistUseCase;

	@Mock
	private AuctionSupport support;
	@Mock
	private AuctionRepository auctionRepository;
	@Mock
	private AuctionOrderRepository auctionOrderRepository;

	@Test
	@DisplayName("재경매 성공: 유찰된 상품(주문 없음)을 재등록한다")
	void relistAuction_success_no_order() {
		// given
		Long oldAuctionId = 1L;
		String memberPublicId = "seller_pub_id";
		AuctionRelistRequestDto request = new AuctionRelistRequestDto(20000L, 1000L, 3);

		AuctionMember seller = AuctionMember.builder().publicId(memberPublicId).build();
		ReflectionTestUtils.setField(seller, "id", 100L);

		Auction oldAuction = Auction.builder()
			.productId(50L).sellerId(100L).startPrice(10000).durationDays(3).build();
		ReflectionTestUtils.setField(oldAuction, "id", oldAuctionId);
		ReflectionTestUtils.setField(oldAuction, "status", AuctionStatus.ENDED);

		// Support Mocking
		given(support.getPublicMember(memberPublicId)).willReturn(seller);
		given(support.findAuctionById(oldAuctionId)).willReturn(oldAuction);

		willDoNothing().given(support).validateSeller(oldAuction, 100L);
		willDoNothing().given(support).validateAuctionEnded(oldAuction);

		// 주문 없음 (유찰)
		given(support.findOrder(oldAuctionId)).willReturn(Optional.empty());

		// 저장될 새 경매 객체 Mocking
		Auction savedAuction = Auction.builder()
			.productId(50L).sellerId(100L).startPrice(20000).durationDays(3).build();
		ReflectionTestUtils.setField(savedAuction, "id", 2L);
		ReflectionTestUtils.setField(savedAuction, "status", AuctionStatus.SCHEDULED);

		given(auctionRepository.save(any(Auction.class))).willReturn(savedAuction);

		// when
		AuctionRelistResponseDto result = auctionRelistUseCase.relistAuction(oldAuctionId, memberPublicId, request);

		// then
		assertThat(result.newAuctionId()).isEqualTo(2L);
		assertThat(result.productId()).isEqualTo(50L);
		verify(auctionRepository).save(any(Auction.class));
	}

	@Test
	@DisplayName("재경매 성공: 결제 실패(FAILED)된 상품을 재등록한다 (Case B)")
	void relistAuction_success_failed_order() {
		// given
		Long oldAuctionId = 1L;
		Auction oldAuction = Auction.builder().productId(50L).sellerId(100L).durationDays(1).build();
		ReflectionTestUtils.setField(oldAuction, "id", oldAuctionId);

		AuctionMember seller = AuctionMember.builder().build();
		ReflectionTestUtils.setField(seller, "id", 100L);

		// [수정] finalPrice 추가 (NPE 방지)
		AuctionOrder failedOrder = AuctionOrder.builder()
			.finalPrice(10000)
			.build();
		ReflectionTestUtils.setField(failedOrder, "status", AuctionOrderStatus.FAILED);

		given(support.getPublicMember(anyString())).willReturn(seller);
		given(support.findAuctionById(anyLong())).willReturn(oldAuction);
		given(support.findOrder(oldAuctionId)).willReturn(Optional.of(failedOrder));

		// 저장 Mock
		Auction newAuction = Auction.builder().productId(50L).durationDays(1).build();
		ReflectionTestUtils.setField(newAuction, "status", AuctionStatus.SCHEDULED);
		given(auctionRepository.save(any(Auction.class))).willReturn(newAuction);

		// when
		AuctionRelistResponseDto result =
			auctionRelistUseCase.relistAuction(oldAuctionId, "seller", new AuctionRelistRequestDto(100L, 10L, 1));

		// then
		assertThat(result).isNotNull(); // 예외 없이 성공
	}

	@Test
	@DisplayName("재경매 실패: 이미 판매 완료(SUCCESS)된 상품 (Case A)")
	void relistAuction_fail_already_sold() {
		// given
		Long oldAuctionId = 1L;
		Auction oldAuction = Auction.builder().productId(50L).sellerId(100L).durationDays(1).build();
		ReflectionTestUtils.setField(oldAuction, "id", oldAuctionId);

		AuctionMember seller = AuctionMember.builder().build();
		ReflectionTestUtils.setField(seller, "id", 100L);

		// [수정] finalPrice 추가 (NPE 방지)
		AuctionOrder successOrder = AuctionOrder.builder()
			.finalPrice(10000)
			.build();
		ReflectionTestUtils.setField(successOrder, "status", AuctionOrderStatus.SUCCESS);

		given(support.getPublicMember(anyString())).willReturn(seller);
		given(support.findAuctionById(anyLong())).willReturn(oldAuction);
		given(support.findOrder(oldAuctionId)).willReturn(Optional.of(successOrder));

		// when & then
		assertThatThrownBy(() ->
			auctionRelistUseCase.relistAuction(oldAuctionId, "seller", new AuctionRelistRequestDto(100L, 10L, 1))
		)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUCTION_ALREADY_SOLD);
	}

	@Test
	@DisplayName("재경매 실패: 판매자가 아닌 경우 (Support 검증 실패)")
	void relistAuction_fail_seller_mismatch() {
		// given
		Auction oldAuction = Auction.builder().sellerId(100L).durationDays(1).build();
		AuctionMember stranger = AuctionMember.builder().build();
		ReflectionTestUtils.setField(stranger, "id", 999L); // 다른 ID

		given(support.getPublicMember(anyString())).willReturn(stranger);
		given(support.findAuctionById(anyLong())).willReturn(oldAuction);

		// Support가 예외를 던지도록 설정
		willThrow(new CustomException(ErrorType.UNAUTHORIZED_AUCTION_SELLER))
			.given(support).validateSeller(oldAuction, 999L);

		// when & then
		assertThatThrownBy(() ->
			auctionRelistUseCase.relistAuction(1L, "stranger", new AuctionRelistRequestDto(100L, 10L, 1))
		)
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.UNAUTHORIZED_AUCTION_SELLER);
	}
}