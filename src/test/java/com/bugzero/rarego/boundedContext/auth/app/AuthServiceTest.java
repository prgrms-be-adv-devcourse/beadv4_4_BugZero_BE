package com.bugzero.rarego.boundedContext.auth.app;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class AuthServiceTest {
	@Autowired
	private AuthService authService;

	@Test
	@DisplayName("authTokenService 서비스가 존재한다.")
	void t1() {
		assertThat(authService).isNotNull();
	}
}