package com.bugzero.rarego.in;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.bugzero.rarego.app.NotificationFacade;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.in.dto.NotificationResponseDto;
import com.bugzero.rarego.in.dto.NotificationUnreadCountResponseDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {
	private final NotificationFacade notificationFacade;

	@GetMapping
	public SuccessResponseDto<PagedResponseDto<NotificationResponseDto>> getNotifications(
		@AuthenticationPrincipal MemberPrincipal memberPrincipal,
		@RequestParam(required = false, defaultValue = "false") Boolean onlyUnread,
		@PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
	) {
		PagedResponseDto<NotificationResponseDto> response = notificationFacade.getNotifications(
			memberPrincipal.publicId(), onlyUnread, pageable);

		return SuccessResponseDto.from(SuccessType.OK, response);
	}

	@GetMapping("/unread-count")
	public SuccessResponseDto<NotificationUnreadCountResponseDto> getUnreadCount(
		@AuthenticationPrincipal MemberPrincipal memberPrincipal) {
		NotificationUnreadCountResponseDto response = notificationFacade.getUnreadCount(memberPrincipal.publicId());

		return SuccessResponseDto.from(SuccessType.OK, response);
	}

	@PatchMapping("/{id}/read")
	public SuccessResponseDto<Void> markAsRead(@AuthenticationPrincipal MemberPrincipal memberPrincipal,
		@PathVariable Long id) {
		notificationFacade.markAsRead(memberPrincipal.publicId(), id);

		return SuccessResponseDto.from(SuccessType.OK);
	}

	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@AuthenticationPrincipal MemberPrincipal memberPrincipal) {
		return notificationFacade.subscribe(memberPrincipal.publicId());
	}
}
