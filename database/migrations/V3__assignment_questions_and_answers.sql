
CREATE TABLE IF NOT EXISTS assignment_questions (
    question_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT UNSIGNED NOT NULL,
    question_text TEXT NOT NULL,
    question_type ENUM('SHORT_TEXT', 'LONG_TEXT') NOT NULL,
    model_answer TEXT NULL,
    rubric TEXT NULL,
    points DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    position INT UNSIGNED NOT NULL,
    PRIMARY KEY (question_id),
    UNIQUE KEY uq_assignment_question_position (assignment_id, position),
    KEY idx_assignment_questions_assignment (assignment_id),
    CONSTRAINT fk_assignment_questions_assignment
        FOREIGN KEY (assignment_id)
        REFERENCES assignments(assignment_id)
        ON UPDATE RESTRICT
        ON DELETE CASCADE,
    CONSTRAINT chk_assignment_questions_points CHECK (points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS assignment_answers (
    answer_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    submission_id BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    answer_text MEDIUMTEXT NULL,
    ai_score DECIMAL(8,2) NULL,
    ai_feedback TEXT NULL,
    final_score DECIMAL(8,2) NULL,
    final_feedback TEXT NULL,
    evaluated_by BIGINT UNSIGNED NULL,
    evaluated_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (answer_id),
    UNIQUE KEY uq_submission_question (submission_id, question_id),
    KEY idx_assignment_answers_submission (submission_id),
    KEY idx_assignment_answers_question (question_id),
    KEY idx_assignment_answers_evaluator (evaluated_by),
    CONSTRAINT fk_assignment_answers_submission
        FOREIGN KEY (submission_id)
        REFERENCES assignment_submissions(submission_id)
        ON UPDATE RESTRICT
        ON DELETE CASCADE,
    CONSTRAINT fk_assignment_answers_question
        FOREIGN KEY (question_id)
        REFERENCES assignment_questions(question_id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT fk_assignment_answers_evaluator
        FOREIGN KEY (evaluated_by)
        REFERENCES users(user_id)
        ON UPDATE RESTRICT
        ON DELETE RESTRICT,
    CONSTRAINT chk_assignment_answers_ai_score
        CHECK (ai_score IS NULL OR ai_score >= 0),
    CONSTRAINT chk_assignment_answers_final_score
        CHECK (final_score IS NULL OR final_score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
