package com.bugzero.rarego.boundedContext.product.auction.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class AuctionDeleteAuctionUseCaseTest {

	@Mock
	private ProductAuctionSupport productAuctionSupport;

	@InjectMocks
	private AuctionDeleteAuctionUseCase useCase;

	private final String PUBLIC_ID = "seller-uuid";
	private final Long PRODUCT_ID = 100L;

	private AuctionMember commonSeller;
	private Auction spyAuction;

	@BeforeEach
	void setUp() {
		// 공통 판매자 객체 생성
		commonSeller = AuctionMember.builder()
			.id(1L)
			.publicId(PUBLIC_ID)
			.build();

		// 공통 경매 객체 생성 (softDelete 호출 여부 확인을 위해 spy 사용)
		Auction auction = Auction.builder()
			.startPrice(10000)
			.durationDays(7)
			.build();

		spyAuction = spy(auction);
	}

	@Test
	@DisplayName("성공: 유효한 권한을 가진 사용자가 삭제 요청 시 경매가 소프트 삭제된다")
	void deleteAuction_Success() {
		// given
		given(productAuctionSupport.getAuctionMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productAuctionSupport.getAuctionByProductId(PRODUCT_ID)).willReturn(spyAuction);

		// isAbleToChange는 void이므로 문제 없으면 통과

		// when
		useCase.deleteAuction(PUBLIC_ID, PRODUCT_ID);

		// then
		// 1. 권한 체크가 정상적으로 수행되었는가
		verify(productAuctionSupport).isAbleToChange(commonSeller, spyAuction);

		// 2. 엔티티의 softDelete 로직이 호출되었는가 (핵심 비즈니스 로직)
		verify(spyAuction).softDelete();
	}

	@Test
	@DisplayName("실패: 삭제 가능한 상태가 아니면 예외가 발생하고 삭제 로직이 실행되지 않는다")
	void deleteAuction_Fail_NotAbleToChange() {
		// given
		given(productAuctionSupport.getAuctionMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productAuctionSupport.getAuctionByProductId(PRODUCT_ID)).willReturn(spyAuction);

		// 삭제 불가 예외 시뮬레이션
		willThrow(new CustomException(ErrorType.AUCTION_DELETE_FAILED))
			.given(productAuctionSupport).isAbleToChange(any(), any());

		// when & then
		assertThatThrownBy(() -> useCase.deleteAuction(PUBLIC_ID, PRODUCT_ID))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_DELETE_FAILED);

		// 예외 발생 시 softDelete는 절대로 호출되면 안 됨
		verify(spyAuction, never()).softDelete();
	}

}