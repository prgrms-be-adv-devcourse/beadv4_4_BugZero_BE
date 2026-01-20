package com.bugzero.rarego.boundedContext.member.app;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.domain.MemberClearField;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateRequestDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateResponseDto;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.event.EventPublisher;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;
import com.bugzero.rarego.shared.member.domain.MemberDto;
import com.bugzero.rarego.shared.member.event.MemberUpdatedEvent;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional
public class MemberUpdateMemberUseCase {

	private final MemberSupport memberSupport;
	private final MemberRepository memberRepository;
	private final EventPublisher eventPublisher;

	public MemberUpdateResponseDto updateMe(String publicId, String role, MemberUpdateRequestDto requestDto) {
		Member member = memberSupport.findByPublicId(publicId);

		validateClearFieldsPolicy(requestDto);

		// 1) 삭제(clear) 먼저 적용
		applyClears(member, requestDto.clearFields());

		// 2) 수정된 필드에 대해서만 정책 검증 후 적용
		validateAfterPatch(requestDto, member);

		// 3) 저장 후 이벤트 발생
		memberRepository.save(member);
		eventPublisher.publish(new MemberUpdatedEvent(MemberDto.from(member)));
		return MemberUpdateResponseDto.from(member);
	}


	// ClearField와 수정본 일치하는지 확인 (null ClearField에 존재하는데 dto에 수정 내용이 들어왔다면 오류처리합니다.)
	private void validateClearFieldsPolicy(MemberUpdateRequestDto dto) {
		Set<MemberClearField> clearFields = dto.clearFields();
		if (clearFields == null || clearFields.isEmpty()) return;

		if (clearFields.contains(MemberClearField.INTRO) && dto.intro() != null) {
			throw new CustomException(ErrorType.MEMBER_UPDATED_FAILED);
		}
		if (clearFields.contains(MemberClearField.ZIPCODE) && dto.zipCode() != null) {
			throw new CustomException(ErrorType.MEMBER_UPDATED_FAILED);
		}
		if (clearFields.contains(MemberClearField.ADDRESS) && dto.address() != null) {
			throw new CustomException(ErrorType.MEMBER_UPDATED_FAILED);
		}
		if (clearFields.contains(MemberClearField.ADDRESS_DETAIL) && dto.addressDetail() != null) {
			throw new CustomException(ErrorType.MEMBER_UPDATED_FAILED);
		}
		if (clearFields.contains(MemberClearField.CONTACT_PHONE) && dto.contactPhone() != null) {
			throw new CustomException(ErrorType.MEMBER_UPDATED_FAILED);
		}
		if (clearFields.contains(MemberClearField.REAL_NAME) && dto.realName() != null) {
			throw new CustomException(ErrorType.MEMBER_UPDATED_FAILED);
		}
	}

	// null 가능한 영역 적용 (삭제)
	private void applyClears(Member member, Set<MemberClearField> clearFields) {
		if (clearFields == null || clearFields.isEmpty()) return;

		if (clearFields.contains(MemberClearField.INTRO)) {
			member.changeIntro(null);
		}
		if (clearFields.contains(MemberClearField.ZIPCODE)) {
			member.changeZipCode(null);
		}
		if (clearFields.contains(MemberClearField.ADDRESS)) {
			member.changeAddress(null);
		}
		if (clearFields.contains(MemberClearField.ADDRESS_DETAIL)) {
			member.changeAddressDetail(null);
		}
		if (clearFields.contains(MemberClearField.CONTACT_PHONE)) {
			member.changeContactPhone(null);
		}
		if (clearFields.contains(MemberClearField.REAL_NAME)) {
			member.changeRealName(null);
		}
	}

	private void validateAfterPatch(MemberUpdateRequestDto dto, Member member) {
		applyNickname(dto, member);
		applyIntro(dto, member);
		applyZipCode(dto, member);
		applyAddress(dto, member);
		applyAddressDetail(dto, member);
		applyContactPhone(dto, member);
		applyRealName(dto, member);
	}

	private void applyNickname(MemberUpdateRequestDto dto, Member member) {
		if (dto.nickname() == null) return;

		String nickname = dto.nickname().trim();
		if (nickname.isBlank() || nickname.length() > 50) {
			throw new CustomException(ErrorType.MEMBER_INVALID_NICKNAME);
		}
		member.changeNickname(nickname);
	}

	// intro: trim 후 "" 저장 허용
	private void applyIntro(MemberUpdateRequestDto dto, Member member) {
		if (dto.intro() == null) return;

		String intro = dto.intro().trim();
		if (intro.length() > 255) {
			throw new CustomException(ErrorType.MEMBER_INVALID_INTRO);
		}
		member.changeIntro(intro);
	}

	private void applyZipCode(MemberUpdateRequestDto dto, Member member) {
		if (dto.zipCode() == null) return;

		String zip = dto.zipCode().trim();
		if (zip.isBlank() || !zip.matches("\\d{5}")) {
			throw new CustomException(ErrorType.MEMBER_INVALID_ZIPCODE);
		}
		member.changeZipCode(zip);
	}

	private void applyAddress(MemberUpdateRequestDto dto, Member member) {
		if (dto.address() == null) return;

		String addr = dto.address().trim();
		if (addr.isBlank() || addr.length() > 255) {
			throw new CustomException(ErrorType.MEMBER_INVALID_ADDRESS);
		}
		member.changeAddress(addr);
	}

	private void applyAddressDetail(MemberUpdateRequestDto dto, Member member) {
		if (dto.addressDetail() == null) return;

		String detail = dto.addressDetail().trim();
		if (detail.isBlank() || detail.length() > 255) {
			throw new CustomException(ErrorType.MEMBER_INVALID_ADDRESS_DETAIL);
		}
		member.changeAddressDetail(detail);
	}

	private void applyContactPhone(MemberUpdateRequestDto dto, Member member) {
		if (dto.contactPhone() == null) return;

		String phone = dto.contactPhone().trim();
		if (phone.isBlank()) throw new CustomException(ErrorType.MEMBER_INVALID_PHONE_NUMBER);

		// 하이픈 제거해서 저장
		String normalized = phone.replaceAll("-", "");
		if (!normalized.matches("\\d{10,11}")) {
			throw new CustomException(ErrorType.MEMBER_INVALID_PHONE_NUMBER);
		}

		member.changeContactPhone(normalized);
	}

	private void applyRealName(MemberUpdateRequestDto dto, Member member) {
		if (dto.realName() == null) return;

		String realName = dto.realName().trim();
		if (realName.isBlank() || realName.length() > 10) {
			throw new CustomException(ErrorType.MEMBER_INVALID_REALNAME);
		}
		// 한글 혹은 영어만
		if(!realName.matches("^[가-힣a-zA-Z]+$")){
			throw new CustomException(ErrorType.MEMBER_INVALID_REALNAME);
		}
		member.changeRealName(realName);
	}
}
