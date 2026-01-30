package com.bugzero.rarego.bounded_context.auction.in;

import com.bugzero.rarego.bounded_context.auction.app.AuctionFacade;
import com.bugzero.rarego.bounded_context.auction.domain.AuctionStatus;
import com.bugzero.rarego.bounded_context.auction.in.dto.AuctionBookmarkListResponseDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.exception.GlobalExceptionHandler;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.auction.dto.AuctionListResponseDto;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuctionMemberControllerTest {

    @InjectMocks
    private AuctionMemberController auctionMemberController;

    @Mock
    private AuctionFacade auctionFacade;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(auctionMemberController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return MemberPrincipal.class.isAssignableFrom(parameter.getParameterType());
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                                return new MemberPrincipal("test-public-id", "USER");
                            }
                        }
                )
                .build();
    }

    @Test
    @DisplayName("성공: 내 입찰 목록을 조회하면 페이징 결과를 반환한다")
    void getMyBids_success() throws Exception {
        // given
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

        given(auctionFacade.getMyBids(eq("test-public-id"), eq(AuctionStatus.IN_PROGRESS), any(Pageable.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/members/me/bids")
                        .param("auctionStatus", "IN_PROGRESS"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].auctionId").value(20L))
                .andExpect(jsonPath("$.pageDto.totalItems").value(1));
    }

    @Test
    @DisplayName("성공: 내 관심 경매 목록 조회 시 200 OK와 중첩된 경매 정보를 반환한다")
    void getMyBookmarks_success() throws Exception {
        // given
        // 1. 공통 경매 정보 (내부 객체) 생성
        AuctionListResponseDto auctionInfo = new AuctionListResponseDto(
                100L,                   // auctionId
                500L,                   // productId
                "레고 밀레니엄 팔콘",      // productName
                "https://image.com/1",  // thumbnailUrl
                "STAR_WARS",            // category
                850000,                 // currentPrice
                800000,                 // startPrice
                15,                     // bidsCount
                AuctionStatus.IN_PROGRESS,
                LocalDateTime.of(2026, 1, 30, 23, 59) // endTime
        );

        // 2. 관심 목록 DTO (외부 객체) 생성 - Composition 구조
        AuctionBookmarkListResponseDto auctionBookmarkListResponseDto = AuctionBookmarkListResponseDto.of(
                1L,          // bookmarkId
                auctionInfo  // 중첩된 경매 정보
        );

        PagedResponseDto<AuctionBookmarkListResponseDto> response = new PagedResponseDto<>(
                List.of(auctionBookmarkListResponseDto),
                new PageDto(1, 10, 1, 1, false, false)
        );

        // Mocking: publicId와 상관없이 응답하도록 설정
        given(auctionFacade.getMyBookmarks(anyString(), any(Pageable.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/members/me/bookmarks")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                // 최상위 bookmarkId 검증
                .andExpect(jsonPath("$.data[0].bookmarkId").value(1L))

                // auctionInfo 내부 계층 검증 (도연님의 record 필드명과 일치시켜야 함)
                .andExpect(jsonPath("$.data[0].auctionInfo.auctionId").value(100L))
                .andExpect(jsonPath("$.data[0].auctionInfo.productId").value(500L))
                .andExpect(jsonPath("$.data[0].auctionInfo.productName").value("레고 밀레니엄 팔콘"))
                .andExpect(jsonPath("$.data[0].auctionInfo.currentPrice").value(850000))
                .andExpect(jsonPath("$.data[0].auctionInfo.bidsCount").value(15))
                .andExpect(jsonPath("$.data[0].auctionInfo.auctionStatus").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("성공: 관심 경매가 없을 때 빈 목록을 반환한다")
    void getMyBookmarks_emptyList() throws Exception {
        // given
        PagedResponseDto<AuctionBookmarkListResponseDto> response = new PagedResponseDto<>(
                List.of(),
                new PageDto(1, 10, 0, 0, false, false)
        );

        given(auctionFacade.getMyBookmarks(any(String.class), any(Pageable.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/members/me/bookmarks")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @DisplayName("실패: 회원을 찾을 수 없는 경우 404를 반환한다")
    void getMyBookmarks_memberNotFound() throws Exception {
        // given
        given(auctionFacade.getMyBookmarks(any(String.class), any(Pageable.class)))
                .willThrow(new CustomException(ErrorType.MEMBER_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/v1/members/me/bookmarks")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}