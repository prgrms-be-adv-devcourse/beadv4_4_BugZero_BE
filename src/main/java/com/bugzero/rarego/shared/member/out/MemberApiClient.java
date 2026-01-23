package com.bugzero.rarego.shared.member.out;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.exception.InternalApiErrorHandler;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberWithdrawRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberWithdrawResponseDto;
import org.springframework.beans.factory.annotation.Value;

@Service
public class MemberApiClient {
    private final RestClient restClient;
    private final RestClient internalRestClient;
    private final InternalApiErrorHandler errorHandler;

    public MemberApiClient(
            @Value("${custom.global.internalBackUrl}") String internalBackUrl,
            InternalApiErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/members")
                .build();
        this.internalRestClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/internal/members")
                .build();
    }

    public MemberJoinResponseDto join(String email) {
        MemberJoinRequestDto request = new MemberJoinRequestDto(email);
        SuccessResponseDto<MemberJoinResponseDto> response = restClient.post()
                .uri("/me")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, errorHandler::handle)
                .body(new ParameterizedTypeReference<>() {
                });
        if (response == null || response.data() == null) {
            throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
        }
        return response.data();
    }

    public Long findMemberIdByPublicId(String publicId) {
            SuccessResponseDto<Long> response = restClient.get()
                    .uri("/me")
                    .header("X-Public-Id", publicId)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError,
                            (httpRequest, httpResponse) -> errorHandler.handleWithDefault(httpRequest, httpResponse, ErrorType.MEMBER_NOT_FOUND))
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.data() == null) {
                throw new CustomException(ErrorType.MEMBER_NOT_FOUND);
            }

            return response.data();
    }

    public String withdraw(String publicId) {
        MemberWithdrawRequestDto request = new MemberWithdrawRequestDto(publicId);
        SuccessResponseDto<MemberWithdrawResponseDto> response = internalRestClient.post()
                .uri("/withdraw")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (response == null || response.data() == null) {
            throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
        }
        return response.data().publicId();
    }
}
