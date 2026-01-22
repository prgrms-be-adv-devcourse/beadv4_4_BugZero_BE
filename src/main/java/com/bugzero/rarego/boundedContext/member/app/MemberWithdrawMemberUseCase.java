package com.bugzero.rarego.boundedContext.member.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.domain.MemberDto;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MemberWithdrawMemberUseCase {
	private final MemberSupport memberSupport;
	private final MemberRepository memberRepository;
	private final EventPublisher eventPublisher;

	public String withdraw(String publicId) {
		Member member = memberSupport.findByPublicId(publicId);
		if (member.isDeleted()) {
			throw new CustomException(ErrorType.MEMBER_MEMBER_DELETED);
		}

		member.softDelete();
		memberRepository.save(member);
		eventPublisher.publish(new MemberUpdatedEvent(MemberDto.from(member)));
		return member.getPublicId();
	}
}
