package com.openstudy.openstudy.service;

import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsDTO;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsDTO.*;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsData.*;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsQuery;
import com.openstudy.openstudy.dto.analytics.TeacherAnalyticsScope;
import com.openstudy.openstudy.provider.analytics.TeacherAnalyticsDataProvider;
import com.openstudy.openstudy.provider.analytics.TeacherRAnalyticsProvider;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TeacherAnalyticsService {
    private final TeacherAnalyticsDataProvider dataProvider;
    private final TeacherRAnalyticsProvider rProvider;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    public TeacherAnalyticsService(TeacherAnalyticsDataProvider dataProvider, TeacherRAnalyticsProvider rProvider) {
        this.dataProvider = dataProvider;
        this.rProvider = rProvider;
    }

    public TeacherAnalyticsDTO getAnalytics(TeacherAnalyticsScope scope, TeacherAnalyticsQuery query,
            String studentId, String reportId) {
        if (scope == null || query == null) { throw new ResponseStatusException(HttpStatus.FORBIDDEN); }
        if ((!"all".equals(query.classId()) && !scope.classIds().contains(query.classId()))
                || (!"all".equals(query.subjectId()) && !scope.subjectIds().contains(query.subjectId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Class or subject is outside your assigned scope.");
        }
        Snapshot source = dataProvider.load(scope);
        if (source.demo() != scope.teacher().isDemoIdentity()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "The analytics data adapter does not match the teacher access mode.");
        }
        // Defense in depth: even a faulty adapter cannot expand the trusted scope.
        var classes = source.classes().stream().filter(c -> scope.classIds().contains(c.id())).toList();
        var subjects = source.subjects().stream().filter(s -> scope.subjectIds().contains(s.id())).toList();
        Set<String> classIds = classes.stream().map(Cohort::id)
                .filter(id -> "all".equals(query.classId()) || id.equals(query.classId())).collect(Collectors.toSet());
        if (!source.demo() && classes.isEmpty() && scope.classIds().isEmpty()) {
            // An empty class ID means subject-only scope, never an invented class record.
            classIds.add("");
        }
        Set<String> subjectIds = subjects.stream().map(Subject::id)
                .filter(id -> "all".equals(query.subjectId()) || id.equals(query.subjectId())).collect(Collectors.toSet());
        var students = source.students().stream().filter(s -> classIds.contains(s.classId())
                && s.subjectIds().stream().anyMatch(subjectIds::contains))
                .map(s -> new Student(s.id(), s.rollNumber(), s.name(), s.classId(),
                        s.subjectIds().stream().filter(subjectIds::contains).collect(Collectors.toSet()))).toList();
        var assessments = source.assessments().stream().filter(a -> classIds.contains(a.classId()) && subjectIds.contains(a.subjectId())).toList();
        Map<String, Student> studentMap = students.stream().collect(Collectors.toMap(Student::id, s -> s));
        Map<String, Assessment> assessmentMap = assessments.stream().collect(Collectors.toMap(Assessment::id, a -> a));
        Map<Key, Result> results = source.results().stream().filter(r -> {
            var student = studentMap.get(r.studentId());
            var assessment = assessmentMap.get(r.assessmentId());
            return student != null && assessment != null && eligible(student, assessment);
        }).collect(Collectors.toMap(r -> new Key(r.studentId(), r.assessmentId()), r -> r));
        results.values().forEach(r -> {
            if (r.score() != null && r.score() > assessmentMap.get(r.assessmentId()).maximumScore()) {
                throw new IllegalStateException("Analytics adapter returned a score above the assessment maximum.");
            }
        });
        var context = new Context(assessments, assessmentMap, results, subjects, studentMap);
        var roster = students.stream().map(s -> row(s, context)).toList();
        String search = query.search().toLowerCase(Locale.ROOT);
        Comparator<StudentRow> comparator = switch (query.sort()) {
            case "NAME" -> Comparator.comparing(r -> r.student().name(), String.CASE_INSENSITIVE_ORDER);
            case "SUBMISSION" -> Comparator.<StudentRow>comparingDouble(r -> rate(r.submitted(), r.assigned())).reversed();
            default -> Comparator.comparing(StudentRow::average, Comparator.nullsLast(Comparator.reverseOrder()));
        };
        var filtered = roster.stream().filter(r -> (r.student().name().toLowerCase(Locale.ROOT).contains(search)
                || r.student().rollNumber().toLowerCase(Locale.ROOT).contains(search))
                && ("ALL".equals(query.band()) || query.band().equals(r.band())))
                .sorted(comparator.thenComparing(r -> r.student().id())).toList();
        StudentRow selected = studentId == null ? (filtered.isEmpty() ? null : filtered.getFirst())
                : filtered.stream().filter(r -> studentId.equals(r.student().id())).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Student not found in this scope."));
        var detail = selected == null ? null : detail(selected, context);
        var graded = results.values().stream().filter(r -> r.score() != null).map(r -> normalized(r, context)).toList();
        int expected = roster.stream().mapToInt(StudentRow::assigned).sum();
        int submitted = roster.stream().mapToInt(StudentRow::submitted).sum();
        var overview = new Overview(roster.size(), graded.size(), average(graded.stream()),
                graded.stream().max(Double::compareTo).orElse(null), graded.stream().min(Double::compareTo).orElse(null),
                average(scores(context, null, null, "QUIZ")), average(scores(context, null, null, "ASSIGNMENT")),
                expected == 0 ? null : rate(submitted, expected), trends(context, null), distribution(roster));
        var assessmentRows = assessments.stream().sorted(Comparator.comparing(Assessment::date).reversed().thenComparing(Assessment::id))
                .map(a -> assessmentRow(a, students, context)).toList();
        var comparison = subjectScores(context, null);
        var questions = source.questions().stream().filter(q -> assessmentMap.containsKey(q.assessmentId())
                && "QUIZ".equals(assessmentMap.get(q.assessmentId()).type()))
                .map(q -> new QuestionRow(assessmentMap.get(q.assessmentId()).title()
                        + (assessmentMap.get(q.assessmentId()).classId().isBlank() ? "" : " · " + assessmentMap.get(q.assessmentId()).classId()),
                        q.label(), q.difficulty(), q.answered(), q.correct(), rate(q.correct(), q.answered()))).toList();
        var reports = rProvider.savedReports(scope, query).stream().filter(r -> !r.classIds().isEmpty() && !r.subjectIds().isEmpty()
                && classIds.containsAll(r.classIds()) && subjectIds.containsAll(r.subjectIds())
                && r.demo() == scope.teacher().isDemoIdentity()
                && ("ALL".equals(query.method()) || query.method().equals(r.method()))).toList();
        var selectedReport = reportId == null ? (reports.isEmpty() ? null : reports.getFirst())
                : reports.stream().filter(r -> reportId.equals(r.id())).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found in this scope."));
        return new TeacherAnalyticsDTO(classes, subjects, query, overview, filtered, detail,
                assessmentRows, comparison, questions, reports, selectedReport, source.demo(), source.notices(), statistics(graded));
    }

    private StudentRow row(Student student, Context context) {
        var applicable = context.assessments().stream().filter(a -> eligible(student, a)).toList();
        int submitted = 0;
        int graded = 0;
        for (var assessment : applicable) {
            var result = context.results().get(new Key(student.id(), assessment.id()));
            if (result != null && !"MISSING".equals(result.status())) { submitted++; }
            if (result != null && result.score() != null) { graded++; }
        }
        Double average = average(scores(context, student.id(), null, null));
        String band = average == null ? "NOT_GRADED" : average < 50 ? "NEEDS_ATTENTION" : average < 65 ? "DEVELOPING" : average < 80 ? "ON_TRACK" : "STRONG";
        String label = switch (band) {
            case "NOT_GRADED" -> "Not graded";
            case "NEEDS_ATTENTION" -> "Needs attention";
            case "DEVELOPING" -> "Developing";
            case "ON_TRACK" -> "On track";
            default -> "Strong";
        };
        return new StudentRow(student, average, average(scores(context, student.id(), null, "QUIZ")),
                average(scores(context, student.id(), null, "ASSIGNMENT")), applicable.size(), submitted, graded,
                applicable.size() - submitted, band, label);
    }

    private StudentDetail detail(StudentRow row, Context context) {
        var history = context.assessments().stream().filter(a -> eligible(row.student(), a))
                .sorted(Comparator.comparing(Assessment::date).reversed())
                .map(a -> {
                    var result = context.results().get(new Key(row.student().id(), a.id()));
                    return new History(a.title(), subjectLabel(context, a.subjectId()), a.type(), a.date().format(DATE),
                            result == null ? "MISSING" : result.status(), result == null || result.score() == null ? null : normalized(result, context),
                            result == null ? "Submission is outstanding." : result.feedback());
                }).toList();
        var feedback = history.stream().filter(h -> "GRADED".equals(h.status()) && h.feedback() != null && !h.feedback().isBlank())
                .map(History::feedback).distinct().limit(3).toList();
        return new StudentDetail(row, subjectScores(context, row.student().id()), history, trends(context, row.student().id()), feedback);
    }

    private AssessmentRow assessmentRow(Assessment a, List<Student> students, Context context) {
        int expected = (int) students.stream().filter(s -> eligible(s, a)).count();
        var results = context.results().values().stream().filter(r -> r.assessmentId().equals(a.id())).toList();
        int submitted = (int) results.stream().filter(r -> !"MISSING".equals(r.status())).count();
        int graded = (int) results.stream().filter(r -> r.score() != null).count();
        return new AssessmentRow(a.id(), a.title(), a.classId(), subjectLabel(context, a.subjectId()), a.type(),
                a.date().format(DATE), expected, submitted, graded,
                average(results.stream().filter(r -> r.score() != null).map(r -> normalized(r, context))), rate(submitted, expected));
    }

    private List<SubjectScore> subjectScores(Context context, String studentId) {
        return context.subjects().stream().filter(s -> context.assessments().stream().anyMatch(a -> a.subjectId().equals(s.id())
                        && (studentId == null || eligible(context.students().get(studentId), a))))
                .map(s -> new SubjectScore(s.label(), average(scores(context, studentId, s.id(), "QUIZ")),
                        average(scores(context, studentId, s.id(), "ASSIGNMENT")), average(scores(context, studentId, s.id(), null)),
                        (int) scores(context, studentId, s.id(), null).count())).toList();
    }

    private List<Trend> trends(Context context, String studentId) {
        return context.assessments().stream().filter(a -> studentId == null || eligible(context.students().get(studentId), a))
                .map(a -> a.date().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)))
                .distinct().sorted().map(week -> new Trend(week.format(DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)),
                        average(weeklyScores(context, studentId, "QUIZ", week)), average(weeklyScores(context, studentId, "ASSIGNMENT", week)))).toList();
    }

    private Stream<Double> weeklyScores(Context context, String studentId, String type, LocalDate week) {
        return context.results().values().stream().filter(r -> r.score() != null && (studentId == null || studentId.equals(r.studentId())))
                .filter(r -> {
                    var a = context.assessmentMap().get(r.assessmentId());
                    return a.type().equals(type) && !a.date().isBefore(week) && a.date().isBefore(week.plusWeeks(1));
                }).map(r -> normalized(r, context));
    }

    private List<Band> distribution(List<StudentRow> roster) {
        String[] keys = {"NEEDS_ATTENTION", "DEVELOPING", "ON_TRACK", "STRONG", "NOT_GRADED"};
        String[] labels = {"Below 50%", "50–64%", "65–79%", "80–100%", "Not graded"};
        return java.util.stream.IntStream.range(0, keys.length).mapToObj(i -> {
            int count = (int) roster.stream().filter(r -> keys[i].equals(r.band())).count();
            return new Band(labels[i], count, rate(count, roster.size()));
        }).toList();
    }

    private Stream<Double> scores(Context context, String studentId, String subjectId, String type) {
        return context.results().values().stream().filter(r -> r.score() != null && (studentId == null || studentId.equals(r.studentId())))
                .filter(r -> {
                    var a = context.assessmentMap().get(r.assessmentId());
                    return (subjectId == null || subjectId.equals(a.subjectId())) && (type == null || type.equals(a.type()));
                }).map(r -> normalized(r, context));
    }

    private Double average(Stream<Double> scores) {
        var average = scores.mapToDouble(Double::doubleValue).average();
        return average.isPresent() ? average.getAsDouble() : null;
    }
    private static boolean eligible(Student student, Assessment assessment) {
        return student != null && student.classId().equals(assessment.classId())
                && student.subjectIds().contains(assessment.subjectId())
                && (assessment.eligibleStudentIds() == null || assessment.eligibleStudentIds().contains(student.id()));
    }
    private DescriptiveStatistics statistics(List<Double> values) {
        var sorted = values.stream().sorted().toList();
        if (sorted.isEmpty()) { return new DescriptiveStatistics(0, null, null, null, null, null); }
        double mean = average(sorted.stream());
        Double deviation = sorted.size() < 2 ? null : Math.sqrt(sorted.stream()
                .mapToDouble(value -> (value - mean) * (value - mean)).sum() / (sorted.size() - 1));
        return new DescriptiveStatistics(sorted.size(), mean, quantile(sorted, 0.5), deviation,
                quantile(sorted, 0.25), quantile(sorted, 0.75));
    }
    private double quantile(List<Double> sorted, double fraction) {
        double position = (sorted.size() - 1) * fraction;
        int lower = (int) Math.floor(position);
        int upper = Math.min(lower + 1, sorted.size() - 1);
        return sorted.get(lower) + (sorted.get(upper) - sorted.get(lower)) * (position - lower);
    }
    private double normalized(Result r, Context c) { return r.score() / c.assessmentMap().get(r.assessmentId()).maximumScore() * 100; }
    private double rate(int count, int total) { return total == 0 ? 0 : count * 100.0 / total; }
    private String subjectLabel(Context c, String id) { return c.subjects().stream().filter(s -> s.id().equals(id)).map(Subject::label).findFirst().orElse(id); }
    private record Key(String studentId, String assessmentId) { }
    private record Context(List<Assessment> assessments, Map<String, Assessment> assessmentMap, Map<Key, Result> results,
            List<Subject> subjects, Map<String, Student> students) { }
}
