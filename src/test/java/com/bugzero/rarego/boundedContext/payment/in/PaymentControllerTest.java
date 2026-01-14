package com.bugzero.rarego.boundedContext.payment.in;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
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

import com.bugzero.rarego.boundedContext.payment.app.PaymentFacade;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessType;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PaymentController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PaymentFacade paymentFacade;

	@Test
	@DisplayName("성공: 결제 요청이 정상 처리되면 HTTP 201과 결과 데이터를 반환한다")
	void requestPayment_success() throws Exception {
		// given
		Long memberId = 1L;
		Integer amount = 10000;
		PaymentRequestDto requestDto = new PaymentRequestDto(amount);

		PaymentRequestResponseDto responseDto = new PaymentRequestResponseDto(
			"ORDER-UUID-1234",
			amount,
			"PENDING",
			"user@example.com"
		);

		given(paymentFacade.requestPayment(eq(memberId), any(PaymentRequestDto.class)))
			.willReturn(responseDto);

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges")
				.param("memberId", String.valueOf(memberId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value(SuccessType.CREATED.getHttpStatus()))
			.andExpect(jsonPath("$.message").value(SuccessType.CREATED.getMessage()))
			.andExpect(jsonPath("$.data.orderId").value("ORDER-UUID-1234"));
	}

	@Test
	@DisplayName("실패: 존재하지 않는 회원(MEMBER_NOT_FOUND)인 경우 HTTP 404를 반환한다")
	void requestPayment_fail_member_not_found() throws Exception {
		// given
		Long memberId = 999L;
		PaymentRequestDto requestDto = new PaymentRequestDto(10000);

		given(paymentFacade.requestPayment(eq(memberId), any(PaymentRequestDto.class)))
			.willThrow(new CustomException(ErrorType.MEMBER_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges")
				.param("memberId", String.valueOf(memberId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(ErrorType.MEMBER_NOT_FOUND.getHttpStatus()))
			.andExpect(jsonPath("$.message").value(ErrorType.MEMBER_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("실패: 유효하지 않은 파라미터(Validation)인 경우 HTTP 400을 반환한다")
	void requestPayment_fail_validation() throws Exception {
		// given
		Long memberId = 1L;

		PaymentRequestDto invalidRequest = new PaymentRequestDto(-500);

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges")
				.param("memberId", String.valueOf(memberId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}
}