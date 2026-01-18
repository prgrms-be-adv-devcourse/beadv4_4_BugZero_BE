package com.bugzero.rarego.global.util;

public class MaskingUtils {
    // 실명 마스킹 ex 김*수
    public static String maskRealName(String realName) {
        // input null
        if (realName == null || realName.isBlank()) return "";

        int length = realName.length();
        
        if (length == 1) {
            return realName.charAt(0) + "*";
        }
        return realName.charAt(0) + "*".repeat(length - 2) + realName.charAt(length - 1);
    }

    // 번호 마스킹 ex 010-****-9999
    public static String maskPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) return "";
        StringBuilder maskedPhoneNumber = new StringBuilder(phoneNumber);
        int length = phoneNumber.length();

        // 10자리 번호 마스킹 ex 031-***-1231
        if (length == 10) {
            maskedPhoneNumber.replace(3, 6, "*");
        }
        if (length == 11) {
            maskedPhoneNumber.replace(3, 7, "*");
        }
        return maskedPhoneNumber.toString();
    }
}