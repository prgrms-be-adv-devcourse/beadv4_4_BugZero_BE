package com.bugzero.rarego.boundedContext.member.in;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import com.bugzero.rarego.boundedContext.member.app.MemberFacade;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;

@Configuration
@Profile("!test")
public class MemberDataInit {
	private final MemberDataInit self;
	private final MemberRepository memberRepository;
	private final MemberFacade memberFacade;

	public MemberDataInit(@Lazy MemberDataInit self, MemberRepository memberRepository, MemberFacade memberFacade) {
		this.self = self;
		this.memberRepository = memberRepository;
		this.memberFacade = memberFacade;
	}

	@Bean
	public ApplicationRunner memberBaseInitDataRunner() {
		return args -> self.makeBaseMembers();
	}

	public void makeBaseMembers() {
		if (memberRepository.count() > 0) {
			return;
		}

		memberFacade.join("system@rarego.com");
	}
}
