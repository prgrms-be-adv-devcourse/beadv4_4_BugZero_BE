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
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.bugzero.rarego.global.security.CustomAccessDeniedHandler;
import com.bugzero.rarego.global.security.CustomAuthenticationEntryPoint;
import com.bugzero.rarego.global.security.InternalAuthenticationFilter;
import com.bugzero.rarego.global.security.JwtAuthenticationFilter;
import com.bugzero.rarego.global.security.JwtParser;
import com.bugzero.rarego.global.security.OAuth2SecurityConfigurer;
import com.bugzero.rarego.global.security.SecurityPaths;

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
		OAuth2SecurityConfigurer oauth2SecurityConfigurer,
		InternalAuthenticationFilter internalAuthenticationFilter) throws Exception {
		http.authorizeHttpRequests(
			auth -> auth
				.requestMatchers(CorsUtils::isPreFlightRequest).permitAll()
				.requestMatchers(SecurityPaths.PUBLIC).permitAll()
				.requestMatchers(HttpMethod.GET, SecurityPaths.PUBLIC_GET).permitAll()
				.anyRequest().authenticated()
		);
		http.headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin));
		http.csrf(AbstractHttpConfigurer::disable);
		http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
		oauth2SecurityConfigurer.configure(http);
		http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		http.exceptionHandling(ex -> ex
			.authenticationEntryPoint(authenticationEntryPoint)
			.accessDeniedHandler(accessDeniedHandler)
		);
		http.addFilterBefore(internalAuthenticationFilter, JwtAuthenticationFilter.class);
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
}
