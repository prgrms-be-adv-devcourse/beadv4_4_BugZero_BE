package com.bugzero.rarego.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ErrorType {
	INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다.")
	;

	private final Integer httpStatus;
	private final String message;
}
