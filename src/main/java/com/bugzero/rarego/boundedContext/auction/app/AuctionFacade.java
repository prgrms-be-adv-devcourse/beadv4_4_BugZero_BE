package com.bugzero.rarego.boundedContext.auction.app;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionFacade {

	private final AuctionCreateBidUseCase auctionCreateBidUseCase;
	private final BidRepository bidRepository;

	@Transactional
	public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, Long memberId, int bidAmount) {
		return auctionCreateBidUseCase.createBid(auctionId, memberId, bidAmount);
	}

	// 경매 입찰 기록 조회
	public PagedResponseDto<BidLogResponseDto> getBidLogs(Long auctionId, Pageable pageable) {
		Page<Bid> bidPage = bidRepository.findAllByAuctionIdOrderByBidTimeDesc(auctionId, pageable);

		Page<BidLogResponseDto> dtoPage = bidPage.map(BidLogResponseDto::from);

		return new PagedResponseDto<>(
			dtoPage.getContent(),
			PageDto.from(dtoPage)
		);
	}

	// 나의 입찰 내역 조회
	public PagedResponseDto<MyBidResponseDto> getMyBids(Long memberId, AuctionStatus status ,Pageable pageable) {
		Page<Bid> bidPage = bidRepository.findMyBids(memberId, status, pageable);

		Page<MyBidResponseDto> dtoPage = bidPage.map(bid -> MyBidResponseDto.from(bid, bid.getAuction()));

		return new PagedResponseDto<>(
			dtoPage.getContent(),
			PageDto.from(dtoPage)
		);
	}

}
