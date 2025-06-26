package com.matching.ezgg.domain.member.repository;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.matching.ezgg.domain.member.entity.Member;

import jakarta.transaction.Transactional;

@DataJpaTest
@Transactional
class MemberRepositoryTest {

	@Autowired
	private MemberRepository memberRepository;

	private Member testMember;

	@BeforeEach
	void setUp() {
		testMember = Member.builder()
			.memberUsername("TestUser")
			.password("TestPassword1!")
			.email("test@gmail.com")
			.role("ROLE_USER")
			.build();
	}

	@AfterEach
	void tearDown() {
		memberRepository.deleteAll();
	}

	@Test
	@DisplayName("사용자명으로 회원을 조회할 수 있다")
	void findByMemberUsername_성공() {
		// Given
		Member savedMember = memberRepository.save(testMember);

		// When
		Optional<Member> foundMember = memberRepository.findByMemberUsername(testMember.getMemberUsername());

		// Then
		assertThat(foundMember).isPresent();

		assertAll("조회된 회원 정보 검증",
			() -> assertThat(foundMember.get().getId()).isEqualTo(savedMember.getId()),
			() -> assertThat(foundMember.get().getMemberUsername()).isEqualTo("TestUser"),
			() -> assertThat(foundMember.get().getEmail()).isEqualTo("test@gmail.com")
		);
	}

	@Test
	@DisplayName("존재하지 않는 사용자명으로 조회시 빈 결과를 반환한다")
	void findByMemberUsername_존재하지않음() {
		// When
		Optional<Member> foundMember = memberRepository.findByMemberUsername("NonExistentUser");

		// Then
		assertThat(foundMember).isEmpty();
	}

	@Test
	@DisplayName("이메일로 회원을 조회할 수 있다")
	void findByEmail_성공() {
		// Given
		Member savedMember = memberRepository.save(testMember);

		// When
		Optional<Member> foundMember = memberRepository.findByEmail(testMember.getEmail());

		// Then
		assertThat(foundMember).isPresent();

		assertAll("조회된 회원 정보 검증",
			() -> assertThat(foundMember.get().getId()).isEqualTo(savedMember.getId()),
			() -> assertThat(foundMember.get().getMemberUsername()).isEqualTo("TestUser"),
			() -> assertThat(foundMember.get().getEmail()).isEqualTo("test@gmail.com")
		);
	}

	@Test
	@DisplayName("존재하지 않는 이메일로 조회시 빈 결과를 반환한다")
	void findByEmail_존재하지않음() {
		// When
		Optional<Member> foundMember = memberRepository.findByEmail("nonexistent@gmail.com");

		// Then
		assertThat(foundMember).isEmpty();
	}

	@Test
	@DisplayName("사용자명 존재 여부를 확인할 수 있다")
	void existsByMemberUsername_존재함() {
		// Given
		memberRepository.save(testMember);

		// When
		boolean exists = memberRepository.existsByMemberUsername("TestUser");

		// Then
		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("존재하지 않는 사용자명은 false를 반환한다")
	void existsByMemberUsername_존재하지않음() {
		// When
		boolean exists = memberRepository.existsByMemberUsername("NonExistentUser");

		// Then
		assertThat(exists).isFalse();
	}

	@Test
	@DisplayName("이메일 존재 여부를 확인할 수 있다")
	void existsByEmail_존재함() {
		// Given
		memberRepository.save(testMember);

		// When
		boolean exists = memberRepository.existsByEmail("test@gmail.com");

		// Then
		assertThat(exists).isTrue();
	}

	@Test
	@DisplayName("존재하지 않는 이메일은 false를 반환한다")
	void existsByEmail_존재하지않음() {
		// When
		boolean exists = memberRepository.existsByEmail("nonexistent@gmail.com");

		// Then
		assertThat(exists).isFalse();
	}

	@Test
	@DisplayName("회원을 저장할 수 있다")
	void save_성공() {
		// When
		Member savedMember = memberRepository.save(testMember);

		// Then
		assertAll("저장된 회원 정보 검증",
			() -> assertThat(savedMember.getId()).isNotNull(),
			() -> assertThat(savedMember.getMemberUsername()).isEqualTo("TestUser"),
			() -> assertThat(savedMember.getEmail()).isEqualTo("test@gmail.com"),
			() -> assertThat(savedMember.getRole()).isEqualTo("ROLE_USER")
		);
	}

	@Test
	@DisplayName("ID로 회원을 조회할 수 있다")
	void findById_성공() {
		// Given
		Member savedMember = memberRepository.save(testMember);

		// When
		Optional<Member> foundMember = memberRepository.findById(savedMember.getId());

		// Then
		assertThat(foundMember).isPresent();

		assertAll("ID로 조회된 회원 정보 검증",
			() -> assertThat(foundMember.get().getId()).isEqualTo(savedMember.getId()),
			() -> assertThat(foundMember.get().getMemberUsername()).isEqualTo("TestUser"),
			() -> assertThat(foundMember.get().getEmail()).isEqualTo("test@gmail.com")
		);
	}

	@Test
	@DisplayName("존재하지 않는 ID로 조회시 빈 결과를 반환한다")
	void findById_존재하지않음() {
		// When
		Optional<Member> foundMember = memberRepository.findById(999L);

		// Then
		assertThat(foundMember).isEmpty();
	}

	@Test
	@DisplayName("모든 회원을 조회할 수 있다")
	void findAll_성공() {
		// Given
		memberRepository.save(testMember);

		Member anotherMember = Member.builder()
			.memberUsername("AnotherUser")
			.password("AnotherPassword1!")
			.email("another@gmail.com")
			.role("ROLE_ADMIN")
			.build();
		memberRepository.save(anotherMember);

		// When
		var allMembers = memberRepository.findAll();

		// Then
		assertThat(allMembers).hasSize(2);
		assertThat(allMembers)
			.extracting(Member::getMemberUsername)
			.containsExactlyInAnyOrder("TestUser", "AnotherUser");
	}
}
