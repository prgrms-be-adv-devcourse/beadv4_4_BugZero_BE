package com.bugzero.rarego.boundedContext.auth.domain;

import com.bugzero.rarego.shared.member.domain.ReplicaMember;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Table(name = "AUTH_MEMBER")
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AuthMember extends ReplicaMember{
}

