package com.bugzero.rarego.global.config;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.EnableJdbcJobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

@Configuration
@EnableBatchProcessing
@EnableJdbcJobRepository
public class BatchConfig { // 운영 환경이 아닐 때 앱 실행 시 마다 batch 관련 테이블 초기화
	@Bean
	@Profile("!prod")
	public DataSourceInitializer notProdDataSourceInitializer(DataSource dataSource) {
		ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
		populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-drop-mysql.sql"));
		populator.addScript(new ClassPathResource("org/springframework/batch/core/schema-mysql.sql"));
		populator.setContinueOnError(true);

		DataSourceInitializer initializer = new DataSourceInitializer();
		initializer.setDataSource(dataSource);
		initializer.setDatabasePopulator(populator);
		return initializer;
	}
}
