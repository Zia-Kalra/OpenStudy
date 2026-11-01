package com.openstudy.openstudy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class StudentQuizServiceTest {

    @Mock
    private JdbcTemplate db;

    private StudentQuizService service;

    @BeforeEach
    void setUp() {
        service = new StudentQuizService(db);
    }

    @Test
    void availableQuizQueryIncludesPublishedQuizzesWithoutEnrollmentFilteringWithinAvailabilityWindow() {
        when(db.queryForList(anyString())).thenReturn(List.of());
        when(db.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        service.availableQuizzes(25L);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(db, org.mockito.Mockito.times(2)).queryForList(sql.capture());
        String listingSql = sql.getAllValues().getFirst();
        assertTrue(listingSql.contains("q.status='PUBLISHED'"));
        assertTrue(listingSql.contains("JOIN subjects s ON s.subject_id=q.subject_id"));
        assertFalse(listingSql.contains("subject_enrollments"));
        assertTrue(listingSql.contains("q.available_from<=NOW()"));
        assertTrue(listingSql.contains("q.available_until>=NOW()"));
        assertTrue(listingSql.contains("question_bank_questions"));
        String diagnosticSql = sql.getAllValues().get(1);
        assertTrue(diagnosticSql.contains("q.created_by"));
        assertTrue(diagnosticSql.contains("q.available_from"));
        assertTrue(diagnosticSql.contains("q.available_until"));
        assertTrue(diagnosticSql.contains("@@session.time_zone"));
        assertTrue(diagnosticSql.contains("exclusion_reason"));
    }

    @Test
    void availableQuizFromUnenrolledSubjectIsReturnedWithStudentPageFields() {
        Map<String, Object> quiz = Map.of(
                "quiz_id", 5L,
                "title", "Algebra review",
                "instructions", "Show your work.",
                "subject_name", "Mathematics",
                "available_from", Timestamp.valueOf("2026-10-01 09:00:00"),
                "available_until", Timestamp.valueOf("2026-10-30 17:00:00"),
                "time_limit_minutes", 30,
                "question_count", 10,
                "total_marks", new BigDecimal("20.00"),
                "supported_for_attempt", true);
        when(db.queryForList(anyString())).thenReturn(List.of(new java.util.LinkedHashMap<>(quiz)));
        when(db.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        List<Map<String, Object>> available = service.availableQuizzes(25L);

        assertEquals("Algebra review", available.getFirst().get("title"));
        assertEquals("Mathematics", available.getFirst().get("subject_name"));
        assertEquals(10, available.getFirst().get("question_count"));
        assertEquals(30, available.getFirst().get("time_limit_minutes"));
        assertTrue(available.getFirst().containsKey("available_from"));
        assertTrue(available.getFirst().containsKey("available_until"));
        assertTrue(available.getFirst().containsKey("supported_for_attempt"));
    }

    @Test
    void listingAttachesOnlyAuthenticatedStudentsAttemptRecordsAndKeepsEarlierResults() {
        Map<String, Object> quiz = new java.util.LinkedHashMap<>(Map.of(
                "quiz_id", 5L, "title", "Algebra review", "subject_name", "Mathematics",
                "question_count", 2, "total_marks", new BigDecimal("4.00"), "supported_for_attempt", true));
        Map<String, Object> earlier = Map.of(
                "quiz_id", 5L, "title", "Algebra review", "subject_name", "Mathematics",
                "question_count", 2, "total_marks", new BigDecimal("4.00"), "attempt_id", 100L,
                "attempt_number", 1, "status", "SUBMITTED", "submitted_at", Timestamp.valueOf("2026-10-20 10:00:00"),
                "score", new BigDecimal("3.00"));
        Map<String, Object> active = Map.of(
                "quiz_id", 5L, "title", "Algebra review", "subject_name", "Mathematics",
                "question_count", 2, "total_marks", new BigDecimal("4.00"), "attempt_id", 101L,
                "attempt_number", 2, "status", "IN_PROGRESS", "started_at", Timestamp.valueOf("2026-10-21 10:00:00"),
                "score", BigDecimal.ZERO);
        when(db.queryForList(anyString())).thenReturn(List.of(quiz));
        when(db.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(earlier, active));

        Map<String, Object> listed = service.availableQuizzes(25L).getFirst();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attempts = (List<Map<String, Object>>) listed.get("attempts");
        assertEquals(2, attempts.size());
        assertEquals("SUBMITTED", attempts.get(0).get("status"));
        assertEquals(100L, attempts.get(0).get("attemptId"));
        assertEquals("IN_PROGRESS", attempts.get(1).get("status"));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object[]> parameters = ArgumentCaptor.forClass(Object[].class);
        verify(db).queryForList(sql.capture(), parameters.capture());
        assertTrue(sql.getValue().contains("a.student_user_id=?"));
        assertTrue(sql.getValue().contains("'IN_PROGRESS','SUBMITTED','GRADED'"));
        assertEquals(25L, parameters.getValue()[0]);
    }

    @Test
    void startAllowsAnUnenrolledStudentAndResumesExistingUnexpiredAttempt() {
        Map<String, Object> quiz = Map.of("quiz_id", 5L, "subject_id", 8, "status", "PUBLISHED",
                "available_now", true, "not_expired", true);
        Map<String, Object> inProgress = Map.of("attempt_id", 100L, "started_at", Timestamp.valueOf("2026-10-01 10:00:00"));
        Map<String, Object> summary = new java.util.HashMap<>(activeAttempt());
        summary.put("expired", false);
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FROM quizzes WHERE quiz_id")) return List.of(quiz);
            if (sql.contains("WHERE quiz_id=? AND student_user_id=? AND status='IN_PROGRESS'")) return List.of(inProgress);
            if (sql.contains("server_now")) return List.of(summary);
            return List.of();
        });
        Map<String, Object> attempt = service.startOrResume(25L, 5L);

        assertEquals(100L, attempt.get("attemptId"));
        assertEquals(true, attempt.get("resumed"));
        verify(db, org.mockito.Mockito.never()).update(
                any(org.springframework.jdbc.core.PreparedStatementCreator.class),
                any(org.springframework.jdbc.support.KeyHolder.class));
    }

    @Test
    void startRejectsQuizzesOutsidePublishedAvailabilityWindowWithoutEnrollmentLookup() {
        when(db.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.startOrResume(25L, 5L));

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(db).queryForList(sql.capture(), any(Object[].class));
        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertTrue(sql.getValue().contains("FROM quizzes WHERE quiz_id=?"));
        assertTrue(sql.getValue().contains("available_from<=NOW()"));
        assertTrue(sql.getValue().contains("available_until>=NOW()"));
        assertFalse(sql.getValue().contains("subject_enrollments"));
    }

    @Test
    void startRejectsUnavailableNewAttemptButStillAllowsExistingAttemptToResume() {
        Map<String, Object> unavailableQuiz = Map.of("quiz_id", 5L, "status", "PUBLISHED",
                "available_now", false, "not_expired", true);
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FROM quizzes WHERE quiz_id")) return List.of(unavailableQuiz);
            return List.of();
        });

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.startOrResume(25L, 5L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(db, org.mockito.Mockito.never()).queryForObject(contains("COUNT(*) FROM quiz_questions"),
                eq(Integer.class), any(Object[].class));
    }

    @Test
    void answerSavePersistsOnlyOptionValidatedForAttemptQuestion() {
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FOR UPDATE")) return List.of(activeAttempt());
            if (sql.contains("SELECT b.question_type")) return List.of(Map.of("question_type", "SINGLE_CHOICE"));
            if (sql.contains("option_id IN")) return List.of(Map.of("option_id", 13L));
            return List.of();
        });

        Map<String, Object> saved = service.saveAnswer(25L, 100L, 7L, 13L);

        assertEquals(true, saved.get("saved"));
        verify(db).update(contains("INSERT INTO quiz_answers"), eq(100L), eq(7L), eq(13L), isNull());
    }

    @Test
    void multipleChoiceAnswerPersistsAllValidatedOptionsInExistingAnswerTextColumn() {
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FOR UPDATE")) return List.of(activeAttempt());
            if (sql.contains("SELECT b.question_type")) return List.of(Map.of("question_type", "MULTIPLE_CHOICE"));
            if (sql.contains("option_id IN")) return List.of(Map.of("option_id", 13L), Map.of("option_id", 14L));
            return List.of();
        });

        service.saveAnswer(25L, 100L, 7L, List.of(13L, 14L));

        verify(db).update(contains("INSERT INTO quiz_answers"),
                eq(100L), eq(7L), eq(13L), eq("[13,14]"));
    }

    @Test
    void answerSaveRejectsOptionFromAnotherQuestion() {
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FOR UPDATE")) return List.of(activeAttempt());
            if (sql.contains("SELECT b.question_type")) return List.of(Map.of("question_type", "SINGLE_CHOICE"));
            return List.of();
        });

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.saveAnswer(25L, 100L, 7L, 99L));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void answerSaveRejectsMultipleOptionsForSingleChoiceQuestion() {
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FOR UPDATE")) return List.of(activeAttempt());
            if (sql.contains("SELECT b.question_type")) return List.of(Map.of("question_type", "SINGLE_CHOICE"));
            return List.of();
        });

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.saveAnswer(25L, 100L, 7L, List.of(13L, 14L)));

        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
    }

    @Test
    void expiredAnswerSaveFinalizesInsteadOfWritingTheLateAnswer() {
        Map<String, Object> expired = new java.util.HashMap<>(activeAttempt());
        expired.put("expired", true);
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FOR UPDATE")) return List.of(expired);
            if (sql.contains("SELECT qq.question_id, qq.points, b.question_type")) return List.of();
            return List.of();
        });

        Map<String, Object> response = service.saveAnswer(25L, 100L, 7L, 13L);

        assertEquals(true, response.get("expired"));
        verify(db).update(contains("UPDATE quiz_attempts SET status='SUBMITTED'"),
                eq(BigDecimal.ZERO), eq(100L), eq(25L));
    }

    @Test
    void submitIsIdempotentForAnAlreadySubmittedAttempt() {
        Map<String, Object> submitted = new java.util.HashMap<>(activeAttempt());
        submitted.put("status", "SUBMITTED");
        when(db.queryForList(anyString(), any(Object[].class))).thenReturn(List.of(submitted));

        Map<String, Object> response = service.submit(25L, 100L);

        assertEquals(true, response.get("alreadySubmitted"));
        assertEquals("SUBMITTED", response.get("status"));
    }

    @Test
    void submitScoresCorrectAnswerOnceAndStoresAwardedPoints() {
        Map<String, Object> scoreRow = Map.of(
                "question_id", 7L, "points", new BigDecimal("2.50"), "question_type", "SINGLE_CHOICE",
                "selected_option_id", 13L, "option_id", 13L, "is_correct", true);
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FOR UPDATE")) return List.of(activeAttempt());
            if (sql.contains("SELECT qq.question_id, qq.points, b.question_type")) return List.of(scoreRow);
            return List.of();
        });

        Map<String, Object> response = service.submit(25L, 100L);

        assertEquals("SUBMITTED", response.get("status"));
        verify(db).update(contains("INSERT INTO quiz_answers"),
                eq(100L), eq(7L), eq(13L), isNull(), eq(new BigDecimal("2.50")));
        verify(db).update(contains("UPDATE quiz_attempts SET status='SUBMITTED'"),
                eq(new BigDecimal("2.50")), eq(100L), eq(25L));
    }

    @Test
    void submitAwardsMultipleChoiceMarksOnlyWhenEveryCorrectOptionIsSelected() {
        Map<String, Object> firstOption = Map.of(
                "question_id", 7L, "points", new BigDecimal("2.00"), "question_type", "MULTIPLE_CHOICE",
                "selected_option_id", 13L, "answer_text", "[13,14]", "option_id", 13L, "is_correct", true);
        Map<String, Object> secondOption = Map.of(
                "question_id", 7L, "points", new BigDecimal("2.00"), "question_type", "MULTIPLE_CHOICE",
                "selected_option_id", 13L, "answer_text", "[13,14]", "option_id", 14L, "is_correct", true);
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("FOR UPDATE")) return List.of(activeAttempt());
            if (sql.contains("SELECT qq.question_id, qq.points, b.question_type")) return List.of(firstOption, secondOption);
            return List.of();
        });

        service.submit(25L, 100L);

        verify(db).update(contains("INSERT INTO quiz_answers"),
                eq(100L), eq(7L), eq(13L), eq("[13,14]"), eq(new BigDecimal("2.00")));
        verify(db).update(contains("UPDATE quiz_attempts SET status='SUBMITTED'"),
                eq(new BigDecimal("2.00")), eq(100L), eq(25L));
    }

    @Test
    void attemptQuestionsDoNotContainCorrectAnswerFlags() {
        Map<String, Object> summary = new java.util.HashMap<>(activeAttempt());
        summary.put("title", "Safe quiz");
        summary.put("subject_name", "Science");
        summary.put("time_limit_minutes", null);
        summary.put("remaining_seconds", null);
        Map<String, Object> question = Map.of(
                "question_id", 7L, "question_text", "Question", "question_type", "SINGLE_CHOICE",
                "points", BigDecimal.ONE, "position", 1, "selected_option_id", 13L,
                "option_id", 13L, "option_text", "Answer", "option_position", 1);
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("server_now")) return List.of(summary);
            if (sql.contains("LEFT JOIN quiz_answers")) return List.of(question);
            return List.of();
        });

        Map<String, Object> attempt = service.attempt(25L, 100L);

        assertFalse(attempt.containsKey("correct"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) attempt.get("questions");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) questions.getFirst().get("options");
        assertFalse(options.getFirst().containsKey("is_correct"));
        assertFalse(options.getFirst().containsKey("correct"));
    }

    @Test
    void attemptMapsBankQuestionTypePointsAndOptionsWithoutCorrectFlags() {
        Map<String, Object> summary = new java.util.HashMap<>(activeAttempt());
        summary.put("title", "Multiple answer quiz");
        Map<String, Object> optionRow = Map.of(
                "question_id", 7L, "question_text", "Choose all correct answers",
                "question_type", "MULTIPLE_CHOICE", "points", new BigDecimal("3.00"), "position", 1,
                "selected_option_id", 13L, "answer_text", "[13,14]",
                "option_id", 14L, "option_text", "Second choice", "option_position", 2);
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("server_now")) return List.of(summary);
            if (sql.contains("LEFT JOIN quiz_answers")) return List.of(optionRow);
            return List.of();
        });

        Map<String, Object> attempt = service.attempt(25L, 100L);

        @SuppressWarnings("unchecked")
        Map<String, Object> question = ((List<Map<String, Object>>) attempt.get("questions")).getFirst();
        assertEquals("MULTIPLE_CHOICE", question.get("questionType"));
        assertEquals(new BigDecimal("3.00"), question.get("points"));
        assertEquals(List.of(13L, 14L), question.get("selectedOptionIds"));
        @SuppressWarnings("unchecked")
        Map<String, Object> option = ((List<Map<String, Object>>) question.get("options")).getFirst();
        assertEquals(14L, option.get("optionId"));
        assertEquals("Second choice", option.get("text"));
        assertFalse(option.containsKey("is_correct"));
    }

    @Test
    void submittedResultIncludesSelectedAndCorrectAnswersAndScoreCounts() {
        Map<String, Object> summary = new java.util.HashMap<>(activeAttempt());
        summary.put("status", "SUBMITTED");
        summary.put("title", "Review quiz");
        summary.put("subject_name", "Science");
        summary.put("score", new BigDecimal("2.00"));
        summary.put("submitted_at", Timestamp.valueOf("2026-10-28 10:00:00"));
        Map<String, Object> first = Map.of(
                "question_id", 7L, "position", 1, "points", new BigDecimal("2.00"),
                "question_text", "Choose both", "selected_option_id", 13L, "answer_text", "[13,14]",
                "awarded_points", new BigDecimal("2.00"), "option_id", 13L, "option_text", "A", "is_correct", true);
        Map<String, Object> second = Map.of(
                "question_id", 7L, "position", 1, "points", new BigDecimal("2.00"),
                "question_text", "Choose both", "selected_option_id", 13L, "answer_text", "[13,14]",
                "awarded_points", new BigDecimal("2.00"), "option_id", 14L, "option_text", "B", "is_correct", true);
        when(db.queryForList(anyString(), any(Object[].class))).thenAnswer(invocation -> {
            String sql = invocation.getArgument(0);
            if (sql.contains("server_now")) return List.of(summary);
            if (sql.contains("bo.is_correct")) return List.of(first, second);
            return List.of();
        });

        Map<String, Object> result = service.result(25L, 100L);

        assertEquals(1, result.get("correctCount"));
        assertEquals(0, result.get("incorrectCount"));
        assertEquals(new BigDecimal("2.00"), result.get("score"));
        @SuppressWarnings("unchecked")
        Map<String, Object> question = ((List<Map<String, Object>>) result.get("questions")).getFirst();
        assertEquals("A, B", question.get("selected_answer"));
        assertEquals("A, B", question.get("correct_answer"));
        assertEquals("CORRECT", question.get("outcome"));
    }

    @Test
    void attemptLookupDoesNotRevealAnotherStudentsAttempt() {
        when(db.queryForList(anyString(), any(Object[].class))).thenReturn(List.of());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.attempt(999L, 100L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    private Map<String, Object> activeAttempt() {
        return Map.of("attempt_id", 100L, "quiz_id", 5L, "status", "IN_PROGRESS", "expired", false,
                "started_at", Timestamp.valueOf(LocalDateTime.of(2026, 10, 1, 10, 0)));
    }
}
