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
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionSupport {

	private final AuctionRepository auctionRepository;
	private final AuctionMemberRepository auctionMemberRepository;
	private final ProductRepository productRepository;
	private final AuctionOrderRepository auctionOrderRepository;

	public Auction findAuctionById(Long auctionId) {
		return auctionRepository.findById(auctionId)
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

	public Product getProduct(Long productId) {
		return productRepository.findById(productId)
			.orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));
	}

	public AuctionOrder getOrder(Long auctionId) {
		return auctionOrderRepository.findByAuctionId(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.ORDER_NOT_FOUND));
	}

	public Optional<AuctionOrder> findOrder(Long auctionId) {
		return auctionOrderRepository.findByAuctionId(auctionId);
	}
  
  	public AuctionMember findMemberByPublicId(String publicId) {
        return auctionMemberRepository.findByPublicId(publicId)
                .orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
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
}
