package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.model.User;
import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentDashboardController {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter DASHBOARD_DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final JdbcTemplate db;

    public StudentDashboardController(JdbcTemplate db) {
        this.db = db;
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
        int pendingAssignments = db.queryForObject("SELECT COUNT(*) FROM assignments a "
                + "WHERE a.status='PUBLISHED' AND a.due_at BETWEEN ? AND ? "
                + "AND NOT EXISTS (SELECT 1 FROM assignment_submissions s WHERE s.assignment_id=a.assignment_id "
                + "AND s.student_user_id=? AND s.status IN ('SUBMITTED','GRADED'))",
                Integer.class, now, upcomingEnd, studentId);
        model.addAttribute("stats", List.of(
                Map.of("label", "Available Quizzes", "value", "3", "accent", "blue"),
                Map.of("label", "Pending Assignments", "value", pendingAssignments, "accent", "purple"),
                Map.of("label", "Completed Tasks", "value", "5", "accent", "green"),
                Map.of("label", "Average Score", "value", "76.4%", "accent", "orange")));
        model.addAttribute("recentQuizzes", List.of(
                Map.of("title", "Java Basics Quiz", "meta", "20 Questions", "time", "Due: 20 Oct 2026", "status", "Ongoing"),
                Map.of("title", "DBMS Concepts Quiz", "meta", "15 Questions", "time", "Due: 25 Oct 2026", "status", "Pending"),
                Map.of("title", "R Programming Quiz", "meta", "20 Questions", "time", "Due: 20 Oct 2026", "status", "Ongoing"),
                Map.of("title", "Operating Systems Quiz", "meta", "25 Questions", "time", "Due: 05 Nov 2026", "status", "Upcoming")));
        List<Map<String, Object>> recentAssignments = db.queryForList("SELECT a.assignment_id,a.title,a.due_at, "
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

    @GetMapping("/student/quizzes")
    public String quizzes(Model model, HttpSession session) {
        addCurrentUser(model, session);
        model.addAttribute("activePage", "quizzes");
        model.addAttribute("quizzes", List.of(
                Map.of("title", "Java Basics Quiz", "questions", "20 Questions", "marks", "30 marks", "due", "Due: 20 Oct 2026", "difficulty", "Easy", "status", "Ongoing"),
                Map.of("title", "DBMS Concepts Quiz", "questions", "15 Questions", "marks", "15 marks", "due", "Due: 25 Oct 2026", "difficulty", "Medium", "status", "Upcoming"),
                Map.of("title", "R Programming Quiz", "questions", "20 Questions", "marks", "30 marks", "due", "Due: 20 Oct 2026", "difficulty", "Medium", "status", "Ongoing"),
                Map.of("title", "Operating Systems Quiz", "questions", "25 Questions", "marks", "25 marks", "due", "Due: 05 Nov 2026", "difficulty", "Hard", "status", "Upcoming")));
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
        model.addAttribute("results", List.of(
                Map.of("title", "Java Basics Quiz", "type", "Quiz", "score", "80/100", "status", "Passed", "date", "15 Oct 2026"),
                Map.of("title", "DBMS Concepts Quiz", "type", "Quiz", "score", "50/100", "status", "Pending", "date", "15 Oct 2026"),
                Map.of("title", "Java Collections Assignment", "type", "Assignment", "score", "92/100", "status", "Passed", "date", "10 Oct 2026"),
                Map.of("title", "R Programming Quiz", "type", "Quiz", "score", "72/100", "status", "Passed", "date", "08 Oct 2026")));
        return "student/results";
    }

    @GetMapping("/student/performance")
    public String performance(Model model, HttpSession session) {
        addCurrentUser(model, session);
        model.addAttribute("activePage", "performance");
        return "student/performance";
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
