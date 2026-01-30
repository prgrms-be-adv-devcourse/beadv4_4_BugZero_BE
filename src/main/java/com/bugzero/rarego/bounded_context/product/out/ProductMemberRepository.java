package com.bugzero.rarego.bounded_context.product.out;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bugzero.rarego.bounded_context.product.domain.ProductMember;

public interface ProductMemberRepository extends JpaRepository<ProductMember, Long> {
	Optional<ProductMember> findByPublicIdAndDeletedIsFalse(String publicId);
}
