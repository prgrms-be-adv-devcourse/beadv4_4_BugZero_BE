package com.bugzero.rarego.event;

import com.bugzero.rarego.in.dto.NotificationResponseDto;

public record NotificationCreatedEvent(
	String publicId,
	NotificationResponseDto response
) {
}
