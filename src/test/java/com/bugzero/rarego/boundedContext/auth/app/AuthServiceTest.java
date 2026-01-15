package com.bugzero.rarego.boundedContext.auth.app;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.util.ReflectionTestUtils.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
	@Mock
	private AuthIssueTokenUseCase authIssueTokenUseCase;

	@InjectMocks
	private AuthService authService;

	@BeforeEach
	void setUp() throws Exception {
		setField(authIssueTokenUseCase, "accessTokenExpireSeconds", 3600);
		setField(authIssueTokenUseCase, "refreshTokenExpireSeconds", 7200);
	}

	@Test
	@DisplayName("authTokenService 서비스가 존재한다.")
	void t1() {
		assertThat(authService).isNotNull();
	}
}
