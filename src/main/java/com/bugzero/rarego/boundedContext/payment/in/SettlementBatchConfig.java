package com.bugzero.rarego.boundedContext.payment.in;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.SimplePartitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import com.bugzero.rarego.boundedContext.payment.app.PaymentFacade;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SettlementBatchConfig {
	@Value("${custom.payment.settlement.chunkSize:10}")
	private int chunkSize;

	private static final int THREAD_SIZE = 5;

	private final PaymentFacade paymentFacade;
	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;

	@Bean
	public Job settlementJob() {
		return new JobBuilder("settlementJob", jobRepository)
			.start(mainStep())
			.build();
	}

	// 작업을 분할하고 subStep에 스레드를 할당하여 실행
	@Bean
	public Step mainStep() {
		return new StepBuilder("mainStep", jobRepository)
			.partitioner("subStep", new SimplePartitioner()) // 작업을 복제
			.step(subStep())
			.gridSize(THREAD_SIZE)  // 스레드 생성
			.taskExecutor(executor()) // 병렬 실행을 위한 스레드 풀
			.build();
	}

	// 실제 정산 로직 수행
	@Bean
	public Step subStep() {
		return new StepBuilder("settlementProcessStep", jobRepository)
			.tasklet((contribution, chunkContext) -> {
				int processedCount = paymentFacade.processSettlements(chunkSize);

				if (processedCount == 0) {
					return RepeatStatus.FINISHED;
				}

				contribution.incrementWriteCount(processedCount);

				return RepeatStatus.CONTINUABLE;
			}, transactionManager).build();
	}

	// 정산 스레드 풀 설정
	@Bean
	public TaskExecutor executor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(THREAD_SIZE);
		executor.setMaxPoolSize(THREAD_SIZE);
		executor.setThreadNamePrefix("settlement-thread-");
		executor.initialize();
		return executor;
	}
}