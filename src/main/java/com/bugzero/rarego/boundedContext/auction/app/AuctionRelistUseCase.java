package com.bugzero.rarego.boundedContext.auction.app;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.auction.dto.AuctionRelistRequestDto;
import com.bugzero.rarego.shared.auction.dto.AuctionRelistResponseDto;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuctionRelistUseCase {

	private final AuctionSupport support; // 기존 Support 재사용
	private final AuctionRepository auctionRepository;
	private final AuctionOrderRepository auctionOrderRepository;

	@Transactional
	public AuctionRelistResponseDto relistAuction(Long oldAuctionId, String memberPublicId, AuctionRelistRequestDto request) {
		// 회원 및 기존 경매 조회
		AuctionMember seller = support.getPublicMember(memberPublicId);
		Auction oldAuction = support.findAuctionById(oldAuctionId);

		support.validateSeller(oldAuction, seller.getId());
		support.validateAuctionEnded(oldAuction);

		// 판매 완료 여부 검증 (이미 결제 완료된 건은 재등록 불가)
		validateCanRelist(oldAuction.getId());

		// 새 경매 생성
		Auction newAuction = Auction.builder()
			// 기존 상품 Id 재사용
			.productId(oldAuction.getProductId())
			.sellerId(seller.getId())
			.startPrice(request.getStartPrice().intValue())
			.startTime(LocalDateTime.now())
			.endTime(LocalDateTime.now().plusDays(request.getDurationDays()))
			.durationDays(request.getDurationDays())
			.build();

		// *참고: Auction 생성자에서 status는 기본적으로 SCHEDULED로 설정됨
		// TODO: 이 부분은 바로 IN_PROGRESS로 해야할지 기본 생성자대로 SCHEDULED로 해야할지 결정 필요
		Auction savedAuction = auctionRepository.save(newAuction);

		return AuctionRelistResponseDto.builder()
			.newAuctionId(savedAuction.getId())
			.productId(savedAuction.getProductId())
			.status(savedAuction.getStatus())
			.message("동일 상품 재경매가 성공적으로 생성되었습니다.")
			.build();
	}

	// 재등록 가능 여부 확인 (낙찰되었으나 결제 완료된 건이 있는지)
	private void validateCanRelist(Long auctionId) {
		support.findOrder(auctionId).ifPresent(order -> {
			if (order.getStatus() == AuctionOrderStatus.SUCCESS) {
				throw new CustomException(ErrorType.AUCTION_ALREADY_SOLD, "이미 판매가 완료된 상품입니다.");
			}
		});
	}
}
