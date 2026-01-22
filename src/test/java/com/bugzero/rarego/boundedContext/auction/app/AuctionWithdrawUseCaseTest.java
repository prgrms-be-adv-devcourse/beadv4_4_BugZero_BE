package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.in.dto.AuctionWithdrawResponseDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuctionWithdrawUseCaseTest {

    @InjectMocks
    private AuctionWithdrawUseCase auctionWithdrawUseCase;

    @Mock
    private AuctionSupport auctionSupport;

    private final String PUBLIC_ID = "test-public-id";
    private final Long MEMBER_ID = 1L;
    private final Long AUCTION_ID = 100L;

    @Test
    @DisplayName("성공: 유찰된 경매(주문 없음)는 판매 포기가 가능하다")
    void execute_success_no_order() {
        AuctionMember seller = createMember(MEMBER_ID);
        Auction auction = createAuction(MEMBER_ID, AuctionStatus.ENDED, true);

        given(auctionSupport.findMemberByPublicId(PUBLIC_ID)).willReturn(seller);
        given(auctionSupport.findAuctionById(AUCTION_ID)).willReturn(auction);
        given(auctionSupport.findOrderByAuctionId(AUCTION_ID)).willReturn(Optional.empty());

        AuctionWithdrawResponseDto result = auctionWithdrawUseCase.execute(AUCTION_ID, PUBLIC_ID);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("성공: 결제 실패(FAILED)한 주문이 있는 경매는 판매 포기가 가능하다")
    void execute_success_failed_order() {
        AuctionMember seller = createMember(MEMBER_ID);
        Auction auction = createAuction(MEMBER_ID, AuctionStatus.ENDED, true);

        AuctionOrder failedOrder = AuctionOrder.builder()
                .finalPrice(0)
                .build();
        failedOrder.fail();

        given(auctionSupport.findMemberByPublicId(PUBLIC_ID)).willReturn(seller);
        given(auctionSupport.findAuctionById(AUCTION_ID)).willReturn(auction);
        given(auctionSupport.findOrderByAuctionId(AUCTION_ID)).willReturn(Optional.of(failedOrder));

        auctionWithdrawUseCase.execute(AUCTION_ID, PUBLIC_ID);

        assertThat(auction.getStatus()).isEqualTo(AuctionStatus.WITHDRAWN);
    }

    @Test
    @DisplayName("실패: 판매자 본인이 아니면 예외가 발생한다")
    void execute_fail_not_seller() {
        AuctionMember notSeller = createMember(2L);
        Auction auction = createAuction(MEMBER_ID, AuctionStatus.ENDED, true);

        given(auctionSupport.findMemberByPublicId(PUBLIC_ID)).willReturn(notSeller);
        given(auctionSupport.findAuctionById(AUCTION_ID)).willReturn(auction);

        assertThatThrownBy(() -> auctionWithdrawUseCase.execute(AUCTION_ID, PUBLIC_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorType.AUCTION_NOT_SELLER.getMessage());
    }

    @Test
    @DisplayName("실패: 검수 전인 경매는 포기할 수 없다")
    void execute_fail_not_inspected() {
        AuctionMember seller = createMember(MEMBER_ID);
        Auction auction = createAuction(MEMBER_ID, AuctionStatus.SCHEDULED, false);

        given(auctionSupport.findMemberByPublicId(PUBLIC_ID)).willReturn(seller);
        given(auctionSupport.findAuctionById(AUCTION_ID)).willReturn(auction);

        assertThatThrownBy(() -> auctionWithdrawUseCase.execute(AUCTION_ID, PUBLIC_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorType.AUCTION_WITHDRAW_NOT_INSPECTED.getMessage());
    }

    @Test
    @DisplayName("실패: 결제 완료(SUCCESS)된 주문이 있으면 포기할 수 없다")
    void execute_fail_already_paid() {
        AuctionMember seller = createMember(MEMBER_ID);
        Auction auction = createAuction(MEMBER_ID, AuctionStatus.ENDED, true);

        AuctionOrder successOrder = AuctionOrder.builder()
                .finalPrice(0)
                .build();
        successOrder.complete();

        given(auctionSupport.findMemberByPublicId(PUBLIC_ID)).willReturn(seller);
        given(auctionSupport.findAuctionById(AUCTION_ID)).willReturn(auction);
        given(auctionSupport.findOrderByAuctionId(AUCTION_ID)).willReturn(Optional.of(successOrder));

        assertThatThrownBy(() -> auctionWithdrawUseCase.execute(AUCTION_ID, PUBLIC_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorType.AUCTION_WITHDRAW_ALREADY_PAID.getMessage());
    }

    @Test
    @DisplayName("실패: 결제 대기(PROCESSING) 중인 주문이 있으면 포기할 수 없다")
    void execute_fail_payment_in_progress() {
        AuctionMember seller = createMember(MEMBER_ID);
        Auction auction = createAuction(MEMBER_ID, AuctionStatus.ENDED, true);

        AuctionOrder processingOrder = AuctionOrder.builder()
                .finalPrice(0)
                .build();

        given(auctionSupport.findMemberByPublicId(PUBLIC_ID)).willReturn(seller);
        given(auctionSupport.findAuctionById(AUCTION_ID)).willReturn(auction);
        given(auctionSupport.findOrderByAuctionId(AUCTION_ID)).willReturn(Optional.of(processingOrder));

        assertThatThrownBy(() -> auctionWithdrawUseCase.execute(AUCTION_ID, PUBLIC_ID))
                .isInstanceOf(CustomException.class)
                .hasMessage(ErrorType.AUCTION_WITHDRAW_PAYMENT_IN_PROGRESS.getMessage());
    }

    // --- Helper Methods ---

    private AuctionMember createMember(Long id) {
        return AuctionMember.builder()
                .id(id)
                .publicId(PUBLIC_ID)
                .build();
    }

    private Auction createAuction(Long sellerId, AuctionStatus status, boolean isInspected) {
        Auction auction = Auction.builder()
                .productId(50L)
                .sellerId(sellerId)
                .durationDays(7)
                .startTime(isInspected ? LocalDateTime.now().minusDays(1) : null)
                .build();

        if (status == AuctionStatus.IN_PROGRESS || status == AuctionStatus.ENDED) {
            auction.forceStartForTest();
        }
        if (status == AuctionStatus.ENDED) {
            auction.end();
        }
        return auction;
    }
}