package com.openstudy.openstudy.service.ai;

import com.openstudy.openstudy.dto.AssignmentEvaluationRequest;
import com.openstudy.openstudy.dto.AssignmentEvaluationResult;
import com.openstudy.openstudy.dto.GeneratedMCQDTO;
import com.openstudy.openstudy.dto.MCQGenerationRequest;
import java.util.List;
import com.openstudy.openstudy.dto.analytics.AnalyticsInsightRequest;

/**
 * Shared AI contract owned by Member 5.
 * Member 2 should call {@link #generateMCQs(MCQGenerationRequest)} and persist after teacher review.
 * Member 3 should call {@link #evaluateAssignmentAnswer(AssignmentEvaluationRequest)} and keep final grading authority.
 * Implementations must not write to MySQL.
 */
public interface AIService {

    List<GeneratedMCQDTO> generateMCQs(MCQGenerationRequest request);

    AssignmentEvaluationResult evaluateAssignmentAnswer(AssignmentEvaluationRequest request);

    default AnalyticsInsightRequest.Selection summarizeAnalytics(AnalyticsInsightRequest request) {
        throw new AiServiceException(AiErrorCode.UNEXPECTED_RESPONSE, "Analytics insights require the live Groq provider.");
    }
}
