package com.bugzero.rarego.boundedContext.payment.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.boundedContext.payment.domain.PaymentMember;
import com.bugzero.rarego.boundedContext.payment.domain.Wallet;
import com.bugzero.rarego.boundedContext.payment.in.dto.WalletResponseDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class PaymentGetMyWalletUseCaseTest {

	@InjectMocks
	private PaymentGetMyWalletUseCase useCase;

	@Mock
	private PaymentSupport paymentSupport;

	@Test
	@DisplayName("성공: 유효한 멤버의 지갑 정보를 DTO로 변환하여 반환한다")
	void getMyWallet_success() {
		// given
		String publicId = "member_123";
		PaymentMember mockMember = mock(PaymentMember.class);
		Wallet mockWallet = mock(Wallet.class);

		given(paymentSupport.findMemberByPublicId(publicId)).willReturn(mockMember);
		given(paymentSupport.findWalletByMember(mockMember)).willReturn(mockWallet);

		given(mockWallet.getMember()).willReturn(mockMember);
		given(mockMember.getPublicId()).willReturn(publicId);

		given(mockWallet.getBalance()).willReturn(10000);
		given(mockWallet.getHoldingAmount()).willReturn(500);

		// when
		WalletResponseDto response = useCase.getMyWallet(publicId);

		// then
		assertThat(response.balance()).isEqualTo(10000);
		assertThat(response.holdingAmount()).isEqualTo(500);

		verify(paymentSupport, times(1)).findMemberByPublicId(publicId);
		verify(paymentSupport, times(1)).findWalletByMember(mockMember);
	}

	@Test
	@DisplayName("실패: 지갑이 존재하지 않을 경우 Support에서 발생한 예외가 그대로 전파된다")
	void getMyWallet_fail_wallet_not_found() {
		// given
		String publicId = "member_123";
		PaymentMember mockMember = mock(PaymentMember.class);

		given(paymentSupport.findMemberByPublicId(publicId)).willReturn(mockMember);

		given(paymentSupport.findWalletByMember(mockMember))
			.willThrow(new CustomException(ErrorType.WALLET_NOT_FOUND));

		// when & then
		assertThatThrownBy(() -> useCase.getMyWallet(publicId))
			.isInstanceOf(CustomException.class)
			.satisfies(e -> {
				CustomException ce = (CustomException)e;
				assertThat(ce.getErrorType()).isEqualTo(ErrorType.WALLET_NOT_FOUND);
			});

		verify(paymentSupport).findWalletByMember(mockMember);
	}
}