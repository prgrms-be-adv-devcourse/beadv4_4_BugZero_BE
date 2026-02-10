package com.bugzero.rarego.app;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Member;
import com.bugzero.rarego.out.MemberRepository;
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
	private final ApplicationEventPublisher eventPublisher;

	public String withdraw(String publicId) {
		Member member = memberSupport.findByPublicId(publicId);
		if (member.isDeleted()) {
			throw new CustomException(ErrorType.MEMBER_MEMBER_DELETED);
		}

		member.softDelete();
		Member saved = memberRepository.saveAndFlush(member);
		eventPublisher.publishEvent(new MemberUpdatedEvent(MemberDto.from(saved)));
		return saved.getPublicId();
	}
}
