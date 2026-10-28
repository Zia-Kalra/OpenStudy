package com.openstudy.openstudy.controller;

import static com.openstudy.openstudy.dto.analytics.AnalyticsResponse.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openstudy.openstudy.dto.analytics.AnalyticsFilter;
import com.openstudy.openstudy.dto.analytics.ApiResult;
import com.openstudy.openstudy.service.analytics.AnalyticsService;
import com.openstudy.openstudy.service.analytics.AnalyticsUserContext;
import com.openstudy.openstudy.service.analytics.AnalyticsUserContext.Role;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/analytics")
@Profile("!mock")
public class AnalyticsApiController {

    private final AnalyticsService service;
    private final AnalyticsUserContext users;

    public AnalyticsApiController(AnalyticsService service, AnalyticsUserContext users) {
        this.service = service;
        this.users = users;
    }

    // ---------------------------------------------------------------- student

    @GetMapping("/student/overview")
    public ApiResult<StudentOverview> studentOverview(HttpServletRequest req,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        long id = users.requireUserId(req, Role.STUDENT);
        return ApiResult.ok(service.studentOverview(id, filter(subjectId, type, from, to)));
    }

    @GetMapping("/student/subjects")
    public ApiResult<List<SubjectStat>> studentSubjects(HttpServletRequest req,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        long id = users.requireUserId(req, Role.STUDENT);
        return ApiResult.ok(service.studentSubjects(id, filter(null, type, from, to)));
    }

    @GetMapping("/student/trends")
    public ApiResult<StudentTrends> studentTrends(HttpServletRequest req,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        long id = users.requireUserId(req, Role.STUDENT);
        return ApiResult.ok(service.studentTrends(id, filter(subjectId, type, from, to)));
    }

    @GetMapping("/student/assessments")
    public ApiResult<List<AssessmentRow>> studentAssessments(HttpServletRequest req,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        long id = users.requireUserId(req, Role.STUDENT);
        return ApiResult.ok(service.studentAssessments(id, filter(subjectId, type, from, to)));
    }

    @GetMapping("/student/insights")
    public ApiResult<InsightResult> studentInsights(HttpServletRequest req,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        long id = users.requireUserId(req, Role.STUDENT);
        return ApiResult.ok(service.studentInsights(id, filter(null, null, from, to)));
    }

    // ---------------------------------------------------------------- teacher

    @GetMapping("/teacher/filters")
    public ApiResult<Filters> teacherFilters(HttpServletRequest req) {
        long id = users.requireUserId(req, Role.TEACHER);
        return ApiResult.ok(service.teacherFilters(id));
    }

    @GetMapping("/teacher/class-overview")
    public ApiResult<TeacherClassOverview> classOverview(HttpServletRequest req,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        long id = users.requireUserId(req, Role.TEACHER);
        return ApiResult.ok(service.teacherClassOverview(id, filter(subjectId, type, from, to)));
    }

    @GetMapping("/teacher/subjects")
    public ApiResult<List<SubjectStat>> teacherSubjects(HttpServletRequest req,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        long id = users.requireUserId(req, Role.TEACHER);
        return ApiResult.ok(service.teacherSubjectStats(id, filter(subjectId, type, from, to)));
    }

    @GetMapping("/teacher/distribution")
    public ApiResult<Distribution> distribution(HttpServletRequest req,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        long id = users.requireUserId(req, Role.TEACHER);
        return ApiResult.ok(service.teacherDistribution(id, filter(subjectId, type, from, to)));
    }

    @GetMapping("/teacher/trends")
    public ApiResult<List<MonthStat>> teacherTrends(HttpServletRequest req,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        long id = users.requireUserId(req, Role.TEACHER);
        return ApiResult.ok(service.teacherTrends(id, filter(subjectId, type, from, to)));
    }

    @GetMapping("/teacher/question-performance")
    public ApiResult<List<QuestionStat>> questionPerformance(HttpServletRequest req,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String type) {
        long id = users.requireUserId(req, Role.TEACHER);
        return ApiResult.ok(service.teacherQuestions(id, filter(subjectId, type, null, null)));
    }

    @GetMapping("/teacher/insights")
    public ApiResult<InsightResult> teacherInsights(HttpServletRequest req,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        long id = users.requireUserId(req, Role.TEACHER);
        return ApiResult.ok(service.teacherInsights(id, filter(subjectId, type, from, to)));
    }

    @GetMapping("/teacher/statistical-report")
    public ApiResult<StatisticalReport> statisticalReport(HttpServletRequest req,
            @RequestParam(required = false) Long subjectId, @RequestParam(required = false) String type,
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        long id = users.requireUserId(req, Role.TEACHER);
        return ApiResult.ok(service.teacherStatisticalReport(id, filter(subjectId, type, from, to)));
    }

    // ---------------------------------------------------------------- helpers

    private static AnalyticsFilter filter(Long subjectId, String type, String from, String to) {
        return AnalyticsFilter.of(subjectId, type, date(from, "from"), date(to, "to"));
    }

    private static LocalDate date(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("'" + name + "' must be a date in yyyy-MM-dd format.");
        }
    }
}