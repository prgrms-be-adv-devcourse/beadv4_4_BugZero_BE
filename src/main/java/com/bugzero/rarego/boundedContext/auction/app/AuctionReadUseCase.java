package com.bugzero.rarego.boundedContext.auction.app;

import static com.bugzero.rarego.boundedContext.auction.domain.AuctionViewerRoleStatus.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.*;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistListResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.*;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.auction.dto.*;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;
import com.bugzero.rarego.shared.product.out.ProductApiClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionReadUseCase {

	private final AuctionSupport support;
	private final BidRepository bidRepository;
	private final AuctionMemberRepository auctionMemberRepository;
	private final AuctionRepository auctionRepository;
	private final AuctionOrderRepository auctionOrderRepository;
	private final AuctionBookmarkRepository auctionBookmarkRepository;
	private final ProductApiClient productApiClient;

	// 경매 입찰 기록 조회
	public PagedResponseDto<BidLogResponseDto> getBidLogs(Long auctionId, Pageable pageable) {
		Page<Bid> bidPage = bidRepository.findAllByAuctionIdOrderByBidTimeDesc(auctionId, pageable);
		Map<Long, String> bidderMap = getBidderPublicIdMap(bidPage.getContent());

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
		AuctionMember member = support.getPublicMember(memberPublicId);
		Page<Bid> bidPage = bidRepository.findAllByBidderIdAndAuctionStatus(member.getId(), status, pageable);
		List<Bid> bids = bidPage.getContent();
		Map<Long, Auction> auctionMap = getAuctionMap(bids);

		Page<MyBidResponseDto> dtoPage = bidPage.map(bid -> {
			Auction auction = auctionMap.get(bid.getAuctionId());
			if (auction == null) throw new CustomException(ErrorType.AUCTION_NOT_FOUND);
			return MyBidResponseDto.from(bid, auction);
		});

		return new PagedResponseDto<>(dtoPage.getContent(), PageDto.from(dtoPage));
	}

	// 나의 판매 내역 조회
	public PagedResponseDto<MySaleResponseDto> getMySales(String memberPublicId, AuctionFilterType auctionFilterType, Pageable pageable) {
		AuctionMember member = support.getPublicMember(memberPublicId);

		// [변경] Client를 통해 판매자의 상품 ID 목록 조회
		List<Long> myProductIds = productApiClient.getProductIdsBySellerId(member.getId());

		Page<Auction> auctionPage = fetchAuctionsByFilter(myProductIds, auctionFilterType, pageable);
		List<Auction> auctions = auctionPage.getContent();

		if (auctions.isEmpty()) {
			return new PagedResponseDto<>(List.of(), PageDto.from(auctionPage));
		}

		Set<Long> productIds = auctions.stream().map(Auction::getProductId).collect(Collectors.toSet());
		Set<Long> auctionIds = auctions.stream().map(Auction::getId).collect(Collectors.toSet());

		// [변경] Client를 통해 상품 정보 Bulk 조회
		Map<Long, ProductAuctionResponseDto> productMap = productApiClient.getProducts(productIds).stream()
			.collect(Collectors.toMap(ProductAuctionResponseDto::getId, p -> p));

		Map<Long, AuctionOrder> orderMap = auctionOrderRepository.findAllByAuctionIdIn(auctionIds).stream()
			.collect(Collectors.toMap(AuctionOrder::getAuctionId, Function.identity()));

		Map<Long, Integer> bidCountMap = bidRepository.countByAuctionIdIn(auctionIds).stream()
			.collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

		List<MySaleResponseDto> dtoList = auctions.stream()
			.map(auction -> MySaleResponseDto.from(
				auction,
				productMap.get(auction.getProductId()), // DTO 전달 (MySaleResponseDto.from 수정 필요)
				orderMap.get(auction.getId()),
				bidCountMap.getOrDefault(auction.getId(), 0)))
			.toList();

		return new PagedResponseDto<>(dtoList, PageDto.from(auctionPage));
	}

	// 경매 상세 조회
	public AuctionDetailResponseDto getAuctionDetail(Long auctionId, String memberPublicId) {
		AuctionMember member = (memberPublicId != null) ? support.getPublicMember(memberPublicId) : null;
		Auction auction = support.findAuctionById(auctionId);

		// [변경] 상품 상세 정보 조회 (이미지 포함)
		ProductAuctionResponseDto product = productApiClient.getProduct(auction.getProductId())
			.orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));

		Bid highestBid = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId).orElse(null);
		Bid myLastBid = (member != null)
			? bidRepository.findTopByAuctionIdAndBidderIdOrderByBidAmountDesc(auctionId, member.getId()).orElse(null)
			: null;

		return AuctionDetailResponseDto.from(
			auction,
			product.getName(),
			product.getDescription(),
			product.getImageUrls(), // DTO에 포함된 이미지 URL 사용
			highestBid,
			myLastBid,
			(member != null) ? member.getId() : null);
	}

	// 낙찰 기록 상세 조회
	public AuctionOrderResponseDto getAuctionOrder(Long auctionId, String memberPublicId) {
		AuctionMember member = support.getPublicMember(memberPublicId);
		AuctionOrder order = support.getOrder(auctionId);
		Auction auction = support.findAuctionById(auctionId);

		AuctionViewerRoleStatus viewerRole = determineViewerRole(order, member.getId());
		if (viewerRole == GUEST) throw new CustomException(ErrorType.AUCTION_ORDER_ACCESS_DENIED);

		// [변경] 상품 정보 조회
		ProductAuctionResponseDto product = productApiClient.getProduct(auction.getProductId())
			.orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));

		Long traderId = viewerRole == BUYER ? order.getSellerId() : order.getBidderId();
		AuctionMember trader = support.getMember(traderId);
		String statusDescription = convertStatusDescription(order.getStatus(), viewerRole);

		return AuctionOrderResponseDto.from(
			order,
			viewerRole.name(),
			statusDescription,
			product.getName(),
			product.getThumbnailUrl(),
			trader.getPublicId(),
			trader.getContactPhone());
	}

	// 경매 목록 조회 (Bulk + 검색)
	public PagedResponseDto<AuctionListResponseDto> getAuctions(AuctionSearchCondition condition, Pageable pageable) {
		Pageable sortedPageable = applySorting(pageable, condition.getSort());
		List<Long> matchedProductIds = null;

		// [변경] 검색 조건이 있을 경우 Client를 통해 Product ID 검색
		if (condition.getKeyword() != null || condition.getCategory() != null) {
			matchedProductIds = productApiClient.searchProductIds(condition.getKeyword(), condition.getCategory());
			if (matchedProductIds.isEmpty()) {
				return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(Page.empty()));
			}
		}

		// [변경] 검수 승인된 상품 ID 목록 조회
		List<Long> approvedProductIds = productApiClient.getApprovedProductIds();

		// 검색된 Product ID로 Auction 조회
		Page<Auction> auctionPage = auctionRepository.findAllBySearchConditions(
			condition.getIds(),
			condition.getStatus(),
			matchedProductIds,
			approvedProductIds,
			sortedPageable);

		List<Auction> auctions = auctionPage.getContent();
		if (auctions.isEmpty()) return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(auctionPage));

		Set<Long> productIds = auctions.stream().map(Auction::getProductId).collect(Collectors.toSet());
		Set<Long> auctionIds = auctions.stream().map(Auction::getId).collect(Collectors.toSet());

		// [변경] 상품 정보 Bulk 조회
		Map<Long, ProductAuctionResponseDto> productMap = productApiClient.getProducts(productIds).stream()
			.collect(Collectors.toMap(ProductAuctionResponseDto::getId, Function.identity()));

		Map<Long, Integer> bidCountMap = bidRepository.countByAuctionIdIn(auctionIds).stream()
			.collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

		List<AuctionListResponseDto> dtos = auctions.stream()
			.map(auction -> {
				ProductAuctionResponseDto product = productMap.get(auction.getProductId());
				String thumbnail = (product != null) ? product.getThumbnailUrl() : null;
				int bidCount = bidCountMap.getOrDefault(auction.getId(), 0);

				// AuctionListResponseDto.from 파라미터 수정 필요 (Product Entity -> ProductAuctionResponseDto)
				return AuctionListResponseDto.from(auction, product, thumbnail, bidCount);
			})
			.toList();

		return new PagedResponseDto<>(dtos, PageDto.from(auctionPage));
	}

	// 나의 낙찰 목록 조회
	public PagedResponseDto<MyAuctionOrderListResponseDto> getMyAuctionOrders(String memberPublicId, AuctionOrderStatus status, Pageable pageable) {
		AuctionMember member = support.getPublicMember(memberPublicId);
		Page<AuctionOrder> orderPage = auctionOrderRepository.findAllByBidderIdAndStatus(member.getId(), status, pageable);
		List<AuctionOrder> orders = orderPage.getContent();

		if (orders.isEmpty()) return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(orderPage));

		Set<Long> auctionIds = orders.stream().map(AuctionOrder::getAuctionId).collect(Collectors.toSet());
		Map<Long, Auction> auctionMap = auctionRepository.findAllById(auctionIds).stream()
			.collect(Collectors.toMap(Auction::getId, Function.identity()));

		Set<Long> productIds = auctionMap.values().stream().map(Auction::getProductId).collect(Collectors.toSet());

		// [변경] 상품 정보 Bulk 조회
		Map<Long, ProductAuctionResponseDto> productMap = productApiClient.getProducts(productIds).stream()
			.collect(Collectors.toMap(ProductAuctionResponseDto::getId, Function.identity()));

		List<MyAuctionOrderListResponseDto> dtos = orders.stream()
			.map(order -> {
				Auction auction = auctionMap.get(order.getAuctionId());
				if (auction == null) return null;

				ProductAuctionResponseDto product = productMap.get(auction.getProductId());
				String thumbnailUrl = (product != null) ? product.getThumbnailUrl() : null;

				return MyAuctionOrderListResponseDto.from(order, product, thumbnailUrl);
			})
			.filter(Objects::nonNull)
			.toList();

		return new PagedResponseDto<>(dtos, PageDto.from(orderPage));
	}

	// 내 관심 경매 목록 조회
	public PagedResponseDto<WishlistListResponseDto> getMyBookmarks(String memberPublicId, Pageable pageable) {
		AuctionMember member = support.getPublicMember(memberPublicId);
		Page<AuctionBookmark> bookmarkPage = auctionBookmarkRepository.findAllByMemberId(member.getId(), pageable);

		if (bookmarkPage.isEmpty()) return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(bookmarkPage));

		List<Long> auctionIds = bookmarkPage.getContent().stream().map(AuctionBookmark::getAuctionId).toList();
		List<Auction> auctions = auctionRepository.findAllById(auctionIds);

		List<AuctionListResponseDto> auctionDtos = convertToAuctionListDtos(auctions);
		Map<Long, AuctionListResponseDto> auctionDtoMap = auctionDtos.stream()
			.collect(Collectors.toMap(AuctionListResponseDto::auctionId, Function.identity()));

		List<WishlistListResponseDto> finalDtos = bookmarkPage.getContent().stream()
			.map(bookmark -> WishlistListResponseDto.of(bookmark.getId(), auctionDtoMap.get(bookmark.getAuctionId())))
			.toList();

		return new PagedResponseDto<>(finalDtos, PageDto.from(bookmarkPage));
	}

	// Helper Methods

	private List<AuctionListResponseDto> convertToAuctionListDtos(List<Auction> auctions) {
		if (auctions.isEmpty()) return Collections.emptyList();

		Set<Long> auctionIds = auctions.stream().map(Auction::getId).collect(Collectors.toSet());
		Set<Long> productIds = auctions.stream().map(Auction::getProductId).collect(Collectors.toSet());

		// [변경] Client 사용
		Map<Long, ProductAuctionResponseDto> productMap = productApiClient.getProducts(productIds).stream()
			.collect(Collectors.toMap(ProductAuctionResponseDto::getId, Function.identity()));

		Map<Long, Integer> bidCountMap = bidRepository.countByAuctionIdIn(auctionIds).stream()
			.collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

		return auctions.stream()
			.map(auction -> {
				ProductAuctionResponseDto product = productMap.get(auction.getProductId());
				String thumbnail = (product != null) ? product.getThumbnailUrl() : null;
				return AuctionListResponseDto.from(auction, product, thumbnail, bidCountMap.getOrDefault(auction.getId(), 0));
			})
			.toList();
	}

	private Map<Long, String> getBidderPublicIdMap(List<Bid> bids) {
		if (bids.isEmpty()) return Collections.emptyMap();
		Set<Long> bidderIds = bids.stream().map(Bid::getBidderId).collect(Collectors.toSet());
		return auctionMemberRepository.findAllById(bidderIds).stream()
			.collect(Collectors.toMap(AuctionMember::getId, AuctionMember::getPublicId));
	}

	private Map<Long, Auction> getAuctionMap(List<Bid> bids) {
		if (bids.isEmpty()) return Collections.emptyMap();
		Set<Long> auctionIds = bids.stream().map(Bid::getAuctionId).collect(Collectors.toSet());
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

	private String convertStatusDescription(AuctionOrderStatus status, AuctionViewerRoleStatus role) {
		if (status == AuctionOrderStatus.PROCESSING) return role == BUYER ? "결제 대기중" : "입금 대기중";
		else if (status == AuctionOrderStatus.SUCCESS) return "결제 완료";
		return "-";
	}

	private AuctionViewerRoleStatus determineViewerRole(AuctionOrder order, Long memberId) {
		if (Objects.equals(memberId, order.getBidderId())) return BUYER;
		if (Objects.equals(memberId, order.getSellerId())) return SELLER;
		return GUEST;
	}

	private Pageable applySorting(Pageable pageable, String sortStr) {
		if (sortStr == null) return pageable;
		Sort sort = Sort.unsorted();
		if ("CLOSING_SOON".equalsIgnoreCase(sortStr)) sort = Sort.by(Sort.Direction.ASC, "endTime");
		else if ("NEWEST".equalsIgnoreCase(sortStr)) sort = Sort.by(Sort.Direction.DESC, "createdAt");
		else sort = Sort.by(Sort.Direction.DESC, "id");
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}
}