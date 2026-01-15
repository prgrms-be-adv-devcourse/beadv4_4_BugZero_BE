package com.bugzero.rarego.boundedContext.auth.domain;


public enum AuthRole {
	USER, ADMIN, SELLER;

	public String securityRole() {
		return "ROLE_" + name();
	}
}
