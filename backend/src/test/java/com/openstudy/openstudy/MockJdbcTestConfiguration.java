
package com.openstudy.openstudy;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@TestConfiguration
public class MockJdbcTestConfiguration {

    @Bean
    JdbcTemplate jdbcTemplate() {
        return org.mockito.Mockito.mock(JdbcTemplate.class);
    }
}
