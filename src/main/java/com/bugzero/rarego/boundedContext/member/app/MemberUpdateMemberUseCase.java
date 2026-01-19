package com.bugzero.rarego.boundedContext.member.app;

import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bugzero.rarego.boundedContext.member.domain.Member;
import com.bugzero.rarego.boundedContext.member.domain.MemberClearField;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateRequestDto;
import com.bugzero.rarego.boundedContext.member.domain.MemberUpdateResponseDto;
import com.bugzero.rarego.boundedContext.member.out.MemberRepository;
import com.bugzero.rarego.global.exception.CustomException;
import com.bugzero.rarego.global.response.ErrorType;

import lombok.RequiredArgsConstructor;
@Service
@RequiredArgsConstructor
@Transactional
public class MemberUpdateMemberUseCase {

	private final MemberSupport memberSupport;
	private final MemberRepository memberRepository;

	public MemberUpdateResponseDto updateMe(String publicId, String role, MemberUpdateRequestDto dto) {
		Member member = memberSupport.findByPublicId(publicId);

		// SELLER면 intro 제외 전부 "요청에 존재"해야 함
		if (isSeller(role)) {
			validateSellerRequired(dto);
		}

		validateClearFieldsPolicy(role, dto);

		// 1) 삭제(clear) 먼저 적용 (SELLER면 일부 clear 금지)
		applyClears(member, role, dto.clearFields());

		// 2) 수정된 필드에 대해서만 정책 검증 후 적용
		validateAfterPatch(dto, member);

		memberRepository.save(member);
		return MemberUpdateResponseDto.from(member);
	}

	private boolean isSeller(String role) {
		return "SELLER".equals(role);
	}

	/**
	 * SELLER일 때 intro 제외 필드들은 null이면 안 됨 (= 요청에서 빠지면 안 됨)
	 */
	private void validateSellerRequired(MemberUpdateRequestDto dto) {
		if (dto.nickname() == null) throw new CustomException(ErrorType.MEMBER_NICKNAME_REQUIRED);
		if (dto.zipCode() == null) throw new CustomException(ErrorType.MEMBER_ZIPCODE_REQUIRED);
		if (dto.address() == null) throw new CustomException(ErrorType.MEMBER_ADDRESS_REQUIRED);
		if (dto.addressDetail() == null) throw new CustomException(ErrorType.MEMBER_ADDRESS_DETAIL_REQUIRED);
		if (dto.contactPhone() == null) throw new CustomException(ErrorType.MEMBER_PHONE_REQUIRED);
		if (dto.realName() == null) throw new CustomException(ErrorType.MEMBER_REALNAME_REQUIRED);
		// intro는 null이어도 됨
	}

	private void validateClearFieldsPolicy(String role, MemberUpdateRequestDto dto) {
		Set<MemberClearField> clearFields = dto.clearFields();
		if (clearFields == null || clearFields.isEmpty()) return;

		if (isSeller(role)) {
			if (clearFields.contains(MemberClearField.ZIPCODE)
				|| clearFields.contains(MemberClearField.ADDRESS)
				|| clearFields.contains(MemberClearField.ADDRESS_DETAIL)
				|| clearFields.contains(MemberClearField.CONTACT_PHONE)
				|| clearFields.contains(MemberClearField.REAL_NAME)) {
				throw new CustomException(ErrorType.MEMBER_SELLER_REQUIRED_FIELD_CANNOT_BE_CLEARED);
			}
		}

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
	private void applyClears(Member member, String role, Set<MemberClearField> clearFields) {
		if (clearFields == null || clearFields.isEmpty()) return;

		// SELLER면 intro 제외 삭제를 막는다(= 필수값이니까)
		if (isSeller(role)) {
			if (clearFields.contains(MemberClearField.ZIPCODE)
				|| clearFields.contains(MemberClearField.ADDRESS)
				|| clearFields.contains(MemberClearField.ADDRESS_DETAIL)
				|| clearFields.contains(MemberClearField.CONTACT_PHONE)
				|| clearFields.contains(MemberClearField.REAL_NAME)) {
				throw new CustomException(ErrorType.MEMBER_SELLER_REQUIRED_FIELD_CANNOT_BE_CLEARED);
			}
		}

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
			member.changeAddress(null);
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
