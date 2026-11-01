package com.openstudy.openstudy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import com.openstudy.openstudy.controller.QuestionBankController;
import com.openstudy.openstudy.repository.QuestionBankRepository;
import com.openstudy.openstudy.service.AuthService;
import com.openstudy.openstudy.service.MockAuthService;
import com.openstudy.openstudy.service.QuestionBankService;
import com.openstudy.openstudy.service.ai.AIService;
import com.openstudy.openstudy.service.ai.MockAIService;
import com.openstudy.openstudy.service.ai.GroqAIService;
import javax.sql.DataSource;
import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@ActiveProfiles("mock")
class OpenstudyApplicationTests {

    @Autowired
    private ApplicationContext context;

	@Test
	void contextLoads() {
    	assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
    	assertThat(context.getBeansOfType(JdbcTemplate.class))
            .containsKey("jdbcTemplate");
    	assertThat(context.getBeansOfType(QuestionBankRepository.class)).isEmpty();
    	assertThat(context.getBeansOfType(QuestionBankService.class)).isEmpty();
    	assertThat(context.getBeansOfType(QuestionBankController.class)).isEmpty();
    	assertThat(context.getBean(AuthService.class)).isInstanceOf(MockAuthService.class);
    	assertThat(context.getBeansOfType(GroqAIService.class)).isEmpty();
    	assertThat(context.getBean(AIService.class)).isInstanceOf(MockAIService.class);
	}
	@TestConfiguration
	static class MockJdbcConfiguration {
		
		@Bean
		JdbcTemplate jdbcTemplate() {
			return org.mockito.Mockito.mock(JdbcTemplate.class);
		}
	}
}

