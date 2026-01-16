package com.bugzero.rarego.boundedContext.payment.in;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.out.PaymentMemberRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@Profile("dev")
public class PaymentDataInit {
	private final PaymentDataInit self;
	private final PaymentMemberRepository paymentMemberRepository;
	private final WalletRepository walletRepository;

	public PaymentDataInit(@Lazy PaymentDataInit self, PaymentMemberRepository paymentMemberRepository,
		WalletRepository walletRepository) {
		this.self = self;
		this.paymentMemberRepository = paymentMemberRepository;
		this.walletRepository = walletRepository;
	}

	@Bean
	public ApplicationRunner paymentBaseInitDataRunner() {
		return args -> {
			self.makeBasePaymentMember();
		};
	}

	public void makeBasePaymentMember() {
		if (paymentMemberRepository.count() > 0)
			return;

		PaymentMember member = PaymentMember.builder()
			.id(1L)
			.publicId(UUID.randomUUID().toString())
			.email("test@bugzero.com")
			.nickname("테스트유저")
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();

		PaymentMember system = PaymentMember.builder()
			.id(2L)
			.publicId(UUID.randomUUID().toString())
			.email("system@bugzero.com")
			.nickname("시스템")
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();

		member = paymentMemberRepository.save(member);
		system = paymentMemberRepository.save(system);

		Wallet wallet = Wallet.builder()
			.member(member)
			.build();

		Wallet systemWallet = Wallet.builder()
			.member(system)
			.build();

		walletRepository.save(wallet);
		walletRepository.save(systemWallet);
	}
}
