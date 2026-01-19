package com.bugzero.rarego.boundedContext.member.app;

import com.bugzero.rarego.shared.member.domain.MemberJoinResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberFacade {
    private final MemberJoinMemberUseCase memberJoinMemberUseCase;

    @Transactional
    public MemberJoinResponseDto join(String email) {
        return memberJoinMemberUseCase.join(email);
    }
}
