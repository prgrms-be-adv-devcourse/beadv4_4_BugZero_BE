package com.bugzero.rarego.boundedContext.member.in;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
@Profile("dev")
public class MemberDataInit {
	private static final String SELLER_PUBLIC_ID = "eea43e1c-eb7d-406a-8e5d-5ffbbddd488f";
	private static final String BIDDER_PUBLIC_ID = "f8d99e06-fa28-4dad-9371-b7b8baa6ae6b";
	private static final String COMPETITOR_PUBLIC_ID = "competitor_public_3";

	private final MemberDataInit self;
	private final MemberRepository memberRepository;

	public MemberDataInit(@Lazy MemberDataInit self, MemberRepository memberRepository) {
		this.self = self;
		this.memberRepository = memberRepository;
	}

	@Bean
	public ApplicationRunner memberBaseInitDataRunner() {
		return args -> self.makeBaseMembers();
	}

	public void makeBaseMembers() {
		if (memberRepository.count() > 0) {
			log.info("이미 Member 데이터가 존재합니다. 초기화를 건너뜁니다.");
			return;
		}

		memberRepository.save(Member.builder()
			.publicId(SELLER_PUBLIC_ID)
			.email("seller@auction.com")
			.nickname("AuctionSeller")
			.build());

		memberRepository.save(Member.builder()
			.publicId(BIDDER_PUBLIC_ID)
			.email("buyer@auction.com")
			.nickname("AuctionBuyer")
			.build());

		memberRepository.save(Member.builder()
			.publicId(COMPETITOR_PUBLIC_ID)
			.email("comp@test.com")
			.nickname("경쟁자_A")
			.build());

		log.info("Member 테스트 데이터 초기화 완료");
	}
}
