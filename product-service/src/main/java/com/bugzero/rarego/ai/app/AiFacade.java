package com.bugzero.rarego.ai.app;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bugzero.rarego.ai.domain.dto.AiExternalPriceRequestDto;
import com.bugzero.rarego.ai.domain.dto.AiInternalPriceRequestDto;
import com.bugzero.rarego.ai.domain.dto.AiInternalPriceResponseDto;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AiFacade {

	private final AiGetExternalPriceUseCase aiGetExternalPriceUseCase;
	private final AiGetInternalPriceUseCase aiGetInternalPriceUseCase;
	private static final String TEMPLATE = "상품명: %s, 상세내용: %s";

	//외부시세 기반 경매시작가격 추천
	public Flux<String> getExternalPrice(AiExternalPriceRequestDto dto) {
		return aiGetExternalPriceUseCase.execute(dto);
	}

	//내부시세 기반 경매시작가격 추천
	public List<AiInternalPriceResponseDto> getInternalPrice(AiInternalPriceRequestDto dto) {
		return aiGetInternalPriceUseCase.findTopSimilarProducts(dto);
	}
}
