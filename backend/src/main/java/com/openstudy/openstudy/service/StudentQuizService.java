package com.openstudy.openstudy.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Profile("!mock")
public class StudentQuizService {

    private static final Logger log = LoggerFactory.getLogger(StudentQuizService.class);

    private final JdbcTemplate db;

    public StudentQuizService(JdbcTemplate db) {
        this.db = db;
    }

    public List<Map<String, Object>> availableQuizzes(long studentId) {
        List<Map<String, Object>> quizzes = db.queryForList(
                "SELECT q.quiz_id, q.title, q.instructions, s.subject_name, "
                + "q.available_from, q.available_until, q.time_limit_minutes, "
                + "(SELECT COUNT(*) FROM quiz_questions qq WHERE qq.quiz_id=q.quiz_id) question_count, "
                + "(SELECT COALESCE(SUM(qq.points),0) FROM quiz_questions qq WHERE qq.quiz_id=q.quiz_id) total_marks, "
                + "NOT EXISTS (SELECT 1 FROM quiz_questions qq "
                + "JOIN question_bank_questions b ON b.bank_question_id=qq.bank_question_id "
                + "WHERE qq.quiz_id=q.quiz_id AND b.question_type NOT IN ('SINGLE_CHOICE','MULTIPLE_CHOICE')) supported_for_attempt "
                + "FROM quizzes q JOIN subjects s ON s.subject_id=q.subject_id "
                + "WHERE q.status='PUBLISHED' "
                + "AND (q.available_from IS NULL OR q.available_from<=NOW()) "
                + "AND (q.available_until IS NULL OR q.available_until>=NOW()) "
                + "ORDER BY q.available_from, q.created_at DESC");
        try {
            List<Map<String, Object>> excludedPublished = db.queryForList(
                    "SELECT q.quiz_id, q.subject_id, q.status, q.available_from, q.available_until, "
                    + "q.created_by, s.subject_id matched_subject_id, NOW() database_now, "
                    + "@@session.time_zone session_time_zone, "
                    + "CASE WHEN s.subject_id IS NULL THEN 'SUBJECT_JOIN_MISSING' "
                    + "WHEN q.available_from>NOW() THEN 'NOT_OPEN' "
                    + "WHEN q.available_until<NOW() THEN 'EXPIRED' ELSE 'OTHER' END exclusion_reason "
                    + "FROM quizzes q LEFT JOIN subjects s ON s.subject_id=q.subject_id "
                    + "WHERE q.status='PUBLISHED' AND (s.subject_id IS NULL "
                    + "OR q.available_from>NOW() OR q.available_until<NOW()) "
                    + "ORDER BY q.quiz_id LIMIT 50");
            if (!excludedPublished.isEmpty()) {
                log.warn("Student quiz listing excluded published quiz rows={}", excludedPublished);
            }
        } catch (org.springframework.dao.DataAccessException diagnosticFailure) {
            log.warn("Could not collect published quiz exclusion diagnostics ({}).",
                    diagnosticFailure.getClass().getSimpleName());
        }
        Map<Long, Map<String, Object>> quizById = new LinkedHashMap<>();
        for (Map<String, Object> quiz : quizzes) {
            quiz.put("attempts", new ArrayList<Map<String, Object>>());
            quizById.put(number(quiz.get("quiz_id")).longValue(), quiz);
        }
        List<Map<String, Object>> attempts = db.queryForList(
                "SELECT q.quiz_id, q.title, q.instructions, s.subject_name, q.time_limit_minutes, "
                + "(SELECT COUNT(*) FROM quiz_questions qq WHERE qq.quiz_id=q.quiz_id) question_count, "
                + "(SELECT COALESCE(SUM(qq.points),0) FROM quiz_questions qq WHERE qq.quiz_id=q.quiz_id) total_marks, "
                + "a.attempt_id, a.attempt_number, a.status, a.started_at, a.submitted_at, a.score "
                + "FROM quiz_attempts a JOIN quizzes q ON q.quiz_id=a.quiz_id "
                + "JOIN subjects s ON s.subject_id=q.subject_id "
                + "WHERE a.student_user_id=? AND a.status IN ('IN_PROGRESS','SUBMITTED','GRADED') "
                + "ORDER BY q.quiz_id, a.attempt_number DESC", studentId);
        for (Map<String, Object> attempt : attempts) {
            long quizId = number(attempt.get("quiz_id")).longValue();
            Map<String, Object> quiz = quizById.computeIfAbsent(quizId, ignored -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("quiz_id", quizId);
                row.put("title", attempt.get("title"));
                row.put("instructions", attempt.get("instructions"));
                row.put("subject_name", attempt.get("subject_name"));
                row.put("time_limit_minutes", attempt.get("time_limit_minutes"));
                row.put("question_count", attempt.get("question_count"));
                row.put("total_marks", attempt.get("total_marks"));
                row.put("supported_for_attempt", true);
                row.put("attempts", new ArrayList<Map<String, Object>>());
                return row;
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> quizAttempts = (List<Map<String, Object>>) quiz.get("attempts");
            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("attemptId", attempt.get("attempt_id"));
            summary.put("attemptNumber", attempt.get("attempt_number"));
            summary.put("status", attempt.get("status"));
            summary.put("startedAt", attempt.get("started_at"));
            summary.put("submittedAt", attempt.get("submitted_at"));
            summary.put("score", attempt.get("score"));
            summary.put("totalMarks", attempt.get("total_marks"));
            quizAttempts.add(summary);
        }
        return new ArrayList<>(quizById.values());
    }

    @Transactional
    public Map<String, Object> startOrResume(long studentId, long quizId) {
        List<Map<String, Object>> quizRows = db.queryForList(
                "SELECT quiz_id, status, "
                + "(available_from IS NULL OR available_from<=NOW()) available_now, "
                + "(available_until IS NULL OR available_until>=NOW()) not_expired "
                + "FROM quizzes WHERE quiz_id=? FOR UPDATE", quizId);
        if (quizRows.isEmpty()) {
            throw notFound();
        }
        List<Map<String, Object>> inProgress = db.queryForList(
                "SELECT attempt_id, started_at FROM quiz_attempts "
                + "WHERE quiz_id=? AND student_user_id=? AND status='IN_PROGRESS' "
                + "ORDER BY attempt_number DESC LIMIT 1 FOR UPDATE",
                quizId, studentId);
        if (!inProgress.isEmpty()) {
            long attemptId = number(inProgress.get(0).get("attempt_id")).longValue();
            Map<String, Object> timing = timing(attemptId, studentId);
            if (truthy(timing.get("expired"))) {
                finalizeAttempt(attemptId, studentId);
                return attemptStart(attemptId, false, true);
            }
            return attemptStart(attemptId, true, false);
        }
        Map<String, Object> quiz = quizRows.getFirst();
        if (!"PUBLISHED".equals(quiz.get("status")) || !truthy(quiz.get("available_now"))
                || !truthy(quiz.get("not_expired"))) {
            throw notFound();
        }
        Integer questionCount = db.queryForObject(
                "SELECT COUNT(*) FROM quiz_questions WHERE quiz_id=?", Integer.class, quizId);
        if (questionCount == null || questionCount == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This quiz has no questions yet.");
        }
        Integer unsupportedCount = db.queryForObject(
                "SELECT COUNT(*) FROM quiz_questions qq "
                + "JOIN question_bank_questions b ON b.bank_question_id=qq.bank_question_id "
                + "WHERE qq.quiz_id=? AND b.question_type NOT IN ('SINGLE_CHOICE','MULTIPLE_CHOICE')",
                Integer.class, quizId);
        if (unsupportedCount != null && unsupportedCount > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This quiz contains question types that cannot be answered with the current quiz answer format.");
        }

        Integer lastAttempt = db.queryForObject(
                "SELECT COALESCE(MAX(attempt_number),0) FROM quiz_attempts "
                + "WHERE quiz_id=? AND student_user_id=?",
                Integer.class, quizId, studentId);
        KeyHolder key = new GeneratedKeyHolder();
        db.update(connection -> {
            var statement = connection.prepareStatement(
                    "INSERT INTO quiz_attempts "
                    + "(quiz_id, student_user_id, attempt_number, started_at, status) "
                    + "VALUES (?, ?, ?, NOW(), 'IN_PROGRESS')",
                    java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setLong(1, quizId);
            statement.setLong(2, studentId);
            statement.setInt(3, (lastAttempt == null ? 0 : lastAttempt) + 1);
            return statement;
        }, key);
        if (key.getKey() == null) {
            throw new IllegalStateException("Quiz attempt insert did not return an ID.");
        }
        return attemptStart(key.getKey().longValue(), false, false);
    }

    @Transactional
    public Map<String, Object> attempt(long studentId, long attemptId) {
        Map<String, Object> attempt = timing(attemptId, studentId);
        String status = String.valueOf(attempt.get("status"));
        if ("IN_PROGRESS".equals(status) && truthy(attempt.get("expired"))) {
            finalizeAttempt(attemptId, studentId);
            attempt = timing(attemptId, studentId);
        }

        long quizId = number(attempt.get("quiz_id")).longValue();
        List<Map<String, Object>> joinedRows = db.queryForList(
                "SELECT qq.question_id, qq.points, qq.position, b.question_text, b.question_type, "
                + "a.selected_option_id, a.answer_text, qo.option_id, bo.option_text, qo.position option_position "
                + "FROM quiz_questions qq "
                + "JOIN question_bank_questions b ON b.bank_question_id=qq.bank_question_id "
                + "LEFT JOIN quiz_answers a ON a.question_id=qq.question_id AND a.attempt_id=? "
                + "LEFT JOIN quiz_options qo ON qo.question_id=qq.question_id "
                + "LEFT JOIN question_bank_options bo ON bo.bank_question_id=qo.bank_question_id "
                + "AND bo.bank_option_id=qo.bank_option_id "
                + "WHERE qq.quiz_id=? ORDER BY qq.position, qo.position",
                attemptId, quizId);
        Map<Long, Map<String, Object>> questions = new LinkedHashMap<>();
        for (Map<String, Object> row : joinedRows) {
            long questionId = number(row.get("question_id")).longValue();
            Map<String, Object> question = questions.computeIfAbsent(questionId, ignored -> {
                Map<String, Object> value = new LinkedHashMap<>();
                value.put("questionId", questionId);
                value.put("questionText", row.get("question_text"));
                value.put("questionType", row.get("question_type"));
                value.put("points", row.get("points"));
                value.put("position", row.get("position"));
                List<Long> selectedOptionIds = parseSelectedOptionIds(
                        row.get("answer_text"), row.get("selected_option_id"));
                value.put("selectedOptionIds", selectedOptionIds);
                value.put("selectedOptionId", selectedOptionIds.isEmpty() ? null : selectedOptionIds.getFirst());
                value.put("options", new ArrayList<Map<String, Object>>());
                return value;
            });
            if (row.get("option_id") != null) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> options = (List<Map<String, Object>>) question.get("options");
                Map<String, Object> option = new LinkedHashMap<>();
                option.put("optionId", row.get("option_id"));
                option.put("text", row.get("option_text"));
                option.put("position", row.get("option_position"));
                options.add(option);
            }
        }
        attempt.put("questions", new ArrayList<>(questions.values()));
        attempt.put("questionCount", questions.size());
        return attempt;
    }

    @Transactional
    public Map<String, Object> saveAnswer(long studentId, long attemptId, long questionId, Long optionId) {
        return saveAnswer(studentId, attemptId, questionId, optionId == null ? List.of() : List.of(optionId));
    }

    public Map<String, Object> saveAnswer(long studentId, long attemptId, long questionId, List<Long> optionIds) {
        Map<String, Object> attempt = lockAttempt(studentId, attemptId);
        if (!"IN_PROGRESS".equals(attempt.get("status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This quiz attempt is already finalized.");
        }
        if (truthy(attempt.get("expired"))) {
            finalizeAttempt(attemptId, studentId);
            return Map.of("saved", false, "expired", true, "status", "SUBMITTED");
        }
        Map<String, Object> question = db.queryForList(
                "SELECT b.question_type FROM quiz_questions qq "
                + "JOIN question_bank_questions b ON b.bank_question_id=qq.bank_question_id "
                + "WHERE qq.quiz_id=? AND qq.question_id=?",
                attempt.get("quiz_id"), questionId).stream().findFirst().orElseThrow(StudentQuizService::notFound);
        List<Long> selectedIds = optionIds == null ? List.of() : optionIds.stream().distinct().toList();
        if (optionIds != null && selectedIds.size() != optionIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate answer options are not allowed.");
        }
        if ("SINGLE_CHOICE".equals(question.get("question_type")) && selectedIds.size() > 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select only one answer for this question.");
        }
        if (!selectedIds.isEmpty() && db.queryForList(
                "SELECT qo.option_id FROM quiz_questions qq "
                + "JOIN quiz_options qo ON qo.question_id=qq.question_id "
                + "WHERE qq.quiz_id=? AND qq.question_id=? AND qo.option_id IN ("
                + String.join(",", java.util.Collections.nCopies(selectedIds.size(), "?")) + ")",
                parameters(attempt.get("quiz_id"), questionId, selectedIds)).size() != selectedIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Choose answer options belonging to this question.");
        }
        db.update(
                "INSERT INTO quiz_answers "
                + "(attempt_id, question_id, selected_option_id, answer_text, awarded_points) "
                + "VALUES (?, ?, ?, ?, NULL) "
                + "ON DUPLICATE KEY UPDATE selected_option_id=VALUES(selected_option_id), "
                + "answer_text=VALUES(answer_text), awarded_points=NULL, answered_at=NOW()",
                attemptId, questionId, selectedIds.isEmpty() ? null : selectedIds.getFirst(),
                selectedIds.size() > 1 ? selectedIdsJson(selectedIds) : null);
        return Map.of("saved", true, "expired", false, "status", "IN_PROGRESS");
    }

    @Transactional
    public Map<String, Object> submit(long studentId, long attemptId) {
        Map<String, Object> attempt = lockAttempt(studentId, attemptId);
        if ("SUBMITTED".equals(attempt.get("status")) || "GRADED".equals(attempt.get("status"))) {
            return Map.of("attemptId", attemptId, "status", attempt.get("status"), "alreadySubmitted", true);
        }
        if (!"IN_PROGRESS".equals(attempt.get("status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This quiz attempt cannot be submitted.");
        }
        finalizeAttempt(attemptId, studentId);
        return Map.of("attemptId", attemptId, "status", "SUBMITTED", "alreadySubmitted", false,
                "expired", truthy(attempt.get("expired")));
    }

    @Transactional
    public Map<String, Object> result(long studentId, long attemptId) {
        Map<String, Object> attempt = timing(attemptId, studentId);
        if ("IN_PROGRESS".equals(attempt.get("status")) && truthy(attempt.get("expired"))) {
            finalizeAttempt(attemptId, studentId);
            attempt = timing(attemptId, studentId);
        }
        if (!"SUBMITTED".equals(attempt.get("status")) && !"GRADED".equals(attempt.get("status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This quiz has not been submitted yet.");
        }
        List<Map<String, Object>> resultRows = db.queryForList(
                "SELECT qq.question_id, qq.position, qq.points, b.question_text, a.selected_option_id, "
                + "a.answer_text, a.awarded_points, qo.option_id, bo.option_text, bo.is_correct "
                + "FROM quiz_questions qq JOIN question_bank_questions b ON b.bank_question_id=qq.bank_question_id "
                + "LEFT JOIN quiz_answers a ON a.question_id=qq.question_id AND a.attempt_id=? "
                + "LEFT JOIN quiz_options qo ON qo.question_id=qq.question_id "
                + "LEFT JOIN question_bank_options bo ON bo.bank_question_id=qo.bank_question_id "
                + "AND bo.bank_option_id=qo.bank_option_id "
                + "WHERE qq.quiz_id=? ORDER BY qq.position, qo.position",
                attemptId, attempt.get("quiz_id"));
        Map<Long, Map<String, Object>> questionMap = new LinkedHashMap<>();
        Map<Long, Map<Long, String>> optionText = new LinkedHashMap<>();
        Map<Long, java.util.Set<Long>> correctIds = new LinkedHashMap<>();
        for (Map<String, Object> row : resultRows) {
            long questionId = number(row.get("question_id")).longValue();
            Map<String, Object> question = questionMap.computeIfAbsent(questionId, ignored -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("question_id", questionId);
                item.put("position", row.get("position"));
                item.put("points", row.get("points"));
                item.put("question_text", row.get("question_text"));
                item.put("selected_option_id", row.get("selected_option_id"));
                item.put("answer_text", row.get("answer_text"));
                item.put("awarded_points", row.get("awarded_points"));
                return item;
            });
            if (row.get("option_id") != null) {
                long optionId = number(row.get("option_id")).longValue();
                optionText.computeIfAbsent(questionId, ignored -> new LinkedHashMap<>())
                        .put(optionId, String.valueOf(row.get("option_text")));
                if (truthy(row.get("is_correct"))) {
                    correctIds.computeIfAbsent(questionId, ignored -> new java.util.LinkedHashSet<>()).add(optionId);
                }
            }
        }
        List<Map<String, Object>> questions = new ArrayList<>();
        int correctCount = 0;
        for (Map<String, Object> question : questionMap.values()) {
            long questionId = number(question.get("question_id")).longValue();
            List<Long> selected = parseSelectedOptionIds(question.get("answer_text"), question.get("selected_option_id"));
            java.util.Set<Long> correct = correctIds.getOrDefault(questionId, java.util.Set.of());
            boolean answered = !selected.isEmpty();
            boolean isCorrect = answered && !correct.isEmpty() && new java.util.HashSet<>(selected).equals(correct);
            String outcome = !answered ? "UNANSWERED" : isCorrect ? "CORRECT" : "INCORRECT";
            if (isCorrect) correctCount++;
            Map<Long, String> labels = optionText.getOrDefault(questionId, Map.of());
            question.put("selected_answer", selected.stream().map(labels::get).filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.joining(", ")));
            question.put("correct_answer", correct.stream().map(labels::get).filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.joining(", ")));
            question.put("selected_option_ids", selected);
            question.put("outcome", outcome);
            questions.add(question);
        }
        BigDecimal maximum = questions.stream().map(row -> decimal(row.get("points")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal score = decimal(attempt.get("score"));
        BigDecimal percentage = maximum.signum() == 0 ? BigDecimal.ZERO
                : score.multiply(BigDecimal.valueOf(100)).divide(maximum, 2, java.math.RoundingMode.HALF_UP);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("attemptId", attemptId);
        result.put("quizId", attempt.get("quiz_id"));
        result.put("quizTitle", attempt.get("title"));
        result.put("subjectName", attempt.get("subject_name"));
        result.put("status", attempt.get("status"));
        result.put("submittedAt", attempt.get("submitted_at"));
        result.put("score", score);
        result.put("maximumMarks", maximum);
        result.put("percentage", percentage);
        result.put("correctCount", correctCount);
        result.put("incorrectCount", questions.size() - correctCount
                - (int) questions.stream().filter(row -> "UNANSWERED".equals(row.get("outcome"))).count());
        result.put("questions", questions);
        return result;
    }

    private void finalizeAttempt(long attemptId, long studentId) {
        Map<String, Object> locked = db.queryForList(
                "SELECT status FROM quiz_attempts WHERE attempt_id=? AND student_user_id=? FOR UPDATE",
                attemptId, studentId).stream().findFirst().orElseThrow(StudentQuizService::notFound);
        if (!"IN_PROGRESS".equals(locked.get("status"))) {
            return;
        }
        List<Map<String, Object>> answerRows = db.queryForList(
                "SELECT qq.question_id, qq.points, b.question_type, qa.selected_option_id, qa.answer_text, "
                + "qo.option_id, bo.is_correct "
                + "FROM quiz_attempts attempt JOIN quiz_questions qq ON qq.quiz_id=attempt.quiz_id "
                + "JOIN question_bank_questions b ON b.bank_question_id=qq.bank_question_id "
                + "LEFT JOIN quiz_answers qa ON qa.attempt_id=attempt.attempt_id AND qa.question_id=qq.question_id "
                + "LEFT JOIN quiz_options qo ON qo.question_id=qq.question_id "
                + "LEFT JOIN question_bank_options bo ON bo.bank_question_id=qo.bank_question_id "
                + "AND bo.bank_option_id=qo.bank_option_id "
                + "WHERE attempt.attempt_id=? AND attempt.student_user_id=? ORDER BY qq.position, qo.position",
                attemptId, studentId);
        Map<Long, Map<String, Object>> questionRows = new LinkedHashMap<>();
        for (Map<String, Object> row : answerRows) {
            long questionId = number(row.get("question_id")).longValue();
            Map<String, Object> question = questionRows.computeIfAbsent(questionId, ignored -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("question_id", questionId);
                item.put("points", row.get("points"));
                item.put("question_type", row.get("question_type"));
                item.put("selected_option_id", row.get("selected_option_id"));
                item.put("answer_text", row.get("answer_text"));
                item.put("correct_option_ids", new java.util.LinkedHashSet<Long>());
                return item;
            });
            if (truthy(row.get("is_correct")) && row.get("option_id") != null) {
                @SuppressWarnings("unchecked")
                java.util.Set<Long> correct = (java.util.Set<Long>) question.get("correct_option_ids");
                correct.add(number(row.get("option_id")).longValue());
            }
        }
        BigDecimal score = BigDecimal.ZERO;
        for (Map<String, Object> row : questionRows.values()) {
            BigDecimal points = decimal(row.get("points"));
            List<Long> selected = parseSelectedOptionIds(row.get("answer_text"), row.get("selected_option_id"));
            @SuppressWarnings("unchecked")
            java.util.Set<Long> correct = (java.util.Set<Long>) row.get("correct_option_ids");
            boolean correctAnswer = !correct.isEmpty() && new java.util.HashSet<>(selected).equals(correct);
            BigDecimal awarded = correctAnswer ? points : BigDecimal.ZERO;
            score = score.add(awarded);
            db.update(
                    "INSERT INTO quiz_answers "
                    + "(attempt_id, question_id, selected_option_id, answer_text, awarded_points) "
                    + "VALUES (?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE awarded_points=VALUES(awarded_points)",
                    attemptId, row.get("question_id"), row.get("selected_option_id"),
                    selected.size() > 1 ? selectedIdsJson(selected) : null, awarded);
        }
        db.update(
                "UPDATE quiz_attempts SET status='SUBMITTED', submitted_at=COALESCE(submitted_at,NOW()), score=? "
                + "WHERE attempt_id=? AND student_user_id=? AND status='IN_PROGRESS'",
                score, attemptId, studentId);
    }

    private Map<String, Object> lockAttempt(long studentId, long attemptId) {
        return db.queryForList(
                "SELECT a.attempt_id, a.quiz_id, a.status, a.started_at, q.time_limit_minutes, "
                + "CASE WHEN q.time_limit_minutes IS NULL THEN FALSE "
                + "WHEN NOW()>=DATE_ADD(a.started_at, INTERVAL q.time_limit_minutes MINUTE) THEN TRUE ELSE FALSE END expired "
                + "FROM quiz_attempts a JOIN quizzes q ON q.quiz_id=a.quiz_id "
                + "WHERE a.attempt_id=? AND a.student_user_id=? FOR UPDATE",
                attemptId, studentId).stream().findFirst().orElseThrow(StudentQuizService::notFound);
    }

    private Map<String, Object> timing(long attemptId, long studentId) {
        return db.queryForList(
                "SELECT a.attempt_id, a.quiz_id, a.status, a.started_at, a.submitted_at, a.score, "
                + "q.title, q.instructions, q.time_limit_minutes, s.subject_name, NOW() server_now, "
                + "CASE WHEN q.time_limit_minutes IS NULL THEN NULL ELSE "
                + "GREATEST(TIMESTAMPDIFF(SECOND,NOW(),DATE_ADD(a.started_at,INTERVAL q.time_limit_minutes MINUTE)),0) END remaining_seconds, "
                + "CASE WHEN q.time_limit_minutes IS NULL THEN FALSE "
                + "WHEN NOW()>=DATE_ADD(a.started_at,INTERVAL q.time_limit_minutes MINUTE) THEN TRUE ELSE FALSE END expired "
                + "FROM quiz_attempts a JOIN quizzes q ON q.quiz_id=a.quiz_id "
                + "JOIN subjects s ON s.subject_id=q.subject_id "
                + "WHERE a.attempt_id=? AND a.student_user_id=?",
                attemptId, studentId).stream().findFirst().orElseThrow(StudentQuizService::notFound);
    }

    private static Map<String, Object> attemptStart(long attemptId, boolean resumed, boolean expired) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("attemptId", attemptId);
        response.put("resumed", resumed);
        response.put("expired", expired);
        response.put("status", expired ? "SUBMITTED" : "IN_PROGRESS");
        return response;
    }

    private static Object[] parameters(Object quizId, long questionId, List<Long> optionIds) {
        Object[] values = new Object[optionIds.size() + 2];
        values[0] = quizId;
        values[1] = questionId;
        for (int i = 0; i < optionIds.size(); i++) values[i + 2] = optionIds.get(i);
        return values;
    }

    private static String selectedIdsJson(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",", "[", "]"));
    }

    private static List<Long> parseSelectedOptionIds(Object answerText, Object selectedOptionId) {
        if (answerText != null && !answerText.toString().isBlank()) {
            String value = answerText.toString().trim();
            if (value.startsWith("[") && value.endsWith("]")) {
                String inner = value.substring(1, value.length() - 1).trim();
                if (inner.isEmpty()) return List.of();
                List<Long> parsed = new ArrayList<>();
                for (String part : inner.split(",")) {
                    try {
                        long id = Long.parseLong(part.trim());
                        if (id > 0) parsed.add(id);
                    } catch (NumberFormatException ignored) {
                        return selectedOptionId == null ? List.of() : List.of(number(selectedOptionId).longValue());
                    }
                }
                return List.copyOf(parsed);
            }
        }
        return selectedOptionId == null ? List.of() : List.of(number(selectedOptionId).longValue());
    }

    private static Number number(Object value) {
        if (value instanceof Number number) return number;
        if (value == null) return 0;
        return Long.valueOf(value.toString());
    }

    private static BigDecimal decimal(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private static boolean truthy(Object value) {
        return Boolean.TRUE.equals(value) || (value instanceof Number number && number.intValue() != 0);
    }

    private static ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Quiz or attempt not found.");
    }
}
