package com.bugzero.rarego.bounded_context.product.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ProductCondition {
	INSPECTION("상태 검수 중"),
	MISB("박스 상태가 완벽하고 인장이 그대로 붙어 있는 최상급 상태"),
	NISB("새 제품이지만 박스에 약간의 구겨짐이나 흠집이 있는 상태"),
	MISP("비닐 팩 형태의 제품이 미개봉 상태"),
	USED("조립 후 전시했거나 보관 중인 상태")
	;

	private final String description;
}
