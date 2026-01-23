package com.bugzero.rarego.boundedContext.product.in;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.bugzero.rarego.boundedContext.product.app.ProductFacade;
import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.GlobalExceptionHandler;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductCreateRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductCreateResponseDto;
import com.bugzero.rarego.shared.product.dto.ProductImageRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductImageUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateDto;
import com.bugzero.rarego.shared.product.dto.ProductUpdateResponseDto;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableAspectJAutoProxy
@WithMockUser
@Import(ResponseAspect.class)
class ProductControllerTest {

	private MockMvc mockMvc; // Autowired 대신 수동 초기화

	@Autowired
	private ProductController productController; // 테스트 대상 주입

	@MockitoBean
	private ProductFacade productFacade;

	@Autowired
	private ObjectMapper objectMapper;

	// 공통 상수
	private final String PUBLIC_ID = "seller-uuid";
	private final Long PRODUCT_ID = 100L;
	private ProductCreateResponseDto defaultResponse;
	private ProductUpdateResponseDto defaultUpdateResponse;

	@BeforeEach
	void setUp() {
		// 1. ArgumentResolver 설정
		HandlerMethodArgumentResolver mockPrincipalResolver = new HandlerMethodArgumentResolver() {
			@Override
			public boolean supportsParameter(MethodParameter parameter) {
				// MemberPrincipal 타입이 인자에 보이면 여기서 처리
				return MemberPrincipal.class.isAssignableFrom(parameter.getParameterType());
			}

			@Override
			public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
				NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
				// 컨트롤러에 넣어줄 가짜 Record 객체 반환
				return new MemberPrincipal(PUBLIC_ID, "SELLER");
			}
		};

		// 2. MockMvc 수동 빌드 (standaloneSetup)
		mockMvc = MockMvcBuilders.standaloneSetup(productController)
			.setCustomArgumentResolvers(mockPrincipalResolver) // 리졸버 장착
			.setControllerAdvice(new GlobalExceptionHandler()) // 에러 처리기 연결
			.build();

		// 3. Fixture 준비
		defaultResponse = ProductCreateResponseDto.builder()
			.productId(PRODUCT_ID)
			.inspectionStatus(InspectionStatus.PENDING)
			.build();

		defaultUpdateResponse = ProductUpdateResponseDto.builder()
			.productId(PRODUCT_ID)
			.auctionId(2L)
			.build();
	}

	// --- 상품 등록 (POST) 테스트 ---

	@Test
	@DisplayName("성공 - 올바른 상품 정보와 memberId가 전달되면 201 응답을 반환한다")
	void createProduct_success() throws Exception {
		// given
		ProductCreateRequestDto requestDto = createProductRequest("스타워즈 레고", 10000, 7);
		given(productFacade.createProduct(PUBLIC_ID, requestDto)).willReturn(defaultResponse);

		mockMvc.perform(post("/api/v1/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestDto)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.data.productId").value(PRODUCT_ID))
			.andExpect(jsonPath("$.data.inspectionStatus").value("PENDING"))
			.andDo(print());
	}

	@Test
	@DisplayName("실패 - 상품명이 비어있거나 경매 기간이 범위를 벗어나면 400 에러를 반환한다")
	void createProduct_fail_validation() throws Exception {
		// given: 상품명이 비어있는 잘못된 요청
		ProductCreateRequestDto invalidRequest = createProductRequest("", 1000, 31); // @NotBlank, @Max 위반

		// when & then
		mockMvc.perform(post("/api/v1/products")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest())
			.andDo(print());
	}

	// --- 상품 수정 (PATCH) 테스트 ---

	@Test
	@DisplayName("성공 - 상품 수정 API 호출 시 성공하면 200 OK와 수정된 상품 ID를 반환한다")
	void updateProduct_success() throws Exception {
		// given
		ProductUpdateDto updateDto = createUpdateDto("수정된 상품명");
		given(productFacade.updateProduct(eq(PUBLIC_ID), eq(PRODUCT_ID), any(ProductUpdateDto.class)))
			.willReturn(defaultUpdateResponse);

		// when & then
		mockMvc.perform(patch("/api/v1/products/{productId}", PRODUCT_ID)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(updateDto)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.data.productId").value(defaultUpdateResponse.productId()))
			.andExpect(jsonPath("$.data.auctionId").value(defaultUpdateResponse.auctionId()))
			.andDo(print());
	}

	@Test
	@DisplayName("성공 - 상품 삭제 API 호출 시 200 OK를 반환한다")
	void deleteProduct_Success() throws Exception {
		// given
		String publicId = "seller-uuid";
		Long productId = 100L;

		// Facade 호출 시 아무런 예외도 발생하지 않음을 가정 (void 리턴)
		doNothing().when(productFacade).deleteProduct(eq(publicId), eq(productId));

		// when & then
		mockMvc.perform(delete("/api/v1/products/{productId}", productId)
				.contentType(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(200))
			.andExpect(jsonPath("$.message").exists())
			.andDo(print());

		// Facade가 실제로 호출되었는지 검증
		verify(productFacade).deleteProduct(eq(publicId), eq(productId));
	}

	// --- Helper Methods (Fixture Factory) ---

	private ProductCreateRequestDto createProductRequest(String name, int price, int duration) {
		return new ProductCreateRequestDto(
			name,
			Category.스타워즈,
			"설명",
			new ProductAuctionRequestDto(price, duration),
			List.of(new ProductImageRequestDto("https://s3.image.com/test.jpg", 1))
		);
	}

	private ProductUpdateDto createUpdateDto(String name) {
		return new ProductUpdateDto(
			name,
			Category.스타워즈,
			"설명",
			new ProductAuctionUpdateDto(1L, 1000, 7),
			List.of(new ProductImageUpdateDto(1L, "url", 1))
		);
	}

}