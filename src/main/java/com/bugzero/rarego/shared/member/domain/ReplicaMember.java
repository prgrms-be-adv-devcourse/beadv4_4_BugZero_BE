package com.bugzero.rarego.shared.member.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ReplicaMember extends BaseMember {
	@Id
	private Long id;

	private LocalDateTime createdAt;

	private LocalDateTime updatedAt;

}
