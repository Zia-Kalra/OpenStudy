package com.openstudy.openstudy.dto.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsData.*;
import java.util.List;
import java.util.Locale;

/** Immutable view DTOs. JSPs call computed getters explicitly because EL resolves record components before bean properties. */
public record TeacherAnalyticsDTO(List<Cohort> classes, List<Subject> subjects, TeacherAnalyticsQuery query,
        Overview overview, List<StudentRow> students, StudentDetail selectedStudent,
        List<AssessmentRow> assessments, List<SubjectScore> subjectComparison, List<QuestionRow> questions,
        List<RReport> reports, RReport selectedReport, boolean demo, List<String> notices,
        DescriptiveStatistics statistics) {
    public TeacherAnalyticsDTO(List<Cohort> classes, List<Subject> subjects, TeacherAnalyticsQuery query,
            Overview overview, List<StudentRow> students, StudentDetail selectedStudent,
            List<AssessmentRow> assessments, List<SubjectScore> subjectComparison, List<QuestionRow> questions,
            List<RReport> reports, RReport selectedReport, boolean demo) {
        this(classes, subjects, query, overview, students, selectedStudent, assessments, subjectComparison,
                questions, reports, selectedReport, demo, List.of(), new DescriptiveStatistics(0, null, null, null, null, null));
    }
    public TeacherAnalyticsDTO {
        classes = List.copyOf(classes); subjects = List.copyOf(subjects); students = List.copyOf(students);
        assessments = List.copyOf(assessments); subjectComparison = List.copyOf(subjectComparison);
        questions = List.copyOf(questions); reports = List.copyOf(reports); notices = List.copyOf(notices);
    }
    public List<Cohort> getClasses() { return classes; }
    public List<Subject> getSubjects() { return subjects; }
    public TeacherAnalyticsQuery getQuery() { return query; }
    public Overview getOverview() { return overview; }
    public List<StudentRow> getStudents() { return students; }
    public StudentDetail getSelectedStudent() { return selectedStudent; }
    public List<AssessmentRow> getAssessments() { return assessments; }
    public List<SubjectScore> getSubjectComparison() { return subjectComparison; }
    public List<QuestionRow> getQuestions() { return questions; }
    public List<RReport> getReports() { return reports; }
    public RReport getSelectedReport() { return selectedReport; }
    public boolean isDemo() { return demo; }
    public List<String> getNotices() { return notices; }
    public DescriptiveStatistics getStatistics() { return statistics; }

    public static String percent(Double value) { return value == null ? "—" : String.format(Locale.ROOT, "%.1f%%", value); }

    public record Overview(int totalStudents, int gradedCount, Double average, Double highest, Double lowest,
            Double quizAverage, Double assignmentAverage, Double submissionRate, List<Trend> trends, List<Band> distribution) {
        public Overview { trends = List.copyOf(trends); distribution = List.copyOf(distribution); }
        public int getTotalStudents() { return totalStudents; }
        public int getGradedCount() { return gradedCount; }
        public String getAverageDisplay() { return percent(average); }
        public String getHighestDisplay() { return percent(highest); }
        public String getLowestDisplay() { return percent(lowest); }
        public String getQuizDisplay() { return percent(quizAverage); }
        public String getAssignmentDisplay() { return percent(assignmentAverage); }
        public String getSubmissionDisplay() { return percent(submissionRate); }
        public List<Trend> getTrends() { return trends; }
        public List<Band> getDistribution() { return distribution; }
    }
    public record Trend(String label, Double quiz, Double assignment) {
        public Trend { bounded(quiz); bounded(assignment); }
        public String getLabel() { return label; }
        public double getQuizHeight() { return quiz == null ? 0 : quiz; }
        public double getAssignmentHeight() { return assignment == null ? 0 : assignment; }
        public String getQuizDisplay() { return percent(quiz); }
        public String getAssignmentDisplay() { return percent(assignment); }
    }
    public record Band(String label, int count, double percent) {
        public Band { bounded(percent); if (count < 0) { throw new IllegalArgumentException("Invalid chart count."); } }
        public String getLabel() { return label; }
        public int getCount() { return count; }
        public double getPercent() { return percent; }
    }
    public record StudentRow(Student student, Double average, Double quizAverage, Double assignmentAverage,
            int assigned, int submitted, int graded, int missing, String band, String bandLabel) {
        public Student getStudent() { return student; }
        public double getAverage() { return average == null ? 0 : average; }
        public String getAverageDisplay() { return percent(average); }
        public String getQuizDisplay() { return percent(quizAverage); }
        public String getAssignmentDisplay() { return percent(assignmentAverage); }
        public int getAssigned() { return assigned; }
        public int getSubmitted() { return submitted; }
        public int getGraded() { return graded; }
        public int getMissing() { return missing; }
        public String getBand() { return band; }
        public String getBandLabel() { return bandLabel; }
        public String getSubmissionDisplay() { return assigned == 0 ? "—" : submitted + " / " + assigned; }
    }
    public record StudentDetail(StudentRow summary, List<SubjectScore> subjects, List<History> history,
            List<Trend> trends, List<String> feedback) {
        public StudentDetail { subjects = List.copyOf(subjects); history = List.copyOf(history); trends = List.copyOf(trends); feedback = List.copyOf(feedback); }
        public StudentRow getSummary() { return summary; }
        public List<SubjectScore> getSubjects() { return subjects; }
        public List<History> getHistory() { return history; }
        public List<Trend> getTrends() { return trends; }
        public List<String> getFeedback() { return feedback; }
    }
    public record SubjectScore(String label, Double quiz, Double assignment, Double average, int graded) {
        public String getLabel() { return label; }
        public double getAverage() { return average == null ? 0 : average; }
        public String getAverageDisplay() { return percent(average); }
        public String getQuizDisplay() { return percent(quiz); }
        public String getAssignmentDisplay() { return percent(assignment); }
        public int getGraded() { return graded; }
    }
    public record History(String title, String subject, String type, String date, String status, Double percent, String feedback) {
        public String getTitle() { return title; }
        public String getSubject() { return subject; }
        public String getType() { return type; }
        public String getDate() { return date; }
        public String getStatus() { return status; }
        public String getScoreDisplay() { return TeacherAnalyticsDTO.percent(percent); }
        public String getFeedback() { return feedback; }
    }
    public record AssessmentRow(String id, String title, String classId, String subject, String type, String date,
            int expected, int submitted, int graded, Double average, double submissionRate) {
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getClassId() { return classId; }
        public String getClassDisplay() { return classId.isBlank() ? "Not linked" : classId; }
        public String getSubject() { return subject; }
        public String getType() { return type; }
        public String getDate() { return date; }
        public int getExpected() { return expected; }
        public int getSubmitted() { return submitted; }
        public int getGraded() { return graded; }
        public String getAverageDisplay() { return percent(average); }
        public String getSubmissionDisplay() { return expected == 0 ? "\u2014" : percent(submissionRate); }
        public double getSubmissionRate() { return submissionRate; }
    }
    public record QuestionRow(String assessment, String label, String difficulty, int answered, int correct, double correctRate) {
        public String getAssessment() { return assessment; }
        public String getLabel() { return label; }
        public String getDifficulty() { return difficulty; }
        public int getAnswered() { return answered; }
        public int getCorrect() { return correct; }
        public double getCorrectRate() { return correctRate; }
        public String getRateDisplay() { return answered == 0 ? "—" : percent(correctRate); }
    }
    public record RMetric(String label, String value) {
        public String getLabel() { return label; }
        public String getValue() { return value; }
    }
    /** Descriptive summary of real normalized grades; not an R-engine or saved-report output. */
    public record DescriptiveStatistics(int count, Double mean, Double median, Double standardDeviation,
            Double lowerQuartile, Double upperQuartile) {
        public int getCount() { return count; }
        public String getMeanDisplay() { return percent(mean); }
        public String getMedianDisplay() { return percent(median); }
        public String getDeviationDisplay() {
            return standardDeviation == null ? "\u2014" : String.format(Locale.ROOT, "%.2f pp", standardDeviation);
        }
        public String getQuartileDisplay() { return percent(lowerQuartile) + " / " + percent(upperQuartile); }
    }
    /** Stored report output supplied by R; Java does not recompute statistical tests. */
    public record RReport(String id, String title, String method, String methodLabel, java.util.Set<String> classIds,
            java.util.Set<String> subjectIds, String createdAt, String summary, List<RMetric> metrics, List<Band> chart, boolean demo) {
        public RReport { classIds = java.util.Set.copyOf(classIds); subjectIds = java.util.Set.copyOf(subjectIds); metrics = List.copyOf(metrics); chart = List.copyOf(chart); }
        public String getId() { return id; }
        public String getTitle() { return title; }
        public String getMethod() { return method; }
        public String getMethodLabel() { return methodLabel; }
        public String getCreatedAt() { return createdAt; }
        public String getSummary() { return summary; }
        public List<RMetric> getMetrics() { return metrics; }
        public List<Band> getChart() { return chart; }
        public boolean isDemo() { return demo; }
    }

    private static void bounded(Double value) {
        if (value != null && (!Double.isFinite(value) || value < 0 || value > 100)) {
            throw new IllegalArgumentException("Chart percentages must be finite and between zero and 100.");
        }
    }
}
