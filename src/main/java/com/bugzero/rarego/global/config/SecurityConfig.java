package com.bugzero.rarego.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.bugzero.rarego.global.security.CustomAccessDeniedHandler;
import com.bugzero.rarego.global.security.CustomAuthenticationEntryPoint;
import com.bugzero.rarego.boundedContext.auth.app.AuthOAuth2AccountService;
import com.bugzero.rarego.global.security.CustomOAuth2SuccessHandler;
import com.bugzero.rarego.global.security.JwtAuthenticationFilter;
import com.bugzero.rarego.global.security.JwtParser;

import java.util.Arrays;
import java.util.List;

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
		AuthOAuth2AccountService authOAuth2AccountService,
		CustomOAuth2SuccessHandler customOAuth2SuccessHandler) throws Exception {
		http.authorizeHttpRequests(
				auth -> auth
					.requestMatchers("/favicon.ico").permitAll()
					.requestMatchers("/h2-console/**").permitAll()
					.requestMatchers("/**").permitAll()
					.anyRequest().authenticated()
			)
			.headers(
				headers -> headers
					.frameOptions(
						HeadersConfigurer.FrameOptionsConfig::sameOrigin
					)
			).csrf(
				AbstractHttpConfigurer::disable
			).cors(cors -> cors.configurationSource(corsConfigurationSource())
			).oauth2Login(oauth2 -> oauth2
				.userInfoEndpoint(userInfo -> userInfo.userService(authOAuth2AccountService))
				.successHandler(customOAuth2SuccessHandler)
			).sessionManagement(
				sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.exceptionHandling(ex -> ex
				.authenticationEntryPoint(authenticationEntryPoint)
				.accessDeniedHandler(accessDeniedHandler)
			)
			.addFilterBefore(new JwtAuthenticationFilter(jwtParser), UsernamePasswordAuthenticationFilter.class);

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
}
