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
import lombok.Getter;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity implements HasModelTypeCode {

	@Column(nullable = false)
	protected boolean isDeleted = false;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	protected LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	protected LocalDateTime updatedAt;

	public void softDelete() {
		this.isDeleted = true;
		this.updatedAt = LocalDateTime.now();	// 자동 갱신 가능
	}

	@Override
	public String getModelTypeCode() {
		return this.getClass().getSimpleName();
	}

	protected void publishEvent(Object event){
		GlobalConfig.getEventPublisher().publish(event);
	}
}