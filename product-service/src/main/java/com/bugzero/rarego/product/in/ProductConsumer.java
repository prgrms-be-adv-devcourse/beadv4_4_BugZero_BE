package com.bugzero.rarego.product.in;

import static org.springframework.transaction.annotation.Propagation.*;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.product.app.ProductFacade;
import com.bugzero.rarego.shared.member.event.MemberJoinedEvent;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductConsumer {
	private final ProductFacade productFacade;

	@KafkaListener(topics = "member-joined")
	public void handleMemberJoined(MemberJoinedEvent event) {
		try {
			productFacade.syncMember(event.memberDto());
			log.info("[product] 회원 레플리카 등록 완료 - memberPublicId: {}", event.memberDto().publicId());
		} catch (Exception e) {
			log.error("회원 레플리카 등록 실패 - memberPublicId: {}", event.memberDto().publicId(), e);
			throw e;
		}
	}

	@KafkaListener(topics = "member-updated")
	public void handleMemberUpdated(MemberUpdatedEvent event) {
		try {
			productFacade.syncMember(event.memberDto());
			log.info("[product] 회원 레플리카 수정 완료 - memberPublicId: {}", event.memberDto().publicId());
		} catch (Exception e) {
			log.error("회원 레플리카 수정 실패 - memberPublicId: {}", event.memberDto().publicId(), e);
			throw e;
		}
	}
}
