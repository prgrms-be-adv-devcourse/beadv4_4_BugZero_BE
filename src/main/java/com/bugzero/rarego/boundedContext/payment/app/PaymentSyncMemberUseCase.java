package com.bugzero.rarego.boundedContext.payment.app;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.out.PaymentMemberRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;
import com.bugzero.rarego.shared.member.domain.MemberDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class PaymentSyncMemberUseCase {

	private final PaymentMemberRepository paymentMemberRepository;
	private final WalletRepository walletRepository;

	public PaymentMember syncMember(MemberDto member) {

		Optional<PaymentMember> existedOpt = paymentMemberRepository.findById(member.id());

		/**
		 * 기존 객체에 업데이트 로직
		 */
		if (existedOpt.isPresent()) {
			PaymentMember existed = existedOpt.get();

			if (existed.isDeleted() && !member.deleted()) {
				log.info("[SKIP] paymentMember sync 중 삭제된 회원은 복구하지 않음. id={}", member.id());
				return existed;
			}

			// 지연된 이벤트 (현재 updatedAt과 비교 후 느리면) 무시됨
			if (existed.getUpdatedAt() != null
				&& member.updatedAt() != null
				&& !member.updatedAt().isAfter(existed.getUpdatedAt())
				&& !(member.deleted() && !existed.isDeleted())) {

				log.info("[SKIP] paymentMember sync 중 이벤트 지연 무시됨. id={}, existedUpdatedAt={}, eventUpdatedAt={}",
					member.id(), existed.getUpdatedAt(), member.updatedAt());
				return existed; // 스킵
			}

			existed.updateFrom(member);
			softDeleteWalletIfNeeded(member);
			return existed;
		}

		/**
		 여기부터는 신규 가입일 때만 진행
		 **/

		// 1. 객체 생성
		PaymentMember saved =
			PaymentMember.builder()
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
		paymentMemberRepository.save(saved);

		// 2. Wallet 생성
		Wallet wallet = Wallet.builder()
			.member(saved)
			.build();
		walletRepository.save(wallet);

		softDeleteWalletIfNeeded(member);

		return saved;
	}

	private void softDeleteWalletIfNeeded(MemberDto member) {
		if (!member.deleted()) {
			return;
		}

		walletRepository.findByMemberId(member.id())
			.ifPresent(wallet -> {
				wallet.softDelete();
				walletRepository.save(wallet);
			});
	}
}
