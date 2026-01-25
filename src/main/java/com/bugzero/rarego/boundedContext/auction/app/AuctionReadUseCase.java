package com.bugzero.rarego.boundedContext.auction.app;

import static com.bugzero.rarego.boundedContext.auction.domain.AuctionViewerRoleStatus.*;

import java.util.Collections;
import java.util.Comparator;
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
import com.bugzero.rarego.boundedContext.product.app.ProductCreateS3PresignerUrlUseCase;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductImage;
import com.bugzero.rarego.boundedContext.product.out.ProductImageRepository;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.auction.dto.*;

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
	private final ProductRepository productRepository;
	private final AuctionOrderRepository auctionOrderRepository;
	private final ProductImageRepository productImageRepository;
	private final AuctionBookmarkRepository auctionBookmarkRepository;
	private final ProductCreateS3PresignerUrlUseCase s3PresignerUrlUseCase;

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
	public PagedResponseDto<MyBidResponseDto> getMyBids(String memberPublicId, AuctionStatus status,
			Pageable pageable) {
		AuctionMember member = support.getPublicMember(memberPublicId);

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
	public PagedResponseDto<MySaleResponseDto> getMySales(String memberPublicId, AuctionFilterType auctionFilterType,
			Pageable pageable) {
		// 회원 ID 조회
		AuctionMember member = support.getPublicMember(memberPublicId);

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
						bidCountMap.getOrDefault(auction.getId(), 0)))
				.toList();

		return new PagedResponseDto<>(dtoList, PageDto.from(auctionPage));
	}

	// 경매 상세 조회
	public AuctionDetailResponseDto getAuctionDetail(Long auctionId, String memberPublicId) {
		// 회원 ID 조회
		// 로그인한 경우에만 조회, 비로그인이면 null 처리
		AuctionMember member = null;
		if (memberPublicId != null) {
			member = support.getPublicMember(memberPublicId);
		}

		// 1. 경매 조회
		Auction auction = support.findAuctionById(auctionId);

		// 1-2. 상품 정보 조회
		Product product = productRepository.findById(auction.getProductId())
				.orElseThrow(() -> new CustomException(ErrorType.PRODUCT_NOT_FOUND));

		// 1-3. 이미지 목록 조회 (전체)
		List<ProductImage> productImages = productImageRepository.findAllByProductId(product.getId());
		List<String> imageUrls = productImages.stream()
				.sorted(Comparator.comparingInt(ProductImage::getSortOrder))
				.map(ProductImage::getImageUrl)
				.map(s3PresignerUrlUseCase::getPresignedGetUrl)
				.toList();

		// 2. 전체 최고가 입찰 조회
		Bid highestBid = bidRepository.findTopByAuctionIdOrderByBidAmountDesc(auctionId)
				.orElse(null);

		// 3. 나의 마지막 입찰 조회 (로그인 시에만)
		Bid myLastBid = null;
		if (member != null) {
			myLastBid = bidRepository.findTopByAuctionIdAndBidderIdOrderByBidAmountDesc(auctionId, member.getId())
					.orElse(null);
		}

		Long memberId = (member != null) ? member.getId() : null;

		// 4. DTO 변환 (memberId가 null이면 DTO 내부에서 기본값 false/null 처리)
		return AuctionDetailResponseDto.from(
				auction,
				product.getName(),
				product.getDescription(),
				imageUrls,
				highestBid,
				myLastBid,
				memberId);
	}

	// 낙찰 기록 상세 조회
	public AuctionOrderResponseDto getAuctionOrder(Long auctionId, String memberPublicId) {
		// 회원 ID 조회
		AuctionMember member = support.getPublicMember(memberPublicId);
		Long memberId = member.getId();

		AuctionOrder order = support.getOrder(auctionId);
		Auction auction = support.findAuctionById(auctionId);

		AuctionViewerRoleStatus viewerRole = determineViewerRole(order, memberId);

		if (viewerRole == GUEST) {
			throw new CustomException(ErrorType.AUCTION_ORDER_ACCESS_DENIED);
		}

		Product product = support.getProduct(auction.getProductId());

		List<ProductImage> productImages = productImageRepository.findAllByProductId(product.getId());

		String thumbnail = productImages.stream()
				.findFirst()
				.map(ProductImage::getImageUrl)
				.map(s3PresignerUrlUseCase::getPresignedGetUrl)
				.orElse(null);

		Long traderId = viewerRole == BUYER ? order.getSellerId() : order.getBidderId();
		AuctionMember trader = support.getMember(traderId);

		String statusDescription = convertStatusDescription(order.getStatus(), viewerRole);

		return AuctionOrderResponseDto.from(
				order,
				viewerRole.name(),
				statusDescription,
				product.getName(),
				thumbnail,
				trader.getPublicId(),
				trader.getContactPhone());
	}

	// 경매 목록 조회 (Bulk + 검색)
	public PagedResponseDto<AuctionListResponseDto> getAuctions(AuctionSearchCondition condition, Pageable pageable) {

		// 1. 정렬 조건 적용
		Pageable sortedPageable = applySorting(pageable, condition.getSort());

		// 2. 키워드/카테고리 검색 (Product 테이블 조회)
		List<Long> matchedProductIds = null;

		// 키워드나 카테고리 조건이 하나라도 있으면 Product 조회
		if (condition.getKeyword() != null || condition.getCategory() != null) {
			matchedProductIds = productRepository.findIdsBySearchCondition(
					condition.getKeyword(), condition.getCategory());

			// 검색 결과가 없으면 빈 페이지 반환 (최적화)
			if (matchedProductIds.isEmpty()) {
				return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(Page.empty()));
			}
		}

		// 3. 리포지토리 쿼리 메서드 호출

		Page<Auction> auctionPage = auctionRepository.findAllBySearchConditions(
				condition.getIds(),
				condition.getStatus(),
				matchedProductIds,
				sortedPageable);

		List<Auction> auctions = auctionPage.getContent();

		if (auctions.isEmpty()) {
			return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(auctionPage));
		}

		// 4. Bulk Fetching & 매핑
		Set<Long> auctionIds = auctions.stream().map(Auction::getId).collect(Collectors.toSet());
		Set<Long> productIds = auctions.stream().map(Auction::getProductId).collect(Collectors.toSet());

		Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
				.collect(Collectors.toMap(Product::getId, Function.identity()));

		Map<Long, Integer> bidCountMap = bidRepository.countByAuctionIdIn(auctionIds).stream()
				.collect(Collectors.toMap(
						row -> (Long) row[0],
						row -> ((Long) row[1]).intValue()));

		// 이미지 매핑 (기존 로직 유지)
		List<ProductImage> images = productImageRepository.findAllByProductIdIn(productIds);
		Map<Long, String> thumbnailMap = images.stream()
				.sorted(Comparator.comparingInt(ProductImage::getSortOrder))
				.collect(Collectors.toMap(
						img -> img.getProduct().getId(),
						img -> s3PresignerUrlUseCase.getPresignedGetUrl(img.getImageUrl()),
						(existing, replacement) -> existing));

		// 5. DTO 변환 (기존 로직 유지)
		List<AuctionListResponseDto> dtos = auctions.stream()
				.map(auction -> {
					Product product = productMap.get(auction.getProductId());
					String thumbnail = thumbnailMap.get(auction.getProductId());
					int bidCount = bidCountMap.getOrDefault(auction.getId(), 0);

					return AuctionListResponseDto.from(auction, product, thumbnail, bidCount);
				})
				.toList();

		return new PagedResponseDto<>(dtos, PageDto.from(auctionPage));
	}

	// 나의 낙찰 목록 조회
	public PagedResponseDto<MyAuctionOrderListResponseDto> getMyAuctionOrders(String memberPublicId,
			AuctionOrderStatus status, Pageable pageable) {
		AuctionMember member = support.getPublicMember(memberPublicId);

		// status가 null이면 전체, 있으면 필터링해서 가져옴
		Page<AuctionOrder> orderPage = auctionOrderRepository.findAllByBidderIdAndStatus(
				member.getId(),
				status,
				pageable);

		List<AuctionOrder> orders = orderPage.getContent();

		if (orders.isEmpty()) {
			return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(orderPage));
		}

		Set<Long> auctionIds = orders.stream().map(AuctionOrder::getAuctionId).collect(Collectors.toSet());

		Map<Long, Auction> auctionMap = auctionRepository.findAllById(auctionIds).stream()
				.collect(Collectors.toMap(Auction::getId, Function.identity()));

		Set<Long> productIds = auctionMap.values().stream()
				.map(Auction::getProductId)
				.collect(Collectors.toSet());

		Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
				.collect(Collectors.toMap(Product::getId, Function.identity()));

		List<ProductImage> images = productImageRepository.findAllByProductIdIn(productIds);
		Map<Long, String> thumbnailMap = images.stream()
				// sortOrder 오름차순 정렬 (0, 1, 2...)
				.sorted(Comparator.comparingInt(ProductImage::getSortOrder))
				.collect(Collectors.toMap(
						img -> img.getProduct().getId(),
						img -> s3PresignerUrlUseCase.getPresignedGetUrl(img.getImageUrl()),
						(existing, replacement) -> existing));

		List<MyAuctionOrderListResponseDto> dtos = orders.stream()
				.map(order -> {
					Auction auction = auctionMap.get(order.getAuctionId());

					// 경매가 없을 경우(데이터 무결성 문제)
					if (auction == null) {
						log.error("데이터 불일치 감지: 주문(ID={})은 존재하나 경매(ID={})가 없음",
								order.getId(), order.getAuctionId());
						return null;
					}

					Product product = productMap.get(auction.getProductId());
					String thumbnailUrl = thumbnailMap.get(auction.getProductId());

					return MyAuctionOrderListResponseDto.from(order, product, thumbnailUrl);
				})
				.filter(Objects::nonNull) // null 제외
				.toList();

		return new PagedResponseDto<>(dtos, PageDto.from(orderPage));
	}

	// 내 관심 경매 목록 조회
	public PagedResponseDto<WishlistListResponseDto> getMyBookmarks(String memberPublicId, Pageable pageable) {
		AuctionMember member = support.getPublicMember(memberPublicId);

		// 북마크 페이징 조회 (TODO: MSA 분리 시 BookmarkUsecase쪽으로 분리 예상됨, WishlistFacde 생성 필요)
		Page<AuctionBookmark> bookmarkPage = auctionBookmarkRepository.findAllByMemberId(member.getId(), pageable);

		if (bookmarkPage.isEmpty()) {
			return new PagedResponseDto<>(Collections.emptyList(), PageDto.from(bookmarkPage));
		}

		// 북마크된 경매 엔티티들 조회
		List<Long> auctionIds = bookmarkPage.getContent().stream().map(AuctionBookmark::getAuctionId).toList();
		List<Auction> auctions = auctionRepository.findAllById(auctionIds);

		// 공통 변환 로직 호출
		List<AuctionListResponseDto> auctionDtos = convertToAuctionListDtos(auctions);

		// 순서 유지를 위한 Map 생성
		Map<Long, AuctionListResponseDto> auctionDtoMap = auctionDtos.stream()
				.collect(Collectors.toMap(AuctionListResponseDto::auctionId, Function.identity()));

		// 최종 WishlistListResponseDto 조립
		List<WishlistListResponseDto> finalDtos = bookmarkPage.getContent().stream()
				.map(bookmark -> WishlistListResponseDto.of(
						bookmark.getId(),
						auctionDtoMap.get(bookmark.getAuctionId())))
				.toList();

		return new PagedResponseDto<>(finalDtos, PageDto.from(bookmarkPage));
	}

	// =========================================================================
	// Helper Methods (Private)
	// =========================================================================

	private List<AuctionListResponseDto> convertToAuctionListDtos(List<Auction> auctions) {
		if (auctions.isEmpty())
			return Collections.emptyList();

		Set<Long> auctionIds = auctions.stream().map(Auction::getId).collect(Collectors.toSet());
		Set<Long> productIds = auctions.stream().map(Auction::getProductId).collect(Collectors.toSet());

		Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
				.collect(Collectors.toMap(Product::getId, Function.identity()));

		Map<Long, Integer> bidCountMap = bidRepository.countByAuctionIdIn(auctionIds).stream()
				.collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

		List<ProductImage> images = productImageRepository.findAllByProductIdIn(productIds);
		Map<Long, String> thumbnailMap = images.stream()
				.sorted(Comparator.comparingInt(ProductImage::getSortOrder))
				.collect(Collectors.toMap(
						img -> img.getProduct().getId(),
						img -> s3PresignerUrlUseCase.getPresignedGetUrl(img.getImageUrl()),
						(e, r) -> e));

		return auctions.stream()
				.map(auction -> AuctionListResponseDto.from(
						auction,
						productMap.get(auction.getProductId()),
						thumbnailMap.get(auction.getProductId()),
						bidCountMap.getOrDefault(auction.getId(), 0)))
				.toList();
	}

	private Map<Long, String> getBidderPublicIdMap(List<Bid> bids) {
		if (bids.isEmpty())
			return Collections.emptyMap();

		Set<Long> bidderIds = bids.stream()
				.map(Bid::getBidderId)
				.collect(Collectors.toSet());

		return auctionMemberRepository.findAllById(bidderIds).stream()
				.collect(Collectors.toMap(AuctionMember::getId, AuctionMember::getPublicId));
	}

	private Map<Long, Auction> getAuctionMap(List<Bid> bids) {
		if (bids.isEmpty())
			return Collections.emptyMap();

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
	private String convertStatusDescription(AuctionOrderStatus status, AuctionViewerRoleStatus role) {
		if (status == AuctionOrderStatus.PROCESSING) {
			return role == BUYER ? "결제 대기중" : "입금 대기중";
		} else if (status == AuctionOrderStatus.SUCCESS) {
			return "결제 완료";
		}
		return "-";
	}

	private AuctionViewerRoleStatus determineViewerRole(AuctionOrder order, Long memberId) {
		if (Objects.equals(memberId, order.getBidderId()))
			return BUYER;
		if (Objects.equals(memberId, order.getSellerId()))
			return SELLER;
		return GUEST;
	}

	private Pageable applySorting(Pageable pageable, String sortStr) {
		if (sortStr == null)
			return pageable;

		Sort sort = Sort.unsorted();
		// 마감 임박한 순서
		if ("CLOSING_SOON".equalsIgnoreCase(sortStr)) {
			sort = Sort.by(Sort.Direction.ASC, "endTime");
			// 최신 순서
		} else if ("NEWEST".equalsIgnoreCase(sortStr)) {
			sort = Sort.by(Sort.Direction.DESC, "createdAt");
		}
		// TODO: 인기순(MOST_BIDS)은 입찰 수 정렬이므로 DB 컬럼이 없으면 복잡함.
		// 현재는 ID 역순(최신 등록순) 등을 기본으로 처리
		else {
			sort = Sort.by(Sort.Direction.DESC, "id");
		}
		return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
	}
}
