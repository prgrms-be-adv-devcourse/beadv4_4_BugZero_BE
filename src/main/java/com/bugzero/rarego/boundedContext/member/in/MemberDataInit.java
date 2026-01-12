package com.bugzero.rarego.boundedContext.member.in;

import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;

import lombok.RequiredArgsConstructor;

@Profile({"local", "dev"})
@Component
@RequiredArgsConstructor
public class MemberDataInit implements ApplicationRunner {
	private final MemberRepository memberRepository;

	@Override
	public void run(ApplicationArguments args) {
		if (memberRepository.count() > 0) {
			return;
		}

		memberRepository.saveAll(List.of(
			new Member("alice"),
			new Member("bob"),
			new Member("charlie")
		));
	}
}
