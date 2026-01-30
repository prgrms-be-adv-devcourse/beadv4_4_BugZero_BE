package com.bugzero.rarego.bounded_context.payment.in;

import java.time.LocalDateTime;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Profile("prod")
@Component
@RequiredArgsConstructor
public class SettlementScheduler {
	private final JobOperator jobOperator;
	private final Job settlementJob;

	// 매일 새벽 3시에 정산 배치 실행
	@Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
	public void runSettlementJob() {
		try {
			// JobParameter에 실행 시간을 넣어 중복 실행 방지
			JobParameters jobParameters = new JobParametersBuilder()
				.addLocalDateTime("runAt", LocalDateTime.now())
				.toJobParameters();

			jobOperator.start(settlementJob, jobParameters);

			log.info("정산 배치 실행 완료");
		} catch (Exception e) {
			log.error("정산 배치 실행 실패: {}", e.getMessage());
			// TODO: 실패 알림 구현
		}
	}
}
