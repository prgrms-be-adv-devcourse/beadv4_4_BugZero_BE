package com.bugzero.rarego.boundedContext.auction.in;

import com.bugzero.rarego.boundedContext.auction.app.AuctionFacade;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.member.app.MemberFacade;
import com.bugzero.rarego.boundedContext.member.in.MemberController;
import com.bugzero.rarego.global.exception.GlobalExceptionHandler;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.security.MemberPrincipal;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AuctionMemberControllerTest {

	@InjectMocks
	private AuctionMemberController auctionMemberController;

	@Mock
	private AuctionFacade auctionFacade;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.standaloneSetup(auctionMemberController)
			.setControllerAdvice(new GlobalExceptionHandler()) // 전역 예외 처리기 등록
			.setCustomArgumentResolvers(
				new PageableHandlerMethodArgumentResolver(), // Pageable 처리
				new HandlerMethodArgumentResolver() { // MemberPrincipal 커스텀 처리
					@Override
					public boolean supportsParameter(MethodParameter parameter) {
						return MemberPrincipal.class.isAssignableFrom(parameter.getParameterType());
					}

					@Override
					public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
						NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
						// 테스트용 MemberPrincipal 주입 (publicId="2"로 설정)
						return new MemberPrincipal("2", "USER");
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

		given(auctionFacade.getMyBids(eq("2"), eq(AuctionStatus.IN_PROGRESS), any(Pageable.class)))
			.willReturn(response);

		// when & then
		mockMvc.perform(get("/api/v1/members/me/bids")
				.param("auctionStatus", "IN_PROGRESS"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data[0].auctionId").value(20L))
			// PageDto 구조에 따라 jsonPath 수정 (totalElements 등)
			.andExpect(jsonPath("$.pageDto.totalItems").value(1));
	}
}