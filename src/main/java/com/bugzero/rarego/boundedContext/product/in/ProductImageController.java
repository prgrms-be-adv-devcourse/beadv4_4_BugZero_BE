package com.bugzero.rarego.boundedContext.product.in;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.boundedContext.product.app.ProductCreateS3PresignerUrlUseCase;
import com.bugzero.rarego.boundedContext.product.domain.dto.PresignedUrlRequestDto;
import com.bugzero.rarego.boundedContext.product.domain.dto.PresignedUrlResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/products/images")
@RequiredArgsConstructor
public class ProductImageController {

	private final ProductCreateS3PresignerUrlUseCase s3PresignerUrlUseCase;

	@PostMapping("/presigned-url")
	public SuccessResponseDto<PresignedUrlResponseDto>  getPresignedUrl(
		@Valid @RequestBody PresignedUrlRequestDto presignedUrlRequestDto
	) {
		return SuccessResponseDto.from(SuccessType.CREATED,
			s3PresignerUrlUseCase.createPresignerUrl(presignedUrlRequestDto));
	}

}
