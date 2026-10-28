package com.openstudy.openstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openstudy.ai")
public class AiProperties {

    /**
     * mock | groq
     */
    private String provider = "mock";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
