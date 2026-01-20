package com.bugzero.rarego.boundedContext.auction.app;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.*;

@ExtendWith(MockitoExtension.class)
class AuctionFacadeTest {

    @InjectMocks
    private AuctionFacade auctionFacade;

    @Mock
    private AuctionCreateBidUseCase auctionCreateBidUseCase;

    @Mock
    private AuctionReadUseCase auctionReadUseCase;

    @Mock
    private AuctionBookmarkUseCase auctionBookmarkUseCase;

    @Test
    @DisplayName("입찰 생성 요청 시 UseCase를 호출하고 결과를 반환한다")
    void createBid_Success() {
        // given
        Long auctionId = 1L;
        String memberPublicId = "user_uuid";
        int bidAmount = 50000;

        BidResponseDto bidResponse = new BidResponseDto(
            1L, auctionId, "public-id", LocalDateTime.now(), (long) bidAmount, (long) bidAmount
        );

        // 앞서 UseCase가 SuccessResponseDto를 반환하도록 수정했으므로 맞춰서 Mocking
        SuccessResponseDto<BidResponseDto> expectedResponse = SuccessResponseDto.from(SuccessType.CREATED, bidResponse);

        given(auctionCreateBidUseCase.createBid(auctionId, memberPublicId, bidAmount))
            .willReturn(expectedResponse);

        // when
        SuccessResponseDto<BidResponseDto> result = auctionFacade.createBid(auctionId, memberPublicId, bidAmount);

        // then
        assertThat(result.status()).isEqualTo(SuccessType.CREATED.getHttpStatus());
        assertThat(result.data()).isEqualTo(bidResponse);
        verify(auctionCreateBidUseCase).createBid(auctionId, memberPublicId, bidAmount);
    }

    @Test
    @DisplayName("경매 입찰 기록 조회: ReadUseCase에 위임한다")
    void getBidLogs_Success() {
        // given
        Long auctionId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        BidLogResponseDto logDto = new BidLogResponseDto(1L, "user_masked", LocalDateTime.now(), 50000);
        PagedResponseDto<BidLogResponseDto> expectedResponse = new PagedResponseDto<>(
            List.of(logDto), new PageDto(1, 10, 1, 1, false, false)
        );

        given(auctionReadUseCase.getBidLogs(eq(auctionId), any(Pageable.class)))
            .willReturn(expectedResponse);

        // when
        PagedResponseDto<BidLogResponseDto> result = auctionFacade.getBidLogs(auctionId, pageable);

        // then
        assertThat(result.data()).hasSize(1);
        verify(auctionReadUseCase).getBidLogs(eq(auctionId), any(Pageable.class));
    }

    @Test
    @DisplayName("나의 입찰 내역 조회: ReadUseCase에 위임한다")
    void getMyBids_Success() {
        // given
        String memberPublicId = "user_uuid";
        Pageable pageable = PageRequest.of(0, 10);

        MyBidResponseDto myBidDto = new MyBidResponseDto(
            1L, 10L, 50L, 15000, LocalDateTime.now(), AuctionStatus.IN_PROGRESS, 15000, LocalDateTime.now().plusDays(1)
        );
        PagedResponseDto<MyBidResponseDto> expectedResponse = new PagedResponseDto<>(
            List.of(myBidDto), new PageDto(1, 10, 1, 1, false, false)
        );

        given(auctionReadUseCase.getMyBids(eq(memberPublicId), eq(null), any(Pageable.class)))
            .willReturn(expectedResponse);

        // when
        PagedResponseDto<MyBidResponseDto> result = auctionFacade.getMyBids(memberPublicId, null, pageable);

        // then
        assertThat(result.data()).hasSize(1);
        verify(auctionReadUseCase).getMyBids(eq(memberPublicId), eq(null), any(Pageable.class));
    }

    @Test
    @DisplayName("관심 경매 등록: 성공적으로 북마크를 추가한다")
    void addBookmark_Success() {
        // given
        Long auctionId = 1L;
        String publicId = "test-public-id";
        WishlistAddResponseDto responseDto = WishlistAddResponseDto.of(true, auctionId);

        given(auctionBookmarkUseCase.addBookmark(publicId, auctionId))
            .willReturn(responseDto);

        // when
        WishlistAddResponseDto result = auctionFacade.addBookmark(publicId, auctionId);

        // then
        assertThat(result.bookmarked()).isTrue();
        verify(auctionBookmarkUseCase).addBookmark(publicId, auctionId);
    }

    @Test
    @DisplayName("나의 판매 목록 조회: ReadUseCase에 위임한다")
    void getMySales_Success() {
        // given
        String sellerPublicId = "seller_uuid";
        AuctionFilterType filter = AuctionFilterType.ALL;
        Pageable pageable = PageRequest.of(0, 10);

        MySaleResponseDto saleDto = MySaleResponseDto.builder()
            .auctionId(100L)
            .title("Product 1")
            .currentPrice(1000)
            .bidCount(5)
            .tradeStatus(AuctionOrderStatus.PROCESSING)
            .build();

        PagedResponseDto<MySaleResponseDto> expectedResponse = new PagedResponseDto<>(
            List.of(saleDto), new PageDto(1, 10, 1, 1, false, false)
        );

        given(auctionReadUseCase.getMySales(eq(sellerPublicId), eq(filter), any(Pageable.class)))
            .willReturn(expectedResponse);

        // when
        PagedResponseDto<MySaleResponseDto> result = auctionFacade.getMySales(sellerPublicId, filter, pageable);

        // then
        assertThat(result.data()).hasSize(1);
        verify(auctionReadUseCase).getMySales(eq(sellerPublicId), eq(filter), any(Pageable.class));
    }
}