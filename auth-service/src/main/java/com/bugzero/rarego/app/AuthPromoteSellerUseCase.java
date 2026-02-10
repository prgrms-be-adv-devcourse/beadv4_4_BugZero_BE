package com.bugzero.rarego.app;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.domain.Account;
import com.bugzero.rarego.domain.AuthRole;
import com.bugzero.rarego.out.AccountRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthPromoteSellerUseCase {
	private final AuthSupport authSupport;
	private final AccountRepository accountRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void promoteSeller(String memberPublicId) {
		if (memberPublicId == null || memberPublicId.isBlank()) {
			throw new CustomException(ErrorType.AUTH_MEMBER_REQUIRED);
		}

		Account account = authSupport.findByPublicId(memberPublicId);

		AuthRole currentRole = account.getRole();
		if (currentRole != AuthRole.USER) {
			return;
		}
		account.changeRole(AuthRole.SELLER);
	}
}
