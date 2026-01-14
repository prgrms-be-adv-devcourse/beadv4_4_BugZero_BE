package com.bugzero.rarego.boundedContext.payment.in;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;

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
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.shared.payment.dto.DepositHoldRequestDto;
import com.bugzero.rarego.shared.payment.dto.DepositHoldResponseDto;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = InternalPaymentController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
@AutoConfigureMockMvc(addFilters = false)
class InternalPaymentControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private PaymentFacade paymentFacade;

        @Test
        @DisplayName("성공: 보증금 홀드 요청이 정상 처리되면 HTTP 201과 결과 데이터를 반환한다")
        void holdDeposit_Success() throws Exception {
                // given
                DepositHoldRequestDto request = new DepositHoldRequestDto(20000, 4L, 3L);
                DepositHoldResponseDto response = new DepositHoldResponseDto(
                                1L, 3L, 20000, "HOLD", LocalDateTime.now());

                given(paymentFacade.holdDeposit(any(DepositHoldRequestDto.class)))
                                .willReturn(response);

                // when & then
                mockMvc.perform(post("/api/v1/internal/payments/deposits/hold")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andDo(print())
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.status").value(SuccessType.CREATED.getHttpStatus()))
                                .andExpect(jsonPath("$.message").value(SuccessType.CREATED.getMessage()))
                                .andExpect(jsonPath("$.data.depositId").value(1))
                                .andExpect(jsonPath("$.data.auctionId").value(3))
                                .andExpect(jsonPath("$.data.amount").value(20000))
                                .andExpect(jsonPath("$.data.status").value("HOLD"));
        }

        @Test
        @DisplayName("실패: 필수 파라미터가 누락되면 HTTP 400을 반환한다")
        void holdDeposit_Fail_ValidationError() throws Exception {
                // given - amount 누락
                String invalidRequest = """
                                {
                                	"memberId": 4,
                                	"auctionId": 3
                                }
                                """;

                // when & then
                mockMvc.perform(post("/api/v1/internal/payments/deposits/hold")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidRequest))
                                .andDo(print())
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400));
        }
}
