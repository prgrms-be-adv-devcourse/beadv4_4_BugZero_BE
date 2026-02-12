package com.bugzero.rarego.app;

import static com.bugzero.rarego.domain.AuctionViewerRoleStatus.*;

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

import com.bugzero.rarego.domain.Auction;
import com.bugzero.rarego.domain.AuctionBookmark;
import com.bugzero.rarego.domain.AuctionMember;
import com.bugzero.rarego.domain.AuctionOrder;
import com.bugzero.rarego.domain.AuctionOrderStatus;
import com.bugzero.rarego.domain.AuctionViewerRoleStatus;
import com.bugzero.rarego.domain.Bid;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.in.dto.AuctionBookmarkListResponseDto;
import com.bugzero.rarego.in.dto.AuctionDetailResponseDto;
import com.bugzero.rarego.in.dto.AuctionFilterType;
import com.bugzero.rarego.in.dto.AuctionListResponseDto;
import com.bugzero.rarego.in.dto.AuctionOrderResponseDto;
import com.bugzero.rarego.in.dto.AuctionSearchCondition;
import com.bugzero.rarego.in.dto.BidLogResponseDto;
import com.bugzero.rarego.in.dto.MyAuctionOrderListResponseDto;
import com.bugzero.rarego.in.dto.MyBidResponseDto;
import com.bugzero.rarego.in.dto.MySaleResponseDto;
import com.bugzero.rarego.out.AuctionBookmarkRepository;
import com.bugzero.rarego.out.AuctionMemberRepository;
import com.bugzero.rarego.out.AuctionOrderRepository;
import com.bugzero.rarego.out.AuctionRepository;
import com.bugzero.rarego.out.BidRepository;
import com.bugzero.rarego.out.es.ProductSearchClient;
import com.bugzero.rarego.shared.auction.dto.AuctionSortType;
import com.bugzero.rarego.shared.auction.type.AuctionStatus;
import com.bugzero.rarego.shared.product.dto.ProductAuctionResponseDto;

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
	private final ProductSearchClient productSearchClient;

	// 경매 입찰 기록 조회
	public PagedResponseDto<BidLogResponseDto> getBidLogs(Long auctionId, Pageable pageable) {
		Page<Bid> bidPage = bidRepository.findAllByAuctionIdOrderByBidTimeDesc(auctionId, pageable);
		Map<Long, String> bidderMap = getBidderPublicIdMap(bidPage.getContent());

		Page<BidLogResponseDto> dtoPage = bidPage.map(bid -> {
			String publicId = bidderMap.getOrDefault(bid.getBidderId(), "unknown");
			return BidLogResponseDto.from(bid, publicId);
		});

		return new PagedResponseDto<>(dtoPage.getContent(), PageDto.from(dtoPage));
	}

	// 나의 입찰 내역 조회
	public PagedResponseDto<MyBidResponseDto> getMyBids(String memberPublicId, AuctionStatus status,
		Pageable pageable) {
		AuctionMember member = support.getPublicMember(memberPublicId);
		Page<Bid> bidPage = bidRepository.findAllByBidderIdAndAuctionStatus(member.getId(), status, pageable);
		List<Bid> bids = bidPage.getContent();
		Map<Long, Auction> auctionMap = getAuctionMap(bids);

		Page<MyBidResponseDto> dtoPage = bidPage.map(bid -> {
			Auction auction = auctionMap.get(bid.getAuctionId());
			if (auction == null)
				throw new CustomException(ErrorType.AUCTION_NOT_FOUND);
			return MyBidResponseDto.from(bid, auction);
		});

		return new PagedResponseDto<>(dtoPage.getContent(), PageDto.from(dtoPage));
	}

	// 나의 판매 내역 조회
	public PagedResponseDto<MySaleResponseDto> getMySales(String memberPublicId, AuctionFilterType auctionFilterType,
		Pageable pageable) {
		AuctionMember member = support.getPublicMember(memberPublicId);

		List<Long> myProductIds = productSearchClient.getProductIdsBySellerId(member.getId());

		Page<Auction> auctionPage = fetchAuctionsByFilter(myProductIds, auctionFilterType, pageable);
		List<Auction> auctions = auctionPage.getContent();

		if (auctions.isEmpty()) {
			return new PagedResponseDto<>(List.of(), PageDto.from(auctionPage));
		}

		Set<Long> productIds = auctions.stream().map(Auction::getProductId).collect(Collectors.toSet());
		Set<Long> auctionIds = auctions.stream().map(Auction::getId).collect(Collectors.toSet());

		Map<Long, ProductAuctionResponseDto> productMap = productSearchClient.getProducts(productIds);

		Map<Long, AuctionOrder> orderMap = auctionOrderRepository.findAllByAuctionIdIn(auctionIds).stream()
			.collect(Collectors.toMap(AuctionOrder::getAuctionId, Function.identity()));

		Map<Long, Integer> bidCountMap = bidRepository.countByAuctionIdIn(auctionIds).stream()
			.collect(Collectors.toMap(row -> (Long)row[0], row -> ((Long)row[1]).intValue()));

		List<MySaleResponseDto> dtoList = auctions.stream()
			.map(auction -> {
				ProductAuctionResponseDto product = productMap.getOrDefault(
					auction.getProductId(),
					ProductAuctionResponseDto.builder().id(auction.getProductId()).name("알 수 없음").build()
				);

				return MySaleResponseDto.from(
					auction,
					product,
					orderMap.get(auction.getId()),
					bidCountMap.getOrDefault(auction.getId(), 0)
				);
			})
			.toList();

		return new PagedResponseDto<>(dtoList, PageDto.from(auctionPage));
	}

	// 경매 상세 조회
	public AuctionDetailResponseDto getAuctionDetail(Long auctionId, String memberPublicId) {
		AuctionMember member = (memberPublicId != null) ? support.getPublicMember(memberPublicId) : null;
		Auction auction = support.findAuctionById(auctionId);

		// 단건 조회 (ES)
		ProductAuctionResponseDto product = productSearchClient.getProduct(auction.getProductId())
			.orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));

		Bid highestBid = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId).orElse(null);
		Bid myLastBid = (member != null)
			? bidRepository.findTopByAuctionIdAndBidderIdOrderByBidAmountDesc(auctionId, member.getId()).orElse(null)
			: null;

		return AuctionDetailResponseDto.from(
			auction,
			product.name(),
			product.description(),
			product.imageUrls(),
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
		if (viewerRole == GUEST)
			throw new CustomException(ErrorType.AUCTION_ORDER_ACCESS_DENIED);

		ProductAuctionResponseDto product = productSearchClient.getProduct(auction.getProductId())
			.orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));

		Long traderId = viewerRole == BUYER ? order.getSellerId() : order.getBidderId();
		AuctionMember trader = support.getMember(traderId);
		String statusDescription = convertStatusDescription(order.getStatus(), viewerRole);

		return AuctionOrderResponseDto.from(
			order,
			viewerRole.name(),
			statusDescription,
			product.name(),
			product.thumbnailUrl(),
			trader.getPublicId(),
			trader.getContactPhone());
	}

	// 경매 목록 조회
	public PagedResponseDto<AuctionListResponseDto> getAuctions(AuctionSearchCondition condition, Pageable pageable) {
		Pageable sortedPageable = applySorting(pageable, condition.getSort());
		List<Long> matchedProductIds = null;

		// 키워드/카테고리 검색 (ES)
		if (condition.getKeyword() != null || condition.getCategory() != null) {
			matchedProductIds = productSearchClient.searchProductIds(condition.getKeyword(), condition.getCategory());

			// 검색 결과가 없으면 빈 페이지 반환 (DB 조회 방지 최적화)
			if (matchedProductIds.isEmpty()) {
				return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(Page.empty()));
			}
		}

		// 승인된 상품 목록 조회 (ES)
		List<Long> approvedProductIds = productSearchClient.getApprovedProductIds();

		Page<Auction> auctionPage = auctionRepository.findAllBySearchConditions(
			condition.getIds(),
			condition.getStatus(),
			matchedProductIds,
			approvedProductIds,
			sortedPageable);

		List<Auction> auctions = auctionPage.getContent();
		if (auctions.isEmpty())
			return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(auctionPage));

		List<AuctionListResponseDto> dtos = convertToAuctionListDtos(auctions);

		return new PagedResponseDto<>(dtos, PageDto.from(auctionPage));
	}

	// 나의 낙찰 목록 조회
	public PagedResponseDto<MyAuctionOrderListResponseDto> getMyAuctionOrders(String memberPublicId,
		AuctionOrderStatus status, Pageable pageable) {
		AuctionMember member = support.getPublicMember(memberPublicId);
		Page<AuctionOrder> orderPage = auctionOrderRepository.findAllByBidderIdAndStatus(member.getId(), status,
			pageable);
		List<AuctionOrder> orders = orderPage.getContent();

		if (orders.isEmpty())
			return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(orderPage));

		Set<Long> auctionIds = orders.stream().map(AuctionOrder::getAuctionId).collect(Collectors.toSet());
		Map<Long, Auction> auctionMap = auctionRepository.findAllById(auctionIds).stream()
			.collect(Collectors.toMap(Auction::getId, Function.identity()));

		Set<Long> productIds = auctionMap.values().stream().map(Auction::getProductId).collect(Collectors.toSet());

		Map<Long, ProductAuctionResponseDto> productMap = productSearchClient.getProducts(productIds);

		List<MyAuctionOrderListResponseDto> dtos = orders.stream()
			.map(order -> {
				Auction auction = auctionMap.get(order.getAuctionId());
				if (auction == null)
					return null;

				ProductAuctionResponseDto product = productMap.get(auction.getProductId());
				// null safe 처리
				if (product == null) {
					product = ProductAuctionResponseDto.builder().id(auction.getProductId()).name("삭제된 상품").build();
				}
				String thumbnailUrl = product.thumbnailUrl();

				return MyAuctionOrderListResponseDto.from(order, product, thumbnailUrl);
			})
			.filter(Objects::nonNull)
			.toList();

		return new PagedResponseDto<>(dtos, PageDto.from(orderPage));
	}

	// 내 관심 경매 목록 조회
	public PagedResponseDto<AuctionBookmarkListResponseDto> getMyBookmarks(String memberPublicId, Pageable pageable) {
		AuctionMember member = support.getPublicMember(memberPublicId);
		Page<AuctionBookmark> bookmarkPage = auctionBookmarkRepository.findAllByMemberId(member.getId(), pageable);

		if (bookmarkPage.isEmpty())
			return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(bookmarkPage));

		List<Long> auctionIds = bookmarkPage.getContent().stream().map(AuctionBookmark::getAuctionId).toList();
		List<Auction> auctions = auctionRepository.findAllById(auctionIds);

		// Client 사용된 Helper 메서드 호출
		List<AuctionListResponseDto> auctionDtos = convertToAuctionListDtos(auctions);

		Map<Long, AuctionListResponseDto> auctionDtoMap = auctionDtos.stream()
			.collect(Collectors.toMap(AuctionListResponseDto::auctionId, Function.identity()));

		List<AuctionBookmarkListResponseDto> finalDtos = bookmarkPage.getContent().stream()
			.map(bookmark -> AuctionBookmarkListResponseDto.of(bookmark.getId(),
				auctionDtoMap.get(bookmark.getAuctionId())))
			.toList();

		return new PagedResponseDto<>(finalDtos, PageDto.from(bookmarkPage));
	}

	// === Helper Methods ===

	private List<AuctionListResponseDto> convertToAuctionListDtos(List<Auction> auctions) {
		if (auctions.isEmpty())
			return Collections.emptyList();

		Set<Long> auctionIds = auctions.stream().map(Auction::getId).collect(Collectors.toSet());
		Set<Long> productIds = auctions.stream().map(Auction::getProductId).collect(Collectors.toSet());

		// [변경] Bulk 조회 (ES)
		Map<Long, ProductAuctionResponseDto> productMap = productSearchClient.getProducts(productIds);

		Map<Long, Integer> bidCountMap = bidRepository.countByAuctionIdIn(auctionIds).stream()
			.collect(Collectors.toMap(row -> (Long)row[0], row -> ((Long)row[1]).intValue()));

		return auctions.stream()
			.map(auction -> {
				ProductAuctionResponseDto product = productMap.get(auction.getProductId());
				String thumbnail = (product != null) ? product.thumbnailUrl() : null;
				int bidCount = bidCountMap.getOrDefault(auction.getId(), 0);

				return AuctionListResponseDto.from(
					auction,
					product,
					thumbnail,
					bidCount
				);
			})
			.toList();
	}

	private Map<Long, String> getBidderPublicIdMap(List<Bid> bids) {
		if (bids.isEmpty())
			return Collections.emptyMap();
		Set<Long> bidderIds = bids.stream().map(Bid::getBidderId).collect(Collectors.toSet());
		return auctionMemberRepository.findAllById(bidderIds).stream()
			.collect(Collectors.toMap(AuctionMember::getId, AuctionMember::getPublicId));
	}

	private Map<Long, Auction> getAuctionMap(List<Bid> bids) {
		if (bids.isEmpty())
			return Collections.emptyMap();
		Set<Long> auctionIds = bids.stream().map(Bid::getAuctionId).collect(Collectors.toSet());
		return auctionRepository.findAllById(auctionIds).stream()
			.collect(Collectors.toMap(Auction::getId, Function.identity()));
	}

	private Page<Auction> fetchAuctionsByFilter(List<Long> productIds, AuctionFilterType filter, Pageable pageable) {
		if (filter == null) {
			return auctionRepository.findAllByProductIdIn(productIds, pageable);
		}
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
		if (status == AuctionOrderStatus.PROCESSING)
			return role == BUYER ? "결제 대기중" : "입금 대기중";
		else if (status == AuctionOrderStatus.SUCCESS)
			return "결제 완료";
		return "-";
	}

	private AuctionViewerRoleStatus determineViewerRole(AuctionOrder order, Long memberId) {
		if (Objects.equals(memberId, order.getBidderId()))
			return BUYER;
		if (Objects.equals(memberId, order.getSellerId()))
			return SELLER;
		return GUEST;
	}

	private Pageable applySorting(Pageable pageable, AuctionSortType sortType) {
		Sort sort = (sortType != null) ? sortType.getSort() : Sort.unsorted();
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}
}
