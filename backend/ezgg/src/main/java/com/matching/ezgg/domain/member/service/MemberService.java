package com.matching.ezgg.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.matching.ezgg.domain.member.dto.SignupRequest;
import com.matching.ezgg.domain.member.dto.SignupResponse;
import com.matching.ezgg.domain.member.entity.Member;
import com.matching.ezgg.domain.member.jwt.filter.JWTUtil;
import com.matching.ezgg.domain.member.jwt.repository.RedisRefreshTokenRepository;
import com.matching.ezgg.domain.member.repository.MemberRepository;
import com.matching.ezgg.domain.memberInfo.entity.MemberInfo;
import com.matching.ezgg.domain.memberInfo.repository.MemberInfoRepository;
import com.matching.ezgg.domain.memberInfo.service.MemberInfoService;
import com.matching.ezgg.domain.riotApi.service.ApiService;
import com.matching.ezgg.global.exception.ExistEmailException;
import com.matching.ezgg.global.exception.ExistMemberIdException;
import com.matching.ezgg.global.exception.ExistRiotUsernamException;
import com.matching.ezgg.global.exception.InvalidTokenException;
import com.matching.ezgg.global.exception.MemberNotFoundException;
import com.matching.ezgg.global.exception.MemberPassWordNotEqualsException;
import com.matching.ezgg.global.exception.PasswordBadRequestException;
import com.matching.ezgg.global.exception.TokenNotFoundException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService {

	private final MemberRepository memberRepository;
	private final MemberInfoService memberInfoService;
	private final ApiService apiService;
	private final MemberInfoRepository memberInfoRepository;
	private final JWTUtil jwtUtil;
	private final RedisRefreshTokenRepository redisRefreshTokenRepository;
	private final AsyncMemberService asyncMemberService;

	private final PasswordEncoder passwordEncoder;

	@Transactional
	public SignupResponse signup(SignupRequest signupRequest) {

		log.info("아이디 : {}", signupRequest.getMemberUsername());

		if (!signupRequest.getPassword().equals(signupRequest.getConfirmPassword())) {
			throw new MemberPassWordNotEqualsException();
		}

		if (!signupRequest.getPassword()
			.matches("(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*()-+=])[A-Za-z\\d!@#$%^&*()\\-+=]{6,20}$")) {
			throw new PasswordBadRequestException();
		}

		String password = passwordEncoder.encode(signupRequest.getPassword());

		validateDuplicateMember(signupRequest);

		Member newMember = Member.builder()
			.memberUsername(signupRequest.getMemberUsername())
			.password(password)
			.email(signupRequest.getEmail())
			.role("ROLE_USER") // 기본 역할 설정
			.build();

		//Member 엔티티 생성 후 저장
		Member member = memberRepository.save(newMember);

		//MemberInfo 엔티티 생성 후 저장
		String newPuuid = apiService.getMemberPuuid(signupRequest.getRiotUsername(), signupRequest.getRiotTag());
		MemberInfo memberInfo = memberInfoService.createNewMemberInfo(member.getId(), signupRequest.getRiotUsername(),
			signupRequest.getRiotTag(), newPuuid);

		asyncMemberService.updateMatchingAttributesAsync(member.getId());

		return SignupResponse.builder()
			.memberUsername(member.getMemberUsername())
			.email(member.getEmail())
			.riotUsername(memberInfo.getRiotUsername())
			.riotTag(memberInfo.getRiotTag())
			.build();
	}

	public Member findMemberByUsername(String memberUsername) {
		return memberRepository.findByMemberUsername(memberUsername)
			.orElseThrow(MemberNotFoundException::new);
	}

	private void validateDuplicateMember(SignupRequest signupRequest) {
		// 이미 존재하는 회원인지 확인
		if (memberRepository.existsByMemberUsername((signupRequest.getMemberUsername()))) {
			throw new ExistMemberIdException();
		}

		// 이메일 중복 확인
		if (memberRepository.existsByEmail(signupRequest.getEmail())) {
			throw new ExistEmailException();
		}

		// riotUsername and riotTag 중복 검사
		if (memberInfoRepository.existsByRiotUsernameAndRiotTag(signupRequest.getRiotUsername(),
			signupRequest.getRiotTag())) {
			throw new ExistRiotUsernamException();
		}
	}

	/**
	 * Access Token을 블랙리스트에 추가하는 메서드
	 * @param request
	 * return void
	 */
	public void addBlackList(HttpServletRequest request) {
		// Access Token 처리
		String accessToken = jwtUtil.extractTokenFromRequest(request);

		Boolean isExpired = true;

		try {
			isExpired = jwtUtil.isExpired(accessToken);
		} catch (Exception e) {
			log.error(">>>>> Access Token 만료 여부 확인 실패: {}", e.getMessage());
		}

		if (accessToken != null && !isExpired) {
			try {
				// 토큰의 남은 유효시간 계산
				long expirationTime = jwtUtil.getExpirationTime(accessToken);
				long currentTime = System.currentTimeMillis();
				long remainingTime = expirationTime - currentTime;

				if (remainingTime > 0) {
					// Access Token을 블랙리스트에 추가
					redisRefreshTokenRepository.addToBlacklist(accessToken, remainingTime);
					log.info(">>>>> Access Token을 블랙리스트에 추가했습니다.");
				}
			} catch (Exception e) {
				log.error(">>>>> Access Token 블랙리스트 추가 실패: {}", e.getMessage());
			}
		}
	}

	/**
	 * Refresh Token을 삭제하는 메서드
	 * @param request
	 * return void
	 */
	public void deleteRefreshToken(HttpServletRequest request) {
		// Refresh Token 처리
		String refreshToken = null;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals("Refresh")) {
					refreshToken = cookie.getValue();
				}
			}
		}

		String category = null;

		try {
			category = jwtUtil.getCategory(refreshToken);
		} catch (Exception e) {
			log.error(">>>>> Refresh Token 카테고리 확인 실패: {}", e.getMessage());
		}

		if (refreshToken != null && "refresh".equals(jwtUtil.getCategory(refreshToken))) {
			// 유효한 리프레시 토큰이 있으면 refreshToken에서 UUID를 추출해서 Redis에서 삭제
			String UUID = jwtUtil.getUUID(refreshToken);
			log.info(">>>>> Redis에서 UUID: {}", UUID);
			redisRefreshTokenRepository.deleteByUUID(UUID);
			log.info(">>>>> Redis에서 리프레시 토큰 삭제 완료");
		}
	}

	/**
	 * Access Token의 유효성을 검사하는 메서드
	 * @param request
	 */
	public void validateToken(HttpServletRequest request) {
		String accessToken = jwtUtil.extractTokenFromRequest(request);

		if (accessToken == null) {
			throw new TokenNotFoundException();
		}

		Boolean expired = true;

		try {
			expired = jwtUtil.isExpired(accessToken);
		} catch (Exception e) {
			log.error(">>>>> Access Token 만료 여부 확인 실패: {}", e.getMessage());
		}

		if (expired) {
			log.info(">>>>> Access Token이 만료되었습니다.");
			throw new InvalidTokenException();
		}

		if (redisRefreshTokenRepository.isBlacklisted(accessToken)) {
			log.info(">>>>> Access Token이 블랙리스트에 있습니다.");
			throw new InvalidTokenException();
		}
	}
}
