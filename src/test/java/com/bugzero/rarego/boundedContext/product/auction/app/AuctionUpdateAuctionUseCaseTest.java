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
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;

@ExtendWith(MockitoExtension.class)
class AuctionUpdateAuctionUseCaseTest {
	@Mock
	private ProductAuctionSupport productAuctionSupport;

	@InjectMocks
	private AuctionUpdateAuctionUseCase useCase;

	// 공통 상수 관리
	private final String PUBLIC_ID = "seller-uuid";
	private final Long AUCTION_ID = 500L;
	private final Long SELLER_INTERNAL_ID = 1L;

	private AuctionMember commonSeller;
	private Auction spyAuction;


	@BeforeEach
	void setUp() {
		// 멤버 생성
		commonSeller = AuctionMember.builder()
			.id(SELLER_INTERNAL_ID)
			.publicId(PUBLIC_ID)
			.build();

		// 경매 객체 생성 및 ID 주입
		Auction auction = Auction.builder()
			.startPrice(10000)
			.durationDays(7)
			.build();
		ReflectionTestUtils.setField(auction, "id", AUCTION_ID);

		// 매 테스트마다 독립적인 상태를 보장하기 위해 Spy 객체화
		spyAuction = spy(auction);
	}


	@Test
	@DisplayName("경매 수정 성공: 유효한 요청일 경우 호가 단위를 재계산하여 수정을 완료한다")
	void updateAuction_success() {
		// given
		ProductAuctionUpdateDto updateDto = createUpdateDto(20000, 14);

		given(productAuctionSupport.getAuctionMember(PUBLIC_ID)).willReturn(commonSeller);
		given(productAuctionSupport.getAuction(AUCTION_ID)).willReturn(spyAuction);

		// when
		Long resultId = useCase.updateAuction(PUBLIC_ID, updateDto);

		// then
		assertThat(resultId).isEqualTo(AUCTION_ID);
		// 정책 반영 확인: 새로운 호가 단위(500)가 엔티티에 잘 전달되었는가
		verify(spyAuction).update(eq(14), eq(20000));
		verify(productAuctionSupport).isAbleToChange(commonSeller, spyAuction);
	}

	@Test
	@DisplayName("경매 수정 실패: 수정 불가능한 상태이면 예외가 발생한다")
	void updateAuction_fail_notAbleToChange() {
		// given
		ProductAuctionUpdateDto updateDto = createUpdateDto(20000, 14);

		given(productAuctionSupport.getAuctionMember(anyString())).willReturn(commonSeller);
		given(productAuctionSupport.getAuction(anyLong())).willReturn(spyAuction);

		// 수정 불가 예외 설정
		willThrow(new CustomException(ErrorType.AUCTION_ALREADY_IN_PROGRESS))
			.given(productAuctionSupport).isAbleToChange(any(), any());

		// when & then
		assertThatThrownBy(() -> useCase.updateAuction(PUBLIC_ID, updateDto))
			.isInstanceOf(CustomException.class)
			.hasFieldOrPropertyWithValue("errorType", ErrorType.AUCTION_ALREADY_IN_PROGRESS);

		verify(spyAuction, never()).update(anyInt(), anyInt());
	}

	// --- Helper Methods (Fixture Factory) ---
	private ProductAuctionUpdateDto createUpdateDto(int startPrice, int durationDays) {
		return new ProductAuctionUpdateDto(AUCTION_ID, startPrice, durationDays);
	}

}