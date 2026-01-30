package com.bugzero.rarego.bounded_context.product.app;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.bounded_context.product.domain.ProductMember;
import com.bugzero.rarego.bounded_context.product.out.ProductMemberRepository;
import com.bugzero.rarego.shared.member.domain.MemberDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProductSyncMemberUseCase {
	private final ProductMemberRepository productMemberRepository;

	public ProductMember syncMember(MemberDto member) {

		Optional<ProductMember> existedOpt = productMemberRepository.findById(member.id());

		/**
		 * 이미 존재하는 객체 업데이트
		 */
		if (existedOpt.isPresent()) {
			ProductMember existed = existedOpt.get();

			// 지연된 이벤트 (현재 updatedAt과 비교 후 느리면) 무시됨
			if (existed.getUpdatedAt() != null
				&& member.updatedAt() != null
				&& !member.updatedAt().isAfter(existed.getUpdatedAt())) {

				log.info("[SKIP] AuctionMember sync 중 이벤트 지연 무시됨. id={}, existedUpdatedAt={}, eventUpdatedAt={}",
					member.id(), existed.getUpdatedAt(), member.updatedAt());
				return existed; // 스킵
			}

			existed.updateFrom(member);
			return existed;
		}

		/**
		 여기부터는 신규 가입일 때만 진행
		 **/

		ProductMember saved =
			ProductMember.builder()
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
		productMemberRepository.save(saved);
		return saved;
	}
}
