package com.bugzero.rarego.domain;


public enum AuthRole {
	USER, ADMIN, SELLER, SYSTEM;

	public String securityRole() {
		return "ROLE_" + name();
	}
}
