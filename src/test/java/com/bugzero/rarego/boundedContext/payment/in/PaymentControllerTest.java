package com.bugzero.rarego.boundedContext.payment.in;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bugzero.rarego.boundedContext.payment.app.PaymentFacade;
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.AuctionFinalPaymentResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentConfirmResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.PaymentRequestResponseDto;
import com.bugzero.rarego.boundedContext.payment.in.dto.WalletTransactionResponseDto;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = PaymentController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
class PaymentControllerTest {
	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private PaymentFacade paymentFacade;

	@MockitoBean
	private JobOperator jobOperator;

	@MockitoBean
	private Job settlementJob;

	private Authentication createAuth(String publicId, String role) {
		MemberPrincipal principal = new MemberPrincipal(publicId, role);
		return new UsernamePasswordAuthenticationToken(principal, null,
			List.of(new SimpleGrantedAuthority("ROLE_" + role)));
	}

	// ==================== 결제 요청 API 테스트 ====================

	@Test
	@DisplayName("성공: 결제 요청이 정상 처리되면 HTTP 201과 결과 데이터를 반환한다")
	void requestPayment_success() throws Exception {
		// given
		String publicId = "1"; // memberId 역할을 하는 publicId
		Integer amount = 10000;
		PaymentRequestDto requestDto = new PaymentRequestDto(amount);
		PaymentRequestResponseDto responseDto = new PaymentRequestResponseDto("ORDER-UUID-1234", amount, "PENDING",
			"user@example.com");

		given(paymentFacade.requestPayment(eq(publicId), any(PaymentRequestDto.class))).willReturn(responseDto);

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges")
				.with(authentication(createAuth(publicId, "USER"))) // memberId 파라미터 대신 인증 주입
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.orderId").value("ORDER-UUID-1234"));
	}

	@Test
	@DisplayName("실패: 존재하지 않는 회원(MEMBER_NOT_FOUND)인 경우 HTTP 404를 반환한다")
	void requestPayment_fail_member_not_found() throws Exception {
		// given
		String publicId = "999";
		PaymentRequestDto requestDto = new PaymentRequestDto(10000);

		given(paymentFacade.requestPayment(eq(publicId), any(PaymentRequestDto.class)))
			.willThrow(new CustomException(ErrorType.MEMBER_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges")
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
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
		String publicId = "1";

		PaymentRequestDto invalidRequest = new PaymentRequestDto(-500);

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges")
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.status").value(400));
	}

	// ==================== 결제 승인 API 테스트 ====================

	@Test
	@DisplayName("성공: 결제 승인이 정상 처리되면 HTTP 200과 최종 잔액을 반환한다")
	void confirmPayment_success() throws Exception {
		// given
		String publicId = "1";
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("paymentKey_123", "ORDER-001", 10000);
		PaymentConfirmResponseDto responseDto = new PaymentConfirmResponseDto("ORDER-001", 10000, 50000);

		given(paymentFacade.confirmPayment(eq(publicId), any(PaymentConfirmRequestDto.class))).willReturn(responseDto);

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.balance").value(50000));
	}

	@Test
	@DisplayName("실패: 결제 소유자가 일치하지 않는 경우 HTTP 403을 반환한다")
	void confirmPayment_fail_owner_mismatch() throws Exception {
		String hackerId = "hacker-999";
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", "ORDER-001", 10000);

		given(paymentFacade.confirmPayment(eq(hackerId), any()))
			.willThrow(new CustomException(ErrorType.PAYMENT_OWNER_MISMATCH));

		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.with(authentication(createAuth(hackerId, "USER")))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isForbidden());
	}

	@Test
	@DisplayName("실패: 결제 승인 요청 파라미터가 유효하지 않으면(Validation) HTTP 400을 반환한다")
	void confirmPayment_fail_validation() throws Exception {
		// given
		String publicId = "1";
		// orderId가 비어있고, amount가 음수인 잘못된 요청
		PaymentConfirmRequestDto invalidRequest = new PaymentConfirmRequestDto("key", "", -100);

		// when & then (Facade 호출 전 Controller 레벨에서 검증 실패)
		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
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
		String publicId = "1";
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", "ORDER-001", 10000);

		given(paymentFacade.confirmPayment(eq(publicId), any(PaymentConfirmRequestDto.class)))
			.willThrow(new CustomException(ErrorType.ALREADY_PROCESSED_PAYMENT));

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.status").value(409));
	}

	@Test
	@DisplayName("실패: 주문 정보를 찾을 수 없는 경우(PAYMENT_NOT_FOUND) HTTP 404를 반환한다")
	void confirmPayment_fail_not_found() throws Exception {
		// given
		String publicId = "1";
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", "WRONG-ORDER-ID", 10000);

		given(paymentFacade.confirmPayment(eq(publicId), any(PaymentConfirmRequestDto.class)))
			.willThrow(new CustomException(ErrorType.PAYMENT_NOT_FOUND));

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
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
		String publicId = "1";
		PaymentConfirmRequestDto requestDto = new PaymentConfirmRequestDto("key", "ORDER-001", 10000);

		// Toss API 에러 상황 Mocking
		given(paymentFacade.confirmPayment(eq(publicId), any(PaymentConfirmRequestDto.class)))
			.willThrow(new CustomException(ErrorType.PAYMENT_CONFIRM_FAILED));

		// when & then
		mockMvc.perform(post("/api/v1/payments/charges/confirm")
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isBadRequest()) // ★ 핵심: 400 Server Error 확인
			.andExpect(jsonPath("$.status").value(400));
	}

	// ==================== 낙찰 결제 API 테스트 ====================

	@Test
	@DisplayName("성공: 낙찰 결제가 정상 처리되면 HTTP 200과 결과 데이터를 반환한다")
	void auctionFinalPayment_success() throws Exception {
		// given
		String publicId = "1";
		Long auctionId = 100L;
		AuctionFinalPaymentRequestDto requestDto = new AuctionFinalPaymentRequestDto(
			"홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");

		AuctionFinalPaymentResponseDto responseDto = AuctionFinalPaymentResponseDto.of(
			1L, auctionId, "uuid-member-1", 100000, 10000, 100000, java.time.LocalDateTime.now());

		given(paymentFacade.auctionFinalPayment(eq(publicId), eq(auctionId), any(AuctionFinalPaymentRequestDto.class)))
			.willReturn(responseDto);

		// when & then
		mockMvc.perform(post("/api/v1/payments/auctions/{auctionId}", auctionId)
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
			.andExpect(jsonPath("$.data.auctionId").value(auctionId))
			.andExpect(jsonPath("$.data.status").value("PAID"));
	}

	@Test
	@DisplayName("실패: 낙찰자가 아닌 경우(NOT_AUCTION_WINNER) HTTP 403을 반환한다")
	void auctionFinalPayment_fail_not_winner() throws Exception {
		// given
		String publicId = "1";
		Long auctionId = 100L;
		AuctionFinalPaymentRequestDto requestDto = new AuctionFinalPaymentRequestDto(
			"홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");

		given(paymentFacade.auctionFinalPayment(eq(publicId), eq(auctionId), any(AuctionFinalPaymentRequestDto.class)))
			.willThrow(new CustomException(ErrorType.NOT_AUCTION_WINNER));

		// when & then
		mockMvc.perform(post("/api/v1/payments/auctions/{auctionId}", auctionId)
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.message").value(ErrorType.NOT_AUCTION_WINNER.getMessage()));
	}

	@Test
	@DisplayName("실패: 주문 상태가 올바르지 않은 경우(INVALID_ORDER_STATUS) HTTP 409를 반환한다")
	void auctionFinalPayment_fail_invalid_order_status() throws Exception {
		// given
		String publicId = "1";
		Long auctionId = 100L;
		AuctionFinalPaymentRequestDto requestDto = new AuctionFinalPaymentRequestDto(
			"홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");

		given(paymentFacade.auctionFinalPayment(eq(publicId), eq(auctionId), any(AuctionFinalPaymentRequestDto.class)))
			.willThrow(new CustomException(ErrorType.INVALID_ORDER_STATUS));

		// when & then
		mockMvc.perform(post("/api/v1/payments/auctions/{auctionId}", auctionId)
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.message").value(ErrorType.INVALID_ORDER_STATUS.getMessage()));
	}

	@Test
	@DisplayName("실패: 잔액이 부족한 경우(INSUFFICIENT_BALANCE) HTTP 400을 반환한다")
	void auctionFinalPayment_fail_insufficient_balance() throws Exception {
		// given
		String publicId = "1";
		Long auctionId = 100L;
		AuctionFinalPaymentRequestDto requestDto = new AuctionFinalPaymentRequestDto(
			"홍길동", "010-1234-5678", "12345", "서울시", "101호", "문앞");

		given(paymentFacade.auctionFinalPayment(eq(publicId), eq(auctionId), any(AuctionFinalPaymentRequestDto.class)))
			.willThrow(new CustomException(ErrorType.INSUFFICIENT_BALANCE));

		// when & then
		mockMvc.perform(post("/api/v1/payments/auctions/{auctionId}", auctionId)
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andDo(print())
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message").value(ErrorType.INSUFFICIENT_BALANCE.getMessage()));
	}

	// ==================== 지갑 내역 조회 API 테스트 ====================

	@Test
	@DisplayName("성공: 지갑 거래 내역 조회 시 기본 페이징 파라미터와 null 날짜 조건이 적용된다")
	void getWalletTransactions_success_default() throws Exception {
		// given
		String publicId = "1";

		// Mock 응답 데이터 생성 (빈 페이지)
		PagedResponseDto<WalletTransactionResponseDto> emptyPageResponse =
			new PagedResponseDto<>(java.util.Collections.emptyList(),
				new com.bugzero.rarego.global.response.PageDto(1, 10, 0, 0, false, false));

		// Facade 호출 스텁: page=0, size=10, type=null, from=null, to=null (기본값)
		// 인자가 4개에서 6개로 늘어났으므로 eq(), isNull() 등을 맞춰줘야 함
		given(paymentFacade.getWalletTransactions(
			eq(publicId),
			eq(0),
			eq(10),
			isNull(), // type
			isNull(), // from
			isNull()  // to
		)).willReturn(emptyPageResponse);

		// when & then
		mockMvc.perform(get("/api/v1/payments/me/wallet-transactions")
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
				// page, size, transactionType, from, to 생략 -> 기본값/null 적용 확인
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
			.andExpect(jsonPath("$.data.data").isArray());
	}

	@Test
	@DisplayName("성공: 거래 유형(Type)과 날짜 조건(from, to)을 지정하여 지갑 내역을 조회한다")
	void getWalletTransactions_success_filtering() throws Exception {
		// given
		String publicId = "1";
		int page = 1;
		int size = 5;
		com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType type =
			com.bugzero.rarego.boundedContext.payment.domain.WalletTransactionType.TOPUP_DONE;

		// 날짜 조건 설정
		java.time.LocalDate fromDate = java.time.LocalDate.of(2024, 1, 1);
		java.time.LocalDate toDate = java.time.LocalDate.of(2024, 1, 31);

		// Mock 응답 데이터 생성
		PagedResponseDto<WalletTransactionResponseDto> mockResponse =
			new PagedResponseDto<>(java.util.Collections.emptyList(),
				new com.bugzero.rarego.global.response.PageDto(page + 1, size, 0, 0, false, false));

		// Facade 호출 스텁: 파라미터가 정확히 전달되는지 검증
		given(paymentFacade.getWalletTransactions(
			eq(publicId),
			eq(page),
			eq(size),
			eq(type),
			eq(fromDate), // ★ LocalDate 객체 매칭 확인
			eq(toDate)    // ★ LocalDate 객체 매칭 확인
		)).willReturn(mockResponse);

		// when & then
		mockMvc.perform(get("/api/v1/payments/me/wallet-transactions")
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
				.param("page", String.valueOf(page))
				.param("size", String.valueOf(size))
				.param("transactionType", type.name()) // Enum -> String
				.param("from", fromDate.toString())    // "2024-01-01"
				.param("to", toDate.toString())        // "2024-01-31"
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()));
	}

	@Test
	@DisplayName("실패: 날짜 포맷이 올바르지 않으면(yyyy-MM-dd 아님) HTTP 400을 반환한다")
	void getWalletTransactions_fail_invalid_date() throws Exception {
		// given
		String publicId = "1";
		String invalidDate = "2024/01/01"; // 잘못된 포맷

		// when & then
		mockMvc.perform(get("/api/v1/payments/me/wallet-transactions")
				.with(authentication(createAuth(publicId, "USER")))
				.with(csrf())
				.param("from", invalidDate)
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}

	// ==================== 정산 내역 조회 API 테스트 ====================

	@Test
	@DisplayName("성공: 정산 내역 조회 시 필수 파라미터(memberId)만 있으면 기본값이 적용된다")
	void getSettlements_success_default() throws Exception {
		// given
		String publicId = "1";

		// Mock 응답 생성 (빈 페이지)
		PagedResponseDto<com.bugzero.rarego.boundedContext.payment.in.dto.SettlementResponseDto> emptyResponse =
			new PagedResponseDto<>(java.util.Collections.emptyList(),
				new com.bugzero.rarego.global.response.PageDto(1, 10, 0, 0, false, false));

		// Facade 호출 스텁 검증
		// page=0, size=10 (기본값)
		// status=null, from=null, to=null (선택값)
		given(paymentFacade.getSettlements(eq(publicId), eq(0), eq(10), isNull(), isNull(), isNull()))
			.willReturn(emptyResponse);

		// when & then
		mockMvc.perform(get("/api/v1/payments/me/settlements")
				.with(authentication(createAuth(publicId, "SELLER")))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
			.andExpect(jsonPath("$.data.data").isArray());
	}

	@Test
	@DisplayName("성공: 날짜(ISO Date)와 상태 조건을 포함하여 정산 내역을 조회한다")
	void getSettlements_success_filtering() throws Exception {
		// given
		String publicId = "1";
		int page = 0;
		int size = 20;
		com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus status = com.bugzero.rarego.boundedContext.payment.domain.SettlementStatus.DONE;
		java.time.LocalDate fromDate = java.time.LocalDate.of(2024, 1, 1);
		java.time.LocalDate toDate = java.time.LocalDate.of(2024, 1, 31);

		given(paymentFacade.getSettlements(eq(publicId), eq(page), eq(size), eq(status), eq(fromDate), eq(toDate)))
			.willReturn(new PagedResponseDto<>(Collections.emptyList(), new PageDto(1, 20, 0, 0, false, false)));

		// when & then
		mockMvc.perform(get("/api/v1/payments/me/settlements")
				.with(authentication(createAuth(publicId, "SELLER"))) // 권한 체크 통과를 위해 SELLER 주입
				.with(csrf())
				.param("page", String.valueOf(page))
				.param("size", String.valueOf(size))
				.param("status", status.name())
				.param("from", fromDate.toString())
				.param("to", toDate.toString())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("실패: 판매자 권한이 없는 경우(AUTH_FORBIDDEN) HTTP 403을 반환한다")
	void getSettlements_fail_not_seller() throws Exception {
		// given
		String publicId = "1";

		// when & then
		mockMvc.perform(get("/api/v1/payments/me/settlements")
				.with(authentication(createAuth(publicId, "USER"))) // SELLER가 아닌 유저
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.status").value(403));
	}

	@Test
	@DisplayName("실패: 날짜 포맷이 올바르지 않으면(yyyy-MM-dd 아님) HTTP 400을 반환한다")
	void getSettlements_fail_invalid_date_format() throws Exception {
		// given
		String publicId = "1";

		// when & then
		mockMvc.perform(get("/api/v1/payments/me/settlements")
				.with(authentication(createAuth(publicId, "SELLER")))
				.with(csrf())
				.param("from", "2024/01/01")
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isBadRequest());
	}
}
