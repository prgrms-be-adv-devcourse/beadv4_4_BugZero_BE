package com.bugzero.rarego.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.domain.AuctionMember;

public interface AuctionMemberRepository extends JpaRepository<AuctionMember, Long> {
	Optional<AuctionMember> findByPublicId(String publicId);
}
