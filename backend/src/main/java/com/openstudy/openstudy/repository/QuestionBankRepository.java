package com.openstudy.openstudy.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.openstudy.openstudy.dto.QuestionBankOptionRequest;
import com.openstudy.openstudy.dto.QuestionBankQuestionRequest;
import com.openstudy.openstudy.dto.QuestionBankQuestionResponse;

@Repository
public class QuestionBankRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuestionBankRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long findActiveTeacherId(String email) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT user_id FROM users WHERE email = ? AND role = 'TEACHER' AND account_status = 'ACTIVE'",
                (result, rowNumber) -> result.getLong("user_id"), email);
        if (ids.isEmpty()) {
            throw new TeacherNotFoundException();
        }
        return ids.getFirst();
    }

    public long insertQuestion(long teacherId, QuestionBankQuestionRequest request) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement prepared = connection.prepareStatement(
                    "INSERT INTO question_bank_questions "
                            + "(created_by, question_text, question_type, points, difficulty, source, approval_status) "
                            + "VALUES (?, ?, ?, ?, ?, 'MANUAL', 'APPROVED')",
                    Statement.RETURN_GENERATED_KEYS);
            prepared.setLong(1, teacherId);
            prepared.setString(2, request.questionText().trim());
            prepared.setString(3, request.questionType().name());
            prepared.setBigDecimal(4, request.points());
            prepared.setString(5, request.difficulty().name());
            return prepared;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("Question bank insert did not return an ID");
        }
        return key.longValue();
    }

    public void insertOptions(long bankQuestionId, List<QuestionBankOptionRequest> options) {
        for (int index = 0; index < options.size(); index++) {
            QuestionBankOptionRequest option = options.get(index);
            jdbcTemplate.update(
                    "INSERT INTO question_bank_options (bank_question_id, option_text, is_correct, position) "
                            + "VALUES (?, ?, ?, ?)",
                    bankQuestionId, option.text().trim(), option.correct(), index + 1);
        }
    }

    public List<QuestionBankQuestionResponse> findQuestions(long teacherId) {
        return jdbcTemplate.query(
                "SELECT bank_question_id, question_text, question_type, points, difficulty, source, approval_status "
                        + "FROM question_bank_questions WHERE created_by = ? ORDER BY created_at DESC, bank_question_id DESC",
                (result, rowNumber) -> {
                    long questionId = result.getLong("bank_question_id");
                    List<QuestionBankQuestionResponse.Option> options = jdbcTemplate.query(
                            "SELECT bank_option_id, option_text, is_correct, position "
                                    + "FROM question_bank_options WHERE bank_question_id = ? ORDER BY position",
                            (optionResult, optionRow) -> new QuestionBankQuestionResponse.Option(
                                    optionResult.getLong("bank_option_id"), optionResult.getString("option_text"),
                                    optionResult.getBoolean("is_correct"), optionResult.getInt("position")),
                            questionId);
                    return new QuestionBankQuestionResponse(
                            questionId,
                            result.getString("question_text"),
                            result.getString("question_type"),
                            result.getBigDecimal("points"),
                            result.getString("difficulty"),
                            result.getString("source"),
                            result.getString("approval_status"),
                            options);
                }, teacherId);
    }

    public static class TeacherNotFoundException extends RuntimeException {
        public TeacherNotFoundException() {
            super("Authenticated account is not an active teacher");
        }
    }
}