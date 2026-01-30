package com.bugzero.rarego.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {
	@Value("${aws.credentials.access-key}")
	private String accessKey;

	@Value("${aws.credentials.secret-key}")
	private String secretKey;

	@Value("${aws.region}")
	private String region;

	@Bean
	public S3Presigner s3Presigner() {
		// S3Presigner 빌드 (지역 설정 및 자격 증명 공급자 등록)
		return S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(getCredentialsProvider())
			.build();
	}

	@Bean
	public S3Client s3Client() {
		return S3Client.builder()
			.region(Region.of(region))
			.credentialsProvider(getCredentialsProvider()) // [수정] 인증 정보 추가!
			.build();
	}

	// 공통 자격 증명 생성 메서드 (내부에서 활용)
	private StaticCredentialsProvider getCredentialsProvider() {
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
		return StaticCredentialsProvider.create(credentials);
	}


}
