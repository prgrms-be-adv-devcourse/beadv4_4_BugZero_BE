package com.bugzero.rarego.boundedContext.product.in;

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

import com.bugzero.rarego.boundedContext.product.app.ProductCreateS3PresignerUrlUseCase;
import com.bugzero.rarego.boundedContext.product.domain.dto.PresignedUrlRequestDto;
import com.bugzero.rarego.global.aspect.ResponseAspect;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProductImageController.class)
@AutoConfigureMockMvc(addFilters = false)
@EnableAspectJAutoProxy
@Import(ResponseAspect.class)
class ProductImageControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private ProductCreateS3PresignerUrlUseCase s3PresignerUrlUseCase;

	@Test
	@DisplayName("성공 - 올바른 요청을 보내면 200 OK를 반환한다.")
	void getPresignedUrl_Success() throws Exception {
		// given
		PresignedUrlRequestDto validRequest = new PresignedUrlRequestDto("lego.jpg", "image/jpeg");

		// when & then
		mockMvc.perform(post("/api/v1/products/images/presigned-url")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(validRequest)))
			.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("실패 - 잘못된 Content-Type(image/pdf)을 보내면 400 에러를 반환한다.")
	void getPresignedUrl_InvalidContentType_Fail() throws Exception {
		// given
		PresignedUrlRequestDto invalidRequest = new PresignedUrlRequestDto("test.pdf", "application/pdf");

		// when & then
		mockMvc.perform(post("/api/v1/products/images/presigned-url")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(invalidRequest)))
			.andExpect(status().isBadRequest());
	}
}