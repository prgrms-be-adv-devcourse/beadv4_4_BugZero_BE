package com.bugzero.rarego.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.slf4j.Slf4j;

/**
 * 비동기 처리 활성화
 * SSE 이벤트 브로드캐스트를 위해 필요
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
	@Override
	public Executor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		// 1. 기본 스레드 수: 평소에 대기하는 스레드
		executor.setCorePoolSize(10);

		// 2. 최대 스레드 수: 트래픽 폭주시 생성할 최대 스레드
		executor.setMaxPoolSize(50);

		// 3. 대기열 크기: 모든 스레드가 바쁠 때 대기할 작업 수
		executor.setQueueCapacity(500);

		// 4. 스레드 이름 접두사
		executor.setThreadNamePrefix("Async-");

		// 큐가 꽉 차면, 작업을 버리지 않고 요청한 스레드(Main)가 직접 처리하게 함.
		// -> 서버가 터지는 것을 막고 자연스럽게 처리 속도를 조절(Backpressure)합니다.
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

		executor.initialize();
		return executor;
	}

	// 비동기 메서드(void)에서 예외 발생 시 로그 출력
	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, params) ->
			log.error("비동기 작업 중 예외 발생 - Method: {}, Error: {}",
				method.getName(), ex.getMessage(), ex);
	}
}