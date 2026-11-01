package com.openstudy.openstudy.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.openstudy.openstudy.repository.QuestionBankRepository;
import com.openstudy.openstudy.service.QuestionBankService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

class DatabaseTestControllerConfigurationTest {

    @Test
    void registersControllerBeforeJdbcAutoConfigurationProvidesItsDependency() {
        new ApplicationContextRunner()
                .withUserConfiguration(DatabaseTestController.class, QuestionBankController.class,
                        QuestionBankService.class, QuestionBankRepository.class)
                .withConfiguration(AutoConfigurations.of(
                        DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class))
                // Creating the lazy pool does not obtain a connection; this test never queries it.
                .withPropertyValues(
                        "spring.datasource.url=jdbc:mysql://127.0.0.1:1/openstudy_test",
                        "spring.datasource.username=test")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(JdbcTemplate.class)
                        .hasSingleBean(DatabaseTestController.class)
                        .hasSingleBean(QuestionBankController.class)
                        .hasSingleBean(QuestionBankService.class)
                        .hasSingleBean(QuestionBankRepository.class));
    }

    @Test
    void mockProfileDoesNotRegisterDatabaseDiagnosticOrRequireJdbc() {
        new ApplicationContextRunner()
                .withUserConfiguration(DatabaseTestController.class, QuestionBankController.class,
                        QuestionBankService.class, QuestionBankRepository.class)
                .withPropertyValues("spring.profiles.active=mock")
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .doesNotHaveBean(DatabaseTestController.class)
                        .doesNotHaveBean(QuestionBankController.class)
                        .doesNotHaveBean(QuestionBankService.class)
                        .doesNotHaveBean(QuestionBankRepository.class)
                        .doesNotHaveBean(JdbcTemplate.class));
    }
}
