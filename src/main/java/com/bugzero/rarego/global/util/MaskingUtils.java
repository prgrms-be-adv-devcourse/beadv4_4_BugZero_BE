package com.bugzero.rarego.global.util;

public class MaskingUtils {
	// 실명 마스킹 ex 김*수
	public static String maskRealName(String realName) {
		// input null
		if (realName == null || realName.isBlank())
			return "";

		int length = realName.length();

		if (length == 1) {
			return realName.charAt(0) + "*";
		}
		return realName.charAt(0) + "*".repeat(length - 2) + realName.charAt(length - 1);
	}

	// 번호 마스킹 ex 010-****-9999
	public static String maskPhone(String phoneNumber) {
		if (phoneNumber == null || phoneNumber.isBlank()) {
			return "";
		}
		if (phoneNumber.length() < 10) {
			return phoneNumber;
		}

		if (phoneNumber.startsWith("02")) {
			String prefix = "02";
			String last4 = phoneNumber.substring(phoneNumber.length() - 4);
			int middleLen = phoneNumber.length() - 2 - 4;
			return prefix + "*".repeat(middleLen) + last4;
		}

		String prefix = phoneNumber.substring(0, 3);
		String last4 = phoneNumber.substring(phoneNumber.length() - 4);
		int middleLen = phoneNumber.length() - 3 - 4;

		return prefix + "*".repeat(middleLen) + last4;

	}
}