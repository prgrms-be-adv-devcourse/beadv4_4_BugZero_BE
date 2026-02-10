package com.bugzero.rarego.in;

import static org.mockito.BDDMockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.bugzero.rarego.app.NotificationFacade;
import com.bugzero.rarego.global.aspect.ResponseAspect;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.global.security.MemberPrincipal;
import com.bugzero.rarego.in.dto.NotificationResponseDto;
import com.bugzero.rarego.in.dto.NotificationUnreadCountResponseDto;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(controllers = NotificationController.class)
@Import(ResponseAspect.class)
@EnableAspectJAutoProxy
class NotificationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private NotificationFacade notificationFacade;

	// 테스트용 인증 객체 생성 헬퍼
	private Authentication createAuth(String publicId) {
		MemberPrincipal principal = new MemberPrincipal(publicId, "USER");
		return new UsernamePasswordAuthenticationToken(principal, null,
			List.of(new SimpleGrantedAuthority("ROLE_USER")));
	}

	// ==================== 알림 목록 조회 API 테스트 ====================

	@Test
	@DisplayName("성공: 알림 목록 조회 시 기본 페이징과 필터링 없이 정상 반환한다")
	void getNotifications_success_default() throws Exception {
		// given
		String publicId = "member-uuid-123";

		// Mock 응답 데이터
		NotificationResponseDto notification = new NotificationResponseDto(
			1L,
			"입찰 성공",
			"축하합니다!",
			"/auction/1",
			false,
			"AUCTION_WON",
			LocalDateTime.now()
		);

		PagedResponseDto<NotificationResponseDto> responseDto =
			new PagedResponseDto<>(List.of(notification), new PageDto(1, 10, 1, 1, true, true));

		// Facade 호출 스텁: any(Pageable.class)로 페이징 객체 처리
		given(notificationFacade.getNotifications(eq(publicId), eq(false), any(Pageable.class)))
			.willReturn(responseDto);

		// when & then
		mockMvc.perform(get("/api/v1/notifications")
				.with(authentication(createAuth(publicId)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
			.andExpect(jsonPath("$.data.data[0].id").value(1L))
			.andExpect(jsonPath("$.data.data[0].title").value("입찰 성공"));
	}

	@Test
	@DisplayName("성공: '안 읽음' 필터(onlyUnread=true)와 페이징 파라미터를 적용하여 조회한다")
	void getNotifications_success_with_params() throws Exception {
		// given
		String publicId = "member-uuid-123";
		int page = 0;
		int size = 5;

		PagedResponseDto<NotificationResponseDto> emptyResponse =
			new PagedResponseDto<>(Collections.emptyList(), new PageDto(1, size, 0, 0, false, false));

		// Facade 호출 스텁: onlyUnread=true 확인
		given(notificationFacade.getNotifications(eq(publicId), eq(true), any(Pageable.class)))
			.willReturn(emptyResponse);

		// when & then
		mockMvc.perform(get("/api/v1/notifications")
				.with(authentication(createAuth(publicId)))
				.with(csrf())
				.param("onlyUnread", "true")
				.param("page", String.valueOf(page))
				.param("size", String.valueOf(size))
				.param("sort", "createdAt,desc") // 정렬 조건 추가
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
			.andExpect(jsonPath("$.data.data").isArray());
	}

	// ==================== 안 읽은 알림 개수 조회 API 테스트 ====================

	@Test
	@DisplayName("성공: 안 읽은 알림 개수를 정상적으로 반환한다")
	void getUnreadCount_success() throws Exception {
		// given
		String publicId = "member-uuid-123";
		long count = 5L;
		NotificationUnreadCountResponseDto responseDto = new NotificationUnreadCountResponseDto(count);

		given(notificationFacade.getUnreadCount(publicId)).willReturn(responseDto);

		// when & then
		mockMvc.perform(get("/api/v1/notifications/unread-count")
				.with(authentication(createAuth(publicId)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()))
			.andExpect(jsonPath("$.data.count").value(count));
	}

	// ==================== 알림 읽음 처리 API 테스트 ====================

	@Test
	@DisplayName("성공: 알림 읽음 처리가 완료되면 HTTP 200을 반환한다")
	void markAsRead_success() throws Exception {
		// given
		String publicId = "member-uuid-123";
		Long notificationId = 100L;

		// void 메서드이므로 별도 return 설정 불필요 (기본적으로 아무 동작 안함)
		// 명시적으로 작성한다면: willDoNothing().given(notificationFacade).markAsRead(publicId, notificationId);

		// when & then
		mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId)
				.with(authentication(createAuth(publicId)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value(SuccessType.OK.getHttpStatus()));
	}

	@Test
	@DisplayName("실패: 존재하지 않는 알림(NOTIFICATION_NOT_FOUND)인 경우 HTTP 404를 반환한다")
	void markAsRead_fail_not_found() throws Exception {
		// given
		String publicId = "member-uuid-123";
		Long notificationId = 999L;

		willThrow(new CustomException(ErrorType.NOTIFICATION_NOT_FOUND))
			.given(notificationFacade).markAsRead(publicId, notificationId);

		// when & then
		mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId)
				.with(authentication(createAuth(publicId)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.status").value(ErrorType.NOTIFICATION_NOT_FOUND.getHttpStatus()))
			.andExpect(jsonPath("$.message").value(ErrorType.NOTIFICATION_NOT_FOUND.getMessage()));
	}

	@Test
	@DisplayName("실패: 본인의 알림이 아닌 경우(NOTIFICATION_OWNER_MISMATCH) HTTP 403을 반환한다")
	void markAsRead_fail_owner_mismatch() throws Exception {
		// given
		String publicId = "hacker-uuid";
		Long notificationId = 100L;

		// 403 Forbidden 에러 타입 가정
		willThrow(new CustomException(ErrorType.NOTIFICATION_OWNER_MISMATCH))
			.given(notificationFacade).markAsRead(publicId, notificationId);

		// when & then
		mockMvc.perform(patch("/api/v1/notifications/{id}/read", notificationId)
				.with(authentication(createAuth(publicId)))
				.with(csrf())
				.contentType(MediaType.APPLICATION_JSON))
			.andDo(print())
			.andExpect(status().isForbidden()) // 403 확인
			.andExpect(jsonPath("$.status").value(403));
	}
}