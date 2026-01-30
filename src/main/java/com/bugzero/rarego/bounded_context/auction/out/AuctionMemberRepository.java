package com.bugzero.rarego.bounded_context.auction.out;

import java.util.Optional;

import com.bugzero.rarego.bounded_context.auction.domain.AuctionMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionMemberRepository extends JpaRepository<AuctionMember, Long> {
    Optional<AuctionMember> findByPublicId(String publicId);
}