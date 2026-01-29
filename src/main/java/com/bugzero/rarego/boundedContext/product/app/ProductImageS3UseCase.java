package com.bugzero.rarego.boundedContext.product.app;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.bugzero.rarego.boundedContext.product.domain.dto.PresignedUrlRequestDto;
import com.bugzero.rarego.boundedContext.product.domain.dto.PresignedUrlResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductImageS3UseCase {

	private final S3Presigner s3Presigner;

	private final S3Client s3Client;

	@Value("${aws.s3.bucket}")
	private String bucketName;

	@Value("${aws.s3.expiration-minutes}")
	private long expirationMinutes;

	public PresignedUrlResponseDto createPresignerUrl(PresignedUrlRequestDto presignedUrlRequestDto) {
		String uniqueFileName = createUniqueFileName(presignedUrlRequestDto.fileName());
		String contentType = presignedUrlRequestDto.contentType();
		String s3Path = "temp/" + uniqueFileName;

		log.info("Presigned URL 발급: {}, contentType : {}", s3Path, contentType);

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

	public String getPresignedGetUrl(String s3Path) {
		if (s3Path == null || s3Path.isBlank())
			return null;

		// HTTP URL이 이미 포함되어 있다면 그대로 반환 (하위 호환성 유지용)
		if (s3Path.startsWith("http"))
			return s3Path;

		software.amazon.awssdk.services.s3.model.GetObjectRequest getObjectRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest
				.builder()
				.bucket(bucketName)
				.key(s3Path)
				.build();

		software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest presignRequest = software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
				.builder()
				.signatureDuration(Duration.ofMinutes(expirationMinutes))
				.getObjectRequest(getObjectRequest)
				.build();

		return s3Presigner.presignGetObject(presignRequest).url().toString();
	}

	// S3에 이미지를 등록하는 과정은 비동기로 처리
	@Async
	public void confirmImages(List<String> tempPaths) {
		for (String tempPath : tempPaths) {
			try {
				String destinationPath = tempPath.replace("temp/","products/");

				//1. S3 내부 복사
				CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
					.sourceBucket(bucketName)
					.sourceKey(tempPath)
					.destinationBucket(bucketName)
					.destinationKey(destinationPath)
					.build();
				s3Client.copyObject(copyObjectRequest);

				//2. 기존 temp 파일 삭제
				DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
					.bucket(bucketName)
					.key(tempPath)
					.build();
				s3Client.deleteObject(deleteObjectRequest);

				log.info("S3 이미지 확정 완료: {} -> {}", tempPath, destinationPath);
			} catch (Exception e) {
				log.error("S3 이미지 확정 중 오류 발생 (path: {}): {}", tempPath, e.getMessage());
			}
		}
	}

	// 파일명 앞에 UUID를 추가하여 고유한 파일명 생성 (DB 컬럼 길이를 고려하여 원본 파일명은 최대 100자로 제한)
	private String createUniqueFileName(String fileName) {
		String cleanName = fileName != null ? fileName : "image";
		if (cleanName.length() > 100) {
			cleanName = cleanName.substring(0, 100);
		}
		return UUID.randomUUID().toString() + "_" + cleanName;
	}
}
