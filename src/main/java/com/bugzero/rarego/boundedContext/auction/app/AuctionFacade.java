package com.bugzero.rarego.boundedContext.auction.app;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionFacade {

	private final AuctionCreateBidUseCase auctionCreateBidUseCase;
	private final BidRepository bidRepository;
	private final AuctionMemberRepository auctionMemberRepository;
	private final AuctionRepository auctionRepository;
	private final ProductRepository productRepository;

	@Transactional
	public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, Long memberId, int bidAmount) {
		return auctionCreateBidUseCase.createBid(auctionId, memberId, bidAmount);
	}

	// 경매 입찰 기록 조회
	public PagedResponseDto<BidLogResponseDto> getBidLogs(Long auctionId, Pageable pageable) {
		Page<Bid> bidPage = bidRepository.findAllByAuctionIdOrderByBidTimeDesc(auctionId, pageable);

		// 1. 입찰자 정보 Map 생성 (Helper 사용)
		Map<Long, String> bidderMap = getBidderPublicIdMap(bidPage.getContent());

		// 2. DTO 변환
		Page<BidLogResponseDto> dtoPage = bidPage.map(bid -> {
			String publicId = bidderMap.get(bid.getBidderId());
			if (publicId == null) {
				log.warn("입찰자 정보 없음: bidderId={}", bid.getBidderId());
				publicId = "unknown";
			}
			return BidLogResponseDto.from(bid, publicId);
		});


		return new PagedResponseDto<>(dtoPage.getContent(), PageDto.from(dtoPage));
	}

	// 나의 입찰 내역 조회
	public PagedResponseDto<MyBidResponseDto> getMyBids(Long memberId, AuctionStatus status, Pageable pageable) {
		// 1. Bid 목록 조회
		Page<Bid> bidPage = bidRepository.findMyBids(memberId, status, pageable);
		List<Bid> bids = bidPage.getContent();

		// 2. Auction Map 생성 (Helper 메서드 재사용)
		Map<Long, Auction> auctionMap = getAuctionMap(bids);

		// 3. DTO 변환 (상품명 매핑 과정 생략)
		Page<MyBidResponseDto> dtoPage = bidPage.map(bid -> {
			Auction auction = auctionMap.get(bid.getAuctionId());

			// 데이터 무결성 체크 (Auction이 없는 경우 예외 발생)
			if (auction == null) {
				throw new CustomException(ErrorType.AUCTION_NOT_FOUND);
			}

			return MyBidResponseDto.from(bid, auction);
		});

		return new PagedResponseDto<>(dtoPage.getContent(), PageDto.from(dtoPage));
	}


	// =========================================================================
	//  Helper Methods (Private) - 복잡한 조회/매핑 로직을 아래로 숨김
	// =========================================================================

	// 입찰 목록에서 입찰자 ID를 추출하여 PublicId Map 반환
	private Map<Long, String> getBidderPublicIdMap(List<Bid> bids) {
		if (bids.isEmpty()) return Collections.emptyMap();

		Set<Long> bidderIds = bids.stream()
			.map(Bid::getBidderId)
			.collect(Collectors.toSet());

		return auctionMemberRepository.findAllById(bidderIds).stream()
			.collect(Collectors.toMap(AuctionMember::getId, AuctionMember::getPublicId));
	}

	// 입찰 목록에서 경매 ID를 추출하여 Auction Map 반환
	private Map<Long, Auction> getAuctionMap(List<Bid> bids) {
		if (bids.isEmpty()) return Collections.emptyMap();

		Set<Long> auctionIds = bids.stream()
			.map(Bid::getAuctionId)
			.collect(Collectors.toSet());

		return auctionRepository.findAllById(auctionIds).stream()
			.collect(Collectors.toMap(Auction::getId, Function.identity()));
	}

}
