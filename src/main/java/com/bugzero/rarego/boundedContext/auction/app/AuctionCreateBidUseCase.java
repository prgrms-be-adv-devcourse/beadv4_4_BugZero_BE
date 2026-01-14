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
import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionCreateBidUseCase {
	private final AuctionRepository auctionRepository;
	private final BidRepository bidRepository;
	private final MemberRepository memberRepository;
	private final AuctionMemberRepository auctionMemberRepository;
	private final ProductRepository productRepository;

	@Transactional
	public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, Long memberId, int bidAmount) {

		AuctionMember bidder = auctionMemberRepository.findById(memberId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

		/*
		// 회원 조회
		Member bidder = memberRepository.findById(memberId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
		 */

		// 경매 조회
		Auction auction = auctionRepository.findByIdWithLock(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

		// 상품 조회
		// 추후 Auction 에러 작성되면 교체 예정
		Product product = productRepository.findById(auction.getProductId())
			.orElseThrow(() -> new CustomException(ErrorType.INTERNAL_SERVER_ERROR));

		// 보증금 Hold 로직 -> 추후 PaymentService 참조해서 구현 예정

		// 유효성 검증
		validateBid(auction, product, memberId, bidAmount);

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
