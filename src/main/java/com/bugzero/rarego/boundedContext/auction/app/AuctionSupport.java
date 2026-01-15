package com.bugzero.rarego.boundedContext.auction.app;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionSupport {

	private final AuctionRepository auctionRepository;
	private final BidRepository bidRepository;
	private final MemberRepository memberRepository;

	public Auction findAuctionById(Long auctionId) {
		return auctionRepository.findById(auctionId).orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));
	}

	public Page<Bid> findBidsByAuctionId(Long auctionId, Pageable pageable) {
		return bidRepository.findAllByAuctionIdOrderByBidTimeDesc(auctionId, pageable);
	}

	// 차후의 N+1 문제 방지를 위한 bulk 조회
	public Map<Long, String> findPublicIdsByMemberIds(Set<Long> memberIds) {
		List<Member> members = memberRepository.findAllById(memberIds);
		return members.stream()
			.collect(Collectors.toMap(Member::getId, Member::getPublicId));
	}

}
