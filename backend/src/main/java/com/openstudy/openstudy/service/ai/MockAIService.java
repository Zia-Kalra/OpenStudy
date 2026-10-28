package com.openstudy.openstudy.service.ai;

import com.openstudy.openstudy.dto.AssignmentEvaluationRequest;
import com.openstudy.openstudy.dto.AssignmentEvaluationResult;
import com.openstudy.openstudy.dto.GeneratedMCQDTO;
import com.openstudy.openstudy.dto.MCQGenerationRequest;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Local-development stand-in selected explicitly by openstudy.ai.provider=mock.
 * Responses are explicitly labeled as placeholders and must not be treated as Groq output.
 */
@Service
@ConditionalOnProperty(name = "openstudy.ai.provider", havingValue = "mock", matchIfMissing = true)
public class MockAIService implements AIService {

    private static final Logger log = LoggerFactory.getLogger(MockAIService.class);

    private final AiResponseValidator validator;

    public MockAIService(AiResponseValidator validator) {
        this.validator = validator;
    }

    @Override
    public List<GeneratedMCQDTO> generateMCQs(MCQGenerationRequest request) {
        AiRequestValidator.validate(request);
        log.info("AIService mock provider in use. Returning labeled placeholders, not Groq output.");
        int count = request.getNumberOfQuestions();
        List<GeneratedMCQDTO> generated = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String optionA = "Placeholder option A";
            generated.add(new GeneratedMCQDTO(
                    validator.demoPrefix() + " Sample " + request.getSubject() + " question " + i
                            + " about " + request.getTopic() + "?",
                    List.of(optionA, "Placeholder option B", "Placeholder option C", "Placeholder option D"),
                    optionA,
                    validator.demoPrefix() + " This explanation is a local stub for Member 2 wiring.",
                    request.getDifficulty(),
                    AiResponseValidator.SOURCE_MOCK));
        }
        return validator.validateMcqs(request, generated);
    }

    @Override
    public AssignmentEvaluationResult evaluateAssignmentAnswer(AssignmentEvaluationRequest request) {
        AiRequestValidator.validate(request);
        log.info("AIService mock provider in use. Returning labeled placeholders, not Groq output.");
        String feedback = validator.demoPrefix()
                + " Local stub feedback for wiring Member 3. Groq was not called.";
        return validator.validateEvaluation(
                request,
                0,
                feedback,
                List.of(validator.demoPrefix() + " Strengths are not available without Groq."),
                List.of("Configure GROQ_API_KEY and set openstudy.ai.provider=groq for live suggestions."),
                AiResponseValidator.SOURCE_MOCK);
    }
}
