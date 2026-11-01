package com.openstudy.openstudy.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("!mock")
public class DatabaseTestController {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseTestController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/api/db-test")
    public String testDatabase() {
        jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return jdbcTemplate.queryForObject(
            "SELECT DATABASE()",
            String.class
        );
    }
}
