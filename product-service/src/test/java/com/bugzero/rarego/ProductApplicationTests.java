package com.bugzero.rarego;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.bugzero.rarego.ai.config.TestAiConfig;

@SpringBootTest
@Import({TestAiConfig.class})
class ProductApplicationTests {

	@MockitoBean
	private ChatModel chatModel;

	@MockitoBean(name = "ollamaEmbeddingModel")
	private EmbeddingModel embeddingModel;

	@Test
	void contextLoads() {
	}

}
