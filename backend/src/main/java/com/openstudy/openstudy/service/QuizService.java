package com.openstudy.openstudy.service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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

@Service
@Profile("!mock")
public class QuizService {

    private final JdbcTemplate db;

    public QuizService(JdbcTemplate db) {
        this.db = db;
    }

    public long teacherId(String email) {
        List<Long> ids = db.query(
                "SELECT user_id FROM users "
                + "WHERE email = ? AND role = 'TEACHER' "
                + "AND account_status = 'ACTIVE'",
                (rs, row) -> rs.getLong("user_id"),
                email);

        if (ids.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "Active teacher account required");
        }

        return ids.get(0);
    }

    public List<Map<String, Object>> subjects(long teacherId) {
        return db.queryForList(
                "SELECT s.subject_id, s.subject_name "
                + "FROM subjects s "
                + "JOIN subject_teachers st ON st.subject_id = s.subject_id "
                + "WHERE st.teacher_id = ? ORDER BY s.subject_name",
                teacherId);
    }

    /** Finds a subject already assigned to this teacher, or creates and assigns it. */
    @Transactional
    public long findOrCreateSubject(long teacherId, String subjectName) {
        if (subjectName == null || subjectName.isBlank()) {
            throw bad("Subject name is required.");
        }

        String name = subjectName.trim();
        if (name.length() > 150) {
            throw bad("Subject name must be 150 characters or fewer.");
        }

        List<Long> assignedIds = db.query(
                "SELECT s.subject_id FROM subjects s "
                + "JOIN subject_teachers st ON st.subject_id = s.subject_id "
                + "WHERE st.teacher_id = ? AND LOWER(TRIM(s.subject_name)) = LOWER(?) "
                + "ORDER BY s.subject_id LIMIT 1",
                (rs, row) -> rs.getLong("subject_id"), teacherId, name);

        if (!assignedIds.isEmpty()) {
            return assignedIds.get(0);
        }

        List<Long> existingIds = db.query(
                "SELECT subject_id FROM subjects "
                + "WHERE LOWER(TRIM(subject_name)) = LOWER(?) "
                + "ORDER BY subject_id LIMIT 1",
                (rs, row) -> rs.getLong("subject_id"), name);

        long subjectId;
        if (!existingIds.isEmpty()) {
            subjectId = existingIds.get(0);
        } else {
            KeyHolder subjectKey = new GeneratedKeyHolder();
            db.update(connection -> {
                PreparedStatement ps = connection.prepareStatement(
                        "INSERT INTO subjects (subject_name, description, student_id) "
                        + "VALUES (?, ?, NULL)",
                        Statement.RETURN_GENERATED_KEYS);
                ps.setString(1, name);
                ps.setString(2, "Created by teacher for quiz use.");
                return ps;
            }, subjectKey);

            Number generated = subjectKey.getKey();
            if (generated == null) {
                throw new IllegalStateException("Subject ID was not generated.");
            }
            subjectId = generated.longValue();
        }

        db.update(
                "INSERT INTO subject_teachers (subject_id, teacher_id) "
                + "VALUES (?, ?) ON DUPLICATE KEY UPDATE teacher_id = VALUES(teacher_id)",
                subjectId, teacherId);

        return subjectId;
    }

    public List<Map<String, Object>> approvedQuestions(long teacherId) {
        List<Map<String, Object>> questions = db.queryForList(
                "SELECT bank_question_id, question_text, question_type, "
                + "points, difficulty, source "
                + "FROM question_bank_questions "
                + "WHERE created_by = ? AND approval_status = 'APPROVED' "
                + "ORDER BY created_at DESC",
                teacherId);

        for (Map<String, Object> question : questions) {
            long questionId =
                    ((Number) question.get("bank_question_id")).longValue();

            question.put("options", db.queryForList(
                    "SELECT bank_option_id, option_text, position "
                    + "FROM question_bank_options "
                    + "WHERE bank_question_id = ? ORDER BY position",
                    questionId));
        }

        return questions;
    }

    public List<Map<String, Object>> quizzes(long teacherId) {
        return db.queryForList(
                "SELECT q.quiz_id, q.title, q.instructions, "
                + "q.subject_id, s.subject_name, q.status, "
                + "q.available_from, q.available_until, "
                + "q.time_limit_minutes, q.created_at, "
                + "(SELECT COUNT(*) FROM quiz_questions qq "
                + " WHERE qq.quiz_id = q.quiz_id) AS question_count "
                + "FROM quizzes q "
                + "JOIN subjects s ON s.subject_id = q.subject_id "
                + "WHERE q.created_by = ? "
                + "ORDER BY q.created_at DESC",
                teacherId);
    }

    @Transactional
    public long createQuiz(long teacherId, Map<String, Object> input) {
        String title = requiredText(input, "title", 200);
        String instructions = optionalText(input, "instructions");
        String subjectName = optionalText(input, "subjectName");
        long subjectId;
        if (!subjectName.isBlank()) {
            subjectId = findOrCreateSubject(teacherId, subjectName);
        } else {
            // Backward compatibility for clients still sending an existing subject ID.
            subjectId = requiredLong(input, "subjectId");
        }

        Integer timeLimit = null;
        Object timeValue = input.get("timeLimitMinutes");

        if (timeValue != null && !timeValue.toString().isBlank()) {
            try {
                timeLimit = Integer.valueOf(timeValue.toString());
            } catch (NumberFormatException ex) {
                throw bad("Time limit must be a whole number of minutes.");
            }

            if (timeLimit < 1 || timeLimit > 1440) {
                throw bad("Time limit must be between 1 and 1440 minutes.");
            }
        }

        LocalDateTime availableFrom = optionalDate(input.get("availableFrom"));
        LocalDateTime availableUntil = optionalDate(input.get("availableUntil"));

        if (availableFrom != null && availableUntil != null
                && availableUntil.isBefore(availableFrom)) {
            throw bad("The closing time must be after the opening time.");
        }

        List<Long> bankQuestionIds =
                questionIds(input.get("bankQuestionIds"));

        List<Map<String, Object>> manualQuestions =
                manualQuestions(input.get("manualQuestions"));

        if (bankQuestionIds.isEmpty() && manualQuestions.isEmpty()) {
            throw bad("Add at least one manual question or select a question from the bank.");
        }

        if (db.queryForList(
                "SELECT subject_id FROM subject_teachers "
                + "WHERE subject_id = ? AND teacher_id = ?",
                subjectId, teacherId).isEmpty()) {
            throw bad("Choose a subject assigned to your teacher account.");
        }

        // Validate all selected bank questions before creating the quiz.
        for (Long questionId : bankQuestionIds) {
            List<Long> found = db.query(
                    "SELECT bank_question_id "
                    + "FROM question_bank_questions "
                    + "WHERE bank_question_id = ? AND created_by = ? "
                    + "AND approval_status = 'APPROVED'",
                    (rs, row) -> rs.getLong(1),
                    questionId, teacherId);

            if (found.isEmpty()) {
                throw bad("One or more selected questions are unavailable.");
            }

            Integer optionCount = db.queryForObject(
                    "SELECT COUNT(*) FROM question_bank_options "
                    + "WHERE bank_question_id = ?",
                    Integer.class, questionId);

            if (optionCount == null || optionCount < 2) {
                throw bad("Every selected MCQ must have at least two options.");
            }
        }

        String status = optionalText(input, "status");
        if (!"DRAFT".equals(status) && !"PUBLISHED".equals(status)) {
            status = "DRAFT";
        }

        KeyHolder keyHolder = new GeneratedKeyHolder();
        final Integer finalTimeLimit = timeLimit;
        final String finalStatus = status;

        db.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO quizzes "
                    + "(subject_id, created_by, title, instructions, "
                    + "available_from, available_until, time_limit_minutes, status) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);

            ps.setLong(1, subjectId);
            ps.setLong(2, teacherId);
            ps.setString(3, title);
            ps.setString(4, instructions);
            ps.setTimestamp(5, availableFrom == null
                    ? null : Timestamp.valueOf(availableFrom));
            ps.setTimestamp(6, availableUntil == null
                    ? null : Timestamp.valueOf(availableUntil));

            if (finalTimeLimit == null) {
                ps.setNull(7, java.sql.Types.SMALLINT);
            } else {
                ps.setInt(7, finalTimeLimit);
            }

            ps.setString(8, finalStatus);
            return ps;
        }, keyHolder);

        Number generated = keyHolder.getKey();
        if (generated == null) {
            throw new IllegalStateException("Quiz ID was not generated.");
        }

        long quizId = generated.longValue();
        int position = 1;

        // Attach existing approved bank questions first.
        for (Long bankQuestionId : bankQuestionIds) {
            addQuestionToQuiz(quizId, bankQuestionId, position++);
        }

        // Save each manual question into the bank, then attach it to this quiz.
        for (Map<String, Object> manual : manualQuestions) {
            long bankQuestionId = insertManualQuestion(teacherId, manual);
            addQuestionToQuiz(quizId, bankQuestionId, position++);
        }

        return quizId;
    }

    private long insertManualQuestion(
            long teacherId, Map<String, Object> question) {

        KeyHolder keyHolder = new GeneratedKeyHolder();

        db.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO question_bank_questions "
                    + "(created_by, question_text, question_type, points, "
                    + "difficulty, source, approval_status) "
                    + "VALUES (?, ?, ?, ?, ?, 'MANUAL', 'APPROVED')",
                    Statement.RETURN_GENERATED_KEYS);

            ps.setLong(1, teacherId);
            ps.setString(2, question.get("questionText").toString().trim());
            ps.setString(3, question.get("questionType").toString());
            ps.setBigDecimal(4, new BigDecimal(question.get("points").toString()));
            ps.setString(5, question.get("difficulty").toString());

            return ps;
        }, keyHolder);

        Number generated = keyHolder.getKey();
        if (generated == null) {
            throw new IllegalStateException(
                    "Manual question ID was not generated.");
        }

        long bankQuestionId = generated.longValue();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options =
                (List<Map<String, Object>>) question.get("options");

        for (int i = 0; i < options.size(); i++) {
            Map<String, Object> option = options.get(i);

            db.update(
                    "INSERT INTO question_bank_options "
                    + "(bank_question_id, option_text, is_correct, position) "
                    + "VALUES (?, ?, ?, ?)",
                    bankQuestionId,
                    option.get("text").toString().trim(),
                    Boolean.TRUE.equals(option.get("correct")),
                    i + 1);
        }

        return bankQuestionId;
    }

    private void addQuestionToQuiz(
            long quizId, long bankQuestionId, int position) {

        Map<String, Object> bankQuestion = db.queryForMap(
                "SELECT question_text, question_type, points "
                + "FROM question_bank_questions "
                + "WHERE bank_question_id = ?",
                bankQuestionId);
        BigDecimal points = (BigDecimal) bankQuestion.get("points");

        if (points == null) {
            throw bad("Question marks could not be loaded.");
        }

        KeyHolder questionKey = new GeneratedKeyHolder();

        db.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO quiz_questions "
                    + "(quiz_id, bank_question_id, question_text, question_type, "
                    + "position, points) "
                    + "VALUES (?, ?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);

            ps.setLong(1, quizId);
            ps.setLong(2, bankQuestionId);
            ps.setString(3, bankQuestion.get("question_text").toString());
            ps.setString(4, bankQuestion.get("question_type").toString());
            ps.setInt(5, position);
            ps.setBigDecimal(6, points);

            return ps;
        }, questionKey);

        Number generated = questionKey.getKey();
        if (generated == null) {
            throw new IllegalStateException(
                    "Quiz question ID was not generated.");
        }

        long quizQuestionId = generated.longValue();

        List<Map<String, Object>> bankOptions = db.queryForList(
                "SELECT bank_option_id, option_text, is_correct "
                + "FROM question_bank_options "
                + "WHERE bank_question_id = ? ORDER BY position",
                bankQuestionId);

        if (bankOptions.size() < 2) {
            throw bad("Every quiz MCQ must have at least two options.");
        }

        int optionPosition = 1;

        for (Map<String, Object> bankOption : bankOptions) {
            db.update(
                    "INSERT INTO quiz_options "
                    + "(question_id, bank_question_id, bank_option_id, "
                    + "option_text, is_correct, position) "
                    + "VALUES (?, ?, ?, ?, ?, ?)",
                    quizQuestionId,
                    bankQuestionId,
                    bankOption.get("bank_option_id"),
                    bankOption.get("option_text").toString(),
                    bankOption.get("is_correct"),
                    optionPosition++);
        }
    }

    @Transactional
    public void updateStatus(long teacherId, long quizId, String status) {
        if (!List.of("DRAFT", "PUBLISHED", "CLOSED").contains(status)) {
            throw bad("Invalid quiz status.");
        }

        int updated = db.update(
                "UPDATE quizzes SET status = ? "
                + "WHERE quiz_id = ? AND created_by = ?",
                status, quizId, teacherId);

        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Quiz not found.");
        }
    }

    private static List<Map<String, Object>> manualQuestions(Object value) {
        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> values)) {
            throw bad("Manual questions must be a list.");
        }

        if (values.size() > 100) {
            throw bad("A quiz cannot contain more than 100 manual questions.");
        }

        List<Map<String, Object>> result = new ArrayList<>();

        for (Object item : values) {
            if (!(item instanceof Map<?, ?> raw)) {
                throw bad("Each manual question must be an object.");
            }

            Map<String, Object> question = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : raw.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    question.put(key, entry.getValue());
                }
            }

            String text = optionalText(question, "questionText");
            String type = optionalText(question, "questionType");
            String difficulty = optionalText(question, "difficulty");

            if (text.isBlank() || text.length() > 5000) {
                throw bad("Each manual question needs text (maximum 5000 characters).");
            }

            if (!List.of("SINGLE_CHOICE", "MULTIPLE_CHOICE").contains(type)) {
                throw bad("Question type must be SINGLE_CHOICE or MULTIPLE_CHOICE.");
            }

            if (!List.of("EASY", "MEDIUM", "HARD").contains(difficulty)) {
                throw bad("Choose EASY, MEDIUM, or HARD difficulty.");
            }

            BigDecimal points;
            try {
                points = new BigDecimal(String.valueOf(question.get("points")));
            } catch (NumberFormatException ex) {
                throw bad("Enter valid marks for every manual question.");
            }

            if (points.signum() <= 0
                    || points.compareTo(new BigDecimal("100")) > 0) {
                throw bad("Question marks must be greater than 0 and at most 100.");
            }

            Object optionsValue = question.get("options");
            if (!(optionsValue instanceof List<?> options)
                    || options.size() < 2 || options.size() > 6) {
                throw bad("Each manual MCQ must have between 2 and 6 options.");
            }

            int correctCount = 0;

            for (Object optionItem : options) {
                if (!(optionItem instanceof Map<?, ?> option)) {
                    throw bad("Each answer option must be an object.");
                }

                Object optionText = option.get("text");
                if (optionText == null
                        || optionText.toString().isBlank()
                        || optionText.toString().length() > 1000) {
                    throw bad("Every answer option needs text (maximum 1000 characters).");
                }

                Object correct = option.get("correct");
                if (correct != null && !(correct instanceof Boolean)) {
                    throw bad("The correct flag for each option must be true or false.");
                }

                if (Boolean.TRUE.equals(correct)) {
                    correctCount++;
                }
            }

            if (correctCount == 0
                    || ("SINGLE_CHOICE".equals(type) && correctCount != 1)) {
                throw bad(
                        "Single-choice questions need exactly one correct answer; "
                        + "multiple-choice questions need at least one.");
            }

            result.add(question);
        }

        return result;
    }

    private static String requiredText(
            Map<String, Object> input, String key, int maxLength) {

        String value = optionalText(input, key);

        if (value.isBlank()) {
            throw bad(key + " is required.");
        }

        if (value.length() > maxLength) {
            throw bad(key + " is too long.");
        }

        return value;
    }

    private static String optionalText(
            Map<String, Object> input, String key) {

        Object value = input.get(key);
        return value == null ? "" : value.toString().trim();
    }

    private static long requiredLong(
            Map<String, Object> input, String key) {

        try {
            long value = Long.parseLong(String.valueOf(input.get(key)));

            if (value < 1) {
                throw new NumberFormatException();
            }

            return value;
        } catch (Exception ex) {
            throw bad(key + " must be a valid ID.");
        }
    }

    private static List<Long> questionIds(Object value) {
        if (value == null) {
            return List.of();
        }

        if (!(value instanceof List<?> values)) {
            throw bad("Selected question IDs must be a list.");
        }

        List<Long> ids = new ArrayList<>();

        for (Object item : values) {
            try {
                long id = Long.parseLong(item.toString());

                if (id < 1 || ids.contains(id)) {
                    throw new NumberFormatException();
                }

                ids.add(id);
            } catch (NumberFormatException ex) {
                throw bad(
                        "Selected question IDs must be unique positive numbers.");
            }
        }

        return ids;
    }

    private static LocalDateTime optionalDate(Object value) {
        if (value == null || value.toString().isBlank()) {
            return null;
        }

        try {
            return LocalDateTime.parse(value.toString());
        } catch (Exception ex) {
            throw bad("Enter dates in YYYY-MM-DDTHH:MM format.");
        }
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST, message);
    }
}
