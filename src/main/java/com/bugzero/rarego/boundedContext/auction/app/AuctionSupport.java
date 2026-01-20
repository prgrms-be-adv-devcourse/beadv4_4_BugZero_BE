package com.bugzero.rarego.boundedContext.auction.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionSupport {

	private final AuctionRepository auctionRepository;
	private final AuctionMemberRepository auctionMemberRepository;
	private final ProductRepository productRepository;
	private final AuctionOrderRepository auctionOrderRepository;

	public Auction getAuction(Long auctionId) {
		return auctionRepository.findById(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));
	}

	@Transactional
	public Auction getAuctionWithLock(Long auctionId) {
		return auctionRepository.findByIdWithLock(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));
	}

	public AuctionMember getMember(String publicId) {
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

}
