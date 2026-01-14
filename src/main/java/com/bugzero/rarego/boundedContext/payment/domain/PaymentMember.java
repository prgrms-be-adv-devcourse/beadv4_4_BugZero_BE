package com.bugzero.rarego.boundedContext.payment.domain;

import com.bugzero.rarego.shared.member.domain.ReplicaMember;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@SuperBuilder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "PAYMENT_MEMBER")
public class PaymentMember extends ReplicaMember {
}
