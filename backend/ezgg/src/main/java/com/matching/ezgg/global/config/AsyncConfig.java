package com.matching.ezgg.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

	@Bean(name = "memberTaskExecutor")
	public Executor memberTaskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);                //최소 2개 스레드 유지
		executor.setMaxPoolSize(5);                    //최대 5개 스레드 사용
		executor.setQueueCapacity(50);                //대기열 50개 까지
		executor.setThreadNamePrefix("Member-");    //로그에서 스레드이름구분
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy()); // 큐가 가득 찰 때 처리
		executor.initialize();
		return executor;
	}
}
