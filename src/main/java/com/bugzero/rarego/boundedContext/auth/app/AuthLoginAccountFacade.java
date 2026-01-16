package com.bugzero.rarego.boundedContext.auth.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.AccountDto;
import com.bugzero.rarego.boundedContext.auth.domain.Provider;
import com.bugzero.rarego.boundedContext.auth.domain.TokenIssueDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthLoginAccountFacade {
	private final AuthFindAccountUseCase authFindAccountUseCase;
	private final AuthJoinAccountUseCase authJoinAccountUseCase;
	private final AuthIssueTokenUseCase authIssueTokenUseCase;

	public String loginOrSignup(AccountDto accountDto) {
		Provider provider = accountDto.provider();
		String providerId = accountDto.providerId();
		Account account = authFindAccountUseCase
			.findByProviderAndProviderId(provider, providerId)
			.orElseGet(() -> authJoinAccountUseCase.join(provider, providerId));
		TokenIssueDto tokenIssueDto = new TokenIssueDto(account.getMemberPublicId(), account.getRole().name());
		return authIssueTokenUseCase.issueToken(tokenIssueDto, true);
	}
}
