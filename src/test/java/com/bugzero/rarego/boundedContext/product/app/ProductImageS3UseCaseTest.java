package com.bugzero.rarego.boundedContext.product.app;

import static org.assertj.core.api.AssertionsForClassTypes.*;
import static org.mockito.BDDMockito.*;

import java.net.URL;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bugzero.rarego.boundedContext.product.domain.dto.PresignedUrlRequestDto;
import com.bugzero.rarego.boundedContext.product.domain.dto.PresignedUrlResponseDto;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class ProductImageS3UseCaseTest {

	@InjectMocks
	private ProductImageS3UseCase useCase;

	@Mock
	private S3Presigner s3Presigner;

	@Mock
	private S3Client s3Client;

	@BeforeEach
	void setUp() {
		ReflectionTestUtils.setField(useCase, "bucketName", "rarego-bucket");
		ReflectionTestUtils.setField(useCase, "expirationMinutes", 5L);
	}

	@Test
	@DisplayName("DTO를 전달하면 고유한 경로와 Presigned URL이 포함된 응답을 반환한다.")
	void createPresignerUrl_Success() throws Exception {
		// given
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
		assertThat(result.s3Path()).startsWith("temp/");
		assertThat(result.s3Path()).contains("lego_castle.png");

		verify(s3Presigner, times(1)).presignPutObject(any(PutObjectPresignRequest.class));
	}

	@Test
	@DisplayName("S3 경로가 이미 HTTP URL 형태라면 그대로 반환한다.")
	void getPresignedGetUrl_ReturnOriginal_WhenAlreadyUrl() {
		// given
		String s3Path = "http://already-url.com/image.png";

		// when
		String resultUrl = useCase.getPresignedGetUrl(s3Path);

		// then
		assertThat(resultUrl).isEqualTo(s3Path);
		verifyNoInteractions(s3Presigner); // S3Presigner를 호출하지 않아야 함
	}

	@Test
	@DisplayName("비동기로 이미지 확정 로직(복사 및 삭제)이 수행된다.")
	void confirmImages_Success() {
		// given
		List<String> tempPaths = List.of("temp/image1.png", "temp/image2.png");

		// s3Client의 동작은 void이거나 영향이 없으므로 기본 Mock 동작(doNothing)을 따름
		// (Mockito는 기본적으로 void 메서드에 대해 아무 일도 하지 않음)

		// when
		useCase.confirmImages(tempPaths);

		// then
		// 각 이미지당 copy와 delete가 한 번씩 호출되었는지 검증
		verify(s3Client, times(2)).copyObject(any(CopyObjectRequest.class));
		verify(s3Client, times(2)).deleteObject(any(DeleteObjectRequest.class));
	}
}