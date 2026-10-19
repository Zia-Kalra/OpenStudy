package com.openstudy.openstudy.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.openstudy.openstudy.model.User;
import com.openstudy.openstudy.service.AuthService;

import jakarta.servlet.http.HttpSession;

@Controller
public class AuthController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    @Value("${openstudy.auth.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping({"/", "/student", "/student/login", "/login"})
    public String loginPage(@RequestParam(value = "error", required = false) String error, Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", switch (error) {
                case "invalid" -> "Invalid email, password, or role.";
                case "unauthenticated" -> "Please log in to continue.";
                case "loggedOut" -> "You have been logged out.";
                default -> "Authentication failed.";
            });
        }
        return "student/login";
    }

    @GetMapping("/signup")
    public String signupPage() {
        return "student/signup";
    }

    @PostMapping("/signup")
    public String signup(
            @RequestParam String fullName,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        String name = fullName.trim();
        String normalizedEmail = email.trim();
        if (name.isEmpty() || name.length() > 150 || normalizedEmail.length() > 254
                || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || password.length() < 12 || password.length() > 72) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Enter a valid name and email. Passwords must be 12 to 72 characters.");
            return "redirect:/signup";
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "The passwords do not match.");
            return "redirect:/signup";
        }
        try {
            User user = authService.registerStudent(name, normalizedEmail, password);
            session.setAttribute("loggedInUser", user);
            session.setAttribute("userRole", user.getRole());
            redirectAttributes.addFlashAttribute("newAccountId", user.getUserId());
            return "redirect:/student/dashboard";
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", exception.getMessage());
            return "redirect:/signup";
        } catch (UnsupportedOperationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Sign-up requires the database-backed profile. Start the app without the mock profile.");
            return "redirect:/signup";
        } catch (RuntimeException exception) {
            LOGGER.error("Student sign-up failed.", exception);
            redirectAttributes.addFlashAttribute("errorMessage", "Sign-up is unavailable. Check the database connection and try again.");
            return "redirect:/signup";
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "student/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String requestPasswordReset(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            authService.sendPasswordReset(email.trim(), publicBaseUrl + "/reset-password");
        } catch (UnsupportedOperationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password reset requires the database-backed profile. Start the app without the mock profile.");
            return "redirect:/forgot-password";
        } catch (RuntimeException exception) {
            LOGGER.error("Password reset email could not be sent.", exception);
        }
        redirectAttributes.addFlashAttribute("statusMessage",
                "If an active account uses that email and email delivery is configured, reset instructions will be sent.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("resetToken", token == null ? "" : token);
        model.addAttribute("validResetToken", authService.isResetTokenValid(token));
        return "student/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(
            @RequestParam String token,
            @RequestParam String password,
            @RequestParam String confirmPassword,
            RedirectAttributes redirectAttributes) {
        if (password.length() < 12 || password.length() > 72) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password must be 12 to 72 characters.");
            return "redirect:/reset-password?token=" + token;
        }
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("errorMessage", "The passwords do not match.");
            return "redirect:/reset-password?token=" + token;
        }
        try {
            if (authService.resetPassword(token, password)) {
                redirectAttributes.addFlashAttribute("statusMessage", "Password updated. You can now sign in.");
                return "redirect:/login";
            }
        } catch (UnsupportedOperationException exception) {
            redirectAttributes.addFlashAttribute("errorMessage", "Password reset requires the database-backed profile.");
            return "redirect:/login";
        }
        redirectAttributes.addFlashAttribute("errorMessage", "This reset link is invalid or has expired.");
        return "redirect:/reset-password";
    }

    @PostMapping("/login")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String role,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User user = authService.authenticate(email, password, role);
        if (user == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Invalid email, password, or role.");
            return "redirect:/login?error=invalid";
        }

        session.setAttribute("loggedInUser", user);
        session.setAttribute("userRole", user.getRole());

        if ("teacher".equalsIgnoreCase(user.getRole())) {
            return "redirect:/teacher/dashboard";
        }
        return "redirect:/student/dashboard";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login?error=loggedOut";
    }
}
