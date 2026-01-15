package com.bugzero.rarego.boundedContext.auction.out;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionMemberRepository extends JpaRepository<AuctionMember, Long> {
}