package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsDTO.*;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsQuery;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "openstudy.teacher-analytics.r-provider", havingValue = "mock", matchIfMissing = true)
public class MockTeacherRAnalyticsProvider implements TeacherRAnalyticsProvider {
    @Override
    public List<RReport> savedReports(TeacherAnalyticsScope scope, TeacherAnalyticsQuery query) {
        if (!scope.teacher().isDemoIdentity() || !"DEMO-TEACHER-001".equals(scope.teacher().getTeacherId())) { return List.of(); }
        // Precomputed illustrative fixtures, not calculations performed by Java or R.
        return List.of(
            report("R-DESC", "Class score summary", "DESCRIPTIVE", "Descriptive statistics", Set.of("CS-2A"),
                "Explore central tendency and spread before comparing groups. These are illustrative saved values.",
                List.of(new RMetric("Mean", "74.2%"), new RMetric("Median", "76.0%"), new RMetric("Standard deviation", "12.6"), new RMetric("IQR", "18.4")),
                List.of(new Band("Lower quartile", 0, 64), new Band("Median", 0, 76), new Band("Upper quartile", 0, 82.4))),
            report("R-TTEST", "Quiz vs assignment comparison", "T_TEST", "Paired t-test", Set.of("CS-2A"),
                "Illustrative paired comparison. A future R result must state pairing, sample size and assumption checks before interpretation.",
                List.of(new RMetric("Paired observations", "12"), new RMetric("t statistic", "2.74"), new RMetric("p value", "0.019"), new RMetric("Mean difference", "+5.1 points")),
                List.of(new Band("Quiz mean", 0, 70.8), new Band("Assignment mean", 0, 75.9))),
            report("R-ANOVA", "Compare assigned classes", "ANOVA", "ANOVA", Set.of("CS-2A", "CS-2B"),
                "Illustrative comparison across assigned classes. Significance does not identify which groups differ; inspect assumptions and post-hoc tests in R.",
                List.of(new RMetric("Groups", "2"), new RMetric("F statistic", "1.83"), new RMetric("p value", "0.193"), new RMetric("Status", "Example output")),
                List.of(new Band("CS-2A", 0, 74.2), new Band("CS-2B", 0, 71.6))),
            report("R-REG", "Assessment progress model", "REGRESSION", "Regression", Set.of("CS-2A", "CS-2B"),
                "Illustrative relationship between assessment week and normalized score. This sample is not a forecast or evidence of causation.",
                List.of(new RMetric("R squared", "0.42"), new RMetric("Week coefficient", "+1.8"), new RMetric("p value", "0.032"), new RMetric("Model", "Score ~ week")),
                List.of(new Band("Week 1 (fitted)", 0, 68), new Band("Week 2 (fitted)", 0, 69.8), new Band("Week 3 (fitted)", 0, 71.6), new Band("Week 4 (fitted)", 0, 73.4))))
            .stream().filter(r -> scope.classIds().containsAll(r.classIds()) && scope.subjectIds().containsAll(r.subjectIds())).toList();
    }

    private RReport report(String id, String title, String method, String label, Set<String> classes,
            String summary, List<RMetric> metrics, List<Band> chart) {
        return new RReport(id, title, method, label, classes, Set.of("DS", "DB"), "09 Oct 2026",
                summary, metrics, chart, true);
    }
}
