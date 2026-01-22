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

		Account account = authSupport.findByPublicId(publicId);
		// 삭제된 계정 404
		if (account.isDeleted()) {
			throw new CustomException(ErrorType.AUTH_ACCOUNT_NOT_FOUND);
		}

		/**
		 * TODO:
		 * 진행중인 product, bid, payment가 있는지 확인
		 *     1. 내 입찰 조회 /members/me/bids => 내가 입찰한 BID가 있는 Auction이 모두 ENDED
		 *     2. 내 판매 물품 조회 /members/me/sales => 내가 검수 맡기고, 경매한 product 모두 완료되어 Auction ENDED
		 *     3. 내 낙찰 주문 조회 /members/me/orders => 내가 낙찰받는 주문이 AuctionOrderStatus.PROCESSING이 아니어야함
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
