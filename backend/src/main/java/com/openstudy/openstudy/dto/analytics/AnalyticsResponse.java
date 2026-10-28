package com.openstudy.openstudy.dto.analytics;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Plain data records returned by the analytics endpoints (and used between layers).
 * Values in percentage points are already rounded to 2 decimals. Statistical sections produced by R
 * are carried as {@link RResult}.
 */
public final class AnalyticsResponse {

    private AnalyticsResponse() {
    }

    /** One completed assessment (see AnalyticsRepository.RESULTS). Internal: never serialized for teachers. */
    public record ResultRow(String assessmentType, long assessmentId, String title, String subjectName,
                            long studentUserId, double score, double maxScore, double percentage,
                            LocalDateTime completedAt) {
    }

    /** Client-safe row for a student's own assessment tables. */
    public record AssessmentRow(String assessmentType, String title, String subjectName, double score,
                                double maxScore, double percentage, LocalDateTime completedAt) {
    }

    public record SummaryStats(long count, Double average, Double min, Double max,
                               long quizCount, Double quizAverage,
                               long assignmentCount, Double assignmentAverage, long studentCount) {
    }

    public record SubjectStat(long subjectId, String subjectName, long count, Double average, Double min, Double max,
                              long quizCount, Double quizAverage, long assignmentCount, Double assignmentAverage) {
    }

    public record MonthStat(String month, long count, Double average, Double quizAverage, Double assignmentAverage) {
    }

    public record BinStat(String label, int from, int to, long count, double percent) {
    }

    public record BandStat(String label, long count, double percent) {
    }

    /** Position among all students with at least one completed result. Contains no other student's data. */
    public record RankInfo(int rank, int classSize, double percentile) {
    }

    public record Participation(long enrolledStudents, long studentsWithResults, long results, Double rate) {
    }

    public record AssessmentParticipation(String assessmentType, long assessmentId, String title, String subjectName,
                                          long enrolled, long completed, Double average, Double rate) {
    }

    public record QuestionStat(long quizId, String quizTitle, String subjectName, int position, String question,
                               long attempts, long correct, double correctRate, String difficulty, boolean reliable) {
    }

    public record SubjectOption(long subjectId, String subjectName) {
    }

    public record Insight(String type, String category, String message) {
    }

    public record InsightResult(boolean sufficientData, String note, List<Insight> insights,
                                List<String> recommendations) {
    }

    /**
     * Result of one R analysis. status: OK | INSUFFICIENT_DATA | UNAVAILABLE | TIMEOUT | BUSY | ERROR.
     * {@code data} is the JSON tree produced by the R script (null unless status is OK).
     */
    public record RResult(String status, String message, Object data) {
        public static RResult ok(Object data) {
            return new RResult("OK", null, data);
        }

        public static RResult failed(String status, String message) {
            return new RResult(status, message, null);
        }
    }

    // ---- endpoint payloads ----

    public record StudentOverview(SummaryStats summary, RankInfo rank, List<BandStat> bands, String averagingRule) {
    }

    public record StudentTrends(List<MonthStat> monthly, RResult analysis) {
    }

    public record Filters(List<SubjectOption> subjects, List<String> types) {
    }

    public record TeacherClassOverview(SummaryStats summary, Participation participation,
                                       List<AssessmentParticipation> assessments, RResult descriptive,
                                       String averagingRule) {
    }

    public record Distribution(List<BinStat> bins, long total) {
    }

    public record StatisticalReport(RResult descriptive, RResult trends, RResult hypothesis, RResult regression,
                                    long observations, long students) {
    }
}
