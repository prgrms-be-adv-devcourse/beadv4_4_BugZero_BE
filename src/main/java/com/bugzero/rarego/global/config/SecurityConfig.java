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

import com.bugzero.rarego.global.security.CustomAccessDeniedHandler;
import com.bugzero.rarego.global.security.CustomAuthenticationEntryPoint;
import com.bugzero.rarego.boundedContext.auth.app.AuthOAuth2AccountService;
import com.bugzero.rarego.global.security.CustomOAuth2SuccessHandler;
import com.bugzero.rarego.global.security.JwtAuthenticationFilter;
import com.bugzero.rarego.global.security.JwtParser;

import lombok.RequiredArgsConstructor;

@EnableMethodSecurity
@Configuration
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
}
