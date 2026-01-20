package com.bugzero.rarego.boundedContext.member.in;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.member.app.MemberFacade;
import com.bugzero.rarego.boundedContext.member.domain.MemberClearField;
import com.bugzero.rarego.boundedContext.member.domain.MemberMeResponseDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateRequestDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateResponseDto;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.member.domain.MemberJoinRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = MemberController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
@AutoConfigureMockMvc(addFilters = false)
class MemberControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuctionFacade auctionFacade;

	@MockitoBean
	private MemberFacade memberFacade;

	@Test
	@DisplayName("성공: 회원 가입 요청이 정상 처리되면 HTTP 201과 결과 데이터를 반환한다")
	void join_success() throws Exception {
		MemberJoinRequestDto requestDto = new MemberJoinRequestDto("new@example.com");
		MemberJoinResponseDto responseDto = new MemberJoinResponseDto("newbie", "public-id");

		given(memberFacade.join("new@example.com")).willReturn(responseDto);

		mockMvc.perform(post("/api/v1/members/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value(SuccessType.CREATED.getHttpStatus()))
			.andExpect(jsonPath("$.message").value(SuccessType.CREATED.getMessage()))
			.andExpect(jsonPath("$.data.memberPublicId").value("public-id"));
	}

	@Test
	@DisplayName("실패: 이메일이 비어있으면 HTTP 400을 반환한다")
	void join_fail_empty_email() throws Exception {
		MemberJoinRequestDto requestDto = new MemberJoinRequestDto("");

		given(memberFacade.join("")).willThrow(new CustomException(ErrorType.MEMBER_EMAIL_EMPTY));

		mockMvc.perform(post("/api/v1/members/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(ErrorType.MEMBER_EMAIL_EMPTY.getHttpStatus()))
			.andExpect(jsonPath("$.message").value(ErrorType.MEMBER_EMAIL_EMPTY.getMessage()));
	}

	@Test
	@DisplayName("성공: 내 정보 조회 요청이 정상 처리되면 회원 정보를 반환한다")
	void getMe_success() throws Exception {
		MemberMeResponseDto responseDto = new MemberMeResponseDto(
			"public-id",
			"USER",
			"test@example.com",
			"tester",
			"intro",
			"Seoul",
			"Apt 1",
			"12345",
			"010****5678",
			"A***e",
			LocalDateTime.of(2024, 1, 1, 0, 0),
			LocalDateTime.of(2024, 1, 2, 0, 0)
		);

		given(memberFacade.getMe("public-id", "USER")).willReturn(responseDto);

		MemberPrincipal principal = new MemberPrincipal("public-id", "USER");
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			principal,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);
		try {
			mockMvc.perform(get("/api/v1/members/me"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
				.andExpect(jsonPath("$.message").value(SuccessType.OK.getMessage()))
				.andExpect(jsonPath("$.data.publicId").value("public-id"))
				.andExpect(jsonPath("$.data.role").value("USER"))
				.andExpect(jsonPath("$.data.email").value("test@example.com"))
				.andExpect(jsonPath("$.data.contactPhoneMasked").value("010****5678"))
				.andExpect(jsonPath("$.data.realNameMasked").value("A***e"));
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	@DisplayName("성공: 내 정보 수정 요청이 정상 처리되면 수정된 회원 정보를 반환한다")
	void updateMe_success() throws Exception {
		// given
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			"newbie",
			"intro",
			"12345",
			"Seoul",
			"Apt 1",
			null,
			null,
			null
		);
		MemberUpdateResponseDto responseDto = new MemberUpdateResponseDto(
			"public-id",
			"test@example.com",
			"newbie",
			"intro",
			"Seoul",
			"Apt 1",
			"12345",
			"01012345678",
			"Alice",
			LocalDateTime.of(2024, 1, 1, 0, 0),
			LocalDateTime.of(2024, 1, 2, 0, 0)
		);
		given(memberFacade.updateMe("public-id", "USER", requestDto)).willReturn(responseDto);

		MemberPrincipal principal = new MemberPrincipal("public-id", "USER");
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			principal,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);
		try {
			// when
			var result = mockMvc.perform(patch("/api/v1/members/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)));

			// then
			result
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
				.andExpect(jsonPath("$.message").value(SuccessType.OK.getMessage()))
				.andExpect(jsonPath("$.data.publicId").value("public-id"))
				.andExpect(jsonPath("$.data.nickname").value("newbie"));
		} finally {
			SecurityContextHolder.clearContext();
		}
	}

	@Test
	@DisplayName("실패: clearFields와 patch가 동시에 오면 HTTP 400을 반환한다")
	void updateMe_fail_clear_and_patch_conflict() throws Exception {
		// given
		MemberUpdateRequestDto requestDto = new MemberUpdateRequestDto(
			"newbie",
			"intro",
			null,
			null,
			null,
			null,
			null,
			Set.of(MemberClearField.INTRO)
		);
		given(memberFacade.updateMe("public-id", "USER", requestDto))
			.willThrow(new CustomException(ErrorType.MEMBER_UPDATED_FAILED));

		MemberPrincipal principal = new MemberPrincipal("public-id", "USER");
		Authentication authentication = new UsernamePasswordAuthenticationToken(
			principal,
			null,
			List.of(new SimpleGrantedAuthority("ROLE_USER"))
		);

		// when
		SecurityContextHolder.getContext().setAuthentication(authentication);
		try {
			var result = mockMvc.perform(patch("/api/v1/members/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)));

			// then
			result
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(ErrorType.MEMBER_UPDATED_FAILED.getHttpStatus()))
				.andExpect(jsonPath("$.message").value(ErrorType.MEMBER_UPDATED_FAILED.getMessage()));
		} finally {
			SecurityContextHolder.clearContext();
		}
	}
}
