package com.bugzero.rarego.boundedContext.auth.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.auth.domain.Account;
import com.bugzero.rarego.boundedContext.auth.domain.AuthRole;
import com.bugzero.rarego.boundedContext.auth.out.AccountRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthPromoteSellerUseCase {
	private final AuthSupport authSupport;
	private final AccountRepository accountRepository;

	@Transactional
	public void promoteSeller(String memberPublicId) {
		if (memberPublicId == null || memberPublicId.isBlank()) {
			throw new CustomException(ErrorType.AUTH_MEMBER_REQUIRED);
		}

		Account account = authSupport.findByPublicId(memberPublicId);

		AuthRole currentRole = account.getRole();
		if (currentRole == AuthRole.SELLER || currentRole == AuthRole.ADMIN) {
			return;
		}

		account.changeRole(AuthRole.SELLER);
		// 즉시 적용, flush 실패하면 오류 (저장 확인용)
		accountRepository.flush();
	}
}
