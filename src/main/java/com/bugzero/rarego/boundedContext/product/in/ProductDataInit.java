package com.bugzero.rarego.boundedContext.product.in;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.boundedContext.product.domain.ProductMember;
import com.bugzero.rarego.boundedContext.product.out.ProductMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class ProductDataInit implements CommandLineRunner {
	private final ProductMemberRepository productMemberRepository;

	@Override
	public void run(String... args) throws Exception {
		// ProductMember 초기 데이터
		ProductMember admin = ProductMember.builder()
			.id(1L)
			.publicId(UUID.randomUUID().toString())
			.email("test@bugzero.com")
			.nickname("테스트유저")
			.createdAt(LocalDateTime.now())
			.updatedAt(LocalDateTime.now())
			.build();
		productMemberRepository.save(admin);

	}
}
