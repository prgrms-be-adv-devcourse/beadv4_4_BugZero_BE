package com.bugzero.rarego.shared.member.event;

import com.bugzero.rarego.shared.member.domain.MemberDto;

public record MemberJoinedEvent(
	MemberDto memberDto
) {
}