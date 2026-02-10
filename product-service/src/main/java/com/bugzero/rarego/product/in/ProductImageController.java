package com.bugzero.rarego.product.in;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.product.app.ProductImageS3UseCase;
import com.bugzero.rarego.product.domain.dto.PresignedUrlRequestDto;
import com.bugzero.rarego.product.domain.dto.PresignedUrlResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products/images")
@RequiredArgsConstructor
@Tag(name = "Product Image", description = "상품 이미지 관련 API")
public class ProductImageController {

	private final ProductImageS3UseCase s3PresignerUrlUseCase;

	@SecurityRequirement(name = "bearerAuth")
	@Operation(summary = "Presigned URL 발급", description = "S3 이미지 업로드용 Presigned URL을 발급합니다")
	@PostMapping("/presigned-url")
	public SuccessResponseDto<PresignedUrlResponseDto> getPresignedUrl(
		@Valid @RequestBody PresignedUrlRequestDto presignedUrlRequestDto
	) {
		return SuccessResponseDto.from(SuccessType.CREATED,
			s3PresignerUrlUseCase.createPresignerUrl(presignedUrlRequestDto));
	}

}
