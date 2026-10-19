package com.openstudy.openstudy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AssignmentPageController {
    @GetMapping("/assignments/")
    public String assignments() {
        return "assignments";
    }
}
