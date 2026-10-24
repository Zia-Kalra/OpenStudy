package com.openstudy.openstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openstudy.ai")
public class AiProperties {

    /**
     * groq | mock
     */
    private String provider = "groq";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }
}
