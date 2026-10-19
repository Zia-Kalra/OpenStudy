package com.openstudy.openstudy.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openstudy.openstudy.dto.QuestionBankOptionRequest;
import com.openstudy.openstudy.dto.QuestionBankQuestionRequest;
import com.openstudy.openstudy.dto.QuestionBankQuestionResponse;
import com.openstudy.openstudy.repository.QuestionBankRepository;

@Service
public class QuestionBankService {

    private final QuestionBankRepository repository;

    public QuestionBankService(QuestionBankRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<QuestionBankQuestionResponse> list(String authenticatedEmail) {
        long teacherId = repository.findActiveTeacherId(authenticatedEmail);
        return repository.findQuestions(teacherId);
    }

    @Transactional
    public long createManual(String authenticatedEmail, QuestionBankQuestionRequest request) {
        long teacherId = repository.findActiveTeacherId(authenticatedEmail);
        long correctAnswers = request.options().stream().filter(QuestionBankOptionRequest::correct).count();
        if (correctAnswers == 0 || (request.questionType() == QuestionBankQuestionRequest.QuestionType.SINGLE_CHOICE
                && correctAnswers != 1)) {
            throw new InvalidQuestionException("Select exactly one correct option for a single-choice question; "
                    + "select at least one for a multiple-choice question");
        }

        long bankQuestionId = repository.insertQuestion(teacherId, request);
        repository.insertOptions(bankQuestionId, request.options());
        return bankQuestionId;
    }

    public static class InvalidQuestionException extends RuntimeException {
        public InvalidQuestionException(String message) {
            super(message);
        }
    }
}