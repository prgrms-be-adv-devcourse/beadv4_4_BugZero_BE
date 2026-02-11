package com.bugzero.rarego.ai.app;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import com.bugzero.rarego.ai.domain.dto.AiExternalPriceRequestDto;
import com.bugzero.rarego.ai.domain.type.TemporaryCondition;
import com.bugzero.rarego.shared.product.type.Category;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AiGetExternalPriceUseCaseTest {

	private AiGetExternalPriceUseCase useCase;

	@Mock
	private ChatModel chatModel; // ChatClient가 사용하는 핵심 엔진만 모킹

	Duration apiTimeout;

	@Test
	@DisplayName("단위 테스트: ChatClient를 직접 생성하여 검증")
	void executeTest() {
		// 1. 실제 ChatClient.Builder를 사용하여 ChatClient를 직접 만듭니다.
		// 이 방식은 내부 인터페이스를 모킹할 필요가 없어 오류가 나지 않습니다.
		apiTimeout = Duration.ofSeconds(30);
		ChatClient chatClient = ChatClient.builder(chatModel).build();
		useCase = new AiGetExternalPriceUseCase(chatClient, apiTimeout);

		// 2. 가짜 응답 설정
		String mockResponse = "4,500,000원";
		ChatResponse chatResponse = new ChatResponse(List.of(
			new Generation(new AssistantMessage(mockResponse))
		));

		// .stream() 호출 시 반환될 Flux 설정
		given(chatModel.stream(any(Prompt.class))).willReturn(Flux.just(chatResponse));

		// when
		Flux<String> result = useCase.execute(new AiExternalPriceRequestDto(
			Category.STARWARS, TemporaryCondition.MISB, "75192"
		));

		// then
		StepVerifier.create(result)
			.expectNext(mockResponse)
			.verifyComplete();
	}
}