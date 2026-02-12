package com.bugzero.rarego.domain;

import java.util.Map;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

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
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "AUCTION_OUTBOX")
public class AuctionOutbox extends BaseIdAndTime {

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuctionOutboxType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AuctionOutboxStatus status;

	@Column(nullable = false)
	private int retryCount;

	@Column(length = 1024)
	private String lastError;

	@Column(columnDefinition = "JSON", nullable = false)
	private String payload;

	public static AuctionOutbox createAuctionEnded(
		Long auctionId,
		Long bidderId,
		Integer bidAmount,
		Long productId
	) {
		Map<String, Object> payload = Map.of(
			"auctionId", auctionId,
			"bidderId", bidderId,
			"bidAmount", bidAmount,
			"productId", productId
		);

		return AuctionOutbox.builder()
			.type(AuctionOutboxType.AUCTION_ENDED)
			.status(AuctionOutboxStatus.PENDING)
			.retryCount(0)
			.payload(toJson(payload))
			.build();
	}

	// ========== Payload 파싱 헬퍼 메서드 ==========

	public Map<String, Object> getPayloadAsMap() {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(this.payload, new TypeReference<>() {
			});
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse payload: " + this.payload, e);
		}
	}

	public <T> T getPayload(Class<T> clazz) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.readValue(this.payload, clazz);
		} catch (Exception e) {
			throw new RuntimeException("Failed to parse payload: " + this.payload, e);
		}
	}

	private static String toJson(Map<String, Object> payload) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(payload);
		} catch (Exception e) {
			throw new RuntimeException("Failed to convert to JSON", e);
		}
	}

	// ==========  상태 관리 메서드 ==========

	public void markSent() {
		this.status = AuctionOutboxStatus.SENT;
	}

	/**
	 * 복구 가능한 예외: 네트워크 타임아웃, DB 연결 일시적 실패
	 *
	 * @param errorMessage 오류 메시지
	 * @param maxRetry     최대 재시도 횟수
	 */
	public void markFailedTransient(String errorMessage, int maxRetry) {
		this.retryCount += 1;
		this.lastError = "[TRANSIENT] " + errorMessage;

		if (this.retryCount >= maxRetry) {
			// 최대 재시도 횟수 도달 → FAILED로 변환
			this.status = AuctionOutboxStatus.FAILED;
		}
		// PENDING 상태 유지 (재시도 스케줄러가 처리)
	}

	/**
	 * 복구 불가능한 예외: 페이로드 형식 오류, 필드 누락, 타입 불일치
	 *
	 * @param errorMessage 오류 메시지
	 * @param maxRetry     최대 재시도 횟수
	 */
	public void markFailedPermanently(String errorMessage, int maxRetry) {
		this.retryCount += 1;
		this.lastError = "[PERMANENT] " + errorMessage;
		// 재시도 가능성이 없으므로 즉시 FAILED로 변환
		this.status = AuctionOutboxStatus.FAILED;
	}
}
