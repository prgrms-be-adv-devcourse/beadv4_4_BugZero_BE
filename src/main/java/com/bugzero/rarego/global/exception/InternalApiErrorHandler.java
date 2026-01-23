package com.bugzero.rarego.global.exception;

import java.io.IOException;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.ExceptionResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalApiErrorHandler {

    private final ObjectMapper objectMapper;

    public void handle(HttpRequest request, ClientHttpResponse response) throws IOException {
        String body = new String(response.getBody().readAllBytes());

        log.error("Internal API 호출 실패 - URI: {}, Status: {}, Body: {}",
                request.getURI(), response.getStatusCode(), body);

        try {
            ExceptionResponseDto errorResponse = objectMapper.readValue(body, ExceptionResponseDto.class);
            ErrorType errorType = ErrorType.findByCode(errorResponse.code())
                    .orElse(ErrorType.INTERNAL_SERVER_ERROR);
            throw new CustomException(errorType, errorResponse.message());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Internal API 에러 응답 파싱 실패: {}", e.getMessage());
            throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR, "내부 서비스 호출 실패: " + body);
        }
    }

    public void handleWithDefault(HttpRequest request, ClientHttpResponse response, ErrorType defaultErrorType)
            throws IOException {
        String body = new String(response.getBody().readAllBytes());

        log.error("Internal API 호출 실패 - URI: {}, Status: {}, Body: {}",
                request.getURI(), response.getStatusCode(), body);

        try {
            ExceptionResponseDto errorResponse = objectMapper.readValue(body, ExceptionResponseDto.class);
            ErrorType errorType = ErrorType.findByCode(errorResponse.code())
                    .orElse(defaultErrorType);
            throw new CustomException(errorType, errorResponse.message());
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Internal API 에러 응답 파싱 실패: {}", e.getMessage());
            throw new CustomException(defaultErrorType);
        }
    }
}
