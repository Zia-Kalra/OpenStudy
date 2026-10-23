-- Store each student's files against the answer they belong to.
-- No attachment-count limit is imposed; size is validated per uploaded file.
CREATE TABLE IF NOT EXISTS assignment_answer_attachments (
    attachment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    answer_id BIGINT UNSIGNED NOT NULL,
    original_name VARCHAR(180) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT UNSIGNED NOT NULL,
    file_data MEDIUMBLOB NOT NULL,
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (attachment_id),
    KEY idx_answer_attachments_answer (answer_id),
    CONSTRAINT fk_answer_attachments_answer
        FOREIGN KEY (answer_id) REFERENCES assignment_answers(answer_id)
        ON UPDATE RESTRICT ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
