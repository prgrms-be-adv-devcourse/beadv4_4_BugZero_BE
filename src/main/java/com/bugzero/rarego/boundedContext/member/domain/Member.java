package com.bugzero.rarego.boundedContext.member.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Member extends BaseIdAndTime {
	private String username;

	public Member(String username) {
		this.username = username;
	}
}
