package com.bugzero.rarego.bounded_context.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.bugzero.rarego.domain.Account;
import com.bugzero.rarego.domain.AuthRole;
import com.bugzero.rarego.domain.Provider;
import com.bugzero.rarego.out.AccountRepository;
import com.bugzero.rarego.app.AuthJoinAccountUseCase;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import com.bugzero.rarego.shared.member.out.MemberApiClient;

@ExtendWith(MockitoExtension.class)
class AuthJoinAccountUseCaseTest {
	@Mock
	private AccountRepository accountRepository;

	@Mock
	private MemberApiClient memberApiClient;

	@InjectMocks
	private AuthJoinAccountUseCase authJoinAccountUseCase;

	@Test
	@DisplayName("가입 시 provider/providerId로 계정을 생성하고 USER 역할로 저장한다.")
	void joinSavesAccountWithUserRole() {
		when(accountRepository.findByMemberPublicId(anyString())).thenReturn(Optional.empty());
		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(memberApiClient.join(eq("test@example.com"), anyString()))
			.thenAnswer(invocation -> new MemberJoinResponseDto("tester", invocation.getArgument(1)));

		Account result = authJoinAccountUseCase.join(Provider.GOOGLE, "google-123", "test@example.com");

		ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
		verify(accountRepository).save(captor.capture());
		Account saved = captor.getValue();
		ArgumentCaptor<String> memberPublicIdCaptor = ArgumentCaptor.forClass(String.class);
		verify(memberApiClient).join(eq("test@example.com"), memberPublicIdCaptor.capture());
		String memberPublicId = memberPublicIdCaptor.getValue();

		assertThat(saved.getProvider()).isEqualTo(Provider.GOOGLE);
		assertThat(saved.getProviderId()).isEqualTo("google-123");
		assertThat(saved.getRole()).isEqualTo(AuthRole.USER);
		assertThatCode(() -> UUID.fromString(memberPublicId)).doesNotThrowAnyException();
		assertThat(saved.getMemberPublicId()).isEqualTo(memberPublicId);
		assertThat(result).isEqualTo(saved);
	}

	@Test
	@DisplayName("중복 저장이 발생하면 기존 계정을 조회해 반환한다.")
	void joinReturnsExistingAccountWhenDuplicate() {
		Account existing = Account.builder()
			.provider(Provider.NAVER)
			.providerId("naver-456")
			.memberPublicId("1e2c1e52-7e77-4f5d-8c4f-1a2a12b7f9aa")
			.role(AuthRole.USER)
			.build();

		when(accountRepository.findByMemberPublicId(anyString())).thenReturn(Optional.empty());
		when(accountRepository.save(any(Account.class)))
			.thenThrow(new DataIntegrityViolationException("duplicate"));
		when(accountRepository.findByProviderAndProviderId(Provider.NAVER, "naver-456"))
			.thenReturn(Optional.of(existing));
		when(memberApiClient.join(eq("naver@example.com"), anyString()))
			.thenAnswer(invocation -> new MemberJoinResponseDto("naver", invocation.getArgument(1)));

		Account result = authJoinAccountUseCase.join(Provider.NAVER, "naver-456", "naver@example.com");

		assertThat(result).isEqualTo(existing);
		verify(accountRepository).save(any(Account.class));
		verify(accountRepository).findByProviderAndProviderId(Provider.NAVER, "naver-456");
	}

	@Test
	@DisplayName("예상치 못한 예외 AUTH_JOIN_FAILED로 변환한다.")
	void joinWrapsUnexpectedException() {
		when(memberApiClient.join(eq("kakao@example.com"), anyString()))
			.thenAnswer(invocation -> new MemberJoinResponseDto("kakao", invocation.getArgument(1)));
		when(accountRepository.findByMemberPublicId(anyString())).thenReturn(Optional.empty());
		when(accountRepository.save(any(Account.class))).thenThrow(new IllegalStateException("boom"));

		assertThatThrownBy(() -> authJoinAccountUseCase.join(Provider.KAKAO, "kakao-789", "kakao@example.com"))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.AUTH_JOIN_FAILED);
	}

	@Test
	@DisplayName("이미 같은 memberPublicId의 계정이 있으면 기존 계정을 반환한다.")
	void joinReturnsExistingAccountByMemberPublicId() {
		Account existing = Account.builder()
			.provider(Provider.GOOGLE)
			.providerId("google-123")
			.memberPublicId("member-public-id")
			.role(AuthRole.USER)
			.build();

		when(memberApiClient.join(eq("test@example.com"), anyString()))
			.thenAnswer(invocation -> new MemberJoinResponseDto("tester", invocation.getArgument(1)));
		when(accountRepository.findByMemberPublicId(anyString()))
			.thenReturn(Optional.of(existing));

		Account result = authJoinAccountUseCase.join(Provider.GOOGLE, "google-123", "test@example.com");

		assertThat(result).isEqualTo(existing);
		verify(accountRepository, never()).save(any(Account.class));
	}
}
