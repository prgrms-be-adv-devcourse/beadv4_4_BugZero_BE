package com.bugzero.rarego.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.bugzero.rarego.global.security.CustomAccessDeniedHandler;
import com.bugzero.rarego.global.security.CustomAuthenticationEntryPoint;
import com.bugzero.rarego.global.security.JwtAuthenticationFilter;
import com.bugzero.rarego.global.security.JwtParser;
import com.bugzero.rarego.global.security.OAuth2SecurityConfigurer;
import com.bugzero.rarego.global.security.SecurityPaths;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import lombok.RequiredArgsConstructor;

@Configuration
@SecurityScheme(
	name = "bearerAuth",
	type = SecuritySchemeType.HTTP,
	scheme = "bearer",
	bearerFormat = "JWT"
)
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http, JwtParser jwtParser,
		CustomAuthenticationEntryPoint authenticationEntryPoint,
		CustomAccessDeniedHandler accessDeniedHandler,
		ObjectProvider<OAuth2SecurityConfigurer> oauth2ConfigurerProvider) throws Exception {
		http.authorizeHttpRequests(
			auth -> auth
				.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
				.requestMatchers(SecurityPaths.PUBLIC).permitAll()
				.requestMatchers(HttpMethod.GET, SecurityPaths.PUBLIC_GET).permitAll()
				.requestMatchers("/api/v1/internal/**").hasRole("SYSTEM")
				.anyRequest().authenticated()
		);
		http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
		http.csrf(AbstractHttpConfigurer::disable);
		http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
		applyOAuth2IfPresent(http, oauth2ConfigurerProvider);
		http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.exceptionHandling(ex -> ex
			.authenticationEntryPoint(authenticationEntryPoint)
			.accessDeniedHandler(accessDeniedHandler)
		);
		http.addFilterBefore(new JwtAuthenticationFilter(jwtParser),
			UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(List.of(
			"http://localhost:3000",
			"https://rarego.duckdns.org"
		));
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Cache-Control"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	// auth-service에서 구현체가 있으면 적용, 다른 모듈에서 없으면 skip
	private void applyOAuth2IfPresent(HttpSecurity http, ObjectProvider<OAuth2SecurityConfigurer> provider) {
		OAuth2SecurityConfigurer cfg = provider.getIfAvailable();
		if (cfg == null)
			return;
		try {
			cfg.configure(http);
		} catch (Exception e) {
			throw new IllegalStateException("인증 서버에 문제가 발생했습니다.", e);
		}
	}
}
