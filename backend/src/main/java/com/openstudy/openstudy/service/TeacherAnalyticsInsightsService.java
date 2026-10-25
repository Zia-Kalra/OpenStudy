package com.openstudy.openstudy.service;

import com.openstudy.openstudy.dto.analytics.AnalyticsInsightRequest;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsDTO;
import com.openstudy.openstudy.service.ai.AIService;
import com.openstudy.openstudy.service.ai.AiServiceException;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class TeacherAnalyticsInsightsService {
    private final AIService ai;
    public TeacherAnalyticsInsightsService(AIService ai) { this.ai = ai; }

    public Insight explain(TeacherAnalyticsDTO data, boolean student) {
        if (data.demo()) { return unavailable("AI insights are available only for real Aiven results."); }
        var facts = new LinkedHashMap<String, String>();
        var advice = new LinkedHashMap<String, String>();
        int submitted;
        int graded;
        boolean rosterKnown;
        if (student) {
            if (data.selectedStudent() == null) { return unavailable("Select a student with recorded data first."); }
            var row = data.selectedStudent().summary();
            submitted = row.submitted(); graded = row.graded(); rosterKnown = row.rosterKnown();
            facts.put("submissions", "This student has " + submitted + " recorded submissions in the selected scope.");
            if (row.average() != null) { facts.put("average", "The average of this student's final grades is " + row.getAverageDisplay() + "."); }
            if (row.missing() > 0) { facts.put("missing", row.missing() + " expected submissions are outstanding for this student."); }
        } else {
            if (data.overview().totalStudents() == 0 || data.assessments().isEmpty()) {
                return unavailable("Insights need students and recorded assessments in your authorized scope.");
            }
            submitted = data.assessments().stream().mapToInt(a -> a.submitted()).sum();
            graded = data.overview().gradedCount();
            rosterKnown = data.overview().submissionRate() != null;
            facts.put("students", data.overview().totalStudents() + " students are represented in this subject scope.");
            facts.put("submissions", submitted + " submissions are recorded across " + data.assessments().size() + " assessments.");
            if (data.overview().average() != null) { facts.put("average", "The average normalized final grade is " + data.overview().getAverageDisplay() + "."); }
        }
        facts.put("graded", graded + " final grades are available; missing and ungraded work is excluded from averages.");
        int pending = Math.max(0, submitted - graded);
        if (pending > 0) {
            facts.put("pending", pending + " recorded submissions are awaiting a valid final grade.");
            advice.put("REVIEW_PENDING", "Review the recorded work awaiting final grades before comparing performance.");
        }
        if (!rosterKnown) {
            facts.put("coverage", "Enrollment coverage is incomplete; a reliable submission rate cannot be calculated.");
            advice.put("CHECK_COVERAGE", "Confirm enrollment coverage with the team before interpreting participation rates.");
        }
        advice.put("REVIEW_RECORDED", "Use the recorded assessment results and teacher feedback to plan the next review.");
        var request = new AnalyticsInsightRequest(facts, advice);
        try {
            var selection = ai.summarizeAnalytics(request);
            if (selection.factIds().isEmpty() || selection.factIds().size() > 3
                    || !facts.keySet().containsAll(selection.factIds()) || !advice.containsKey(selection.recommendationId())) {
                return unavailable("The AI response could not be grounded in the recorded facts. Please retry.");
            }
            // Render ONLY original Java facts and eligible advice; generated prose/numbers never reach the UI.
            return new Insight(selection.factIds().stream().map(facts::get).toList(), advice.get(selection.recommendationId()), "");
        } catch (AiServiceException failure) {
            String message = switch (failure.getErrorCode()) {
                case MISSING_API_KEY, INVALID_API_KEY -> "Groq credentials are unavailable. Check the backend configuration locally.";
                case RATE_LIMITED -> "Groq is rate limited. Please try again later.";
                case TIMEOUT, NETWORK_FAILURE -> "Groq is temporarily unreachable. Your calculated results are still available.";
                default -> "Groq could not provide a grounded summary. Your calculated results are unchanged.";
            };
            return unavailable(message);
        }
    }
    public static Insight unavailable(String message) { return new Insight(List.of(), "", message); }
    public record Insight(List<String> observations, String recommendation, String message) {
        public Insight { observations = List.copyOf(observations); }
        public List<String> getObservations() { return observations; }
        public String getRecommendation() { return recommendation; }
        public String getMessage() { return message; }
    }
}
