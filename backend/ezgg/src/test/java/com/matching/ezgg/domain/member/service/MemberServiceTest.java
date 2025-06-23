package com.matching.ezgg.domain.member.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

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
import com.matching.ezgg.global.exception.ExistMemberIdException;
import com.matching.ezgg.global.exception.InvalidTokenException;
import com.matching.ezgg.global.exception.MemberNotFoundException;
import com.matching.ezgg.global.exception.PasswordBadRequestException;
import com.matching.ezgg.global.exception.TokenNotFoundException;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	// @Autowired
	@Mock
	private MemberRepository memberRepository;
	@Mock
	private MemberInfoService memberInfoService;
	@Mock
	private ApiService apiService;
	@Mock
	private MemberInfoRepository memberInfoRepository;
	@Mock
	private JWTUtil jwtUtil;
	@Mock
	private RedisRefreshTokenRepository redisRefreshTokenRepository;
	@Mock
	private PasswordEncoder passwordEncoder;

	@InjectMocks
	private MemberService memberService;

	private SignupRequest validSignupRequest;
	private Member mockMember;
	private MemberInfo mockMemberInfo;

	@BeforeEach
	void setUp() {
		// 테스트 데이터 준비
		validSignupRequest = SignupRequest.builder()
			.memberUsername("TestUser")
			.password("TestPassword123!")  // 비밀번호 규칙에 맞는 형태
			.confirmPassword("TestPassword123!")
			.email("test@gmail.com")
			.riotUsername("TestRiot")
			.riotTag("TestTag")
			.build();

		// Mock Member 객체 생성
		mockMember = Member.builder()
			.id(1L)
			.memberUsername("TestUser")
			.password("encodedPassword")
			.email("test@gmail.com")
			.role("ROLE_USER")
			.build();

		// Mock MemberInfo 객체 생성
		mockMemberInfo = MemberInfo.builder()
			.memberId(1L)
			.riotUsername("TestRiot")
			.riotTag("TestTag")
			.build();
	}

	@Test
	@DisplayName("정상적인 회원가입 요청시 성공해야 한다")
	void signup_성공() {
		// Given - Mock 설정을 논리적으로 그룹핑
		String expectedPuuId = "test-puuId-123";
		String encodedPassword = "encodedPassword";

		// 비밀번호 인코딩 설정
		when(passwordEncoder.encode(validSignupRequest.getPassword())).thenReturn(encodedPassword);

		// 중복 검사 Mock 설정
		when(memberRepository.existsByMemberUsername(validSignupRequest.getMemberUsername())).thenReturn(false);
		when(memberRepository.existsByEmail(validSignupRequest.getEmail())).thenReturn(false);
		when(memberInfoRepository.existsByRiotUsernameAndRiotTag(
			validSignupRequest.getRiotUsername(),
			validSignupRequest.getRiotTag())).thenReturn(false);

		// 저장 및 외부 API 호출 Mock 설정
		when(memberRepository.save(any(Member.class))).thenReturn(mockMember);
		when(apiService.getMemberPuuid(validSignupRequest.getRiotUsername(), validSignupRequest.getRiotTag()))
			.thenReturn(expectedPuuId);
		when(memberInfoService.createNewMemberInfo(
			mockMember.getId(),
			validSignupRequest.getRiotUsername(),
			validSignupRequest.getRiotTag(),
			expectedPuuId)).thenReturn(mockMemberInfo);

		// When - 실제 메서드 호출
		SignupResponse response = assertDoesNotThrow(() ->
			memberService.signup(validSignupRequest)
		);

		// Then - 응답 검증 (관련있는 것들끼리 그룹핑)
		assertAll("회원가입 응답 검증",
			() -> assertNotNull(response),
			() -> assertEquals(mockMember.getMemberUsername(), response.getMemberUsername()),
			() -> assertEquals(mockMember.getEmail(), response.getEmail()),
			() -> assertEquals(mockMemberInfo.getRiotUsername(), response.getRiotUsername()),
			() -> assertEquals(mockMemberInfo.getRiotTag(), response.getRiotTag())
		);

		// 검증 로직별로 그룹핑
		assertAll("중복 검사 호출 검증",
			() -> verify(memberRepository).existsByMemberUsername(validSignupRequest.getMemberUsername()),
			() -> verify(memberRepository).existsByEmail(validSignupRequest.getEmail()),
			() -> verify(memberInfoRepository).existsByRiotUsernameAndRiotTag(
				validSignupRequest.getRiotUsername(), validSignupRequest.getRiotTag())
		);

		assertAll("회원 생성 프로세스 검증",
			() -> verify(passwordEncoder).encode(validSignupRequest.getPassword()),
			() -> verify(memberRepository).save(any(Member.class)),
			() -> verify(apiService).getMemberPuuid(validSignupRequest.getRiotUsername(),
				validSignupRequest.getRiotTag()),
			() -> verify(memberInfoService).createNewMemberInfo(mockMember.getId(),
				validSignupRequest.getRiotUsername(), validSignupRequest.getRiotTag(), expectedPuuId)
		);
	}

	@ParameterizedTest
	@ValueSource(strings = { // 잘못된 비밀번호 형식들 각각 invalidPassword에 들어가 테스트를 진행한다
		"",                    // 빈 문자열
		" ",                   // 공백
		"a",                   // 1자
		"12345",              // 숫자만
		"abcdef",             // 소문자만
		"ABCDEF",             // 대문자만
		"!@#$%^",             // 특수문자만
		"Password123",         // 특수문자 없음
		"password123!",        // 대문자 없음
		"PASSWORD123!",        // 소문자 없음
		"Password!",           // 숫자 없음
		"verylongpasswordthatdefinitelyexceedsthelimit!" // 길이 초과
	})
	@DisplayName("잘못된 비밀번호 형식들에 대해 예외가 발생한다")
	void signup_잘못된비밀번호형식_예외발생(String invalidPassword) {
		// Given
		SignupRequest invalidRequest = SignupRequest.builder()
			.memberUsername("TestUser")
			.password(invalidPassword)
			.confirmPassword(invalidPassword)
			.email("test@gmail.com")
			.riotUsername("TestRiot")
			.riotTag("TestTag")
			.build();

		// When & Then
		assertThrows(PasswordBadRequestException.class, () -> {
			memberService.signup(invalidRequest);
		});
	}

	@Test
	@DisplayName("존재하지 않는 회원을 찾으려 할 때 예외가 발생한다")
	void findMemberByUsername() {
		// Given
		String nonExistentUsername = "NonExistentUser";
		when(memberRepository.findByMemberUsername(nonExistentUsername)).thenReturn(java.util.Optional.empty());

		// When & Then
		assertThrows(MemberNotFoundException.class, () -> {
			memberService.findMemberByUsername(nonExistentUsername);
		});

		assertAll("존재하지 않는 회원 조회 검증",
			() -> verify(memberRepository).findByMemberUsername(nonExistentUsername)
		);
	}

	@Test
	@DisplayName("회원가입 시 중복된 회원 아이디가 있는 경우 예외가 발생한다")
	void signup_중복회원아이디_예외발생() {
		// Given
		String existingUsername = "ExistingUser";
		when(memberRepository.existsByMemberUsername(existingUsername)).thenReturn(true);

		SignupRequest duplicateUsernameRequest = SignupRequest.builder()
			.memberUsername(existingUsername)
			.password("ValidPassword123!")
			.confirmPassword("ValidPassword123!")
			.email("new@gmail.com")
			.riotUsername("ValidRiot")
			.riotTag("ValidTag")
			.build();

		// When & Then
		assertThrows(ExistMemberIdException.class, () -> {
			memberService.signup(duplicateUsernameRequest);
		});

		// 중복 검사 후 더 이상 진행되지 않았는지 확인
		assertAll("중복 검사 후 더 이상 진행되지 않았는지 확인",
			() -> verify(passwordEncoder).encode("ValidPassword123!"), // 인코딩은 호출됨
			() -> verify(memberRepository).existsByMemberUsername(existingUsername), // 중복 검사도 호출됨
			() -> verify(memberRepository, never()).save(any(Member.class)), // 저장은 안됨
			() -> verify(apiService, never()).getMemberPuuid(anyString(), anyString()) // 외부 API 호출 안됨
		);
	}

	@Test
	@DisplayName("Access Token을 블랙리스트에 추가한다")
	void addBlackList_정상토큰_블랙리스트추가() {
		// Given
		HttpServletRequest request = mock(HttpServletRequest.class);
		String accessToken = "valid-access-token";
		long expirationTime = System.currentTimeMillis() + 3600000; // 1시간 후 만료

		when(jwtUtil.extractTokenFromRequest(request)).thenReturn(accessToken);
		when(jwtUtil.isExpired(accessToken)).thenReturn(false);
		when(jwtUtil.getExpirationTime(accessToken)).thenReturn(expirationTime);

		// When
		memberService.addBlackList(request);

		// Then
		assertAll("블랙리스트 추가 검증",
			() -> verify(jwtUtil).extractTokenFromRequest(request),
			() -> verify(jwtUtil).isExpired(accessToken),
			() -> verify(jwtUtil).getExpirationTime(accessToken),
			() -> verify(redisRefreshTokenRepository).addToBlacklist(eq(accessToken), anyLong())
		);
	}

	@Test
	@DisplayName("만료된 Access Token은 블랙리스트에 추가하지 않는다")
	void addBlackList_만료된토큰_블랙리스트추가안함() {
		// Given
		HttpServletRequest request = mock(HttpServletRequest.class);
		String expiredToken = "expired-access-token";

		when(jwtUtil.extractTokenFromRequest(request)).thenReturn(expiredToken);
		when(jwtUtil.isExpired(expiredToken)).thenReturn(true);

		// When
		memberService.addBlackList(request);

		// Then
		assertAll("만료된 토큰 블랙리스트 미추가 검증",
			() -> verify(jwtUtil).extractTokenFromRequest(request),
			() -> verify(jwtUtil).isExpired(expiredToken),
			() -> verify(jwtUtil, never()).getExpirationTime(anyString()),
			() -> verify(redisRefreshTokenRepository, never()).addToBlacklist(anyString(), anyLong())
		);
	}

	@Test
	@DisplayName("Refresh Token을 정상적으로 삭제한다")
	void deleteRefreshToken_정상토큰_삭제성공() {
		// Given
		HttpServletRequest request = mock(HttpServletRequest.class);
		String refreshToken = "valid-refresh-token";
		String uuid = "test-uuid-123";

		Cookie refreshCookie = new Cookie("Refresh", refreshToken);
		Cookie[] cookies = {refreshCookie};

		when(request.getCookies()).thenReturn(cookies);
		when(jwtUtil.getCategory(refreshToken)).thenReturn("refresh");
		when(jwtUtil.getUUID(refreshToken)).thenReturn(uuid);

		// When
		memberService.deleteRefreshToken(request);

		// Then
		assertAll("Refresh Token 삭제 검증",
			() -> verify(jwtUtil, times(2)).getCategory(refreshToken),
			() -> verify(jwtUtil).getUUID(refreshToken),
			() -> verify(redisRefreshTokenRepository).deleteByUUID(uuid)
		);
	}

	@Test
	@DisplayName("쿠키가 없으면 Refresh Token 삭제를 하지 않는다")
	void deleteRefreshToken_쿠키없음_삭제안함() {
		// Given
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(request.getCookies()).thenReturn(null);

		// When
		memberService.deleteRefreshToken(request);

		// Then
		assertAll("쿠키 없음 시 삭제 미실행 검증",
			() -> verify(jwtUtil, never()).getCategory(anyString()),
			() -> verify(redisRefreshTokenRepository, never()).deleteByUUID(anyString())
		);
	}

	@Test
	@DisplayName("잘못된 카테고리의 토큰은 삭제하지 않는다")
	void deleteRefreshToken_잘못된카테고리_삭제안함() {
		// Given
		HttpServletRequest request = mock(HttpServletRequest.class);
		String invalidToken = "invalid-category-token";

		Cookie invalidCookie = new Cookie("Refresh", invalidToken);
		Cookie[] cookies = {invalidCookie};

		when(request.getCookies()).thenReturn(cookies);
		when(jwtUtil.getCategory(invalidToken)).thenReturn("access"); // refresh가 아님

		// When
		memberService.deleteRefreshToken(request);

		// Then
		assertAll("잘못된 카테고리 토큰 삭제 미실행 검증",
			() -> verify(jwtUtil, times(2)).getCategory(invalidToken),
			() -> verify(jwtUtil, never()).getUUID(anyString()),
			() -> verify(redisRefreshTokenRepository, never()).deleteByUUID(anyString())
		);
	}

	@Test
	@DisplayName("정상적인 Access Token 검증이 성공한다")
	void validateToken_정상토큰_검증성공() {
		// Given
		HttpServletRequest request = mock(HttpServletRequest.class);
		String validToken = "valid-access-token";

		when(jwtUtil.extractTokenFromRequest(request)).thenReturn(validToken);
		when(jwtUtil.isExpired(validToken)).thenReturn(false);
		when(redisRefreshTokenRepository.isBlacklisted(validToken)).thenReturn(false);

		// When & Then
		assertDoesNotThrow(() -> {
			memberService.validateToken(request);
		});

		assertAll("정상 토큰 검증 성공 확인",
			() -> verify(jwtUtil).extractTokenFromRequest(request),
			() -> verify(jwtUtil).isExpired(validToken),
			() -> verify(redisRefreshTokenRepository).isBlacklisted(validToken)
		);
	}

	@Test
	@DisplayName("토큰이 없으면 TokenNotFoundException이 발생한다")
	void validateToken_토큰없음_예외발생() {
		// Given
		HttpServletRequest request = mock(HttpServletRequest.class);
		when(jwtUtil.extractTokenFromRequest(request)).thenReturn(null);

		// When & Then
		assertThrows(TokenNotFoundException.class, () -> {
			memberService.validateToken(request);
		});

		assertAll("토큰 없음 예외 발생 검증",
			() -> verify(jwtUtil).extractTokenFromRequest(request),
			() -> verify(jwtUtil, never()).isExpired(anyString())
		);
	}

	@Test
	@DisplayName("만료된 토큰이면 InvalidTokenException이 발생한다")
	void validateToken_만료된토큰_예외발생() {
		// Given
		HttpServletRequest request = mock(HttpServletRequest.class);
		String expiredToken = "expired-token";

		when(jwtUtil.extractTokenFromRequest(request)).thenReturn(expiredToken);
		when(jwtUtil.isExpired(expiredToken)).thenReturn(true);

		// When & Then
		assertThrows(InvalidTokenException.class, () -> {
			memberService.validateToken(request);
		});

		assertAll("만료된 토큰 예외 발생 검증",
			() -> verify(jwtUtil).extractTokenFromRequest(request),
			() -> verify(jwtUtil).isExpired(expiredToken),
			() -> verify(redisRefreshTokenRepository, never()).isBlacklisted(anyString())
		);
	}

	@Test
	@DisplayName("블랙리스트에 있는 토큰이면 InvalidTokenException이 발생한다")
	void validateToken_블랙리스트토큰_예외발생() {
		// Given
		HttpServletRequest request = mock(HttpServletRequest.class);
		String blacklistedToken = "blacklisted-token";

		when(jwtUtil.extractTokenFromRequest(request)).thenReturn(blacklistedToken);
		when(jwtUtil.isExpired(blacklistedToken)).thenReturn(false);
		when(redisRefreshTokenRepository.isBlacklisted(blacklistedToken)).thenReturn(true);

		// When & Then
		assertThrows(InvalidTokenException.class, () -> {
			memberService.validateToken(request);
		});

		assertAll("블랙리스트 토큰 예외 발생 검증",
			() -> verify(jwtUtil).extractTokenFromRequest(request),
			() -> verify(jwtUtil).isExpired(blacklistedToken),
			() -> verify(redisRefreshTokenRepository).isBlacklisted(blacklistedToken)
		);
	}
}
