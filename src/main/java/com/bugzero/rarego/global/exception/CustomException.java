package com.bugzero.rarego.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
	private final ErrorType errorType;

	public CustomException(ErrorType errorType) {
		super(errorType.getMessage());
		this.errorType = errorType;
	}

	// 상세 메시지 추가 사용 (상황에 따른 가변적인 메시지)
	public CustomException(ErrorType errorType, String detailMessage) {
		super(detailMessage);
		this.errorType = errorType;
	}
}
