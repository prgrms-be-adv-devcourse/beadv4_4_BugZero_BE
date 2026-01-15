package com.bugzero.rarego.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
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
		// 발급받은 액세스 키와 시크릿 키를 사용하여 인증 정보 생성
		AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

		// S3Presigner 빌드 (지역 설정 및 자격 증명 공급자 등록)
		return S3Presigner.builder()
			.region(Region.of(region))
			.credentialsProvider(StaticCredentialsProvider.create(credentials))
			.build();
	}
}
