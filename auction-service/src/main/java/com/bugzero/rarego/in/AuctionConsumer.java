package com.bugzero.rarego.in;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.bugzero.rarego.app.AuctionFacade;
import com.bugzero.rarego.shared.member.event.MemberJoinedEvent;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionConsumer {
	private final AuctionFacade auctionFacade;

	@KafkaListener(topics = "member-joined")
	public void handleMemberJoined(MemberJoinedEvent event) {
		try {
			auctionFacade.syncMember(event.memberDto());
			log.info("[auction] 회원 레플리카 등록 완료 - memberPublicId: {}", event.memberDto().publicId());
		} catch (Exception e) {
			log.error("회원 레플리카 등록 실패 - memberPublicId: {}", event.memberDto().publicId(), e);
			throw e;
		}
	}

	@KafkaListener(topics = "member-updated")
	public void handleMemberUpdated(MemberUpdatedEvent event) {
		try {
			auctionFacade.syncMember(event.memberDto());
			log.info("[auction] 회원 레플리카 수정 완료 - memberPublicId: {}", event.memberDto().publicId());
		} catch (Exception e) {
			log.error("회원 레플리카 수정 실패 - memberPublicId: {}", event.memberDto().publicId(), e);
			throw e;
		}
	}
}
