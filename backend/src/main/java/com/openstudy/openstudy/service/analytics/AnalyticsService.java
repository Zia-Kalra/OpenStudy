package com.openstudy.openstudy.service.analytics;

import static com.openstudy.openstudy.dto.analytics.AnalyticsResponse.*;

import com.openstudy.openstudy.config.AnalyticsProperties;
import com.openstudy.openstudy.dto.analytics.AnalyticsFilter;
import com.openstudy.openstudy.repository.AnalyticsRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Orchestrates repository queries, R analyses and insight rules.
 *
 * <p>Definition used everywhere: a result's percentage is score / max_score * 100. The "overall
 * average" is the arithmetic mean of those per-assessment percentages (each completed assessment
 * counts once), not total marks divided by total maximum marks. This keeps a 5-mark quiz and a
 * 100-mark assignment comparable.
 */
@Service
@Profile("!mock")
public class AnalyticsService {

    static final String AVERAGING_RULE =
            "Average = mean of per-assessment percentages (score / max score x 100). Only submitted quizzes "
            + "(latest attempt) and finalized assignments are counted.";

    private final AnalyticsRepository repository;
    private final RAnalyticsService r;
    private final AnalyticsProperties props;
    private final InsightEngine insights;
    private final Map<String, CachedReport> reportCache = new ConcurrentHashMap<>();

    private record CachedReport(long expiresAtMillis, StatisticalReport report) {
    }

    public AnalyticsService(AnalyticsRepository repository, RAnalyticsService r, AnalyticsProperties props) {
        this.repository = repository;
        this.r = r;
        this.props = props;
        this.insights = new InsightEngine(props.getMinResultsForInsights(), props.getPassMark());
    }

    // ------------------------------------------------------------------ student

    public StudentOverview studentOverview(long studentId, AnalyticsFilter f) {
        SummaryStats summary = repository.studentSummary(studentId, f);
        RankInfo rank = summary.count() == 0 ? null : repository.studentRank(studentId).orElse(null);
        return new StudentOverview(summary, rank, repository.studentBands(studentId, f), AVERAGING_RULE);
    }

    public List<SubjectStat> studentSubjects(long studentId, AnalyticsFilter f) {
        return repository.studentSubjects(studentId, f);
    }

    public StudentTrends studentTrends(long studentId, AnalyticsFilter f) {
        List<MonthStat> monthly = repository.studentMonthly(studentId, f);
        List<ResultRow> rows = repository.studentResults(studentId, f, props.getMaxRows());
        RResult analysis = r.run(RScript.TRENDS, toDataset(rows), List.of());
        return new StudentTrends(monthly, analysis);
    }

    public List<AssessmentRow> studentAssessments(long studentId, AnalyticsFilter f) {
        List<AssessmentRow> out = new ArrayList<>();
        for (ResultRow row : repository.studentResults(studentId, f, props.getMaxRows())) {
            out.add(new AssessmentRow(row.assessmentType(), row.title(), row.subjectName(), row.score(),
                    row.maxScore(), row.percentage(), row.completedAt()));
        }
        java.util.Collections.reverse(out);   // newest first for tables
        return out;
    }

    public InsightResult studentInsights(long studentId, AnalyticsFilter f) {
        SummaryStats summary = repository.studentSummary(studentId, f);
        List<SubjectStat> subjects = repository.studentSubjects(studentId, f);
        List<Double> chronological = repository.studentResults(studentId, f, props.getMaxRows()).stream()
                .map(ResultRow::percentage).toList();
        return insights.forStudent(summary, subjects, chronological);
    }

    // ------------------------------------------------------------------ teacher

    public void assertTeacherMaySee(long teacherId, AnalyticsFilter f) {
        if (f.subjectId() != null && !repository.teacherCanAccessSubject(teacherId, f.subjectId())) {
            throw new AnalyticsAccessException(HttpStatus.FORBIDDEN, "You do not have access to this subject.");
        }
    }

    public Filters teacherFilters(long teacherId) {
        return new Filters(repository.teacherSubjects(teacherId), List.of("QUIZ", "ASSIGNMENT"));
    }

    public TeacherClassOverview teacherClassOverview(long teacherId, AnalyticsFilter f) {
        assertTeacherMaySee(teacherId, f);
        SummaryStats summary = repository.teacherSummary(teacherId, f);
        long enrolled = repository.teacherEnrolledStudents(teacherId, f);
        Double rate = enrolled == 0 ? null : Math.min(100.0, Math.round(summary.studentCount() * 10000.0 / enrolled) / 100.0);
        Participation participation = new Participation(enrolled, summary.studentCount(), summary.count(), rate);
        List<AssessmentParticipation> perAssessment = repository.teacherAssessmentParticipation(teacherId, f);
        RResult descriptive = summary.count() == 0
                ? RResult.failed("INSUFFICIENT_DATA", "No completed assessments match the selected filters.")
                : r.run(RScript.DESCRIPTIVE, toDataset(repository.teacherResults(teacherId, f, props.getMaxRows())), options());
        return new TeacherClassOverview(summary, participation, perAssessment, descriptive, AVERAGING_RULE);
    }

    public List<SubjectStat> teacherSubjectStats(long teacherId, AnalyticsFilter f) {
        assertTeacherMaySee(teacherId, f);
        return repository.teacherSubjectStats(teacherId, f);
    }

    public Distribution teacherDistribution(long teacherId, AnalyticsFilter f) {
        assertTeacherMaySee(teacherId, f);
        return repository.teacherDistribution(teacherId, f);
    }

    public List<MonthStat> teacherTrends(long teacherId, AnalyticsFilter f) {
        assertTeacherMaySee(teacherId, f);
        return repository.teacherMonthly(teacherId, f);
    }

    public List<QuestionStat> teacherQuestions(long teacherId, AnalyticsFilter f) {
        assertTeacherMaySee(teacherId, f);
        if ("ASSIGNMENT".equals(f.type())) {
            return List.of();   // question accuracy exists for auto-scored quiz questions only
        }
        return repository.teacherQuestionStats(teacherId, f, 25);
    }

    public InsightResult teacherInsights(long teacherId, AnalyticsFilter f) {
        assertTeacherMaySee(teacherId, f);
        SummaryStats summary = repository.teacherSummary(teacherId, f);
        List<Double> pcts = repository.teacherResults(teacherId, f, props.getMaxRows()).stream()
                .map(ResultRow::percentage).toList();
        long enrolled = repository.teacherEnrolledStudents(teacherId, f);
        Double rate = enrolled == 0 ? null : Math.min(100.0, summary.studentCount() * 100.0 / enrolled);
        Participation participation = new Participation(enrolled, summary.studentCount(), summary.count(), rate);
        return insights.forTeacher(summary, pcts, repository.teacherSubjectStats(teacherId, f), participation,
                f.type() != null && f.type().equals("ASSIGNMENT") ? List.of() : repository.teacherQuestionStats(teacherId, f, 25));
    }

    /** Runs all four R analyses once per filter (cached briefly, because each R start costs ~1 s). */
    public StatisticalReport teacherStatisticalReport(long teacherId, AnalyticsFilter f) {
        assertTeacherMaySee(teacherId, f);
        String key = teacherId + "#" + f.cacheKey();
        long now = System.currentTimeMillis();
        CachedReport cached = reportCache.get(key);
        if (cached != null && cached.expiresAtMillis() > now) {
            return cached.report();
        }
        List<ResultRow> rows = repository.teacherResults(teacherId, f, props.getMaxRows());
        List<DatasetRow> dataset = toDataset(rows);
        long students = rows.stream().map(ResultRow::studentUserId).distinct().count();
        List<String> opts = options();
        StatisticalReport report = new StatisticalReport(
                r.run(RScript.DESCRIPTIVE, dataset, opts), r.run(RScript.TRENDS, dataset, opts),
                r.run(RScript.HYPOTHESIS, dataset, opts), r.run(RScript.REGRESSION, dataset, opts),
                rows.size(), students);
        boolean anyOk = List.of(report.descriptive(), report.trends(), report.hypothesis(), report.regression())
                .stream().anyMatch(x -> "OK".equals(x.status()));
        if (props.getReportCacheSeconds() > 0 && anyOk) {
            if (reportCache.size() > 200) {
                reportCache.clear();
            }
            reportCache.put(key, new CachedReport(now + props.getReportCacheSeconds() * 1000L, report));
        }
        return report;
    }

    // ------------------------------------------------------------------ helpers

    private List<String> options() {
        return List.of(String.format(Locale.ROOT, "pass_mark=%.2f", props.getPassMark()),
                String.format(Locale.ROOT, "benchmark=%.2f", props.getPassMark()),
                "alpha=0.05");
    }

    /** Replaces database ids with per-request pseudonyms (S001, S002 ...) before data leaves Java. */
    static List<DatasetRow> toDataset(List<ResultRow> rows) {
        Map<Long, String> keys = new LinkedHashMap<>();
        List<DatasetRow> out = new ArrayList<>(rows.size());
        for (ResultRow row : rows) {
            String key = keys.computeIfAbsent(row.studentUserId(), id -> String.format(Locale.ROOT, "S%03d", keys.size() + 1));
            out.add(new DatasetRow(key, row.assessmentType(), row.subjectName(), row.title(), row.percentage(), row.completedAt()));
        }
        return out;
    }
}
