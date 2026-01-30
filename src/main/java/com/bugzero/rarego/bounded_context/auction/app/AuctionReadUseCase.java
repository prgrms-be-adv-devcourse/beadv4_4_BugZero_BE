package com.bugzero.rarego.bounded_context.auction.app;

import com.bugzero.rarego.bounded_context.auction.domain.*;
import com.bugzero.rarego.bounded_context.auction.in.dto.AuctionBookmarkListResponseDto;
import com.bugzero.rarego.bounded_context.auction.out.*;
import com.bugzero.rarego.bounded_context.product.domain.Product;
import com.bugzero.rarego.bounded_context.product.domain.ProductImage;
import com.bugzero.rarego.bounded_context.product.out.ProductImageRepository;
import com.bugzero.rarego.bounded_context.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.shared.auction.dto.*;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.bugzero.rarego.bounded_context.auction.domain.AuctionViewerRoleStatus.*;

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
        AuctionMember member = support.getMember(memberPublicId);

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
        AuctionMember member = support.getMember(memberPublicId);

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
        // 로그인한 경우에만 조회, 비로그인이면 null 처리
        AuctionMember member = null;
        if (memberPublicId != null) {
            member = support.getMember(memberPublicId);
        }

        // 1. 경매 조회
        Auction auction = support.findAuctionById(auctionId);

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
        return AuctionDetailResponseDto.from(auction, highestBid, myLastBid, memberId);
    }

    // 낙찰 기록 상세 조회
    public AuctionOrderResponseDto getAuctionOrder(Long auctionId, String memberPublicId) {
        // 회원 ID 조회
        AuctionMember member = support.getMember(memberPublicId);
        Long memberId = member.getId();

        AuctionOrder order = support.getOrder(auctionId);
        Auction auction = support.findAuctionById(auctionId);

        AuctionViewerRoleStatus viewerRole = determineViewerRole(order, memberId);

        if (viewerRole == GUEST) {
            throw new CustomException(ErrorType.AUCTION_ORDER_ACCESS_DENIED);
        }

        Product product = support.getProduct(auction.getProductId());

        List<ProductImage> productImages = productImageRepository.findAllByProductId(product.getId());

        String thumbnailUrl = productImages.stream()
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null);

        Long traderId = viewerRole == BUYER ? order.getSellerId() : order.getBidderId();
        AuctionMember trader = support.getMember(traderId);

        String statusDescription = convertStatusDescription(order.getStatus(), viewerRole);

        return AuctionOrderResponseDto.from(
                order,
                viewerRole.name(),
                statusDescription,
                product.getName(),
                thumbnailUrl,
                trader.getPublicId(),
                trader.getContactPhone()
        );
    }

    // 경매 목록 조회 (Bulk + 검색)
    public PagedResponseDto<AuctionListResponseDto> getAuctions(AuctionSearchCondition condition, Pageable pageable) {
        Pageable sortedPageable = applySorting(pageable, condition.getSort());
        Specification<Auction> spec = createSearchSpec(condition);

        Page<Auction> auctionPage = auctionRepository.findAll(spec, sortedPageable);

        List<AuctionListResponseDto> dtos = convertToAuctionListDtos(auctionPage.getContent());

        return new PagedResponseDto<>(dtos, PageDto.from(auctionPage));
    }

    // 내 관심 경매 목록 조회
    public PagedResponseDto<AuctionBookmarkListResponseDto> getMyBookmarks(String memberPublicId, Pageable pageable) {
        AuctionMember member = support.getMember(memberPublicId);

        // 북마크 페이징 조회 (TODO: MSA 분리 시 BookmarkUsecase쪽으로 분리 예상됨, bookmarkFacade 생성 필요)
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

        // 최종 AuctionBookmarkListResponseDto 조립
        List<AuctionBookmarkListResponseDto> finalDtos = bookmarkPage.getContent().stream()
                .map(bookmark -> AuctionBookmarkListResponseDto.of(
                        bookmark.getId(),
                        auctionDtoMap.get(bookmark.getAuctionId())
                ))
                .toList();

        return new PagedResponseDto<>(finalDtos, PageDto.from(bookmarkPage));
    }

    // =========================================================================
    //  Helper Methods (Private)
    // =========================================================================

    private List<AuctionListResponseDto> convertToAuctionListDtos(List<Auction> auctions) {
        if (auctions.isEmpty()) return Collections.emptyList();

        Set<Long> auctionIds = auctions.stream().map(Auction::getId).collect(Collectors.toSet());
        Set<Long> productIds = auctions.stream().map(Auction::getProductId).collect(Collectors.toSet());

        Map<Long, Product> productMap = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        Map<Long, Integer> bidCountMap = bidRepository.countByAuctionIdIn(auctionIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

        List<ProductImage> images = productImageRepository.findAllByProductIdIn(productIds);
        Map<Long, String> thumbnailMap = images.stream()
                .sorted(Comparator.comparingInt(ProductImage::getSortOrder))
                .collect(Collectors.toMap(img -> img.getProduct().getId(), ProductImage::getImageUrl, (e, r) -> e));

        return auctions.stream()
                .map(auction -> AuctionListResponseDto.from(
                        auction,
                        productMap.get(auction.getProductId()),
                        thumbnailMap.get(auction.getProductId()),
                        bidCountMap.getOrDefault(auction.getId(), 0)
                ))
                .toList();
    }

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
    private String convertStatusDescription(AuctionOrderStatus status, AuctionViewerRoleStatus role) {
        if (status == AuctionOrderStatus.PROCESSING) {
            return role == BUYER ? "결제 대기중" : "입금 대기중";
        } else if (status == AuctionOrderStatus.SUCCESS) {
            return "결제 완료";
        }
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

    private Specification<Auction> createSearchSpec(AuctionSearchCondition condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 특정 IDs 필터
            if (condition.getIds() != null && !condition.getIds().isEmpty()) {
                predicates.add(root.get("id").in(condition.getIds()));
            }

            // 상태 필터 (기본: 예정된 경매 제외)
            if (condition.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), condition.getStatus()));
            } else {
                predicates.add(cb.notEqual(root.get("status"), AuctionStatus.SCHEDULED));
            }

            // 키워드/카테고리 검색 (Product 테이블 조회 필요)
            if (condition.getKeyword() != null || condition.getCategory() != null) {
                List<Long> matchedProductIds = productRepository.findIdsBySearchCondition(
                        condition.getKeyword(), condition.getCategory()
                );

                if (matchedProductIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("productId").in(matchedProductIds));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
