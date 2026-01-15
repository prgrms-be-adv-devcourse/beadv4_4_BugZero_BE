package com.bugzero.rarego.boundedContext.auth.in;

import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.AuthRole;
import com.bugzero.rarego.boundedContext.auth.domain.Provider;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@Profile("dev")
public class AuthDataInit {
	private final AuthDataInit self;
	private final AccountRepository accountRepository;

	public AuthDataInit(@Lazy AuthDataInit self, AccountRepository accountRepository) {
		this.self = self;
		this.accountRepository = accountRepository;
	}

	@Bean
	public ApplicationRunner authBaseInitDataRunner() {
		return args -> self.makeBaseAccounts();
	}

	public void makeBaseAccounts() {
		if (accountRepository.count() > 0) {
			log.info("이미 Auth Account 데이터가 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		String newMemberPublicId1 = UUID.randomUUID().toString();
		String newMemberPublicId2 = UUID.randomUUID().toString();
		String newMemberPublicId3 = UUID.randomUUID().toString();

		accountRepository.save(Account.builder()
			.memberPublicId(newMemberPublicId1)
			.role(AuthRole.USER)
			.provider(Provider.GOOGLE)
			.providerId("google-1")
			.build());

		accountRepository.save(Account.builder()
			.memberPublicId(newMemberPublicId2)
			.role(AuthRole.SELLER)
			.provider(Provider.KAKAO)
			.providerId("kakao-2")
			.build());

		accountRepository.save(Account.builder()
			.memberPublicId(newMemberPublicId3)
			.role(AuthRole.ADMIN)
			.provider(Provider.NAVER)
			.providerId("naver-3")
			.build());

		log.info("Auth Account 테스트 데이터 초기화 완료");
	}
}
