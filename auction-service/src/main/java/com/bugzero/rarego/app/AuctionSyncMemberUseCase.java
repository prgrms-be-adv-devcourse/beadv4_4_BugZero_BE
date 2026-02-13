package com.bugzero.rarego.app;

import java.util.Optional;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.domain.AuctionMember;
import com.bugzero.rarego.out.AuctionMemberRepository;
import com.bugzero.rarego.shared.member.domain.MemberDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionSyncMemberUseCase {

	private final AuctionMemberRepository auctionMemberRepository;

	public AuctionMember syncMember(MemberDto member) {

		Optional<AuctionMember> existedOpt = auctionMemberRepository.findById(member.id());

		/**
		 * 이미 존재하는 객체 업데이트
		 */
		if (existedOpt.isPresent()) {
			AuctionMember existed = existedOpt.get();

			if (existed.isDeleted() && !member.deleted()) {
				log.info("[SKIP] auctionMember sync 중 삭제된 회원은 복구하지 않음. id={}", member.id());
				return existed;
			}

			existed.updateFrom(member);
			return existed;
		}

		/**
		 여기부터는 신규 가입일 때만 진행
		 **/

		AuctionMember saved =
			AuctionMember.builder()
				.id(member.id())
				.publicId(member.publicId())
				.email(member.email())
				.nickname(member.nickname())
				.intro(member.intro())
				.address(member.address())
				.addressDetail(member.addressDetail())
				.zipCode(member.zipCode())
				.contactPhone(member.contactPhone())
				.realName(member.realName())
				.createdAt(member.createdAt())
				.updatedAt(member.updatedAt())
				.deleted(member.deleted())
				.build();
		auctionMemberRepository.save(saved);
		return saved;
	}
}
