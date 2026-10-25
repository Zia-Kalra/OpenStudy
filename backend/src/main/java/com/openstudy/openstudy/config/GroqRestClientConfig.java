package com.openstudy.openstudy.config;

import java.time.Duration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@ConditionalOnProperty(name = "openstudy.ai.provider", havingValue = "groq")
public class GroqRestClientConfig {
    @Bean
    RestClient groqRestClient(GroqProperties properties) {
        properties.validateTransport();
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()));
        // Fixed official HTTPS endpoint: never send an API key to a configurable host.
        return RestClient.builder().baseUrl("https://api.groq.com/openai/v1")
                .requestFactory(factory).build();
    }
}
