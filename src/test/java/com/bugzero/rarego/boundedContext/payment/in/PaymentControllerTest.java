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
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
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

	@Test
	@DisplayName("성공: 결제 승인이 정상 처리되면 HTTP 200과 최종 잔액을 반환한다")
	void confirmPayment_success() throws Exception {
		// given
		Long memberId = 1L;
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("paymentKey_123", "ORDER-001", 10000);

		// Mock 응답 생성
		PaymentConfirmResponseDto responseDto = new PaymentConfirmResponseDto("ORDER-001", 10000, 50000);

		// Facade 메서드 Mocking
		given(paymentFacade.confirmPayment(eq(memberId), any(PaymentConfirmRequestDto.class)))
			.willReturn(responseDto);

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.param("memberId", String.valueOf(memberId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isOk()) // 200 OK
			.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
			.andExpect(jsonPath("$.data.orderId").value("ORDER-001"))
			.andExpect(jsonPath("$.data.balance").value(50000));
	}

	@Test
	@DisplayName("실패: 결제 소유자가 일치하지 않는 경우(PAYMENT_OWNER_MISMATCH) HTTP 403을 반환한다")
	void confirmPayment_fail_owner_mismatch() throws Exception {
		// given
		Long hackerId = 999L;
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", "ORDER-001", 10000);

		// 예외 던지기 설정
		given(paymentFacade.confirmPayment(eq(hackerId), any(PaymentConfirmRequestDto.class)))
			.willThrow(new CustomException(ErrorType.PAYMENT_OWNER_MISMATCH));

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.param("memberId", String.valueOf(hackerId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isForbidden()) // 403 Forbidden (ErrorType 정의에 따라 다름)
			.andExpect(jsonPath("$.message").value(ErrorType.PAYMENT_OWNER_MISMATCH.getMessage()));
	}

	@Test
	@DisplayName("실패: 결제 승인 요청 파라미터가 유효하지 않으면(Validation) HTTP 400을 반환한다")
	void confirmPayment_fail_validation() throws Exception {
		// given
		Long memberId = 1L;
		// orderId가 비어있고, amount가 음수인 잘못된 요청
		PaymentConfirmRequestDto invalidRequest = new PaymentConfirmRequestDto("key", "", -100);

		// when & then (Facade 호출 전 Controller 레벨에서 검증 실패)
		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.param("memberId", String.valueOf(memberId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andDo(print())
			.andExpect(status().isBadRequest()) // 400 Bad Request
			.andExpect(jsonPath("$.status").value(400));
	}

	@Test
	@DisplayName("실패: 이미 처리된 결제(ALREADY_PROCESSED)인 경우 HTTP 409(Conflict)를 반환한다")
	void confirmPayment_fail_already_processed() throws Exception {
		// given
		Long memberId = 1L;
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", "ORDER-001", 10000);

		// 이미 처리된 결제 예외 발생 상황 Mocking
		given(paymentFacade.confirmPayment(eq(memberId), any(PaymentConfirmRequestDto.class)))
			.willThrow(new CustomException(ErrorType.ALREADY_PROCESSED_PAYMENT));

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.param("memberId", String.valueOf(memberId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isConflict()) // ★ 핵심: 409 Conflict 확인
			.andExpect(jsonPath("$.status").value(ErrorType.ALREADY_PROCESSED_PAYMENT.getHttpStatus()))
			.andExpect(jsonPath("$.message").value(ErrorType.ALREADY_PROCESSED_PAYMENT.getMessage()));
	}

	@Test
	@DisplayName("실패: 주문 정보를 찾을 수 없는 경우(PAYMENT_NOT_FOUND) HTTP 404를 반환한다")
	void confirmPayment_fail_not_found() throws Exception {
		// given
		Long memberId = 1L;
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", "WRONG-ORDER-ID", 10000);

		given(paymentFacade.confirmPayment(eq(memberId), any(PaymentConfirmRequestDto.class)))
			.willThrow(new CustomException(ErrorType.PAYMENT_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.param("memberId", String.valueOf(memberId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isNotFound()) // ★ 핵심: 404 Not Found 확인
			.andExpect(jsonPath("$.status").value(404));
	}

	@Test
	@DisplayName("실패: 외부 PG사 오류 등으로 승인 실패 시 HTTP 400을 반환한다")
	void confirmPayment_fail_pg_error() throws Exception {
		// given
		Long memberId = 1L;
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", "ORDER-001", 10000);

		// Toss API 에러 상황 Mocking
		given(paymentFacade.confirmPayment(eq(memberId), any(PaymentConfirmRequestDto.class)))
			.willThrow(new CustomException(ErrorType.PAYMENT_CONFIRM_FAILED));

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.param("memberId", String.valueOf(memberId))
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isBadRequest()) // ★ 핵심: 400 Server Error 확인
			.andExpect(jsonPath("$.status").value(400));
	}
}