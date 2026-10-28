package com.openstudy.openstudy.service;

import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import com.openstudy.openstudy.dto.QuestionBankQuestionRequest;
import com.openstudy.openstudy.dto.QuestionBankOptionRequest;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.openstudy.openstudy.dto.QuestionBankQuestionResponse;
import com.openstudy.openstudy.repository.QuestionBankRepository;

import jakarta.validation.Valid;

@Service
@Profile("!mock")
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
        validateSubject(teacherId, request);
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

    @Transactional
    public long createReviewedAi(String requireTeacherEmail, QuestionBankQuestionRequest request) {
        long teacherId = repository.findActiveTeacherId(requireTeacherEmail);
        validateSubject(teacherId, request);
        long correctAnswers = request.options().stream()
                .filter(QuestionBankOptionRequest::correct)
                .count();

        if (correctAnswers == 0
                || (request.questionType()
                        == QuestionBankQuestionRequest.QuestionType.SINGLE_CHOICE
                    && correctAnswers != 1)) {

            throw new InvalidQuestionException(
                    "A single-choice question must have exactly one correct option; "
                    + "a multiple-choice question must have at least one.");
        }

        long questionId =
                repository.insertReviewedAiQuestion(teacherId, request);

        repository.insertOptions(questionId, request.options());

        return questionId;
    }

    @Transactional public void update(String email, long id, QuestionBankQuestionRequest request) {
        long teacherId = repository.findActiveTeacherId(email);
        validateSubject(teacherId, request);
        validateCorrectOptions(request);
        repository.updateQuestion(teacherId, id, request);
    }

    @Transactional public void updateStatus(String email, long id, String status) {
        if (!List.of("DRAFT", "PENDING_REVIEW", "APPROVED", "REJECTED").contains(status)) throw new InvalidQuestionException("Invalid question status");
        repository.updateStatus(repository.findActiveTeacherId(email), id, status);
    }

    @Transactional public void delete(String email, long id) { repository.deleteQuestion(repository.findActiveTeacherId(email), id); }

    private void validateSubject(long teacherId, QuestionBankQuestionRequest request) {
        if (request.subjectId() == null || request.subjectId() < 1 || !repository.teacherOwnsSubject(teacherId, request.subjectId())) throw new InvalidQuestionException("Choose a subject assigned to your teacher account");
    }
    private void validateCorrectOptions(QuestionBankQuestionRequest request) {
        long correct = request.options().stream().filter(QuestionBankOptionRequest::correct).count();
        if (correct == 0 || request.questionType() == QuestionBankQuestionRequest.QuestionType.SINGLE_CHOICE && correct != 1) throw new InvalidQuestionException("Select exactly one correct option");
    }
}
