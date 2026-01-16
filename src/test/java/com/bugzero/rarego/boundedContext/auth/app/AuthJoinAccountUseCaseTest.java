package com.bugzero.rarego.boundedContext.auth.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.AuthRole;
import com.bugzero.rarego.boundedContext.auth.domain.Provider;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;
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
		when(accountRepository.findByMemberPublicId("member-public-id")).thenReturn(Optional.empty());
		when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(memberApiClient.join("test@example.com"))
			.thenReturn(new MemberJoinResponseDto("tester", "member-public-id"));

		Account result = authJoinAccountUseCase.join(Provider.GOOGLE, "google-123", "test@example.com");

		ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);
		verify(accountRepository).save(captor.capture());
		Account saved = captor.getValue();

		assertThat(saved.getProvider()).isEqualTo(Provider.GOOGLE);
		assertThat(saved.getProviderId()).isEqualTo("google-123");
		assertThat(saved.getRole()).isEqualTo(AuthRole.USER);
		assertThat(saved.getMemberPublicId()).isEqualTo("member-public-id");
		assertThat(result).isEqualTo(saved);
		verify(memberApiClient).join("test@example.com");
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

		when(accountRepository.findByMemberPublicId("member-public-id")).thenReturn(Optional.empty());
		when(accountRepository.save(any(Account.class)))
			.thenThrow(new DataIntegrityViolationException("duplicate"));
		when(accountRepository.findByProviderAndProviderId(Provider.NAVER, "naver-456"))
			.thenReturn(Optional.of(existing));
		when(memberApiClient.join("naver@example.com"))
			.thenReturn(new MemberJoinResponseDto("naver", "member-public-id"));

		Account result = authJoinAccountUseCase.join(Provider.NAVER, "naver-456", "naver@example.com");

		assertThat(result).isEqualTo(existing);
		verify(accountRepository).save(any(Account.class));
		verify(accountRepository).findByProviderAndProviderId(Provider.NAVER, "naver-456");
		verify(memberApiClient).join("naver@example.com");
	}

	@Test
	@DisplayName("예상치 못한 예외는 INTERNAL_SERVER_ERROR로 변환한다.")
	void joinWrapsUnexpectedException() {
		when(memberApiClient.join("kakao@example.com"))
			.thenReturn(new MemberJoinResponseDto("kakao", "member-public-id"));
		when(accountRepository.findByMemberPublicId("member-public-id")).thenReturn(Optional.empty());
		when(accountRepository.save(any(Account.class))).thenThrow(new IllegalStateException("boom"));

		assertThatThrownBy(() -> authJoinAccountUseCase.join(Provider.KAKAO, "kakao-789", "kakao@example.com"))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.INTERNAL_SERVER_ERROR);
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

		when(memberApiClient.join("test@example.com"))
			.thenReturn(new MemberJoinResponseDto("tester", "member-public-id"));
		when(accountRepository.findByMemberPublicId("member-public-id"))
			.thenReturn(Optional.of(existing));

		Account result = authJoinAccountUseCase.join(Provider.GOOGLE, "google-123", "test@example.com");

		assertThat(result).isEqualTo(existing);
		verify(accountRepository, never()).save(any(Account.class));
	}
}
