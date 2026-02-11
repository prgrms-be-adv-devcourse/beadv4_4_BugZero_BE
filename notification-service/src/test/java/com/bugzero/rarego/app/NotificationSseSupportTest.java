package com.bugzero.rarego.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.bugzero.rarego.in.dto.NotificationResponseDto;
import com.bugzero.rarego.out.EmitterRepository;

@ExtendWith(MockitoExtension.class)
class NotificationSseSupportTest {

	@Mock
	private EmitterRepository emitterRepository;

	@InjectMocks
	private NotificationSseSupport sseSupport;

	@Test
	@DisplayName("구독(subscribe): SseEmitter를 생성하고 저장소에 저장한 뒤 'connect' 이벤트를 전송한다.")
	void subscribe_success() {
		// given
		String publicId = "user-uuid-123";

		// when
		SseEmitter result = sseSupport.subscribe(publicId);

		// then
		// 1. 반환된 Emitter가 null이 아닌지 확인
		assertThat(result).isNotNull();
		assertThat(result.getTimeout()).isEqualTo(60L * 1000 * 60);

		// 2. 저장소가 호출되었는지 검증 (Key는 publicId로 시작해야 함)
		then(emitterRepository).should(times(1))
			.save(startsWith(publicId + "_"), eq(result));
	}

	@Test
	@DisplayName("전송(send): 해당 유저의 모든 Emitter를 찾아 알림 데이터를 전송한다.")
	void send_success() throws IOException {
		// given
		String publicId = "user-uuid-123";
		String emitterId = "user-uuid-123_123456789";

		// [수정] Builder 대신 생성자 사용 (Record 필드 순서에 맞춤)
		NotificationResponseDto responseDto = new NotificationResponseDto(
			1L,
			"알림 제목",
			"테스트 알림",
			"/link/1",
			false,
			"AUCTION_WON",
			LocalDateTime.now()
		);

		// Mock Emitter 생성
		SseEmitter mockEmitter = mock(SseEmitter.class);
		Map<String, SseEmitter> emitters = Map.of(emitterId, mockEmitter);

		// Repository가 Mock Emitter를 반환하도록 설정
		given(emitterRepository.findAllByMemberPublicId(publicId)).willReturn(emitters);

		// when
		sseSupport.send(publicId, responseDto);

		// then
		// 1. Repository 조회 확인
		then(emitterRepository).should(times(1)).findAllByMemberPublicId(publicId);

		// 2. Emitter.send()가 호출되었는지 확인
		then(mockEmitter).should(times(1)).send(any(SseEmitter.SseEventBuilder.class));
	}

	@Test
	@DisplayName("전송 실패(IOException): 전송 중 예외가 발생하면 해당 Emitter를 저장소에서 삭제한다.")
	void send_fail_io_exception() throws IOException {
		// given
		String publicId = "user-uuid-123";
		String emitterId = "user-uuid-123_123456789";

		// [수정] 생성자 사용 (테스트에 필드 값은 중요하지 않으므로 null 허용)
		NotificationResponseDto responseDto = new NotificationResponseDto(
			null, null, null, null, false, null, null
		);

		SseEmitter mockEmitter = mock(SseEmitter.class);
		Map<String, SseEmitter> emitters = Map.of(emitterId, mockEmitter);

		given(emitterRepository.findAllByMemberPublicId(publicId)).willReturn(emitters);

		// [핵심] send() 호출 시 IOException 발생 설정
		willThrow(new IOException("Broken pipe"))
			.given(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

		// when
		sseSupport.send(publicId, responseDto);

		// then
		// 1. send() 시도 확인
		then(mockEmitter).should(times(1)).send(any(SseEmitter.SseEventBuilder.class));

		// 2. 예외 발생 후 deleteById() 호출 확인 (Zombie Emitter 제거 로직)
		then(emitterRepository).should(times(1)).deleteById(emitterId);
	}

	@Test
	@DisplayName("전송(send): 저장된 Emitter가 없으면 아무 일도 일어나지 않는다.")
	void send_no_emitters() {
		// given
		String publicId = "ghost-user";

		// [수정] 생성자 사용
		NotificationResponseDto responseDto = new NotificationResponseDto(
			null, null, null, null, false, null, null
		);

		// 빈 Map 반환
		given(emitterRepository.findAllByMemberPublicId(publicId)).willReturn(Collections.emptyMap());

		// when
		sseSupport.send(publicId, responseDto);

		// then
		then(emitterRepository).should(times(1)).findAllByMemberPublicId(publicId);
		// 에러 없이 정상 종료됨을 확인
	}
}