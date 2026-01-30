package com.bugzero.rarego.bounded_context.member.app;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bugzero.rarego.bounded_context.member.domain.Member;
import com.bugzero.rarego.bounded_context.member.domain.MemberMeResponseDto;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

@ExtendWith(MockitoExtension.class)
class MemberGetMemberUseCaseTest {

	@Mock
	private MemberSupport memberSupport;

	@InjectMocks
	private MemberGetMemberUseCase memberGetMemberUseCase;

	@Test
	@DisplayName("회원이 존재하면 값을 리턴한다")
	void getMe_success() {
		LocalDateTime createdAt = LocalDateTime.of(2024, 1, 1, 0, 0);
		LocalDateTime updatedAt = LocalDateTime.of(2024, 1, 2, 0, 0);
		Member member = Member.builder()
			.publicId("public-id")
			.email("test@example.com")
			.nickname("tester")
			.intro("intro")
			.address("Seoul")
			.addressDetail("Apt 1")
			.zipCode("12345")
			.contactPhone("01012345678")
			.realName("Alice")
			.createdAt(createdAt)
			.updatedAt(updatedAt)
			.build();

		given(memberSupport.findByPublicId("public-id")).willReturn(member);

		MemberMeResponseDto result = memberGetMemberUseCase.getMe("public-id", "USER");

		assertThat(result.publicId()).isEqualTo("public-id");
		assertThat(result.role()).isEqualTo("USER");
		assertThat(result.email()).isEqualTo("test@example.com");
		assertThat(result.nickname()).isEqualTo("tester");
		assertThat(result.intro()).isEqualTo("intro");
		assertThat(result.address()).isEqualTo("Seoul");
		assertThat(result.addressDetail()).isEqualTo("Apt 1");
		assertThat(result.zipCode()).isEqualTo("12345");
		assertThat(result.contactPhoneMasked()).isEqualTo("01012345678");
		assertThat(result.realNameMasked()).isEqualTo("Alice");
		assertThat(result.createdAt()).isEqualTo(createdAt);
		assertThat(result.updatedAt()).isEqualTo(updatedAt);
		verify(memberSupport).findByPublicId("public-id");
	}

	@Test
	@DisplayName("회원이 없으면 MEMBER_NOT_FOUND 정보를 응답한다")
	void getMe_memberNotFound() {
		given(memberSupport.findByPublicId("missing"))
			.willThrow(new CustomException(ErrorType.MEMBER_NOT_FOUND));

		assertThatThrownBy(() -> memberGetMemberUseCase.getMe("missing", "USER"))
			.isInstanceOf(CustomException.class)
			.extracting("errorType")
			.isEqualTo(ErrorType.MEMBER_NOT_FOUND);
	}
}
