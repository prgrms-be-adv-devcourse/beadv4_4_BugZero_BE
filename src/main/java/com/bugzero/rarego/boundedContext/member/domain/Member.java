package com.bugzero.rarego.boundedContext.member.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import com.bugzero.rarego.shared.member.domain.SourceMember;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "MEMBER_MEMBER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Member extends SourceMember {
}
