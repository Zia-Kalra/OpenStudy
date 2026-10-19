package com.openstudy.openstudy.controller;

import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Member 5 integration placeholder only. Other members should replace the configured
 * module paths with their own routes rather than extending this controller into a
 * competing feature implementation.
 */
@Controller
public class TeacherPlaceholderController {

    private static final Map<String, ModuleMeta> MODULES = Map.of(
            "question-bank", new ModuleMeta("Question Bank", "Member 2", "dashboard"),
            "quizzes", new ModuleMeta("Quizzes", "Member 2", "dashboard"),
            "assignments", new ModuleMeta("Assignments", "Member 3", "dashboard"),
            "submissions", new ModuleMeta("Submissions", "Member 3", "dashboard"),
            "analytics", new ModuleMeta("Analytics", "Member 4", "dashboard"),
            "profile", new ModuleMeta("Profile", "Member 1", "dashboard"),
            "logout", new ModuleMeta("Logout", "Member 1", "dashboard"));

    @GetMapping("/teacher/coming-soon")
    public String comingSoon(@RequestParam(defaultValue = "module") String module, Model model) {
        if ("logout".equalsIgnoreCase(module)) {
            return "redirect:/logout";
        }

        String moduleKey = MODULES.containsKey(module) ? module : "module";
        ModuleMeta meta = MODULES.getOrDefault(
                moduleKey,
                new ModuleMeta("Teacher module", "the owning team member", "dashboard"));
        model.addAttribute("moduleKey", moduleKey);
        model.addAttribute("moduleName", meta.name());
        model.addAttribute("moduleOwner", meta.owner());
        model.addAttribute("activePage", moduleKey);
        return "teacher/coming-soon";
    }

    private record ModuleMeta(String name, String owner, String fallbackActive) {
    }
}
