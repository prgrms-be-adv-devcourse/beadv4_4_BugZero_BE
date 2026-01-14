package com.bugzero.rarego.global.response;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ErrorType {
	INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다."),

	MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND.value(), "존재하지 않는 회원입니다.");

	private final Integer httpStatus;
	private final String message;
}
