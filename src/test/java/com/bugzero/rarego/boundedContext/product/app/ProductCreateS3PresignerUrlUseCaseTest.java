package com.bugzero.rarego.boundedContext.product.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import java.net.URL;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.product.domain.dto.PresignedUrlRequestDto;
import com.bugzero.rarego.boundedContext.product.domain.dto.PresignedUrlResponseDto;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class ProductCreateS3PresignerUrlUseCaseTest {

	@InjectMocks
	private ProductCreateS3PresignerUrlUseCase useCase;

	@Mock
	private S3Presigner s3Presigner;

	@Test
	@DisplayName("DTO를 전달하면 고유한 경로와 Presigned URL이 포함된 응답을 반환한다.")
	void createPresignerUrl_Success() throws Exception {
		// given
		ReflectionTestUtils.setField(useCase, "bucketName", "rarego-bucket");
		ReflectionTestUtils.setField(useCase, "expirationMinutes", 5L);

		PresignedUrlRequestDto requestDto = new PresignedUrlRequestDto("lego_castle.png", "image/png");
		String fakeUrl = "https://rarego-bucket.s3.amazonaws.com/products/unique-uuid_lego_castle.png";

		// S3에 권한요청할 때 필요한 PresignedUrl을 반환한다고 가정
		PresignedPutObjectRequest mockResponse = mock(PresignedPutObjectRequest.class);
		given(mockResponse.url()).willReturn(new URL(fakeUrl));
		given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(mockResponse);

		// when
		PresignedUrlResponseDto result = useCase.createPresignerUrl(requestDto);

		// then
		assertThat(result.url()).isEqualTo(fakeUrl);
		assertThat(result.s3Path()).startsWith("products/");
		assertThat(result.s3Path()).contains("lego_castle.png");

		verify(s3Presigner, times(1)).presignPutObject(any(PutObjectPresignRequest.class));
	}

}