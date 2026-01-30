package com.bugzero.rarego.bounded_context.auth.domain;


public enum AuthRole {
	USER, ADMIN, SELLER;

	public String securityRole() {
		return "ROLE_" + name();
	}
}
