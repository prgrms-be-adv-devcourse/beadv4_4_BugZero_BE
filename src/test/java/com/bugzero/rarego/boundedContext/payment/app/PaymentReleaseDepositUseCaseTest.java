package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.payment.domain.Deposit;
import com.bugzero.rarego.boundedContext.payment.domain.DepositStatus;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.PaymentTransaction;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.out.DepositRepository;
import com.bugzero.rarego.boundedContext.payment.out.PaymentTransactionRepository;
import com.bugzero.rarego.boundedContext.payment.out.WalletRepository;

@ExtendWith(MockitoExtension.class)
class PaymentReleaseDepositUseCaseTest {

    @InjectMocks
    private PaymentReleaseDepositUseCase paymentReleaseDepositUseCase;

    @Mock
    private DepositRepository depositRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private PaymentTransactionRepository transactionRepository;

    @Test
    @DisplayName("성공: 낙찰자 제외 보증금 환급")
    void releaseDeposits_ExcludesWinner() {
        // given
        Long auctionId = 1L;
        Long winnerId = 100L;

        PaymentMember loser1 = mock(PaymentMember.class);
        when(loser1.getId()).thenReturn(101L);
        PaymentMember loser2 = mock(PaymentMember.class);
        when(loser2.getId()).thenReturn(102L);

        Deposit deposit1 = Deposit.create(loser1, auctionId, 10000);
        Deposit deposit2 = Deposit.create(loser2, auctionId, 10000);

        Wallet wallet1 = Wallet.builder().balance(50000).holdingAmount(10000).build();
        Wallet wallet2 = Wallet.builder().balance(50000).holdingAmount(10000).build();

        when(depositRepository.findAllByAuctionIdAndStatusAndMemberIdNot(auctionId, DepositStatus.HOLD, winnerId))
                .thenReturn(List.of(deposit1, deposit2));
        when(walletRepository.findByMemberId(101L)).thenReturn(Optional.of(wallet1));
        when(walletRepository.findByMemberId(102L)).thenReturn(Optional.of(wallet2));

        // when
        paymentReleaseDepositUseCase.releaseDeposits(auctionId, winnerId);

        // then
        assertThat(deposit1.getStatus()).isEqualTo(DepositStatus.RELEASED);
        assertThat(deposit2.getStatus()).isEqualTo(DepositStatus.RELEASED);
        assertThat(wallet1.getHoldingAmount()).isEqualTo(0);
        assertThat(wallet2.getHoldingAmount()).isEqualTo(0);
        verify(transactionRepository, times(2)).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("성공: 유찰 시 전체 환급")
    void releaseDeposits_AllReleasedWhenNoWinner() {
        // given
        Long auctionId = 1L;
        Long winnerId = null; // 유찰

        PaymentMember bidder = mock(PaymentMember.class);
        when(bidder.getId()).thenReturn(101L);

        Deposit deposit = Deposit.create(bidder, auctionId, 10000);
        Wallet wallet = Wallet.builder().balance(50000).holdingAmount(10000).build();

        when(depositRepository.findAllByAuctionIdAndStatus(auctionId, DepositStatus.HOLD))
                .thenReturn(List.of(deposit));
        when(walletRepository.findByMemberId(101L)).thenReturn(Optional.of(wallet));

        // when
        paymentReleaseDepositUseCase.releaseDeposits(auctionId, winnerId);

        // then
        assertThat(deposit.getStatus()).isEqualTo(DepositStatus.RELEASED);
        assertThat(wallet.getHoldingAmount()).isEqualTo(0);
        verify(transactionRepository, times(1)).save(any(PaymentTransaction.class));
    }

    @Test
    @DisplayName("성공: 환급 대상 없음")
    void releaseDeposits_NoDepositsToRelease() {
        // given
        Long auctionId = 1L;
        Long winnerId = 100L;

        when(depositRepository.findAllByAuctionIdAndStatusAndMemberIdNot(auctionId, DepositStatus.HOLD, winnerId))
                .thenReturn(List.of());

        // when
        paymentReleaseDepositUseCase.releaseDeposits(auctionId, winnerId);

        // then
        verify(walletRepository, never()).findByMemberId(anyLong());
        verify(transactionRepository, never()).save(any());
    }
}
