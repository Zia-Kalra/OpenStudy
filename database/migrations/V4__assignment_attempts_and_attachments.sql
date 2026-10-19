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
