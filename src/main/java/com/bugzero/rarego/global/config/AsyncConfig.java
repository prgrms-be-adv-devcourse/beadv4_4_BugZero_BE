package com.bugzero.rarego.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 처리 활성화
 * SSE 이벤트 브로드캐스트를 위해 필요
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // @Async 활성화만 하면 됨
    // 기본 Executor 사용
}