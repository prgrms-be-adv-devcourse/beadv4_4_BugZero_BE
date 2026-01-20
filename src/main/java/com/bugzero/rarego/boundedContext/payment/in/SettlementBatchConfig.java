package com.bugzero.rarego.boundedContext.payment.in;

import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bugzero.rarego.boundedContext.payment.app.PaymentFacade;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class SettlementBatchConfig {
	@Value("${custom.payment.settlement.chunkSize:10}")
	private int chunkSize;

	private final PaymentFacade paymentFacade;
	private final JobRepository jobRepository;

	@Bean
	public Job settlementJob() {
		return new JobBuilder("settlementJob", jobRepository)
			.start(settlementProcessStep())
			.build();
	}

	@Bean
	public Step settlementProcessStep() {
		return new StepBuilder("settlementProcessStep", jobRepository)
			.tasklet((contribution, chunkContext) -> {
				int processedCount = paymentFacade.processSettlements(chunkSize);

				if (processedCount == 0) {
					return RepeatStatus.FINISHED;
				}

				contribution.incrementWriteCount(processedCount);

				return RepeatStatus.CONTINUABLE;
			}).build();
	}
}