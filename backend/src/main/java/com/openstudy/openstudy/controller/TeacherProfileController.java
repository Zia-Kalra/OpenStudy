package com.openstudy.openstudy.controller;

import com.openstudy.openstudy.model.User;
import jakarta.servlet.http.HttpSession;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Teacher account page. Name and email are saved to the users table; the page is guarded by WebMvcConfig. */
@Controller
public class TeacherProfileController {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final JdbcTemplate db;

    public TeacherProfileController(ObjectProvider<JdbcTemplate> database) {
        this.db = database.getIfAvailable();
    }

    @GetMapping("/teacher/profile")
    public String profile(Model model, HttpSession session) {
        User user = (User) session.getAttribute("loggedInUser");
        model.addAttribute("activePage", "profile");
        model.addAttribute("profileUser", user);
        model.addAttribute("memberSince", memberSince(user));
        model.addAttribute("subjects", subjects(user));
        return "teacher/profile";
    }

    @PostMapping("/teacher/profile")
    public String update(@RequestParam String fullName, @RequestParam String email,
                         HttpSession session, RedirectAttributes redirect) {
        User user = (User) session.getAttribute("loggedInUser");
        String name = fullName == null ? "" : fullName.strip();
        String mail = email == null ? "" : email.strip();
        if (user == null || name.isEmpty() || name.length() > 150
                || !mail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || mail.length() > 254) {
            redirect.addFlashAttribute("profileError", "Enter a valid name and email address.");
            return "redirect:/teacher/profile";
        }
        if (db != null && user.getUserId() != null) {
            try {
                db.update("UPDATE users SET full_name=?, email=? WHERE user_id=?", name, mail, user.getUserId());
            } catch (DuplicateKeyException duplicate) {
                redirect.addFlashAttribute("profileError", "That email address is already used by another account.");
                return "redirect:/teacher/profile";
            } catch (DataAccessException failure) {
                redirect.addFlashAttribute("profileError", "Your profile could not be saved right now. Please try again.");
                return "redirect:/teacher/profile";
            }
        }
        user.setFullName(name);
        user.setEmail(mail);
        session.setAttribute("loggedInUser", user);
        redirect.addFlashAttribute("profileMessage", "Profile updated.");
        return "redirect:/teacher/profile";
    }

    private String memberSince(User user) {
        if (db == null || user == null || user.getUserId() == null) return "";
        try {
            List<Map<String, Object>> rows = db.queryForList("SELECT created_at FROM users WHERE user_id=?", user.getUserId());
            return !rows.isEmpty() && rows.getFirst().get("created_at") instanceof Timestamp ts
                    ? ts.toLocalDateTime().format(DATE) : "";
        } catch (DataAccessException ignored) {
            return "";
        }
    }

    private List<String> subjects(User user) {
        if (db == null || user == null || user.getUserId() == null) return List.of();
        try {
            return db.queryForList("SELECT DISTINCT s.subject_name FROM subjects s WHERE s.subject_id IN ("
                    + "SELECT subject_id FROM subject_teachers WHERE teacher_id=? "
                    + "UNION SELECT subject_id FROM quizzes WHERE created_by=? "
                    + "UNION SELECT subject_id FROM assignments WHERE created_by=?) ORDER BY s.subject_name",
                    String.class, user.getUserId(), user.getUserId(), user.getUserId());
        } catch (DataAccessException ignored) {
            return List.of();
        }
    }
}
