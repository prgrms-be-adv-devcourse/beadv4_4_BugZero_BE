package com.bugzero.rarego.boundedContext.product.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.boundedContext.product.domain.ProductMember;

public interface ProductMemberRepository extends JpaRepository<ProductMember, Long> {
	Optional<ProductMember> findByPublicIdAndDeletedIsFalse(String publicId);
}
