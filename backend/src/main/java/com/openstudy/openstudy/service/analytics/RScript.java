package com.openstudy.openstudy.service.analytics;

/**
 * Allow-list of R scripts that may be executed. Callers choose a constant, never a file name,
 * so no request data can influence which script runs.
 */
public enum RScript {
    DESCRIPTIVE("descriptive_analysis.R"),
    TRENDS("performance_trends.R"),
    HYPOTHESIS("hypothesis_testing.R"),
    REGRESSION("regression_analysis.R");

    private final String fileName;

    RScript(String fileName) {
        this.fileName = fileName;
    }

    public String fileName() {
        return fileName;
    }
}
