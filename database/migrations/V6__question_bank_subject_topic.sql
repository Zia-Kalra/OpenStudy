-- Associate reusable question bank records with their teacher's existing subject.
-- Quiz and quiz attempt references remain unchanged; legacy rows stay unassigned.
ALTER TABLE question_bank_questions
    ADD COLUMN subject_id INT NULL AFTER created_by,
    ADD COLUMN topic VARCHAR(200) NOT NULL DEFAULT '' AFTER approval_status,
    ADD KEY idx_bank_questions_subject_topic (subject_id, topic),
    ADD CONSTRAINT fk_bank_questions_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(subject_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT;