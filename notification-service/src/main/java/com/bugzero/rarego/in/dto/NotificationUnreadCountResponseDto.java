package com.bugzero.rarego.in.dto;

public record NotificationUnreadCountResponseDto(
	long count
) {
	public static NotificationUnreadCountResponseDto from(long count) {
		return new NotificationUnreadCountResponseDto(count);
	}
}
