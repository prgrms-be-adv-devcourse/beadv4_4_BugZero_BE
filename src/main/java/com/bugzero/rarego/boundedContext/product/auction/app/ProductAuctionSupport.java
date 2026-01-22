package com.bugzero.rarego.boundedContext.product.auction.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductAuctionSupport {
	private final AuctionMemberRepository auctionMemberRepository;
	private final AuctionRepository auctionRepository;

	public AuctionMember getAuctionMember(String publicId) {
		return auctionMemberRepository.findByPublicId(publicId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
	}


	public Auction getAuction(Long auctionId) {
		return auctionRepository.findByIdAndDeletedIsFalse(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));
	}

	public void isAbleToChange(AuctionMember auctionMember, Auction auction) {
		if (!auction.isSeller(auctionMember.getId())) {
			throw new CustomException(ErrorType.UNAUTHORIZED_AUCTION_SELLER);
		}
		if (!auction.isPending()) {
			throw new CustomException(ErrorType.AUCTION_ALREADY_IN_PROGRESS);
		}
	}

	// 시작가에 따라 호가단위 결정
	public int determineTickSize(int startPrice) {
		if (startPrice < 10000) {
			return 500;
		} else if (startPrice < 50000) {
			return 1000;
		} else if (startPrice < 100000) {
			return 2000;
		} else if (startPrice < 300000) {
			return 5000;
		} else if (startPrice < 1000000) {
			return 10000;
		} else {
			return 30000; // 100만 원 이상
		}
	}

}
