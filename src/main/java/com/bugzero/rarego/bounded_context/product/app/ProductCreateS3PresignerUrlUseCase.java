package com.bugzero.rarego.bounded_context.product.app;

import java.time.Duration;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.bounded_context.product.domain.dto.PresignedUrlRequestDto;
import com.bugzero.rarego.bounded_context.product.domain.dto.PresignedUrlResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductCreateS3PresignerUrlUseCase {

	private final S3Presigner s3Presigner;

	@Value("${aws.s3.bucket}")
	private String bucketName;

	@Value("${aws.s3.expiration-minutes}")
	private long expirationMinutes;

	public PresignedUrlResponseDto createPresignerUrl(PresignedUrlRequestDto presignedUrlRequestDto) {
		String uniqueFileName = createUniqueFileName(presignedUrlRequestDto.fileName());
		String contentType = presignedUrlRequestDto.contentType();
		String s3Path = "products/" + uniqueFileName;

		log.info("create Presigned URL for path: {}, contentType : {}", s3Path, contentType);

		// S3에 어떤 파일을 올릴지에 대한 기본 요청 정보 (PutObjectRequest)
		PutObjectRequest putObjectRequest = PutObjectRequest.builder()
			.bucket(bucketName)
			.key(s3Path)
			.contentType(contentType)
			.build();

		// Presigned URL 요청 설정 (유효 시간 5분)
		PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
			.signatureDuration(Duration.ofMinutes(expirationMinutes)) // 5분 뒤 만료
			.putObjectRequest(putObjectRequest)
			.build();

		// URL 생성
		String url =  s3Presigner.presignPutObject(presignRequest).url().toString();

		// 클라이언트가 s3 에 파일을 저장할 수 있는 경로와 DB에 실제 저장될 경로를 반환
		return PresignedUrlResponseDto.builder()
			.url(url)
			.s3Path(s3Path)
			.build();
	}


	//파일명 앞에 UUID를 추가하여 고유한 파일명 생성.
	private String createUniqueFileName(String fileName) {
		return UUID.randomUUID().toString() + "_" + fileName;
	}
}
