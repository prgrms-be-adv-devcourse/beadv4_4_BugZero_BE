package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.domain.event.AuctionBidCreatedEvent;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.shared.payment.out.PaymentApiClient;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuctionCreateBidUseCase {

	private final AuctionRepository auctionRepository;
	private final BidRepository bidRepository;
	private final AuctionMemberRepository auctionMemberRepository;
	private final PaymentApiClient paymentApiClient;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public BidResponseDto createBid(Long auctionId, String memberPublicId, int bidAmount) {
		// 1. 회원 조회 (publicId -> Entity)
		AuctionMember bidder = auctionMemberRepository.findByPublicId(memberPublicId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

		// 2. 경매 조회
		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

		// 3. 유효성 검증 (AuctionMember 객체 전달)
		validateBid(auction, bidder, bidAmount);

		// TODO: 보증금 정책이 확정되면 변경 예정
		// 현재는 경매 시작 금액의 10%만 보증금으로 책정
		int depositAmount = (int) (auction.getStartPrice() * 0.1);

		// 보증금 Hold (유효성 검증 통과 후 보증금 Hold)
		// TODO: 현재 holdDeposit 같은 경우 memberId를 받기 때문에 추후에 변경 예정

		// DepositHoldResponseDto depositResponse = paymentApiClient.holdDeposit(depositAmount, memberId, auctionId);

		// 4. 현재가 갱신
		auction.updateCurrentPrice(bidAmount);

		// 5. 입찰 정보 저장 (bidder.getId() 사용)
		Bid bid = Bid.builder()
			.auctionId(auctionId)
			.bidderId(bidder.getId())
			.bidAmount(bidAmount)
			.build();

		bidRepository.save(bid);

		eventPublisher.publishEvent(
			AuctionBidCreatedEvent.of(auctionId, bidder.getId(), bidAmount)
		);

		return BidResponseDto.from(
			bid,
			bidder.getPublicId(),
			Long.valueOf(auction.getCurrentPrice())
		);
	}

	private void validateBid(Auction auction, AuctionMember bidder, int bidAmount) {
		// 경매가 진행중이 아닐 때 입찰 방지
		if (auction.getStatus() != AuctionStatus.IN_PROGRESS) {
			throw new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS, "경매가 진행 중인 상태가 아닙니다.");
		}

		// 판매자 본인 입찰 방지 (ID 비교)
		if (auction.getSellerId().equals(bidder.getId())) {
			throw new CustomException(ErrorType.AUCTION_SELLER_CANNOT_BID, "본인 경매에는 입찰할 수 없습니다.");
		}

		// 연속 입찰 방지 (현재 최고 입찰자 = 본인이면 거절)
		Optional<Bid> lastBid = bidRepository.findTopByAuctionIdOrderByBidTimeDesc(auction.getId());
		if (lastBid.isPresent() && lastBid.get().getBidderId().equals(bidder.getId())) {
			throw new CustomException(ErrorType.AUCTION_ALREADY_HIGHEST_BIDDER, "연속 입찰은 불가합니다.");
		}

		// 경매 입찰 가능한 시간인지에 대한 검증
		LocalDateTime now = LocalDateTime.now();
		if (now.isBefore(auction.getStartTime()) || now.isAfter(auction.getEndTime())) {
			throw new CustomException(ErrorType.AUCTION_NOT_IN_PROGRESS, "경매 시간이 아닙니다.");
		}

		// 입찰 금액 검증
		if (bidAmount < auction.getCurrentPrice() + auction.getTickSize()) {
			throw new CustomException(ErrorType.AUCTION_BID_AMOUNT_TOO_LOW, "입찰 금액이 유효하지 않습니다.");
		}
	}
}