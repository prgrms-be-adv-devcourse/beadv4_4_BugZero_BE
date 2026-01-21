package com.bugzero.rarego.boundedContext.auth.app;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.AccountDto;
import com.bugzero.rarego.boundedContext.auth.domain.TokenIssueDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
