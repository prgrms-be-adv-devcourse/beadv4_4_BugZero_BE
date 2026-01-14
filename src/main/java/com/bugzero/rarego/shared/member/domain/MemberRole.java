package com.bugzero.rarego.shared.member.domain;

public enum MemberRole {
	USER, ADMIN, SELLER;

	public String securityRole() {
		return "ROLE_" + name();
	}
}
