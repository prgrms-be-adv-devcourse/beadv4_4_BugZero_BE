package com.bugzero.rarego.boundedContext.auction.in;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auction.domain.Auction;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionMember;
import com.bugzero.rarego.boundedContext.auction.domain.AuctionStatus;
import com.bugzero.rarego.boundedContext.auction.out.AuctionMemberRepository;
import com.bugzero.rarego.boundedContext.auction.out.AuctionRepository;
import com.bugzero.rarego.boundedContext.product.domain.Category;
import com.bugzero.rarego.boundedContext.product.domain.InspectionStatus;
import com.bugzero.rarego.boundedContext.product.domain.Product;
import com.bugzero.rarego.boundedContext.product.domain.ProductCondition;
import com.bugzero.rarego.boundedContext.product.out.ProductRepository;
import com.bugzero.rarego.shared.member.domain.MemberRole;
import com.bugzero.rarego.shared.member.domain.Provider;
import com.bugzero.rarego.shared.member.domain.SourceMember;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@Profile("dev")
public class AuctionDataInit {

	private final AuctionDataInit self;
	private final AuctionRepository auctionRepository;
	private final ProductRepository productRepository;
	private final AuctionMemberRepository auctionMemberRepository;

	public AuctionDataInit(
		@Lazy AuctionDataInit self,
		AuctionRepository auctionRepository,
		ProductRepository productRepository,
		AuctionMemberRepository auctionMemberRepository) {
		this.self = self;
		this.auctionRepository = auctionRepository;
		this.productRepository = productRepository;
		this.auctionMemberRepository = auctionMemberRepository;
	}

	@Bean
	public ApplicationRunner auctionBaseInitDataRunner() {
		return args -> {
			self.makeBaseAuction();
		};
	}

	@Transactional
	public void makeBaseAuction() {
		if (auctionRepository.count() > 0)
			return;

		// 1. 판매자(Member) 조회 (없으면 생성)
		AuctionMember seller = auctionMemberRepository.findById(1L)
			.orElseGet(() -> {
				AuctionMember newMember = AuctionMember.builder()
					.id(1L) // ID 직접 설정
					.publicId(UUID.randomUUID().toString())
					.email("seller@auction.com")
					.nickname("AuctionSeller")
					.role(MemberRole.SELLER)
					.provider(Provider.GOOGLE)
					.providerId("auction_provider_" + UUID.randomUUID())
					.build();
				return auctionMemberRepository.save(newMember);
			});

		AuctionMember buyer = auctionMemberRepository.findById(2L)
			.orElseGet(() -> auctionMemberRepository.save(AuctionMember.builder()
				.id(2L)
				.publicId(UUID.randomUUID().toString())
				.email("buyer@auction.com")
				.nickname("AuctionBuyer")
				.role(MemberRole.USER)
				.provider(Provider.GOOGLE)
				.providerId("buyer_provider_" + UUID.randomUUID())
				.build()));

		// 2. 상품(Product) 생성
		Product product = Product.builder()
			.sellerId(seller.getId())
			.name("스타워즈 제다이 레고")
			.description("미개봉 새상품입니다. 경매로 저렴하게 내놓습니다.")
			.category(Category.스타워즈)
			.productCondition(ProductCondition.MISB)
			.inspectionStatus(InspectionStatus.PENDING)
			.build();

		productRepository.save(product);

		// 3. 경매(Auction) 생성
		Auction auction = Auction.builder()
			.productId(product.getId())
			.startTime(LocalDateTime.now())
			.endTime(LocalDateTime.now().plusDays(3))
			.startPrice(1_000_000)
			.tickSize(50_000)
			.build();

		auction.startAuction();

		auctionRepository.save(auction);

		log.info("Auction Init Data Created: AuctionId={}, ProductId={}", auction.getId(), product.getId());
	}
}