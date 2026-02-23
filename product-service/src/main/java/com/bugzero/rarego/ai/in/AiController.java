package com.bugzero.rarego.ai.in;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.ai.app.AiFacade;
import com.bugzero.rarego.ai.domain.dto.AiExternalPriceRequestDto;
import com.bugzero.rarego.ai.domain.dto.AiInternalPriceRequestDto;
import com.bugzero.rarego.ai.domain.dto.AiInternalPriceResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products/recommendations")
@Slf4j
public class AiController {

	private final AiFacade aiFacade;

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "외부시세 기반 AI 시작가 추천", description = "AI를 사용하여 유사 상품이 외부에서 가격이 어느정도에 형성되어있는지 알려줍니다.")
	@PreAuthorize("hasRole('SELLER')")
	@PostMapping(value = "/external-price", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> getExternalPriceGuide(@Valid @RequestBody AiExternalPriceRequestDto dto) {
		return aiFacade.getExternalPrice(dto);
	}

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "내부시세 기반 AI 시작가 추천", description = "AI를 사용하여 유사 상품이 외부에서 가격이 어느정도에 형성되어있는지 알려줍니다.")
	@PreAuthorize("hasRole('SELLER')")
	@PostMapping(value = "/internal-price")
	public SuccessResponseDto<List<AiInternalPriceResponseDto>> getInternalPriceGuide(
		@Valid @RequestBody AiInternalPriceRequestDto dto) {
		return SuccessResponseDto.from(SuccessType.OK, aiFacade.getInternalPrice(dto));
	}
}
