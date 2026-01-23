package com.bugzero.rarego.boundedContext.auth.app;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.AuthRole;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;
import com.bugzero.rarego.boundedContext.auth.out.RefreshTokenRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.security.JwtParser;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.out.AuctionApiClient;
import com.bugzero.rarego.shared.member.out.MemberApiClient;
import com.bugzero.rarego.shared.payment.out.PaymentApiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthWithdrawAccountUseCase {
    private final JwtParser jwtParser;
    private final AccountRepository accountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthAccessTokenBlacklistUseCase authAccessTokenBlacklistUseCase;
    private final MemberApiClient memberApiClient;
    private final AuctionApiClient auctionApiClient;
    private final PaymentApiClient paymentApiClient;
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

        // 탈퇴 가능 여부 확인
        validateNoActiveActivities(publicId, account.getRole());

        // 회원 정보 삭제
        memberApiClient.withdraw(publicId);

        account.softDelete();

        refreshTokenRepository.deleteByMemberPublicId(publicId);
        authAccessTokenBlacklistUseCase.blacklist(accessToken);
    }

    private void validateNoActiveActivities(String publicId, AuthRole role) {
        // 1. 내 입찰 조회 - 입찰한 경매가 모두 ENDED여야 함
        if (auctionApiClient.hasActiveBids(publicId)) {
            throw new CustomException(ErrorType.WITHDRAWAL_ACTIVE_BID_EXISTS);
        }

        // 2. 내 판매 물품 조회 - 검수/경매가 모두 완료되어야 함
        if (role == AuthRole.SELLER && auctionApiClient.hasActiveSales(publicId)) {
            throw new CustomException(ErrorType.WITHDRAWAL_ACTIVE_SALE_EXISTS);
        }

        // 3. 내 낙찰/판매 주문 결제 현황 조회 - PROCESSING 상태가 아니어야 함
        if (paymentApiClient.hasProcessingOrders(publicId)) {
            throw new CustomException(ErrorType.WITHDRAWAL_PROCESSING_ORDER_EXISTS);
        }
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
