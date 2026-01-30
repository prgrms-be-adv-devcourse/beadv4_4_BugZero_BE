package com.bugzero.rarego.bounded_context.auth.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.bounded_context.auth.domain.AccountDto;
import com.bugzero.rarego.bounded_context.auth.domain.TokenIssueDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthFacade {
	private final AuthIssueTokenUseCase authIssueTokenUseCase;
	private final AuthLoginAccountFacade authLoginAccountFacade;

    public String issueAccessToken(TokenIssueDto tokenIssueDto) {
        return authIssueTokenUseCase.issueToken(tokenIssueDto, true);
    }

    public String issueRefreshToken(TokenIssueDto tokenIssueDto) {
        return authIssueTokenUseCase.issueToken(tokenIssueDto, false);
    }

    public String login(AccountDto accountDto) {
        return authLoginAccountFacade.loginOrSignup(accountDto);
    }
}
