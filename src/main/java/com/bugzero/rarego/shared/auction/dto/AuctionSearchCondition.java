package com.bugzero.rarego.shared.auction.dto;

import com.bugzero.rarego.bounded_context.auction.domain.AuctionStatus;
import lombok.Data;
import java.util.List;

@Data
public class AuctionSearchCondition {
	private List<Long> ids;          // 특정 ID 목록 조회 (찜 목록 등)
	private String keyword;          // 검색어 (상품명)
	private String category;         // 카테고리 (Product 쪽 컬럼)
	private AuctionStatus status;    // 상태 (IN_PROGRESS 등)
	private String sort;             // 정렬 (CLOSING_SOON, NEWEST 등)
}