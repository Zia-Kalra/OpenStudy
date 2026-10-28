package com.openstudy.openstudy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.openstudy.openstudy.dto.analytics.*;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsData.*;
import com.openstudy.openstudy.provider.analytics.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class TeacherAnalyticsServiceTest {
    private final TeacherAnalyticsScope scope = new MockTeacherAnalyticsAccessProvider().resolve(null);
    private final MockTeacherAnalyticsDataProvider data = new MockTeacherAnalyticsDataProvider();
    private final MockTeacherRAnalyticsProvider reports = new MockTeacherRAnalyticsProvider();
    private final TeacherAnalyticsService service = new TeacherAnalyticsService(data, reports);

    private TeacherAnalyticsQuery query(String classId, String subjectId, String search, String band) {
        return new TeacherAnalyticsQuery(classId, subjectId, search, band, null, null);
    }

    @Test
    void populatedDataHasConsistentRostersRatesAndNormalizedScores() {
        var result = service.getAnalytics(scope, query(null, null, null, null), null, null);
        assertThat(result.demo()).isTrue();
        assertThat(result.overview().totalStudents()).isEqualTo(20);
        assertThat(result.students()).hasSize(20);
        assertThat(result.assessments()).hasSize(16);
        assertThat(result.questions()).hasSize(24);
        assertThat(result.reports()).hasSize(4);
        assertThat(result.overview().trends()).hasSize(4);
        assertThat(result.overview().distribution().stream().mapToInt(TeacherAnalyticsDTO.Band::count).sum()).isEqualTo(20);
        assertThat(result.overview().average()).isBetween(0.0, 100.0);
        assertThat(result.overview().submissionRate()).isBetween(0.0, 100.0);
        assertThat(result.students()).allMatch(s -> s.submitted() + s.missing() == s.assigned() && s.graded() <= s.submitted());
        assertThat(result.assessments().stream().mapToInt(TeacherAnalyticsDTO.AssessmentRow::graded).sum())
                .isEqualTo(result.overview().gradedCount());
    }

    @Test
    void searchAndBandFiltersDoNotChangeClassAggregates() {
        var searched = service.getAnalytics(scope, query(null, null, "aarav", null), null, null);
        assertThat(searched.students()).hasSize(1);
        assertThat(searched.students().getFirst().student().name()).isEqualTo("Aarav Mehta");
        assertThat(searched.overview().totalStudents()).isEqualTo(20);
        var strong = service.getAnalytics(scope, query(null, null, null, "STRONG"), null, null);
        assertThat(strong.students()).isNotEmpty().allMatch(s -> s.average() >= 80);
        assertThat(strong.overview().average()).isEqualTo(searched.overview().average());
        assertThat(service.getAnalytics(scope, query(null, null, "nonexistent", null), null, null).selectedStudent()).isNull();
    }

    @Test
    void classSubjectFiltersAndStudentDetailsStayScoped() {
        var result = service.getAnalytics(scope, query("CS-2A", "DS", null, null), "S001", null);
        assertThat(result.students()).hasSize(12).allMatch(s -> s.student().classId().equals("CS-2A"));
        assertThat(result.assessments()).hasSize(4).allMatch(a -> a.subject().equals("Data Structures"));
        assertThat(result.selectedStudent().subjects()).hasSize(1);
        assertThat(result.selectedStudent().summary().student().subjectIds()).containsExactly("DS");
        assertThat(result.selectedStudent().history()).hasSize(4);
        assertThat(result.selectedStudent().history()).anyMatch(h -> h.status().equals("MISSING") && h.percent() == null);
        assertThat(result.selectedStudent().feedback()).isNotEmpty();
        assertThat(result.reports()).isEmpty(); // Both-subject saved reports cannot leak through a single-subject filter.
    }

    @Test
    void rejectsUnassignedClassesSubjectsAndStudentIds() {
        assertStatus(403, () -> service.getAnalytics(scope, query("CS-3C", null, null, null), null, null));
        assertStatus(403, () -> service.getAnalytics(scope, query(null, "PRIVATE", null, null), null, null));
        assertStatus(404, () -> service.getAnalytics(scope, query("CS-2A", null, null, null), "S013", null));
        assertStatus(404, () -> service.getAnalytics(scope, query(null, null, null, null), "S999", null));
        assertStatus(404, () -> service.getAnalytics(scope, query(null, null, null, null), null, "PRIVATE"));
    }

    @Test
    void defensesExcludeExtraRowsAndReportsFromFaultyAdapters() {
        var original = data.load(scope);
        var students = new ArrayList<>(original.students());
        students.add(new Student("PRIVATE", "SECRET", "Unassigned student", "CS-3C", Set.of("DS")));
        var assessments = new ArrayList<>(original.assessments());
        assessments.add(new Assessment("PRIVATE-A", "Unassigned assessment", "CS-3C", "DS", "QUIZ", java.time.LocalDate.of(2026, 9, 1), 100));
        var results = new ArrayList<>(original.results());
        results.add(new Result("PRIVATE", "PRIVATE-A", "GRADED", 99.0, "Private feedback"));
        var expanded = new Snapshot(original.classes(), original.subjects(), students, assessments, results, original.questions(), true);
        var privateReport = new TeacherAnalyticsDTO.RReport("PRIVATE-R", "Private report", "ANOVA", "ANOVA", Set.of("CS-3C"), Set.of("DS"), "Today", "Private output", List.of(), List.of(), true);
        var guarded = new TeacherAnalyticsService(s -> expanded, (s, q) -> List.of(privateReport));
        var result = guarded.getAnalytics(scope, query(null, null, null, null), null, null);
        assertThat(result.students()).hasSize(20).noneMatch(s -> s.student().id().equals("PRIVATE"));
        assertThat(result.assessments()).noneMatch(a -> a.id().equals("PRIVATE-A"));
        assertThat(result.reports()).isEmpty();
        assertThat(result.overview().average()).isEqualTo(service.getAnalytics(scope, query(null, null, null, null), null, null).overview().average());
        assertStatus(404, () -> guarded.getAnalytics(scope, query(null, null, null, null), "PRIVATE", null));
    }

    @Test
    void emptyDataDoesNotInventZeroGradesOrDivideByZero() {
        var empty = new TeacherAnalyticsService(
                s -> new Snapshot(
                        List.of(), List.of(), List.of(),
                        List.of(), List.of(), List.of(), true),
                (s, q) -> List.of()
        );

        var result = empty.getAnalytics(scope, query(null, null, null, null), null, null);

        assertThat(result.overview().totalStudents()).isZero();
        assertThat(result.overview().average()).isNull();
        assertThat(result.overview().highest()).isNull();
        assertThat(result.overview().lowest()).isNull();
        assertThat(result.overview().submissionRate()).isZero();
        assertThat(result.overview().getAverageDisplay()).isEqualTo("—");
        assertThat(result.selectedStudent()).isNull();
    }

    @Test
    void ungradedWorkIsDifferentFromZeroScores() {
        var student = new Student("ONE", "OS-1", "One student", "CS-2A", Set.of("DS"));
        var a = new Assessment("A", "Work", "CS-2A", "DS", "QUIZ", java.time.LocalDate.of(2026, 9, 14), 25);
        var base = new Snapshot(List.of(new Cohort("CS-2A", "A")), List.of(new Subject("DS", "Data Structures")), List.of(student), List.of(a), List.of(new Result("ONE", "A", "SUBMITTED", null, "Pending")), List.of(), true);
        var ungraded = new TeacherAnalyticsService(s -> base, (s, q) -> List.of()).getAnalytics(scope, query(null, null, null, null), null, null);
        assertThat(ungraded.students().getFirst().average()).isNull();
        assertThat(ungraded.students().getFirst().band()).isEqualTo("NOT_GRADED");
        assertThat(ungraded.overview().submissionRate()).isEqualTo(100);
        var zero = new Snapshot(base.classes(), base.subjects(), base.students(), base.assessments(), List.of(new Result("ONE", "A", "GRADED", 0.0, "Review")), List.of(), true);
        assertThat(new TeacherAnalyticsService(s -> zero, (s, q) -> List.of()).getAnalytics(scope, query(null, null, null, null), null, null).overview().average()).isZero();
    }

    @Test
    void reportsAreReadOnlyScopedAndMethodFiltered() {
        var onlyClass = service.getAnalytics(scope, query("CS-2A", null, null, null), null, null);
        assertThat(onlyClass.reports()).hasSize(2);
        assertStatus(404, () -> service.getAnalytics(scope, query("CS-2A", null, null, null), null, "R-ANOVA"));
        var regression = service.getAnalytics(scope, new TeacherAnalyticsQuery(null, null, null, null, null, "REGRESSION"), null, "R-REG");
        assertThat(regression.reports()).hasSize(1);
        assertThat(regression.selectedReport().method()).isEqualTo("REGRESSION");
    }

    @Test
    void productionIdentityCannotAccidentallyReceiveDemoRecords() {
        var real = new TeacherAnalyticsScope(new com.openstudy.openstudy.dto.TeacherIdentityDTO("REAL", "Teacher", "", "Teacher", false), Set.of("CS-2A"), Set.of("DS"));
        assertStatus(503, () -> service.getAnalytics(real, query(null, null, null, null), null, null));
    }

    private void assertStatus(int status, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(status);
    }
}
