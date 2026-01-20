package com.bugzero.rarego.boundedContext.auction.app;

import com.bugzero.rarego.boundedContext.auction.domain.*;
import com.bugzero.rarego.boundedContext.auction.in.dto.WishlistAddResponseDto;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionOrderRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.auction.out.BidRepository;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.global.response.PageDto;
import com.bugzero.rarego.global.response.PagedResponseDto;
import com.bugzero.rarego.global.response.SuccessResponseDto;
import com.bugzero.rarego.shared.auction.dto.*;
import com.bugzero.rarego.shared.member.domain.MemberDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuctionFacade {

    private final AuctionCreateBidUseCase auctionCreateBidUseCase;
    private final AuctionBookmarkUseCase auctionBookmarkUseCase;
    private final AuctionSyncMemberUseCase auctionSyncMemberUseCase;
    private final BidRepository bidRepository;
    private final AuctionMemberRepository auctionMemberRepository;
    private final AuctionRepository auctionRepository;
    private final ProductRepository productRepository;
    private final AuctionOrderRepository auctionOrderRepository;

    @Transactional
    public SuccessResponseDto<BidResponseDto> createBid(Long auctionId, Long memberId, int bidAmount) {
        return auctionCreateBidUseCase.createBid(auctionId, memberId, bidAmount);
    }

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
    public PagedResponseDto<MyBidResponseDto> getMyBids(Long memberId, AuctionStatus status, Pageable pageable) {
        Page<Bid> bidPage = bidRepository.findMyBids(memberId, status, pageable);
        List<Bid> bids = bidPage.getContent();

        Map<Long, Auction> auctionMap = getAuctionMap(bids);

        Page<MyBidResponseDto> dtoPage = bidPage.map(bid -> {
            Auction auction = auctionMap.get(bid.getAuctionId());
            if (auction == null) {
                throw new CustomException(ErrorType.AUCTION_NOT_FOUND);
            }
            return MyBidResponseDto.from(bid, auction);
        });

        return new PagedResponseDto<>(dtoPage.getContent(), PageDto.from(dtoPage));
    }

    public PagedResponseDto<MySaleResponseDto> getMySales(Long memberId, AuctionFilterType auctionFilterType, Pageable pageable) {
        List<Long> myProductIds = productRepository.findAllIdsBySellerId(memberId);

        Page<Auction> auctionPage = fetchAuctionsByFilter(myProductIds, auctionFilterType, pageable);
        List<Auction> auctions = auctionPage.getContent();

        if (auctions.isEmpty()) {
            return new PagedResponseDto<>(List.of(), PageDto.from(auctionPage));
        }

        Set<Long> productIds = auctions.stream().map(Auction::getProductId).collect(Collectors.toSet());
        Set<Long> auctionIds = auctions.stream().map(Auction::getId).collect(Collectors.toSet());

        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, AuctionOrder> orderMap = auctionOrderRepository.findAllByAuctionIdIn(auctionIds).stream()
                .collect(Collectors.toMap(AuctionOrder::getAuctionId, Function.identity()));

        Map<Long, Integer> bidCountMap = bidRepository.countByAuctionIdIn(auctionIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));

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

    @Transactional
    public WishlistAddResponseDto addBookmark(Long memberId, Long auctionId) {
        AuctionMember member = auctionMemberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorType.MEMBER_NOT_FOUND));

        return auctionBookmarkUseCase.addBookmark(member.getId(), auctionId);
    }

    @Transactional
    public AuctionMember syncMember(MemberDto member) {
        return auctionSyncMemberUseCase.syncMember(member);
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
}