package com.bugzero.rarego.boundedContext.auction.app;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrder;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionOrderStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionViewerRoleStatus;
import com.bugzero.rarego.boundedContext.auction.domain.Bid;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductImage;
import com.bugzero.rarego.boundedContext.product.out.ProductImageRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.shared.auction.dto.AuctionFilterType;
import com.bugzero.rarego.shared.auction.dto.AuctionOrderResponseDto;
import com.bugzero.rarego.shared.auction.dto.BidLogResponseDto;
import com.bugzero.rarego.shared.auction.dto.MyBidResponseDto;
import com.bugzero.rarego.shared.auction.dto.MySaleResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionReadUseCase {

	private final BidRepository bidRepository;
	private final AuctionMemberRepository auctionMemberRepository;
	private final AuctionRepository auctionRepository;
	private final ProductRepository productRepository;
	private final AuctionOrderRepository auctionOrderRepository;
	private final ProductImageRepository productImageRepository;

	// 경매 입찰 기록 조회
	public PagedResponseDto<BidLogResponseDto> getBidLogs(Long auctionId, Pageable pageable) {
		Page<Bid> bidPage = bidRepository.findAllByAuctionIdOrderByBidTimeDesc(auctionId, pageable);

		// 1. 입찰자 정보 Map 생성 (Helper 사용)
		Map<Long, String> bidderMap = getBidderPublicIdMap(bidPage.getContent());

		// 2. DTO 변환
		Page<BidLogResponseDto> dtoPage = bidPage.map(bid -> {
			String publicId = bidderMap.get(bid.getBidderId());
			if (publicId == null) {
				log.warn("입찰자 정보 없음: bidderId={}", bid.getBidderId());
				publicId = "unknown";
			}
			return BidLogResponseDto.from(bid, publicId);
		});

		return new PagedResponseDto<>(dtoPage.getContent(), PageDto.from(dtoPage));
	}

	// 나의 입찰 내역 조회
	public PagedResponseDto<MyBidResponseDto> getMyBids(String memberPublicId, AuctionStatus status, Pageable pageable) {
		AuctionMember member = auctionMemberRepository.findByPublicId(memberPublicId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

		// 1. Bid 목록 조회
		Page<Bid> bidPage = bidRepository.findAllByBidderIdAndAuctionStatus(member.getId(), status, pageable);
		List<Bid> bids = bidPage.getContent();

		// 2. Auction Map 생성 (Helper 메서드 재사용)
		Map<Long, Auction> auctionMap = getAuctionMap(bids);

		// 3. DTO 변환
		Page<MyBidResponseDto> dtoPage = bidPage.map(bid -> {
			Auction auction = auctionMap.get(bid.getAuctionId());

			if (auction == null) {
				throw new CustomException(ErrorType.AUCTION_NOT_FOUND);
			}

			return MyBidResponseDto.from(bid, auction);
		});

		return new PagedResponseDto<>(dtoPage.getContent(), PageDto.from(dtoPage));
	}

	// 나의 판매 내역 조회
	public PagedResponseDto<MySaleResponseDto> getMySales(String memberPublicId, AuctionFilterType auctionFilterType, Pageable pageable) {
		// 회원 ID 조회
		AuctionMember member = auctionMemberRepository.findByPublicId(memberPublicId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

		// 상품 Id 목록 조회
		List<Long> myProductIds = productRepository.findAllIdsBySellerId(member.getId());

		// 경매 목록 조회
		Page<Auction> auctionPage = fetchAuctionsByFilter(myProductIds, auctionFilterType, pageable);
		List<Auction> auctions = auctionPage.getContent();

		if (auctions.isEmpty()) {
			return new PagedResponseDto<>(List.of(), PageDto.from(auctionPage));
		}

		// 연관 데이터 일괄적으로 조회
		Set<Long> productIds = auctions.stream().map(Auction::getProductId).collect(Collectors.toSet());
		Set<Long> auctionIds = auctions.stream().map(Auction::getId).collect(Collectors.toSet());

		// 상품 정보
		Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
			.collect(Collectors.toMap(Product::getId, p -> p));

		// 주문 정보 (거래 상태)
		Map<Long, AuctionOrder> orderMap = auctionOrderRepository.findAllByAuctionIdIn(auctionIds).stream()
			.collect(Collectors.toMap(AuctionOrder::getAuctionId, Function.identity()));

		// 입찰 횟수 정보
		Map<Long, Integer> bidCountMap = bidRepository.countByAuctionIdIn(auctionIds).stream()
			.collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

		// DTO 변환
		List<MySaleResponseDto> dtoList = auctions.stream()
			.map(auction -> MySaleResponseDto.from(
				auction,
				productMap.get(auction.getProductId()),
				orderMap.get(auction.getId()),
				bidCountMap.getOrDefault(auction.getId(), 0)
			))
			.toList();

		return new PagedResponseDto<>(dtoList, PageDto.from(auctionPage));
	}

	// 경매 상세 조회
	public AuctionDetailResponseDto getAuctionDetail(Long auctionId, String memberPublicId) {
		// 회원 ID 조회
		AuctionMember member = auctionMemberRepository.findByPublicId(memberPublicId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

		// 1. 경매 조회
		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

		// 2. 전체 최고가 입찰 조회
		Bid highestBid = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)
			.orElse(null);

		// 3. 나의 마지막 입찰 조회 (로그인 시에만)
		Bid myLastBid = null;
		if (member != null) {
			myLastBid = bidRepository.findTopByAuctionIdAndBidderIdOrderByBidAmountDesc(auctionId, member.getId())
				.orElse(null);
		}

		// 4. DTO 변환 (memberId가 null이면 DTO 내부에서 기본값 false/null 처리)
		return AuctionDetailResponseDto.from(auction, highestBid, myLastBid, member.getId());
	}

	// 낙찰 기록 상세 조회
	public AuctionOrderResponseDto getAuctionOrder(Long auctionId, String memberPublicId) {
		// 회원 ID 조회
		AuctionMember member = auctionMemberRepository.findByPublicId(memberPublicId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));
		Long memberId = member.getId();

		AuctionOrder order = auctionOrderRepository.findByAuctionId(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.ORDER_NOT_FOUND));

		Auction auction = auctionRepository.findById(auctionId)
			.orElseThrow(() -> new CustomException(ErrorType.AUCTION_NOT_FOUND));

		String viewerRole = determineViewerRole(order, memberId);

		if (viewerRole.equals("GUEST")) {
			throw new CustomException(ErrorType.AUCTION_ORDER_ACCESS_DENIED);
		}

		Product product = productRepository.findById(auction.getProductId())
			.orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));

		List<ProductImage> productImages = productImageRepository.findAllByProductId(product.getId());

		String thumbnailUrl = productImages.stream()
			// 썸네일 구분 로직이 있다면 filter 추가 (예: .filter(img -> img.getImageType() == ImageType.THUMBNAIL))
			.findFirst() // 구분 로직이 없다면 첫 번째 이미지를 썸네일로 사용
			.map(ProductImage::getImageUrl)
			.orElse(null); // 이미지가 없을 경우 null 처리

		Long traderId = viewerRole.equals("BUYER") ? order.getSellerId() : order.getBidderId();
		AuctionMember trader = auctionMemberRepository.findById(traderId)
			.orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

		String statusDescription = convertStatusDescription(order.getStatus(), viewerRole);

		return AuctionOrderResponseDto.from(
			order,
			viewerRole,
			statusDescription,
			product.getName(),
			thumbnailUrl,
			trader.getPublicId(),
			trader.getContactPhone()
		);
	}

	// =========================================================================
	//  Helper Methods (Private)
	// =========================================================================

	private Map<Long, String> getBidderPublicIdMap(List<Bid> bids) {
		if (bids.isEmpty()) return Collections.emptyMap();

		Set<Long> bidderIds = bids.stream()
			.map(Bid::getBidderId)
			.collect(Collectors.toSet());

		return auctionMemberRepository.findAllById(bidderIds).stream()
			.collect(Collectors.toMap(AuctionMember::getId, AuctionMember::getPublicId));
	}

	private Map<Long, Auction> getAuctionMap(List<Bid> bids) {
		if (bids.isEmpty()) return Collections.emptyMap();

		Set<Long> auctionIds = bids.stream()
			.map(Bid::getAuctionId)
			.collect(Collectors.toSet());

		return auctionRepository.findAllById(auctionIds).stream()
			.collect(Collectors.toMap(Auction::getId, Function.identity()));
	}

	private Page<Auction> fetchAuctionsByFilter(List<Long> productIds, AuctionFilterType filter, Pageable pageable) {
		switch (filter) {
			case ONGOING:
				return auctionRepository.findAllByProductIdInAndStatusIn(productIds,
					List.of(AuctionStatus.SCHEDULED, AuctionStatus.IN_PROGRESS), pageable);
			case COMPLETED:
			case ACTION_REQUIRED:
				return auctionRepository.findAllByProductIdInAndStatusIn(productIds,
					List.of(AuctionStatus.ENDED), pageable);
			default:
				return auctionRepository.findAllByProductIdIn(productIds, pageable);
		}
	}

	// 상태 설명 헬퍼 메서드
	private String convertStatusDescription(AuctionOrderStatus status, String role) {
		if (status == AuctionOrderStatus.PROCESSING) {
			return role.equals("BUYER") ? "결제 대기중" : "입금 대기중";
		} else if (status == AuctionOrderStatus.SUCCESS) {
			return "결제 완료";
		}
		return "-";
	}

	private String determineViewerRole(AuctionOrder order, Long memberId) {
		if (memberId.equals(order.getBidderId())) return AuctionViewerRoleStatus.BUYER.toString();
		if (memberId.equals(order.getSellerId())) return AuctionViewerRoleStatus.SELLER.toString();
		return AuctionViewerRoleStatus.GUEST.toString();
	}
}