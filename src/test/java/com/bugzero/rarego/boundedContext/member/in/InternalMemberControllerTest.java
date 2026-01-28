package com.bugzero.rarego.boundedContext.member.in;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bugzero.rarego.boundedContext.member.app.MemberFacade;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.member.domain.MemberJoinRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberWithdrawRequestDto;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = InternalMemberController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
@AutoConfigureMockMvc(addFilters = false)
class InternalMemberControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private MemberFacade memberFacade;

	@Test
	@DisplayName("성공: 회원 가입 요청이 정상 처리되면 HTTP 201과 결과 데이터를 반환한다")
	void join_success() throws Exception {
		MemberJoinRequestDto requestDto = new MemberJoinRequestDto("new@example.com");
		MemberJoinResponseDto responseDto = new MemberJoinResponseDto("newbie", "public-id");

		given(memberFacade.join("new@example.com")).willReturn(responseDto);

		mockMvc.perform(post("/api/v1/internal/members/me")
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

		mockMvc.perform(post("/api/v1/internal/members/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(ErrorType.MEMBER_EMAIL_EMPTY.getHttpStatus()))
			.andExpect(jsonPath("$.message").value(ErrorType.MEMBER_EMAIL_EMPTY.getMessage()));
	}


	@Test
	@DisplayName("성공: 회원 탈퇴 요청이 정상 처리되면 HTTP 200과 결과 데이터를 반환한다")
	void withdraw_success() throws Exception {
		MemberWithdrawRequestDto requestDto = new MemberWithdrawRequestDto("public-id");
		given(memberFacade.withdraw("public-id")).willReturn("public-id");

		mockMvc.perform(post("/api/v1/internal/members/withdraw")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
			.andExpect(jsonPath("$.message").value(SuccessType.OK.getMessage()))
			.andExpect(jsonPath("$.data.publicId").value("public-id"));
	}

	@Test
	@DisplayName("실패: 이미 탈퇴한 계정이면 HTTP 403을 반환한다")
	void withdraw_fail_forbidden() throws Exception {
		MemberWithdrawRequestDto requestDto = new MemberWithdrawRequestDto("public-id");
		given(memberFacade.withdraw("public-id"))
			.willThrow(new CustomException(ErrorType.AUTH_FORBIDDEN));

		mockMvc.perform(post("/api/v1/internal/members/withdraw")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(ErrorType.AUTH_FORBIDDEN.getHttpStatus()))
			.andExpect(jsonPath("$.message").value(ErrorType.AUTH_FORBIDDEN.getMessage()));
	}
}
