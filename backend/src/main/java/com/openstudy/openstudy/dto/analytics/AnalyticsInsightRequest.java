package com.openstudy.openstudy.dto.analytics;

import java.util.List;
import java.util.Map;

/** Only server-calculated, anonymous facts and eligible advice; no student text or identity. */
public record AnalyticsInsightRequest(Map<String, String> facts, Map<String, String> recommendations) {
    public AnalyticsInsightRequest {
        facts = Map.copyOf(facts);
        recommendations = Map.copyOf(recommendations);
        if (facts.isEmpty() || facts.size() > 12 || recommendations.isEmpty() || recommendations.size() > 6) {
            throw new IllegalArgumentException("Invalid insight facts.");
        }
    }
    public record Selection(List<String> factIds, String recommendationId) {
        public Selection { factIds = List.copyOf(factIds); }
    }
}
