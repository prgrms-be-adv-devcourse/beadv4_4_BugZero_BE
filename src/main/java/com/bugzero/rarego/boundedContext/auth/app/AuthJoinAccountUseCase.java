package com.bugzero.rarego.boundedContext.auth.app;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.AuthRole;
import com.bugzero.rarego.boundedContext.auth.domain.Provider;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import com.bugzero.rarego.shared.member.out.MemberApiClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthJoinAccountUseCase {
	private final AccountRepository accountRepository;
	private final MemberApiClient memberApiClient;
	// Member 서비스 호출 (동기)

	@Transactional
	public Account join(Provider provider, String providerId, String email) {
		// 1) Member 생성/조회(멱등하게) -> memberPublicId 받기
		MemberJoinResponseDto memberResponse = memberApiClient.join(email);
		String memberPublicId = memberResponse.memberPublicId();

		if (memberPublicId == null || memberPublicId.isBlank()) {
			throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
		}
		// 멱등성 처리: 이미 같은 memberPublicId가 있으면 Account 생성하지 않고 기존 Account 반환
		return accountRepository.findByMemberPublicId(memberPublicId)
			.orElseGet(() -> createAccount(provider, providerId, memberPublicId));
	}

	private Account createAccount(Provider provider, String providerId, String memberPublicId) {
		// 2) Account 생성 (provider+providerId 유니크)
		try {
			Account account = Account.builder()
				.provider(provider)
				.providerId(providerId)
				.memberPublicId(memberPublicId)
				.role(AuthRole.USER)
				.build();
			return accountRepository.save(account);
		} catch (DataIntegrityViolationException e) {
			// 동시성 문제로 이미 만들어졌다면 다시 조회해서 반환
			return accountRepository.findByProviderAndProviderId(provider, providerId)
				.orElseThrow(() -> e);
		} catch (Exception e) {
			throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
		}
	}
}
