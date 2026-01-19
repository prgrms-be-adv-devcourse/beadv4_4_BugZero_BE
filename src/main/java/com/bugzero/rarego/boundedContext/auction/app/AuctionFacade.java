package com.bugzero.rarego.boundedContext.auction.app;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.auction.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionFilterType;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MySaleResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionFacade {

	private final AuctionCreateBidUseCase auctionCreateBidUseCase;
	private final AuctionReadUseCase auctionReadUseCase;

	// 쓰기 작업
	@Transactional
	public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, Long memberId, int bidAmount) {
		BidResponseDto result = auctionCreateBidUseCase.createBid(auctionId, memberId, bidAmount);
		return SuccessResponseDto.from(SuccessType.CREATED, result);
	}

	// 읽기 작업
	public PagedResponseDto<BidLogResponseDto> getBidLogs(Long auctionId, Pageable pageable) {
		return auctionReadUseCase.getBidLogs(auctionId, pageable);
	}

	public PagedResponseDto<MyBidResponseDto> getMyBids(Long memberId, AuctionStatus status, Pageable pageable) {
		return auctionReadUseCase.getMyBids(memberId, status, pageable);
	}

	public PagedResponseDto<MySaleResponseDto> getMySales(Long memberId, AuctionFilterType auctionFilterType, Pageable pageable) {
		return auctionReadUseCase.getMySales(memberId, auctionFilterType, pageable);
	}

	public SuccessResponseDto<AuctionDetailResponseDto> getAuctionDetail(Long auctionId) {
		AuctionDetailResponseDto detail = auctionReadUseCase.getAuctionDetail(auctionId);
		return SuccessResponseDto.from(SuccessType.OK, detail);
	}

	public SuccessResponseDto<AuctionOrderResponseDto> getAuctionOrder(Long auctionId, Long memberId) {
		AuctionOrderResponseDto response = auctionReadUseCase.getAuctionOrder(auctionId, memberId);
		return SuccessResponseDto.from(SuccessType.OK, response);
	}
}