package com.bugzero.rarego.shared.member.domain;

import com.bugzero.rarego.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseMember extends BaseEntity {
	@Column(nullable = false, unique = true, length = 36)
	private String publicId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private MemberRole role;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, length = 50)
	private String nickname;

	private String intro;

	@Column(length = 500)
	private String address;

	@Column(columnDefinition = "CHAR(5)", length = 5)
	private String zipCode;

	@Column(length = 11)
	private String contactPhone;

	@Column(columnDefinition = "TEXT")
	private String realNameEnc;
}
