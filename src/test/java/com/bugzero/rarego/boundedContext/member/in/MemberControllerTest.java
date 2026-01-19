package com.bugzero.rarego.boundedContext.member.in;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.member.app.MemberFacade;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.AuctionFilterType;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MySaleResponseDto;
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

    @Autowired
    private MemberController memberController;

    @MockitoBean
    private AuctionFacade auctionFacade;

    @MockitoBean
    private MemberFacade memberFacade;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(
                new PageableHandlerMethodArgumentResolver(),
                new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return UserDetails.class.isAssignableFrom(parameter.getParameterType());
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        return User.withUsername("2")
                            .password("password")
                            .roles("USER")
                            .build();
                    }
                },
                new HandlerMethodArgumentResolver() {
                    @Override
                    public boolean supportsParameter(MethodParameter parameter) {
                        return MemberPrincipal.class.isAssignableFrom(parameter.getParameterType());
                    }

                    @Override
                    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                        NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                        // [수정 2] MemberPrincipal 생성자 수정 (String publicId, String role)
                        // 기존 코드: new MemberPrincipal(2L, "2", List.of()) -> 에러 발생
                        return new MemberPrincipal("2", "ROLE_SELLER"); 
                    }
                }
            )
            .build();
    }

    @Test
    @DisplayName("GET /members/me/bids - 내 입찰 내역 조회 성공")
    void getMyBids_success() throws Exception {
        Long memberId = 2L;
        MyBidResponseDto myBidDto = new MyBidResponseDto(
            1L, 1L, 10L, 10000L, LocalDateTime.now(), AuctionStatus.IN_PROGRESS, 10000L, LocalDateTime.now().plusDays(1)
        );

        PagedResponseDto<MyBidResponseDto> response = new PagedResponseDto<>(
            List.of(myBidDto), new PageDto(1, 10, 1, 1, false, false)
        );

        given(auctionFacade.getMyBids(eq(memberId), eq(null), any(Pageable.class)))
            .willReturn(response);

        mockMvc.perform(get("/api/v1/members/me/bids")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].bidAmount").value(10000))
            .andExpect(jsonPath("$.data[0].auctionStatus").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("GET /members/me/sales - 내 판매 목록 조회 성공")
    void getMySales_success() throws Exception {
        Long memberId = 2L;
        MySaleResponseDto mySaleDto = MySaleResponseDto.builder()
            .auctionId(10L)
            .title("내 판매 상품")
            .currentPrice(20000)
            .bidCount(5)
            .tradeStatus(AuctionOrderStatus.PROCESSING)
            .build();

        PagedResponseDto<MySaleResponseDto> response = new PagedResponseDto<>(
            List.of(mySaleDto), new PageDto(1, 10, 1, 1, false, false)
        );

        given(auctionFacade.getMySales(eq(memberId), eq(AuctionFilterType.ALL), any(Pageable.class)))
            .willReturn(response);

        mockMvc.perform(get("/api/v1/members/me/sales")
                .param("filter", "ALL")
                .contentType(MediaType.APPLICATION_JSON))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].title").value("내 판매 상품"))
            .andExpect(jsonPath("$.data[0].bidCount").value(5))
            .andExpect(jsonPath("$.data[0].tradeStatus").value("PROCESSING"));
    }

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
    @DisplayName("성공: 내 입찰 목록 조회 (파라미터 포함) 성공")
    @WithMockUser(username = "2", roles = "USER")
    void getMyBids_with_status_success() throws Exception {
        MyBidResponseDto bid = new MyBidResponseDto(
            10L,
            20L,
            30L,
            4000L,
            LocalDateTime.of(2024, 1, 1, 10, 0),
            AuctionStatus.IN_PROGRESS,
            5000L,
            LocalDateTime.of(2024, 1, 2, 10, 0)
        );
        PagedResponseDto<MyBidResponseDto> response = new PagedResponseDto<>(
            List.of(bid),
            new PageDto(1, 20, 1, 1, false, false)
        );

        given(auctionFacade.getMyBids(eq(2L), eq(AuctionStatus.IN_PROGRESS), any(Pageable.class)))
            .willReturn(response);

        mockMvc.perform(get("/api/v1/members/me/bids")
                .param("auctionStatus", "IN_PROGRESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].auctionId").value(20L))
            .andExpect(jsonPath("$.pageDto.totalItems").value(1));
    }
}
