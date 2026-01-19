package com.bugzero.rarego.boundedContext.auth.in;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bugzero.rarego.boundedContext.auth.app.AuthService;
import com.bugzero.rarego.boundedContext.auth.domain.TokenIssueDto;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = AuthController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

	@Test
	@DisplayName("성공: 테스트 로그인 요청 시 토큰을 반환한다")
	void login_success() throws Exception {
		TokenIssueDto request = new TokenIssueDto("public-id", "USER");

		given(authService.issueAccessToken(request)).willReturn("access-token");

		mockMvc.perform(post("/api/v1/auth/test/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(request)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
			.andExpect(jsonPath("$.data").value("access-token"));
	}

	@Test
	@DisplayName("성공: 인증 확인 요청 시 현재 사용자 정보를 반환한다")
	void check_success() throws Exception {
		MemberPrincipal principal = new MemberPrincipal("public-id", "USER");
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			principal,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);
		try {
			mockMvc.perform(get("/api/v1/auth/test/check"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
				.andExpect(jsonPath("$.data").value("MemberPrincipal[publicId=public-id, role=USER]"));
		} finally {
			SecurityContextHolder.clearContext();
		}
	}
}
