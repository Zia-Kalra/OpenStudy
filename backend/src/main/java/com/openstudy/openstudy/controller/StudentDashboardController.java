package com.openstudy.openstudy.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.openstudy.openstudy.model.User;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentDashboardController {

    private static final Logger log = LoggerFactory.getLogger(StudentDashboardController.class);
    
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DASHBOARD_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final JdbcTemplate db;
    private static final DateTimeFormatter RESULT_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public StudentDashboardController(ObjectProvider<JdbcTemplate> database) {
        this.db = database.getIfAvailable();
    }

    @GetMapping("/student/dashboard")
    public String dashboard(Model model, HttpSession session) {
        addCurrentUser(model, session);
        model.addAttribute("activePage", "dashboard");
        User user = (User) session.getAttribute("loggedInUser");
        long studentId = user == null || user.getUserId() == null ? -1L : user.getUserId();
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        LocalDateTime upcomingEnd = now.plusMonths(1);
        LocalDateTime attemptedStart = now.minusMonths(1);
        Integer pendingAssignments = 0;
        if (db != null) {
            Integer count = db.queryForObject(
                "SELECT COUNT(*) FROM assignments a "
                    + "WHERE a.status='PUBLISHED' AND a.due_at BETWEEN ? AND ? "
                    + "AND NOT EXISTS (SELECT 1 FROM assignment_submissions s "
                    + "WHERE s.assignment_id=a.assignment_id "
                    + "AND s.student_user_id=? AND s.status IN ('SUBMITTED','GRADED'))",
                Integer.class, now, upcomingEnd, studentId
            );
            
            pendingAssignments = count == null ? 0 : count;
        }
        
        int availableQuizzes = 0;
        int completedTasks = 0;
        String averageScore = "—";
        List<Map<String, Object>> recentQuizzes = List.of();
        if (db != null) {
            availableQuizzes = countOrZero(
                "SELECT COUNT(*) FROM quizzes q WHERE q.status='PUBLISHED' "
                    + "AND (q.available_from IS NULL OR q.available_from<=?) "
                    + "AND (q.available_until IS NULL OR q.available_until>=?) "
                    + "AND NOT EXISTS (SELECT 1 FROM quiz_attempts a WHERE a.quiz_id=q.quiz_id "
                    + "AND a.student_user_id=? AND a.status IN ('SUBMITTED','GRADED'))",
                now, now, studentId);
            completedTasks = countOrZero(
                "SELECT (SELECT COUNT(*) FROM assignment_submissions WHERE student_user_id=? AND status IN ('SUBMITTED','GRADED')) "
                    + "+ (SELECT COUNT(*) FROM quiz_attempts WHERE student_user_id=? AND status IN ('SUBMITTED','GRADED'))",
                studentId, studentId);
            Double average = db.queryForObject(
                "SELECT AVG(pct) FROM ("
                    + "SELECT s.score/a.max_score*100 pct FROM assignment_submissions s "
                    + "JOIN assignments a ON a.assignment_id=s.assignment_id "
                    + "WHERE s.student_user_id=? AND s.status='GRADED' AND s.score IS NOT NULL AND a.max_score>0 "
                    + "UNION ALL "
                    + "SELECT t.score/tot.points*100 FROM quiz_attempts t "
                    + "JOIN (SELECT quiz_id, SUM(points) points FROM quiz_questions GROUP BY quiz_id) tot ON tot.quiz_id=t.quiz_id "
                    + "WHERE t.student_user_id=? AND t.status IN ('SUBMITTED','GRADED') AND t.score IS NOT NULL AND tot.points>0"
                    + ") scores",
                Double.class, studentId, studentId);
            if (average != null) averageScore = String.format(java.util.Locale.ROOT, "%.1f%%", average);
            recentQuizzes = db.queryForList(
                "SELECT q.title, q.available_until, "
                    + "(SELECT COUNT(*) FROM quiz_questions qq WHERE qq.quiz_id=q.quiz_id) question_count, "
                    + "CASE WHEN EXISTS (SELECT 1 FROM quiz_attempts a WHERE a.quiz_id=q.quiz_id AND a.student_user_id=? "
                    + "AND a.status IN ('SUBMITTED','GRADED')) THEN 'Completed' "
                    + "WHEN EXISTS (SELECT 1 FROM quiz_attempts a WHERE a.quiz_id=q.quiz_id AND a.student_user_id=? "
                    + "AND a.status='IN_PROGRESS') THEN 'In Progress' ELSE 'Pending' END quiz_status "
                    + "FROM quizzes q WHERE q.status='PUBLISHED' "
                    + "AND (q.available_from IS NULL OR q.available_from<=?) "
                    + "AND (q.available_until IS NULL OR q.available_until>=?) "
                    + "ORDER BY q.created_at DESC LIMIT 4",
                studentId, studentId, now, now);
            recentQuizzes.forEach(quiz -> {
                quiz.put("meta", quiz.get("question_count") + " Questions");
                Object until = quiz.get("available_until");
                quiz.put("time", until instanceof java.sql.Timestamp ts
                        ? "Due: " + ts.toLocalDateTime().format(DASHBOARD_DATE) : "No deadline");
                quiz.put("status", quiz.get("quiz_status"));
            });
        }
        model.addAttribute("stats", List.of(
                Map.of("label", "Available Quizzes", "value", availableQuizzes, "accent", "purple", "icon", "quiz", "href", "/student/quizzes"),
                Map.of("label", "Pending Assignments", "value", pendingAssignments, "accent", "peach", "icon", "assignment", "href", "/student/assignments"),
                Map.of("label", "Completed Tasks", "value", completedTasks, "accent", "mint", "icon", "check-circle", "href", "/student/results"),
                Map.of("label", "Average Score", "value", averageScore, "accent", "lavender", "icon", "star", "href", "/student/performance")));
        model.addAttribute("recentQuizzes", recentQuizzes);
        List<Map<String, Object>> recentAssignments = db == null ? List.of() : db.queryForList("SELECT a.assignment_id,a.title,a.due_at, "
                + "(SELECT COUNT(*) FROM assignment_questions q WHERE q.assignment_id=a.assignment_id) question_count, "
                + "CASE WHEN recent_submission.submitted_at IS NOT NULL THEN 'Attempted' ELSE 'Pending' END assignment_status, "
                + "recent_submission.submitted_at "
                + "FROM assignments a LEFT JOIN (SELECT assignment_id,MAX(submitted_at) submitted_at "
                + "FROM assignment_submissions WHERE student_user_id=? AND status IN ('SUBMITTED','GRADED') "
                + "AND submitted_at BETWEEN ? AND ? GROUP BY assignment_id) recent_submission "
                + "ON recent_submission.assignment_id=a.assignment_id "
                + "WHERE a.status='PUBLISHED' AND ((recent_submission.submitted_at IS NOT NULL) "
                + "OR (a.due_at BETWEEN ? AND ? AND NOT EXISTS (SELECT 1 FROM assignment_submissions s "
                + "WHERE s.assignment_id=a.assignment_id AND s.student_user_id=? AND s.status IN ('SUBMITTED','GRADED'))) ) "
                + "ORDER BY CASE WHEN recent_submission.submitted_at IS NOT NULL THEN 0 ELSE 1 END, "
                + "COALESCE(recent_submission.submitted_at,a.due_at) DESC LIMIT 10",
                studentId, attemptedStart, now, now, upcomingEnd, studentId);
        recentAssignments.forEach(assignment -> {
            assignment.put("meta", assignment.get("question_count") + " Questions");
            boolean attempted = "Attempted".equals(assignment.get("assignment_status"));
            Object dateValue = attempted ? assignment.get("submitted_at") : assignment.get("due_at");
            LocalDateTime displayDate = dateValue instanceof java.sql.Timestamp timestamp
                    ? timestamp.toLocalDateTime() : (LocalDateTime) dateValue;
            assignment.put("time", (attempted ? "Submitted: " : "Due: ") + displayDate.format(DASHBOARD_DATE));
            assignment.put("status", attempted ? "Attempted" : "Pending");
        });
        model.addAttribute("recentAssignments", recentAssignments);
        return "student/dashboard";
    }

    private int countOrZero(String sql, Object... args) {
        Integer value = db.queryForObject(sql, Integer.class, args);
        return value == null ? 0 : value;
    }

    @GetMapping("/student/quizzes")
    public String quizzes(Model model, HttpSession session) {
        addCurrentUser(model, session);
        model.addAttribute("activePage", "quizzes");
        return "student/quizzes";
    }

    @GetMapping("/student/assignments")
    public String assignments(Model model, HttpSession session) {
        addCurrentUser(model, session);
        model.addAttribute("assignmentUiRole", "STUDENT");
        model.addAttribute("activePage", "assignments");
        return "student/assignments";
    }

    @GetMapping("/student/results")
    public String results(Model model, HttpSession session) {
        addCurrentUser(model, session);
        model.addAttribute("activePage", "results");
        User user = (User) session.getAttribute("loggedInUser");
        long studentId = user == null || user.getUserId() == null ? -1L : user.getUserId();
        List<Map<String, Object>> resultRows = db == null ? List.of() : db.queryForList("SELECT a.title,a.max_score,s.score,s.status,s.evaluation_status,s.graded_at,s.updated_at, "
                + "(SELECT SUM(COALESCE(aa.final_score,aa.ai_score,0)) FROM assignment_answers aa WHERE aa.submission_id=s.submission_id) evaluated_score, "
                + "(SELECT MAX(aa.evaluated_at) FROM assignment_answers aa WHERE aa.submission_id=s.submission_id) answer_evaluated_at "
                + "FROM assignment_submissions s JOIN assignments a ON a.assignment_id=s.assignment_id "
                + "WHERE s.student_user_id=? AND s.status IN ('SUBMITTED','GRADED') "
                + "ORDER BY COALESCE(s.graded_at,s.submitted_at) DESC", studentId);
        List<Map<String, Object>> results = resultRows.stream().map(row -> {
            java.math.BigDecimal maxScore = (java.math.BigDecimal) row.get("max_score");
            java.math.BigDecimal score = (java.math.BigDecimal) row.get("score");
            java.math.BigDecimal evaluatedScore = (java.math.BigDecimal) row.get("evaluated_score");
            boolean teacherEvaluated = "GRADED".equalsIgnoreCase(String.valueOf(row.get("status")));
            boolean aiEvaluated = "EVALUATED".equalsIgnoreCase(String.valueOf(row.get("evaluation_status")));
            boolean evaluated = (teacherEvaluated || aiEvaluated) && (score != null || evaluatedScore != null);
            String status = "Pending result";
            String marks = "Pending result";
            String date = "—";
            if (evaluated) {
                java.math.BigDecimal finalScore = teacherEvaluated && score != null ? score : evaluatedScore != null ? evaluatedScore : score;
                marks = formatMarks(finalScore) + "/" + formatMarks(maxScore);
                status = finalScore.multiply(java.math.BigDecimal.valueOf(100))
                        .compareTo(maxScore.multiply(java.math.BigDecimal.valueOf(40))) > 0 ? "Passed" : "Failed";
                Object evaluatedAt = teacherEvaluated ? row.get("graded_at") : row.get("answer_evaluated_at");
                if (evaluatedAt == null) evaluatedAt = row.get("updated_at");
                if (evaluatedAt instanceof java.sql.Timestamp timestamp)
                    date = timestamp.toLocalDateTime().format(RESULT_DATE);
                else if (evaluatedAt instanceof java.time.LocalDateTime dateTime)
                    date = dateTime.format(RESULT_DATE);
            }
            return Map.<String, Object>of("title", String.valueOf(row.get("title")), "type", "Assignment",
                    "score", marks, "status", status, "date", date);
        }).toList();
        // Quiz results can be merged into this shared row format by the quiz module: title, type, score, status, date.
        model.addAttribute("results", results);
        return "student/results";
    }

    private String formatMarks(java.math.BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    /** The static demo performance page was replaced by the data-backed analytics page. */
    @GetMapping("/student/performance")
    public String performance() {
        return "redirect:/student/analytics";
    }

    @GetMapping("/student/profile")
    public String profile(Model model, HttpSession session) {
        addCurrentUser(model, session);
        model.addAttribute("activePage", "profile");
        return "student/profile";
    }

    @PostMapping("/student/profile")
    public String updateProfile(
            @RequestParam(required = false) String studentId,
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String course,
            @RequestParam(required = false) String semester,
            @RequestParam(required = false) String institution,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String year,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String normalizedName = clean(fullName);
        String normalizedEmail = clean(email);
        String normalizedStudentId = clean(studentId);
        if (normalizedName.isEmpty() || normalizedName.length() > 150
                || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || normalizedEmail.length() > 254
                || normalizedStudentId.length() > 50) {
            redirectAttributes.addFlashAttribute("profileError", "Enter a valid name, email, and student ID (up to 50 characters).");
            return "redirect:/student/profile";
        }

        User user = (User) session.getAttribute("loggedInUser");
        user.setFullName(normalizedName);
        user.setEmail(normalizedEmail);
        user.setStudentId(normalizedStudentId);
        user.setPhoneNumber(clean(phoneNumber));
        user.setCourse(clean(course));
        user.setSemester(clean(semester));
        user.setInstitution(clean(institution));
        user.setDepartment(clean(department));
        user.setYear(clean(year));
        session.setAttribute("loggedInUser", user);
        redirectAttributes.addFlashAttribute("profileMessage", "Profile updated for this session.");
        return "redirect:/student/profile";
    }

    private void addCurrentUser(Model model, HttpSession session) {
        User user = session == null ? null : (User) session.getAttribute("loggedInUser");
        String displayName = user != null && user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : "Student";
        String email = user != null && user.getEmail() != null ? user.getEmail() : "student@example.com";
        String initial = displayName.isEmpty() ? "S" : displayName.substring(0, 1).toUpperCase();

        model.addAttribute("currentUser", user);
        model.addAttribute("currentUserName", displayName);
        model.addAttribute("currentUserEmail", email);
        model.addAttribute("currentUserInitial", initial);
        model.addAttribute("studentId", user == null ? null : user.getStudentId());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
