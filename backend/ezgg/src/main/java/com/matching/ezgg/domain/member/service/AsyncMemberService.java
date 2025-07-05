package com.matching.ezgg.domain.member.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.matching.ezgg.domain.matching.service.MatchingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncMemberService {

	private final MatchingService matchingService;

	@Async("memberTaskExecutor")
	public void updateMatchingAttributesAsync(Long memberId) {
		try {
			log.info("회원 매칭 속성 업데이트 시작 - memberId: {}", memberId);
			matchingService.updateAllAttributesOfMember(memberId);
			log.info("회원 매칭 속성 업데이트 완료 - memberId: {}", memberId);
		} catch (Exception e) {
			log.error("회원 매칭 속성 업데이트 실패 - memberId: {}", memberId, e);
		}
	}
}
