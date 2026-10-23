package com.openstudy.openstudy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import com.openstudy.openstudy.service.ai.AIService;
import com.openstudy.openstudy.service.ai.MockAIService;
import com.openstudy.openstudy.service.ai.GroqAIService;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("mock")
class OpenstudyApplicationTests {

    @Autowired
    private ApplicationContext context;

	@Test
	void contextLoads() {
		assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
		assertThat(context.getBeansOfType(GroqAIService.class)).isEmpty();
		assertThat(context.getBean(AIService.class)).isInstanceOf(MockAIService.class);
	}

}
