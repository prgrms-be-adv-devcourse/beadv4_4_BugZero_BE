package com.bugzero.rarego.boundedContext.auth.domain;

import java.util.Map;

import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

public final class OAuth2AttributeMapper {

	private OAuth2AttributeMapper() {}

	public static AccountDto toAccountDto(String provider, String attributeKey, Map<String, Object> attributes) {
		validateOauth2Attributes(provider, attributeKey, attributes);

		// 각 제공자별로 매핑 처리
		// switch 문 사용 이유 : if-else보다 가독성이 좋음, 케이스가 바뀌지 않고 적음
		return switch (provider) {
			case "google" -> fromGoogle(attributeKey, attributes);
			case "kakao"  -> fromKakao(attributeKey, attributes);
			case "naver"  -> fromNaver(attributeKey, attributes);
			default -> throw new CustomException(ErrorType.AUTH_OAUTH2_INVALID_RESPONSE);
		};
	}

	private static void validateOauth2Attributes(String provider, String attributeKey, Map<String, Object> attributes) {
		if (provider == null || provider.isBlank()) {
			throw new CustomException(ErrorType.AUTH_OAUTH2_INVALID_RESPONSE);
		}
		if (attributeKey == null || attributeKey.isBlank()) {
			throw new CustomException(ErrorType.AUTH_OAUTH2_INVALID_RESPONSE);
		}
		if (attributes == null || attributes.isEmpty()) {
			throw new CustomException(ErrorType.AUTH_OAUTH2_INVALID_RESPONSE);
		}
	}

	private static AccountDto fromGoogle(String attributeKey, Map<String, Object> attributes) {
		String providerId = safeAsString(attributes.get(attributeKey)); // 보통 "sub"
		String email = safeAsString(attributes.get("email"));
		return new AccountDto(providerId, email, Provider.GOOGLE);
	}

	@SuppressWarnings("unchecked")
	private static AccountDto fromKakao(String attributeKey, Map<String, Object> attributes) {
		// 카카오는 id가 최상위 attributes.get("id") 에 있음
		String providerId = safeAsString(attributes.get(attributeKey)); // 보통 "id"

		Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
		String email = null;
		if (kakaoAccount != null) {
			email = safeAsString(kakaoAccount.get("email")); // 개발에서 email 비허용. null 가능
		}
		if (email == null || email.isBlank()) {
			email = buildPlaceholderEmail("kakao", providerId);
		}

		return new AccountDto(providerId, email, Provider.KAKAO);
	}

	// 이후에 개발 예정
	@SuppressWarnings("unchecked")
	private static AccountDto fromNaver(String attributeKey, Map<String, Object> attributes) {
		// 네이버는 response 안에 있음
		Map<String, Object> response = (Map<String, Object>) attributes.get("response");
		String providerId = safeAsString(response.get(attributeKey)); // 보통 "id"
		String email = safeAsString(response.get("email"));
		return new AccountDto(providerId, email, Provider.NAVER);
	}

	private static String safeAsString(Object v) {
		return v == null ? null : v.toString();
	}

	// 이메일이 없는 경우 대체 이메일 생성 => 카카오는 개발 단계에서 이메일 제공 해주지 않음
	private static String buildPlaceholderEmail(String provider, String providerId) {
		if (providerId == null || providerId.isBlank()) {
			throw new CustomException(ErrorType.AUTH_OAUTH2_INVALID_RESPONSE);
		}
		return provider + "_" + providerId + "@noemail.invalid";
	}
}
