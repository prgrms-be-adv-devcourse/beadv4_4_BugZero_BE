package com.bugzero.rarego.boundedContext.auction.app;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;
import com.bugzero.rarego.shared.product.out.ProductApiClient;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionSupport {

	private final AuctionRepository auctionRepository;
	private final AuctionMemberRepository auctionMemberRepository;
	private final AuctionOrderRepository auctionOrderRepository;
	private final ProductApiClient productApiClient;

	public Auction findAuctionById(Long auctionId) {
		return auctionRepository.findById(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));
	}

	public Auction getAuctionByProductId(Long productId) {
		return auctionRepository.findByProductId(productId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));
	}

	@Transactional
	public Auction getAuctionWithLock(Long auctionId) {
		return auctionRepository.findByIdWithLock(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));
	}

	public AuctionMember getPublicMember(String publicId) {
		return auctionMemberRepository.findByPublicId(publicId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
	}

	public AuctionMember getMember(Long memberId) {
		return auctionMemberRepository.findById(memberId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
	}

	public ProductAuctionResponseDto getProduct(Long productId) {
		return productApiClient.getProduct(productId)
			.orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));
	}

	public AuctionOrder getOrder(Long auctionId) {
		return auctionOrderRepository.findByAuctionId(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.ORDER_NOT_FOUND));
	}

	public Optional<AuctionOrder> findOrder(Long auctionId) {
		return auctionOrderRepository.findByAuctionId(auctionId);
	}

  	public Optional<AuctionOrder> findOrderByAuctionId(Long auctionId) {
        return auctionOrderRepository.findByAuctionId(auctionId);
    }

	// 판매자 권한 검증
	public void validateSeller(Auction auction, Long memberId) {
		if (!auction.getSellerId().equals(memberId)) {
			throw new CustomException(ErrorType.UNAUTHORIZED_AUCTION_SELLER);
		}
	}

	// 경매 종료 상태 검증
	public void validateAuctionEnded(Auction auction) {
		if (auction.getStatus() != AuctionStatus.ENDED) {
			throw new CustomException(ErrorType.AUCTION_NOT_ENDED);
		}
	}

	// 경매정보 수정 가능상태 확인
	public void isAbleToChange(AuctionMember auctionMember, Auction auction) {
		if (!auction.isSeller(auctionMember.getId())) {
			throw new CustomException(ErrorType.UNAUTHORIZED_AUCTION_SELLER);
		}
		if (auction.hasStartTime()) {
			throw new CustomException(ErrorType.AUCTION_ALREADY_IN_PROGRESS);
		}
	}
}
