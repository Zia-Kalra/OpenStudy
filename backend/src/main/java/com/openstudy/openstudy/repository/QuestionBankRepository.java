package com.openstudy.openstudy.repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.openstudy.openstudy.dto.QuestionBankOptionRequest;
import com.openstudy.openstudy.dto.QuestionBankQuestionRequest;
import com.openstudy.openstudy.dto.QuestionBankQuestionResponse;

@Repository
@Profile("!mock")
public class QuestionBankRepository {

    private final JdbcTemplate jdbcTemplate;

    public QuestionBankRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long findActiveTeacherId(String email) {
        List<Long> ids = jdbcTemplate.query(
                "SELECT user_id FROM users WHERE email = ? "
                        + "AND role = 'TEACHER' AND account_status = 'ACTIVE'",
                (result, rowNumber) -> result.getLong("user_id"),
                email);

        if (ids.isEmpty()) {
            throw new TeacherNotFoundException();
        }

        return ids.getFirst();
    }

    public long insertQuestion(
            long teacherId,
            QuestionBankQuestionRequest request) {

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement prepared = connection.prepareStatement(
                    "INSERT INTO question_bank_questions "
                            + "(created_by, question_text, question_type, points, "
                            + "difficulty, source, approval_status, subject_id, topic) "
                            + "VALUES (?, ?, ?, ?, ?, 'MANUAL', 'APPROVED', ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);

            prepared.setLong(1, teacherId);
            prepared.setString(2, request.questionText().trim());
            prepared.setString(3, request.questionType().name());
            prepared.setBigDecimal(4, request.points());
            prepared.setString(5, request.difficulty().name());
            prepared.setLong(6, request.subjectId());
            prepared.setString(7, request.topic() == null ? "" : request.topic().trim());

            return prepared;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException(
                    "Question bank insert did not return an ID");
        }

        return key.longValue();
    }

    /**
     * Saves an AI-generated question after teacher review.
     */
    public long insertReviewedAiQuestion(
            long teacherId,
            QuestionBankQuestionRequest request) {

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement prepared = connection.prepareStatement(
                    "INSERT INTO question_bank_questions "
                            + "(created_by, question_text, question_type, points, "
                            + "difficulty, source, approval_status, subject_id, topic) "
                            + "VALUES (?, ?, ?, ?, ?, 'AI', 'APPROVED', ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);

            prepared.setLong(1, teacherId);
            prepared.setString(2, request.questionText().trim());
            prepared.setString(3, request.questionType().name());
            prepared.setBigDecimal(4, request.points());
            prepared.setString(5, request.difficulty().name());
            prepared.setLong(6, request.subjectId());
            prepared.setString(7, request.topic() == null ? "" : request.topic().trim());

            return prepared;
        }, keyHolder);

        Number key = keyHolder.getKey();

        if (key == null) {
            throw new IllegalStateException(
                    "AI question insert did not return an ID");
        }

        return key.longValue();
    }

    public void insertOptions(
            long bankQuestionId,
            List<QuestionBankOptionRequest> options) {

        for (int index = 0; index < options.size(); index++) {
            QuestionBankOptionRequest option = options.get(index);

            jdbcTemplate.update(
                    "INSERT INTO question_bank_options "
                            + "(bank_question_id, option_text, is_correct, position) "
                            + "VALUES (?, ?, ?, ?)",
                    bankQuestionId,
                    option.text().trim(),
                    option.correct(),
                    index + 1);
        }
    }

    public List<QuestionBankQuestionResponse> findQuestions(long teacherId) {
        return jdbcTemplate.query(
                "SELECT b.bank_question_id, b.question_text, b.question_type, "
                        + "b.points, b.difficulty, b.source, b.approval_status, b.subject_id, s.subject_name, b.topic "
                        + "FROM question_bank_questions b LEFT JOIN subjects s ON s.subject_id=b.subject_id "
                        + "WHERE b.created_by = ? "
                        + "ORDER BY b.created_at DESC, b.bank_question_id DESC",
                (result, rowNumber) -> {
                    long questionId = result.getLong("bank_question_id");

                    List<QuestionBankQuestionResponse.Option> options =
                            jdbcTemplate.query(
                                    "SELECT bank_option_id, option_text, "
                                            + "is_correct, position "
                                            + "FROM question_bank_options "
                                            + "WHERE bank_question_id = ? "
                                            + "ORDER BY position",
                                    (optionResult, optionRow) ->
                                            new QuestionBankQuestionResponse.Option(
                                                    optionResult.getLong("bank_option_id"),
                                                    optionResult.getString("option_text"),
                                                    optionResult.getBoolean("is_correct"),
                                                    optionResult.getInt("position")),
                                    questionId);

                    long subjectId = result.getLong("subject_id");
                    Long nullableSubjectId = result.wasNull() ? null : subjectId;
                    return new QuestionBankQuestionResponse(
                            questionId,
                            result.getString("question_text"),
                            result.getString("question_type"),
                            result.getBigDecimal("points"),
                            result.getString("difficulty"),
                            result.getString("source"),
                            result.getString("approval_status"),
                            nullableSubjectId,
                            result.getString("subject_name"),
                            result.getString("topic"),
                            options);
                },
                teacherId);
    }

    public boolean teacherOwnsSubject(long teacherId, long subjectId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM subject_teachers WHERE teacher_id=? AND subject_id=?",
                Integer.class, teacherId, subjectId);
        return count != null && count > 0;
    }

    public List<java.util.Map<String,Object>> findTeacherSubjects(long teacherId) {
        return jdbcTemplate.queryForList("SELECT s.subject_id, s.subject_name FROM subjects s JOIN subject_teachers st ON st.subject_id=s.subject_id WHERE st.teacher_id=? ORDER BY s.subject_name", teacherId);
    }

    public void updateQuestion(long teacherId, long questionId, QuestionBankQuestionRequest request) {
        int updated = jdbcTemplate.update("UPDATE question_bank_questions SET question_text=?, question_type=?, points=?, difficulty=?, subject_id=?, topic=? WHERE bank_question_id=? AND created_by=?",
                request.questionText().trim(), request.questionType().name(), request.points(), request.difficulty().name(), request.subjectId(), request.topic() == null ? "" : request.topic().trim(), questionId, teacherId);
        if (updated == 0) throw new QuestionNotFoundException();
        jdbcTemplate.update("DELETE FROM question_bank_options WHERE bank_question_id=?", questionId);
        insertOptions(questionId, request.options());
    }

    public void updateStatus(long teacherId, long questionId, String status) {
        int updated = jdbcTemplate.update("UPDATE question_bank_questions SET approval_status=? WHERE bank_question_id=? AND created_by=?",
                status, questionId, teacherId);
        if (updated == 0) throw new QuestionNotFoundException();
    }

    public void deleteQuestion(long teacherId, long questionId) {
        Integer references = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM quiz_questions WHERE bank_question_id=?", Integer.class, questionId);
        if (references != null && references > 0) throw new QuestionInUseException();
        jdbcTemplate.update("DELETE FROM question_bank_options WHERE bank_question_id=? AND EXISTS (SELECT 1 FROM question_bank_questions WHERE bank_question_id=? AND created_by=?)", questionId, questionId, teacherId);
        if (jdbcTemplate.update("DELETE FROM question_bank_questions WHERE bank_question_id=? AND created_by=?", questionId, teacherId) == 0) throw new QuestionNotFoundException();
    }

    public static class QuestionNotFoundException extends RuntimeException { }
    public static class QuestionInUseException extends RuntimeException { }

    public static class TeacherNotFoundException extends RuntimeException {

        public TeacherNotFoundException() {
            super("Authenticated account is not an active teacher");
        }
    }
}
