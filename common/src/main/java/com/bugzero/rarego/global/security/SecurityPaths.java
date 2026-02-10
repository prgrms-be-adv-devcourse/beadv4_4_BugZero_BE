package com.bugzero.rarego.global.security;

public final class SecurityPaths {
	private SecurityPaths() {
	}

	// 로그인 없이 허용할 엔드포인트들
	public static final String[] PUBLIC = {
		"/favicon.ico",
		"/h2-console/**",
		"/swagger-ui/**",
		"/v3/api-docs/**",
		"/swagger-ui.html",
		"/api-docs/**",


 // OAuth2 로그인 관련
		"/oauth2/**",
		"/login/**",

		// Auction bid stream
		"/api/v1/auctions/*/subscribe",
		"/api/v1/auctions/*/subscribers/count",
		"/api/v1/auctions/subscribers/count",

		// Auth
		"/api/v1/auth/logout",
		"/api/v1/auth/refresh",

		// 테스트코드
		"/api/v1/auth/test/login",
		"/api/v1/auth/test/check",
		"/api/v1/auth/test/admin",
		"/api/v1/payments/settlement",
		"/actuator/**"
	};

	public static final String[] PUBLIC_GET = {
		// Auction
		"/api/v1/auctions",
		"/api/v1/auctions/*",
		"/api/v1/auctions/*/bids",
	};
}