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
import com.bugzero.rarego.shared.member.domain.MemberDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionFacade {

	private final AuctionCreateBidUseCase auctionCreateBidUseCase;
	private final AuctionReadUseCase auctionReadUseCase;
	private final BidRepository bidRepository;
	private final AuctionMemberRepository auctionMemberRepository;
	private final AuctionRepository auctionRepository;
	private final ProductRepository productRepository;
	private final AuctionOrderRepository auctionOrderRepository;
	private final AuctionSyncMemberUseCase auctionSyncMemberUseCase;

	// 쓰기 작업 (입찰 생성)
	@Transactional
	public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, String memberPublicId, int bidAmount) {
		BidResponseDto result = auctionCreateBidUseCase.createBid(auctionId, memberPublicId, bidAmount);
		return SuccessResponseDto.from(SuccessType.CREATED, result);
	}

	// 읽기 작업 (입찰 기록)
	public PagedResponseDto<BidLogResponseDto> getBidLogs(Long auctionId, Pageable pageable) {
		return auctionReadUseCase.getBidLogs(auctionId, pageable);
	}

	// 내 입찰 내역
	public PagedResponseDto<MyBidResponseDto> getMyBids(String memberPublicId, AuctionStatus status, Pageable pageable) {
		return auctionReadUseCase.getMyBids(memberPublicId, status, pageable);
	}

	// 내 판매 내역
	public PagedResponseDto<MySaleResponseDto> getMySales(String memberPublicId, AuctionFilterType auctionFilterType, Pageable pageable) {
		return auctionReadUseCase.getMySales(memberPublicId, auctionFilterType, pageable);
	}

	// 경매 상세 조회
	public SuccessResponseDto<AuctionDetailResponseDto> getAuctionDetail(Long auctionId, String memberPublicId) {
		AuctionDetailResponseDto detail = auctionReadUseCase.getAuctionDetail(auctionId, memberPublicId);
		return SuccessResponseDto.from(SuccessType.OK, detail);
	}

	// 낙찰 기록 상세 조회
	public SuccessResponseDto<AuctionOrderResponseDto> getAuctionOrder(Long auctionId, String memberPublicId) {
		AuctionOrderResponseDto response = auctionReadUseCase.getAuctionOrder(auctionId, memberPublicId);
		return SuccessResponseDto.from(SuccessType.OK, response);
	}

	@Transactional
	public AuctionMember syncMember(MemberDto member) {
		return auctionSyncMemberUseCase.syncMember(member);
	}

}
