-- Reusable quiz question bank migration.
-- Apply after V2__openstudy_features.sql and V3__assignment_questions_and_answers.sql.
-- Fresh installations must first apply schema.sql, V2, V3, then this migration.
-- This migration does not connect to or apply itself to any database.
--
-- Existing question_id and option_id values remain the quiz-association identifiers,
-- so quiz_answers and historical attempts retain their foreign-key relationships.
-- Existing question/option content is copied to canonical bank tables using those
-- same numeric IDs before redundant content columns are removed. Existing items are
-- kept distinct; legacy data has no reliable provenance for content deduplication.

CREATE TABLE IF NOT EXISTS question_bank_questions (
    bank_question_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    created_by BIGINT UNSIGNED NOT NULL,
    question_text TEXT NOT NULL,
    question_type ENUM('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SHORT_TEXT') NOT NULL DEFAULT 'SINGLE_CHOICE',
    points DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    difficulty ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL DEFAULT 'MEDIUM',
    source ENUM('MANUAL', 'AI') NOT NULL DEFAULT 'MANUAL',
    approval_status ENUM('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (bank_question_id),
    KEY idx_bank_questions_owner_status (created_by, approval_status, created_at),
    KEY idx_bank_questions_status_difficulty (approval_status, difficulty),
    CONSTRAINT fk_bank_questions_creator
        FOREIGN KEY (created_by) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_bank_questions_points CHECK (points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS question_bank_options (
    bank_option_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    bank_question_id BIGINT UNSIGNED NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    position INT UNSIGNED NOT NULL,
    PRIMARY KEY (bank_option_id),
    UNIQUE KEY uq_bank_option_position (bank_question_id, position),
    UNIQUE KEY uq_bank_question_option (bank_question_id, bank_option_id),
    CONSTRAINT fk_bank_options_question
        FOREIGN KEY (bank_question_id) REFERENCES question_bank_questions(bank_question_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Preserve legacy IDs so existing quiz and answer rows keep their identities.
INSERT IGNORE INTO question_bank_questions (
    bank_question_id, created_by, question_text, question_type, points,
    difficulty, source, approval_status
)
SELECT
    question.question_id,
    quiz.created_by,
    question.question_text,
    question.question_type,
    question.points,
    'MEDIUM',
    'MANUAL',
    'APPROVED'
FROM quiz_questions question
JOIN quizzes quiz ON quiz.quiz_id = question.quiz_id;

INSERT IGNORE INTO question_bank_options (
    bank_option_id, bank_question_id, option_text, is_correct, position
)
SELECT
    option_row.option_id,
    question.question_id,
    option_row.option_text,
    option_row.is_correct,
    option_row.position
FROM quiz_options option_row
JOIN quiz_questions question ON question.question_id = option_row.question_id;

-- Keep the existing quiz question IDs as association IDs and add their canonical bank link.
ALTER TABLE quiz_questions
    ADD COLUMN bank_question_id BIGINT UNSIGNED NULL AFTER question_id;

UPDATE quiz_questions
SET bank_question_id = question_id;

ALTER TABLE quiz_questions
    MODIFY COLUMN bank_question_id BIGINT UNSIGNED NOT NULL,
    ADD UNIQUE KEY uq_quiz_question_bank_link (question_id, bank_question_id),
    ADD KEY idx_quiz_questions_bank (bank_question_id),
    ADD CONSTRAINT fk_quiz_questions_bank
        FOREIGN KEY (bank_question_id) REFERENCES question_bank_questions(bank_question_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT;

-- Keep the existing option IDs as quiz-option association IDs. Composite foreign keys
-- prevent attaching an option from a different bank question to this quiz question.
ALTER TABLE quiz_options
    ADD COLUMN bank_question_id BIGINT UNSIGNED NULL AFTER question_id,
    ADD COLUMN bank_option_id BIGINT UNSIGNED NULL AFTER bank_question_id;

UPDATE quiz_options option_row
JOIN quiz_questions question ON question.question_id = option_row.question_id
SET option_row.bank_question_id = question.bank_question_id,
    option_row.bank_option_id = option_row.option_id;

ALTER TABLE quiz_options
    MODIFY COLUMN bank_question_id BIGINT UNSIGNED NOT NULL,
    MODIFY COLUMN bank_option_id BIGINT UNSIGNED NOT NULL,
    ADD KEY idx_quiz_options_bank_option (bank_question_id, bank_option_id),
    ADD CONSTRAINT fk_quiz_options_question_link
        FOREIGN KEY (question_id, bank_question_id)
        REFERENCES quiz_questions(question_id, bank_question_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    ADD CONSTRAINT fk_quiz_options_bank_option
        FOREIGN KEY (bank_question_id, bank_option_id)
        REFERENCES question_bank_options(bank_question_id, bank_option_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT;

-- Canonical question/option content and correctness now live only in the bank tables.
-- Quiz ordering stays on the association rows; attempt/answer IDs and FKs are unchanged.


-- Assignment attempt timing and safely stored attachment metadata.
-- V2/V3 already own assignment, submission, question and answer records.
ALTER TABLE assignments
    ADD COLUMN attempt_duration_minutes SMALLINT UNSIGNED NOT NULL DEFAULT 60
        COMMENT 'Attempt timer duration; independent from due_at';

ALTER TABLE assignment_submissions
    ADD COLUMN attempt_started_at DATETIME NULL,
    ADD COLUMN attempt_expires_at DATETIME NULL,
    ADD COLUMN evaluation_status ENUM('PENDING', 'PROCESSING', 'EVALUATED', 'FAILED')
        NOT NULL DEFAULT 'PENDING';

CREATE TABLE assignment_attachments (
    attachment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    submission_id BIGINT UNSIGNED NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT UNSIGNED NOT NULL,
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attachment_id),
    KEY idx_assignment_attachments_submission (submission_id),
    CONSTRAINT fk_assignment_attachments_submission
        FOREIGN KEY (submission_id) REFERENCES assignment_submissions(submission_id)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE subjects
    MODIFY COLUMN student_id BIGINT NULL;