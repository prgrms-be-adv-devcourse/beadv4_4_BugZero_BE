package com.bugzero.rarego.boundedContext.auth.app;

import static com.bugzero.rarego.global.response.ErrorType.*;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.AccountDto;
import com.bugzero.rarego.boundedContext.auth.domain.AuthRole;
import com.bugzero.rarego.boundedContext.auth.domain.Provider;
import com.bugzero.rarego.boundedContext.auth.domain.TokenIssueDto;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final AuthIssueTokenUseCase authIssueTokenUseCase;
	private final AuthLoginAccountFacade authLoginAccountFacade;

	public String issueAccessToken(TokenIssueDto tokenIssueDto) {
		return authIssueTokenUseCase.issueToken(tokenIssueDto, true);
	}

	public String issueRefreshToken(TokenIssueDto tokenIssueDto) {
		return  authIssueTokenUseCase.issueToken(tokenIssueDto, false);
	}

	public String login(AccountDto accountDto) {
		return authLoginAccountFacade.loginOrSignup(accountDto);
	}
}
