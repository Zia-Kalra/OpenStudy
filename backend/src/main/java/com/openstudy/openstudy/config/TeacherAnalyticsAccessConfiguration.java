package com.openstudy.openstudy.config;

import com.openstudy.openstudy.provider.analytics.TeacherAnalyticsAccessProvider;
import com.openstudy.openstudy.provider.analytics.UnavailableTeacherAnalyticsAccessProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration(proxyBeanMethods = false)
public class TeacherAnalyticsAccessConfiguration {
    @Bean
    @ConditionalOnMissingBean(TeacherAnalyticsAccessProvider.class)
    TeacherAnalyticsAccessProvider unavailableTeacherAnalyticsAccessProvider() {
        return new UnavailableTeacherAnalyticsAccessProvider();
    }
}
