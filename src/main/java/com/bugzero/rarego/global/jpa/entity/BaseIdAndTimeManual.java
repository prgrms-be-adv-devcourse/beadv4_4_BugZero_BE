package com.bugzero.rarego.global.jpa.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@SuperBuilder
@Getter
public abstract class BaseIdAndTimeManual extends BaseEntity {
    @Id
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}