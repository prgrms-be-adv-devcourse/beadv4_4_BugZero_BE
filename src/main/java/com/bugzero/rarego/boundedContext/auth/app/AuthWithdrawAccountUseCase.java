package com.bugzero.rarego.boundedContext.auth.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;
import com.bugzero.rarego.boundedContext.auth.out.RefreshTokenRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.security.JwtParser;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.member.out.MemberApiClient;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthWithdrawAccountUseCase {
	private final JwtParser jwtParser;
	private final AccountRepository accountRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final AuthAccessTokenBlacklistUseCase authAccessTokenBlacklistUseCase;
	private final MemberApiClient memberApiClient;
	private final AuthSupport authSupport;

	@Transactional
	public void withdraw(String accessToken) {
		String publicId = extractPublicId(accessToken);

		Account account = authSupport.findByMemberPublicId(publicId);
		if (account.isDeleted()) {
			throw new CustomException(ErrorType.AUTH_ACCOUNT_DELETED);
		}

		/**
		 * TODO:
		 * 진행중인 product, bid, payment가 있는지 확인
		 *     1. 내 상품 조회 /members/me/products
		 *     2. 내 입찰 조회 /members/me/bids
		 *     3. 내 판매 물품 조회 /members/me/sales
		 *     4. 내 낙찰 주문 조회 /members/me/orders
		 */

		memberApiClient.withdraw(publicId);

		account.softDelete();
		accountRepository.save(account);

		refreshTokenRepository.deleteByMemberPublicId(publicId);
		authAccessTokenBlacklistUseCase.blacklist(accessToken);
	}

	private String extractPublicId(String accessToken) {
		if (accessToken == null || accessToken.isBlank()) {
			throw new CustomException(ErrorType.AUTH_UNAUTHORIZED);
		}

		MemberPrincipal principal = jwtParser.parsePrincipal(accessToken);
		if (principal == null || principal.publicId() == null || principal.publicId().isBlank()) {
			throw new CustomException(ErrorType.AUTH_UNAUTHORIZED);
		}
		return principal.publicId();
	}
}
