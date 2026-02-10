package com.bugzero.rarego.in;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bugzero.rarego.app.AuctionFacade;
import com.bugzero.rarego.app.AuctionOrderService;
import com.bugzero.rarego.app.AuctionSettleAuctionFacade;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.global.response.SuccessType;
import com.bugzero.rarego.in.dto.AuctionAutoSettleResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionRequestDto;
import com.bugzero.rarego.shared.product.dto.ProductAuctionUpdateDto;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("api/v1/internal/auctions")
@RequiredArgsConstructor
@Tag(name = "Internal - Auction", description = "내부 경매 API (시스템 전용)")
@Hidden
public class InternalAuctionController {

	private final AuctionSettleAuctionFacade facade;
	private final AuctionFacade auctionFacade;
	private final AuctionOrderService auctionOrderService;

	@Operation(summary = "경매 정산", description = "종료된 경매를 정산합니다")
	@PostMapping("/settle")
	public SuccessResponseDto<AuctionAutoSettleResponseDto> settle() {
		return SuccessResponseDto.from(SuccessType.OK, facade.settle());
	}

	@Operation(summary = "진행 중인 입찰이 있는지 확인", description = "진행 중인 입찰이 있는지 확인합니다")
	@GetMapping("/members/{publicId}/bids/active")
	public SuccessResponseDto<Boolean> hasActiveBids(@PathVariable String publicId) {
		return SuccessResponseDto.from(SuccessType.OK, auctionFacade.hasActiveBids(publicId));
	}

	@Operation(summary = "진행 중인 판매가 있는지 확인", description = "진행 중인 판매가 있는지 확인합니다")
	@GetMapping("/members/{publicId}/sales/active")
	public SuccessResponseDto<Boolean> hasActiveSales(@PathVariable String publicId) {
		return SuccessResponseDto.from(SuccessType.OK, auctionFacade.hasActiveSales(publicId));
	}

	@GetMapping("/members/{publicId}/orders/processing")
	public SuccessResponseDto<Boolean> hasProcessingOrders(@PathVariable String publicId) {
		return SuccessResponseDto.from(SuccessType.OK, auctionFacade.hasProcessingOrders(publicId));
	}

	@Operation(summary = "경매 주문 조회", description = "auctionId 기준 주문 정보를 조회합니다.")
	@GetMapping("/orders/{auctionId}")
	public SuccessResponseDto<AuctionOrderDto> getOrder(@PathVariable Long auctionId) {
		AuctionOrderDto order = auctionOrderService.findByAuctionId(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_ORDER_NOT_FOUND));
		return SuccessResponseDto.from(SuccessType.OK, order);
	}

	@Operation(summary = "경매 주문 완료 처리", description = "auctionId 기준 주문을 완료 처리합니다.")
	@PostMapping("/orders/{auctionId}/complete")
	public SuccessResponseDto<Void> completeOrder(@PathVariable Long auctionId) {
		auctionOrderService.completeOrder(auctionId);
		return SuccessResponseDto.from(SuccessType.OK);
	}

	@Operation(summary = "경매 주문 실패 처리", description = "auctionId 기준 주문을 실패 처리합니다.")
	@PostMapping("/orders/{auctionId}/fail")
	public SuccessResponseDto<Void> failOrder(@PathVariable Long auctionId) {
		auctionOrderService.failOrder(auctionId);
		return SuccessResponseDto.from(SuccessType.OK);
	}

	@Operation(summary = "경매 주문 환불 처리", description = "auctionId 기준 주문을 환불 처리합니다.")
	@PostMapping("/orders/{auctionId}/refund")
	public SuccessResponseDto<AuctionOrderDto> refundOrder(@PathVariable Long auctionId) {
		AuctionOrderDto order = auctionOrderService.refundOrderWithLock(auctionId);
		return SuccessResponseDto.from(SuccessType.OK, order);
	}

	@Operation(summary = "결제 타임아웃 주문 조회", description = "deadline 이전 생성된 PROCESSING 주문을 조회합니다.")
	@GetMapping("/orders/timeout")
	public ResponseEntity<SuccessResponseDto<List<AuctionOrderDto>>> findTimeoutOrders(
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime deadline,
		@RequestParam int page,
		@RequestParam int size
	) {
		Slice<AuctionOrderDto> slice = auctionOrderService.findTimeoutOrders(deadline, PageRequest.of(page, size));
		return ResponseEntity.ok()
			.header("X-Has-Next", Boolean.toString(slice.hasNext()))
			.body(SuccessResponseDto.from(SuccessType.OK, slice.getContent()));
	}

	@Operation(summary = "결제 마감 임박 주문 조회", description = "targetEndedAt 이전 종료된 PROCESSING 주문 중 알림 미발송 건을 조회합니다.")
	@GetMapping("/orders/expiring-soon")
	public ResponseEntity<SuccessResponseDto<List<AuctionOrderDto>>> findExpiringSoonOrders(
		@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime targetEndedAt,
		@RequestParam int page,
		@RequestParam int size
	) {
		Slice<AuctionOrderDto> slice = auctionOrderService.findExpiringSoonOrders(
			targetEndedAt,
			PageRequest.of(page, size)
		);

		return ResponseEntity.ok()
			.header("X-Has-Next", Boolean.toString(slice.hasNext()))
			.body(SuccessResponseDto.from(SuccessType.OK, slice.getContent()));
	}

	@Operation(summary = "알림 발송 완료 처리", description = "주문의 알림 발송 시각(noticedAt)을 업데이트하여 중복 발송을 방지합니다.")
	@PatchMapping("/orders/{orderId}/notice")
	public ResponseEntity<Void> markAsNoticed(@PathVariable Long orderId) {
		auctionOrderService.markAsNoticed(orderId);

		return ResponseEntity.ok().build();
	}

	@Operation(summary = "경매정보 생성", description = "신규 상품 경매 정보를 생성합니다.")
	@PostMapping("/{productId}/{publicId}")
	public SuccessResponseDto<Long> createAuction(
		@PathVariable Long productId,
		@PathVariable String publicId,
		@Valid @RequestBody ProductAuctionRequestDto productAuctionRequestDto
	) {
		return SuccessResponseDto.from(SuccessType.CREATED,
			auctionFacade.createAuction(productId, publicId, productAuctionRequestDto));
	}

	@Operation(summary = "경매정보 수정", description = "검수 확정 전인 경매 정보를 수정합니다.")
	@PatchMapping("/{publicId}")
	public SuccessResponseDto<Long> updateAuction(
		@PathVariable String publicId,
		@Valid @RequestBody ProductAuctionUpdateDto productAuctionUpdateDto
	) {
		return SuccessResponseDto.from(SuccessType.OK,
			auctionFacade.updateAuction(publicId, productAuctionUpdateDto));
	}

	@Operation(summary = "경매정보 삭제", description = "검수 확정 전인 경매 정보를 삭제합니다.")
	@DeleteMapping("/{productId}/{publicId}")
	public SuccessResponseDto<Void> deleteAuction(
		@PathVariable String publicId,
		@PathVariable Long productId
	) {
		auctionFacade.deleteAuction(publicId, productId);
		return SuccessResponseDto.from(SuccessType.OK);
	}
}

