package com.bugzero.rarego.global.jpa.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.bugzero.rarego.global.config.GlobalConfig;
import com.bugzero.rarego.standard.modelType.HasModelTypeCode;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@MappedSuperclass
@SuperBuilder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class BaseEntity implements HasModelTypeCode {

	@Builder.Default
	@Column(nullable = false)
	protected boolean deleted = false;

	public void softDelete() {
		this.deleted = true;
	}

	public abstract Long getId();

	public abstract LocalDateTime getCreatedAt();

	public abstract LocalDateTime getUpdatedAt();

	public boolean isDeleted() {
		return deleted;
	}

	@Override
	public String getModelTypeCode() {
		return this.getClass().getSimpleName();
	}

	protected void publishEvent(Object event) {
		GlobalConfig.getEventPublisher().publish(event);
	}
}