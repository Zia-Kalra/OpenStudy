package com.openstudy.openstudy.provider.analytics;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsData.*;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "openstudy.teacher-analytics.provider", havingValue = "mock", matchIfMissing = true)
public class MockTeacherAnalyticsDataProvider implements TeacherAnalyticsDataProvider {
    private final Snapshot fixture = createFixture();

    @Override
    public Snapshot load(TeacherAnalyticsScope scope) {
        Set<String> assigned = "DEMO-TEACHER-001".equals(scope.teacher().getTeacherId())
                ? Set.of("CS-2A", "CS-2B") : Set.of();
        var classes = fixture.classes().stream().filter(c -> assigned.contains(c.id()) && scope.classIds().contains(c.id())).toList();
        var classIds = classes.stream().map(Cohort::id).collect(Collectors.toSet());
        var subjects = fixture.subjects().stream().filter(s -> scope.subjectIds().contains(s.id())).toList();
        var students = fixture.students().stream().filter(s -> classIds.contains(s.classId())
                && s.subjectIds().stream().anyMatch(scope.subjectIds()::contains)).toList();
        var studentIds = students.stream().map(Student::id).collect(Collectors.toSet());
        var assessments = fixture.assessments().stream().filter(a -> classIds.contains(a.classId()) && scope.subjectIds().contains(a.subjectId())).toList();
        var assessmentIds = assessments.stream().map(Assessment::id).collect(Collectors.toSet());
        return new Snapshot(classes, subjects, students, assessments,
                fixture.results().stream().filter(r -> studentIds.contains(r.studentId()) && assessmentIds.contains(r.assessmentId())).toList(),
                fixture.questions().stream().filter(q -> assessmentIds.contains(q.assessmentId())).toList(), true);
    }

    private Snapshot createFixture() {
        var classes = List.of(new Cohort("CS-2A", "Computer Science · 2A"), new Cohort("CS-2B", "Computer Science · 2B"));
        var subjects = List.of(new Subject("DS", "Data Structures"), new Subject("DB", "Databases"));
        String[] names = {"Aarav Mehta", "Ananya Rao", "Dev Patel", "Isha Nair", "Kabir Shah", "Meera Joshi",
                "Neel Verma", "Riya Kapoor", "Sana Ali", "Vihaan Das", "Zoya Khan", "Arjun Sen",
                "Diya Menon", "Harsh Gupta", "Kavya Iyer", "Manav Roy", "Nisha Paul", "Rehan Malik", "Tara Bose", "Yash Jain"};
        int[] baseline = {42, 55, 64, 67, 71, 76, 81, 85, 89, 94, 58, 73, 49, 61, 68, 74, 80, 86, 92, 65};
        var students = new ArrayList<Student>();
        for (int i = 0; i < names.length; i++) {
            students.add(new Student("S%03d".formatted(i + 1), "OS-%03d".formatted(i + 1), names[i],
                    i < 12 ? "CS-2A" : "CS-2B", Set.of("DS", "DB")));
        }
        var assessments = new ArrayList<Assessment>();
        var results = new ArrayList<Result>();
        var questions = new ArrayList<Question>();
        for (var cohort : classes) {
            for (int week = 0; week < 4; week++) {
                String subject = week % 2 == 0 ? "DS" : "DB";
                for (String type : List.of("QUIZ", "ASSIGNMENT")) {
                    String id = cohort.id() + "-" + type + "-" + (week + 1);
                    double maximum = "QUIZ".equals(type) ? 25 : 50;
                    String title = (subject.equals("DS") ? "Algorithms" : "SQL and relational design")
                            + (type.equals("QUIZ") ? " quiz " : " assignment ") + (week / 2 + 1);
                    var assessment = new Assessment(id, title, cohort.id(), subject, type,
                            LocalDate.of(2026, 9, 14).plusWeeks(week).plusDays(type.equals("QUIZ") ? 0 : 2), maximum);
                    assessments.add(assessment);
                    int answered = 0;
                    for (int i = 0; i < students.size(); i++) {
                        var student = students.get(i);
                        if (!student.classId().equals(cohort.id())) { continue; }
                        int seed = i + week + (type.equals("QUIZ") ? 0 : 3);
                        String status = seed % 13 == 0 ? "MISSING" : seed % 17 == 0 ? "SUBMITTED" : "GRADED";
                        double percent = Math.min(98, baseline[i] + week * 2 + (type.equals("ASSIGNMENT") ? 4 : -2));
                        Double score = status.equals("GRADED") ? Math.round(percent * maximum) / 100.0 : null;
                        String feedback = status.equals("MISSING") ? "Submission is outstanding. Follow up with the student."
                                : status.equals("SUBMITTED") ? "Submitted; teacher feedback is pending."
                                : percent < 60 ? "Review core concepts and practise worked examples before the next assessment."
                                : percent < 80 ? "Good progress. Explain the reasoning more clearly and check edge cases."
                                : "Strong understanding. Extend solutions with examples and justify design choices.";
                        results.add(new Result(student.id(), id, status, score, feedback));
                        if (!status.equals("MISSING")) { answered++; }
                    }
                    if (type.equals("QUIZ")) {
                        questions.add(new Question(id, "Q1 · Core concepts", "EASY", answered, (int) Math.round(answered * .88)));
                        questions.add(new Question(id, "Q2 · Application", "MEDIUM", answered, (int) Math.round(answered * .67)));
                        questions.add(new Question(id, "Q3 · Reasoning", "HARD", answered, (int) Math.round(answered * .42)));
                    }
                }
            }
        }
        return new Snapshot(classes, subjects, students, assessments, results, questions, true);
    }
}
