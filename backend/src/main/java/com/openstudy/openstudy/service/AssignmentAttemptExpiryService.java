package com.openstudy.openstudy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Component
@Profile("!mock")
@ConditionalOnProperty(name = "openstudy.assignments.expiry.enabled", havingValue = "true", matchIfMissing = true)
public class AssignmentAttemptExpiryService {
    private static final Logger log = LoggerFactory.getLogger(AssignmentAttemptExpiryService.class);
    private final JdbcTemplate db;

    public AssignmentAttemptExpiryService(JdbcTemplate db) { this.db = db; }

    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void submitExpiredAttempts() {
        db.update("INSERT INTO assignment_answers(submission_id,question_id,answer_text) "
                + "SELECT s.submission_id,q.question_id,'' FROM assignment_submissions s "
                + "JOIN assignment_questions q ON q.assignment_id=s.assignment_id "
                + "LEFT JOIN assignment_answers aa ON aa.submission_id=s.submission_id AND aa.question_id=q.question_id "
                + "WHERE s.status='DRAFT' AND s.attempt_expires_at<=DATE_SUB(NOW(),INTERVAL 15 SECOND) AND aa.answer_id IS NULL");
        int submitted = db.update("UPDATE assignment_submissions SET status='SUBMITTED',submitted_at=NOW(),evaluation_status='PENDING' "
                + "WHERE status='DRAFT' AND attempt_expires_at<=DATE_SUB(NOW(),INTERVAL 15 SECOND)");
        if (submitted > 0) log.info("Automatically submitted {} expired assignment attempt(s).", submitted);
    }
}
