package com.openstudy.openstudy.dto.analytics;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Read-only normalized data shared by the JDBC and explicit demo adapters. */
public final class TeacherAnalyticsData {
    private TeacherAnalyticsData() { }

    public record Cohort(String id, String label) {
        public String getId() { return id; }
        public String getLabel() { return label; }
    }
    public record Subject(String id, String label) {
        public String getId() { return id; }
        public String getLabel() { return label; }
    }
    public record Student(String id, String rollNumber, String name, String classId, Set<String> subjectIds) {
        public Student { subjectIds = Set.copyOf(subjectIds); }
        public String getId() { return id; }
        public String getRollNumber() { return rollNumber; }
        public String getName() { return name; }
        public String getClassId() { return classId; }
        public String getClassDisplay() { return classId.isBlank() ? "Not linked" : classId; }
    }
    public record Assessment(String id, String title, String classId, String subjectId, String type,
                             LocalDate date, double maximumScore, Set<String> eligibleStudentIds) {
        public Assessment(String id, String title, String classId, String subjectId, String type,
                LocalDate date, double maximumScore) {
            this(id, title, classId, subjectId, type, date, maximumScore, null);
        }
        public Assessment {
            if (eligibleStudentIds != null) { eligibleStudentIds = Set.copyOf(eligibleStudentIds); }
            if (!Set.of("QUIZ", "ASSIGNMENT").contains(type) || date == null
                    || !Double.isFinite(maximumScore) || maximumScore <= 0) {
                throw new IllegalArgumentException("Invalid assessment data.");
            }
        }
    }
    public record Result(String studentId, String assessmentId, String status, Double score, String feedback) {
        public Result {
            if (!Set.of("GRADED", "SUBMITTED", "MISSING").contains(status)
                    || ("GRADED".equals(status) && (score == null || !Double.isFinite(score) || score < 0))
                    || (!"GRADED".equals(status) && score != null)) {
                throw new IllegalArgumentException("Invalid result data.");
            }
        }
    }
    /** Counts are supplied by the analytics data adapter, not inferred from a student's final score. */
    public record Question(String assessmentId, String label, String difficulty, int answered, int correct) {
        public Question {
            if (!Set.of("EASY", "MEDIUM", "HARD").contains(difficulty)
                    || answered < 0 || correct < 0 || correct > answered) {
                throw new IllegalArgumentException("Invalid question data.");
            }
        }
    }
    public record Snapshot(List<Cohort> classes, List<Subject> subjects, List<Student> students,
                           List<Assessment> assessments, List<Result> results, List<Question> questions, boolean demo,
                           List<String> notices) {
        public Snapshot(List<Cohort> classes, List<Subject> subjects, List<Student> students,
                List<Assessment> assessments, List<Result> results, List<Question> questions, boolean demo) {
            this(classes, subjects, students, assessments, results, questions, demo, List.of());
        }
        public Snapshot {
            classes = List.copyOf(classes);
            subjects = List.copyOf(subjects);
            students = List.copyOf(students);
            assessments = List.copyOf(assessments);
            results = List.copyOf(results);
            questions = List.copyOf(questions);
            notices = List.copyOf(notices);
        }
    }
}
