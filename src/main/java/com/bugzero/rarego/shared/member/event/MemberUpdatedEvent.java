package com.bugzero.rarego.shared.member.event;

import com.bugzero.rarego.shared.member.domain.MemberDto;

/**
 * 회원 수정 발생 이벤트
 * @param memberDto
 */
public record MemberUpdatedEvent(
	MemberDto memberDto
) {}