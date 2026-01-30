package com.bugzero.rarego.boundedContext.product.domain;

import com.bugzero.rarego.global.jpa.entity.BaseIdAndTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 외부에서 기본 생성자 호출 차단
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더 외 생성자 사용 제한
@Builder(access = AccessLevel.PRIVATE) // 외부에서 직접 빌더 호출 방지 (create 메서드만 통하도록)
@Table(name = "PRODUCT_IMAGE")
public class ProductImage extends BaseIdAndTime {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(length = 500, nullable = false)
	private String imageUrl;

	private int sortOrder;

	//객체 생성의 유일한 창구
	public static ProductImage createConfirmedImage(Product product, String imageUrl, int sortOrder) {
		return ProductImage.builder()
			.product(product)
			.imageUrl(urlReplace(imageUrl))
			.sortOrder(sortOrder)
			.build();
	}

	public void update(String imageUrl, int sortOrder) {
		this.imageUrl = urlReplace(imageUrl);
		this.sortOrder = sortOrder;
	}

	private static String urlReplace(String url) {
		return url.replace("temp/", "products/");
	}
}
