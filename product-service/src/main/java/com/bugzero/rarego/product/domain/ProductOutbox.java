package com.bugzero.rarego.product.domain;

import java.util.UUID;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import com.bugzero.rarego.shared.auction.type.AuctionProductEventType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Table(name = "PRODUCT_OUTBOX")
public class ProductOutbox extends BaseIdAndTime {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuctionProductEventType eventType;

	Long productId;

	String publicId;

	String requestId;

	@Column(columnDefinition = "TEXT")
	private String payload;        // dto를 직렬화한 값

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ProductOutboxStatus status;

	@Column(nullable = false)
	private int retryCount;

	@Column(length = 1024)
	private String lastError;

	@Builder(access = AccessLevel.PRIVATE)
	private ProductOutbox(AuctionProductEventType eventType, Long productId, String publicId,
		String requestId, String payload) {
		this.eventType = eventType;
		this.productId = productId;
		this.publicId = publicId;
		this.requestId = requestId;
		this.payload = payload;
		this.status = ProductOutboxStatus.PENDING;
		this.retryCount = 0;
	}

	public static ProductOutbox createNewEvent(Long productId, String publicId,
		AuctionProductEventType type, String payload) {
		return ProductOutbox.builder()
			.eventType(type)
			.productId(productId)
			.publicId(publicId)
			.requestId("REQ-" + UUID.randomUUID())
			.payload(payload)
			.build();
	}

	public void markSent() {
		this.status = ProductOutboxStatus.SENT;
	}

	public void markFailed(String errorMessage, int maxRetry) {
		this.retryCount += 1;
		this.lastError = errorMessage;
		if (this.retryCount >= maxRetry) {
			this.status = ProductOutboxStatus.FAILED;
		}
	}

}
