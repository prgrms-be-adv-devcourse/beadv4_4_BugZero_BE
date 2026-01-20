package com.bugzero.rarego.boundedContext.auction.out;

import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuctionMemberRepository extends JpaRepository<AuctionMember, Long> {
    Optional<AuctionMember> findByPublicId(String publicId);
}