package com.bugzero.rarego.boundedContext.auction.app;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.shared.payment.out.PaymentApiClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionCreateBidUseCase {
	private final AuctionRepository auctionRepository;
	private final BidRepository bidRepository;
	private final AuctionMemberRepository auctionMemberRepository;
	private final ProductRepository productRepository;
	private final PaymentApiClient paymentApiClient;

	@Transactional
	public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, String memberPublicId, int bidAmount) {

		AuctionMember bidder = auctionMemberRepository.findByPublicId(memberPublicId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

		Long memberId = bidder.getId();

		// 경매 조회
		Auction auction = auctionRepository.findByIdWithLock(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

		// 상품 조회
		// 추후 Auction 에러 작성되면 교체 예정
		Product product = productRepository.findById(auction.getProductId())
			.orElseThrow(() -> new CustomException(ErrorType.INTERNAL_SERVER_ERROR));

		// 유효성 검증
		validateBid(auction, product, memberId, bidAmount);

		// TODO: 보증금 정책이 확정되면 변경 예정
		// 현재는 경매 시작 금액의 10%만 보증금으로 책정
		int depositAmount = (int) (auction.getStartPrice() * 0.1);

		// 보증금 Hold (유효성 검증 통과 후 보증금 Hold)
		DepositHoldResponseDto depositResponse = paymentApiClient.holdDeposit(depositAmount, memberId, auctionId);

		// 가격 업데이트
		if (auction.getCurrentPrice() == null || !auction.getCurrentPrice().equals(bidAmount)) {
			auction.updateCurrentPrice(bidAmount);
		}

		Bid bid = Bid.builder()
			.auctionId(auctionId)
			.bidderId(memberId)
			.bidAmount(bidAmount)
			.build();

		bidRepository.save(bid);

		BidResponseDto responseDto = BidResponseDto.from(
			bid,
			bidder.getPublicId(),
			Long.valueOf(auction.getCurrentPrice())
		);

		return SuccessResponseDto.from(SuccessType.CREATED, responseDto);
	}

	public void validateBid(Auction auction, Product product, Long memberId, int bidAmount) {
		// 경매가 진행중이 아닐 때 입찰 방지
		if (auction.getStatus() != AuctionStatus.IN_PROGRESS) {
			throw new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS, "경매가 진행 중인 상태가 아닙니다.");
		}

		// 연속 입찰 방지(현재 최고 입찰자 = 본인이면 거절)
		Optional<Bid> lastBid = bidRepository.findTopByAuctionIdOrderByBidTimeDesc(auction.getId());
		if (lastBid.isPresent() && lastBid.get().getBidderId().equals(memberId)) {
			throw new CustomException(ErrorType.AUCTION_ALREADY_HIGHEST_BIDDER, "연속 입찰은 불가합니다. (현재 최고 입찰자)");
		}

		// 판매자 본인 입찰 방지
		if (product.getSellerId() == memberId) {
			throw new CustomException(ErrorType.AUCTION_SELLER_CANNOT_BID, "본인 경매에는 입찰할 수 없습니다.");
		}

		// 경매 입찰 가능한 시간인지에 대한 검증
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(auction.getStartTime()) || now.isAfter(auction.getEndTime())) {
			throw new CustomException(ErrorType.AUCTION_TIME_INVALID, "입찰 가능한 시간이 아닙니다.");
		}

		int nextMinBid;

		if (auction.getCurrentPrice() == null) {
			nextMinBid = auction.getStartPrice();
		} else {
			nextMinBid = auction.getCurrentPrice() + auction.getTickSize();
		}

		if (bidAmount < nextMinBid) {
			throw new CustomException(ErrorType.AUCTION_BID_AMOUNT_TOO_LOW,
				String.format("최소 입찰가(%d)보다 낮습니다.", nextMinBid));
		}
	}
}
