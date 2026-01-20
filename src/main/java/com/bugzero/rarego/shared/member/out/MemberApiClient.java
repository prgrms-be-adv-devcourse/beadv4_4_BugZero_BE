package com.bugzero.rarego.shared.member.out;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinRequestDto;
import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class MemberApiClient {
    private final RestClient restClient;

    public MemberApiClient(@Value("${custom.global.internalBackUrl}") String internalBackUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(internalBackUrl + "/api/v1/members")
                .build();
    }

    public MemberJoinResponseDto join(String email) {
        MemberJoinRequestDto request = new MemberJoinRequestDto(email);
        SuccessResponseDto<MemberJoinResponseDto> response = restClient.post()
                .uri("/me")
                .body(request)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
        if (response == null || response.data() == null) {
            throw new CustomException(ErrorType.INTERNAL_SERVER_ERROR);
        }
        return response.data();
    }

    public Long findMemberIdByPublicId(String publicId) {
        try {
            SuccessResponseDto<Long> response = restClient.get()
                    .uri("/me")
                    .header("X-Public-Id", publicId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.data() == null) {
                throw new CustomException(ErrorType.MEMBER_NOT_FOUND);
            }

            return response.data();
        } catch (Exception e) {
            throw new CustomException(ErrorType.MEMBER_NOT_FOUND);
        }
    }
}
