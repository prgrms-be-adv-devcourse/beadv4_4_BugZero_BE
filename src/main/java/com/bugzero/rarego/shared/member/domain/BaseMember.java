package com.bugzero.rarego.shared.member.domain;

import static lombok.AccessLevel.*;

import com.bugzero.rarego.global.jpa.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@SuperBuilder
@Setter(value = PROTECTED)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseMember extends BaseEntity {
	@Column(nullable = false, unique = true, length = 36)
	private String publicId;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, unique = true, length = 50)
	private String nickname;

	private String intro;

	@Column(length = 255)
	private String address;

	@Column(name = "address_detail", length = 255)
	private String addressDetail;

	@Column(columnDefinition = "CHAR(5)", length = 5)
	private String zipCode;

	@Column(length = 11)
	private String contactPhone;

	@Column(name = "real_name", length = 10)
	private String realName;

	public void changeNickname(String nickname) {
		this.nickname = nickname;
	}

	public void changeIntro(String intro) {
		this.intro = intro;
	}

	public void changeZipCode(String zipCode) {
		this.zipCode = zipCode;
	}

	public void changeAddress(String address) {
		this.address = address;
	}

	public void changeAddressDetail(String addressDetail) {
		this.addressDetail = addressDetail;
	}

	public void changeIdentity(String contactPhone, String realName) {
		this.contactPhone = contactPhone;
		this.realName = realName;
	}
	protected void updateFrom(MemberDto member) {
		this.publicId = member.publicId();
		this.email = member.email();
		this.nickname = member.nickname();
		this.intro = member.intro();
		this.address = member.address();
		this.addressDetail = member.addressDetail();
		this.zipCode = member.zipCode();
		this.contactPhone = member.contactPhone();
		this.realName = member.realName();
	}
}
