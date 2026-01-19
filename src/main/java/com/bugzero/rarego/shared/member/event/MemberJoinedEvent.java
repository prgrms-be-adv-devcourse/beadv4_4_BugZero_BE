package com.bugzero.rarego.shared.member.event;

import com.bugzero.rarego.shared.member.domain.MemberDto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberJoinedEvent {
	private final MemberDto member;
}