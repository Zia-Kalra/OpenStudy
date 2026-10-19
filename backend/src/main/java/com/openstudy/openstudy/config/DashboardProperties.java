package com.openstudy.openstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openstudy.dashboard")
public class DashboardProperties {

    /**
     * Data provider selector: mock (Phase 1) or jdbc (future Member integration).
     */
    private String provider = "mock";

    /**
     * When true, the UI labels dashboard figures as demo data.
     */
    private boolean demoMode = true;

    /**
     * populated | empty — controls mock lists and zeroed statistics.
     */
    private String mockScenario = "populated";

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    public String getMockScenario() {
        return mockScenario;
    }

    public void setMockScenario(String mockScenario) {
        this.mockScenario = mockScenario;
    }

    public boolean isEmptyScenario() {
        return "empty".equalsIgnoreCase(mockScenario);
    }
}
