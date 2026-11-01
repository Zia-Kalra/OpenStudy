package com.openstudy.openstudy.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.openstudy.openstudy.dto.QuestionBankOptionRequest;
import com.openstudy.openstudy.dto.QuestionBankQuestionRequest;
import com.openstudy.openstudy.repository.QuestionBankRepository;

@ExtendWith(MockitoExtension.class)
class QuestionBankServiceTest {

    @Mock
    private QuestionBankRepository repository;

    @InjectMocks
    private QuestionBankService service;

    @Test
    void savesManualSingleChoiceQuestionAndItsOptions() {
        QuestionBankQuestionRequest request = new QuestionBankQuestionRequest(
                "Which is correct?",
                QuestionBankQuestionRequest.QuestionType.SINGLE_CHOICE,
                BigDecimal.ONE,
                QuestionBankQuestionRequest.Difficulty.EASY,
                List.of(
                        new QuestionBankOptionRequest("Correct", true),
                        new QuestionBankOptionRequest("Incorrect", false)), 1L, "Basics");
        when(repository.findActiveTeacherId("teacher@example.test")).thenReturn(12L);
        when(repository.teacherOwnsSubject(12L, 1L)).thenReturn(true);
        when(repository.insertQuestion(12L, request)).thenReturn(41L);

        long result = service.createManual("teacher@example.test", request);

        assertEquals(41L, result);
        verify(repository).insertOptions(41L, request.options());
    }

    @Test
    void rejectsSingleChoiceQuestionWithMultipleCorrectAnswers() {
        QuestionBankQuestionRequest request = new QuestionBankQuestionRequest(
                "Choose one",
                QuestionBankQuestionRequest.QuestionType.SINGLE_CHOICE,
                BigDecimal.ONE,
                QuestionBankQuestionRequest.Difficulty.MEDIUM,
                List.of(
                        new QuestionBankOptionRequest("First", true),
                        new QuestionBankOptionRequest("Second", true)));
        when(repository.findActiveTeacherId("teacher@example.test")).thenReturn(12L);

        assertThrows(QuestionBankService.InvalidQuestionException.class,
                () -> service.createManual("teacher@example.test", request));

        verify(repository, never()).insertQuestion(12L, request);
    }

    @Test
    void rejectsMultipleChoiceQuestionWithoutCorrectAnswer() {
        QuestionBankQuestionRequest request = new QuestionBankQuestionRequest(
                "Choose all that apply",
                QuestionBankQuestionRequest.QuestionType.MULTIPLE_CHOICE,
                BigDecimal.ONE,
                QuestionBankQuestionRequest.Difficulty.HARD,
                List.of(
                        new QuestionBankOptionRequest("First", false),
                        new QuestionBankOptionRequest("Second", false)));
        when(repository.findActiveTeacherId("teacher@example.test")).thenReturn(12L);

        assertThrows(QuestionBankService.InvalidQuestionException.class,
                () -> service.createManual("teacher@example.test", request));

        verify(repository, never()).insertQuestion(12L, request);
    }
}
