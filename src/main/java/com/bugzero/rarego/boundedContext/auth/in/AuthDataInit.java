package com.bugzero.rarego.boundedContext.auth.in;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;

import com.bugzero.rarego.boundedContext.auth.domain.AuthMember;
import com.bugzero.rarego.boundedContext.auth.out.AuthMemberRepository;
import com.bugzero.rarego.shared.member.domain.MemberRole;
import com.bugzero.rarego.shared.member.domain.Provider;

@Configuration
@Profile("dev")
public class AuthDataInit {
	private final AuthDataInit self;
	private final AuthMemberRepository authMemberRepository;

	public AuthDataInit(@Lazy AuthDataInit self, AuthMemberRepository authMemberRepository) {
		this.self = self;
		this.authMemberRepository = authMemberRepository;
	}

	@Bean
	public ApplicationRunner authBaseInitDataRunner() {
		return args -> self.makeBaseAuthMembers();
	}

	public void makeBaseAuthMembers() {
		if (authMemberRepository.count() > 0)
			return;

		LocalDateTime now = LocalDateTime.now();
		List<AuthMember> members = List.of(
			makeMember(1L, "auth1@bugzero.com", "auth1", MemberRole.USER, now),
			makeMember(2L, "auth2@bugzero.com", "auth2", MemberRole.USER, now),
			makeMember(3L, "auth3@bugzero.com", "auth3", MemberRole.SELLER, now),
			makeMember(4L, "auth4@bugzero.com", "auth4", MemberRole.USER, now),
			makeMember(5L, "auth5@bugzero.com", "auth5", MemberRole.ADMIN, now)
		);

		authMemberRepository.saveAll(members);
	}

	private static AuthMember makeMember(Long id, String email, String nickname, MemberRole role, LocalDateTime now) {
		return AuthMember.builder()
			.id(id)
			.publicId(UUID.randomUUID().toString())
			.email(email)
			.nickname(nickname)
			.role(role)
			.provider(Provider.GOOGLE)
			.providerId("provider_" + id)
			.createdAt(now)
			.updatedAt(now)
			.build();
	}
}
