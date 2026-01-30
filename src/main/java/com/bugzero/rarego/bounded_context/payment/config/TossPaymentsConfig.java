package com.bugzero.rarego.bounded_context.payment.config;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Configuration
public class TossPaymentsConfig {
	@Value("${toss.payments.secretKey}")
	private String secretKey;

	@Value("${toss.payments.url}")
	private String url;

	@Bean
	public RestClient tossPaymentsRestClient() {
		String encodedSecretKey = Base64.getEncoder()
			.encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

		return RestClient.builder()
			.baseUrl(url)
			.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedSecretKey)
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.build();
	}
}
