-- OpenStudy V2 feature migration
-- Target: MySQL 8.0+ / Aiven MySQL
-- Safety: creates new tables only. Does NOT drop, rename, or alter existing
-- `students` or `subjects` tables and does not modify their rows.
--
-- Review and test on a restored backup before applying to the live database.
-- This file intentionally does not enable Flyway or modify application config.

USE openstudy_db;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    full_name VARCHAR(150) NOT NULL,
    email VARCHAR(254) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('STUDENT', 'TEACHER') NOT NULL,
    account_status ENUM('ACTIVE', 'PENDING', 'SUSPENDED') NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    UNIQUE KEY uq_users_email (email),
    KEY idx_users_role_status (role, account_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Existing subjects.subject_id is INT (signed); match its type exactly.
CREATE TABLE IF NOT EXISTS subject_teachers (
    subject_id INT NOT NULL,
    teacher_id BIGINT UNSIGNED NOT NULL,
    assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (subject_id, teacher_id),
    KEY idx_subject_teachers_teacher (teacher_id),
    CONSTRAINT fk_subject_teachers_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(subject_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_subject_teachers_user
        FOREIGN KEY (teacher_id) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS subject_enrollments (
    subject_id INT NOT NULL,
    student_user_id BIGINT UNSIGNED NOT NULL,
    enrolled_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enrollment_status ENUM('ENROLLED', 'COMPLETED', 'WITHDRAWN') NOT NULL DEFAULT 'ENROLLED',
    PRIMARY KEY (subject_id, student_user_id),
    KEY idx_enrollments_student_status (student_user_id, enrollment_status),
    CONSTRAINT fk_enrollments_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(subject_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_enrollments_student
        FOREIGN KEY (student_user_id) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS study_resources (
    resource_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    subject_id INT NOT NULL,
    uploaded_by BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NULL,
    resource_type ENUM('PDF', 'VIDEO', 'LINK', 'DOCUMENT', 'OTHER') NOT NULL DEFAULT 'OTHER',
    resource_url VARCHAR(2048) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (resource_id),
    KEY idx_resources_subject_created (subject_id, created_at),
    KEY idx_resources_uploader (uploaded_by),
    CONSTRAINT fk_resources_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(subject_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_resources_uploader
        FOREIGN KEY (uploaded_by) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS study_sessions (
    session_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    student_user_id BIGINT UNSIGNED NOT NULL,
    subject_id INT NULL,
    activity_type ENUM('READING', 'VIDEO', 'QUIZ', 'ASSIGNMENT', 'GENERAL') NOT NULL DEFAULT 'GENERAL',
    started_at DATETIME NOT NULL,
    ended_at DATETIME NULL,
    duration_seconds INT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (session_id),
    KEY idx_sessions_student_started (student_user_id, started_at),
    KEY idx_sessions_subject_started (subject_id, started_at),
    CONSTRAINT fk_sessions_student
        FOREIGN KEY (student_user_id) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_sessions_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(subject_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_sessions_time CHECK (ended_at IS NULL OR ended_at >= started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS resource_progress (
    student_user_id BIGINT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    status ENUM('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED') NOT NULL DEFAULT 'NOT_STARTED',
    progress_percent DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    last_accessed_at DATETIME NULL,
    completed_at DATETIME NULL,
    PRIMARY KEY (student_user_id, resource_id),
    KEY idx_resource_progress_resource (resource_id),
    CONSTRAINT fk_resource_progress_student
        FOREIGN KEY (student_user_id) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_resource_progress_resource
        FOREIGN KEY (resource_id) REFERENCES study_resources(resource_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_resource_progress_percent CHECK (progress_percent >= 0 AND progress_percent <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS assignments (
    assignment_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    subject_id INT NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    instructions TEXT NULL,
    due_at DATETIME NULL,
    max_score DECIMAL(8,2) NOT NULL DEFAULT 100.00,
    status ENUM('DRAFT', 'PUBLISHED', 'CLOSED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (assignment_id),
    KEY idx_assignments_subject_status_due (subject_id, status, due_at),
    KEY idx_assignments_creator (created_by),
    CONSTRAINT fk_assignments_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(subject_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_assignments_creator
        FOREIGN KEY (created_by) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_assignments_max_score CHECK (max_score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS assignment_submissions (
    submission_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    assignment_id BIGINT UNSIGNED NOT NULL,
    student_user_id BIGINT UNSIGNED NOT NULL,
    submission_text MEDIUMTEXT NULL,
    submission_url VARCHAR(2048) NULL,
    submitted_at DATETIME NULL,
    score DECIMAL(8,2) NULL,
    feedback TEXT NULL,
    graded_by BIGINT UNSIGNED NULL,
    graded_at DATETIME NULL,
    status ENUM('DRAFT', 'SUBMITTED', 'GRADED', 'RETURNED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (submission_id),
    UNIQUE KEY uq_assignment_submission_student (assignment_id, student_user_id),
    KEY idx_submissions_student_status (student_user_id, status),
    KEY idx_submissions_grader (graded_by),
    CONSTRAINT fk_submissions_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignments(assignment_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_submissions_student
        FOREIGN KEY (student_user_id) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_submissions_grader
        FOREIGN KEY (graded_by) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_submissions_score CHECK (score IS NULL OR score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS quizzes (
    quiz_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    subject_id INT NOT NULL,
    created_by BIGINT UNSIGNED NOT NULL,
    title VARCHAR(200) NOT NULL,
    instructions TEXT NULL,
    available_from DATETIME NULL,
    available_until DATETIME NULL,
    time_limit_minutes SMALLINT UNSIGNED NULL,
    status ENUM('DRAFT', 'PUBLISHED', 'CLOSED') NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (quiz_id),
    KEY idx_quizzes_subject_status (subject_id, status),
    KEY idx_quizzes_creator (created_by),
    CONSTRAINT fk_quizzes_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(subject_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_quizzes_creator
        FOREIGN KEY (created_by) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_quizzes_availability CHECK (
        available_until IS NULL OR available_from IS NULL OR available_until >= available_from
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS quiz_questions (
    question_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    quiz_id BIGINT UNSIGNED NOT NULL,
    question_text TEXT NOT NULL,
    question_type ENUM('SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'SHORT_TEXT') NOT NULL DEFAULT 'SINGLE_CHOICE',
    points DECIMAL(8,2) NOT NULL DEFAULT 1.00,
    position INT UNSIGNED NOT NULL,
    PRIMARY KEY (question_id),
    UNIQUE KEY uq_quiz_question_position (quiz_id, position),
    KEY idx_questions_quiz (quiz_id),
    CONSTRAINT fk_questions_quiz
        FOREIGN KEY (quiz_id) REFERENCES quizzes(quiz_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_questions_points CHECK (points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS quiz_options (
    option_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    question_id BIGINT UNSIGNED NOT NULL,
    option_text TEXT NOT NULL,
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    position INT UNSIGNED NOT NULL,
    PRIMARY KEY (option_id),
    UNIQUE KEY uq_question_option_position (question_id, position),
    KEY idx_options_question (question_id),
    CONSTRAINT fk_options_question
        FOREIGN KEY (question_id) REFERENCES quiz_questions(question_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS quiz_attempts (
    attempt_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    quiz_id BIGINT UNSIGNED NOT NULL,
    student_user_id BIGINT UNSIGNED NOT NULL,
    attempt_number SMALLINT UNSIGNED NOT NULL DEFAULT 1,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at DATETIME NULL,
    score DECIMAL(8,2) NULL,
    status ENUM('IN_PROGRESS', 'SUBMITTED', 'GRADED', 'ABANDONED') NOT NULL DEFAULT 'IN_PROGRESS',
    PRIMARY KEY (attempt_id),
    UNIQUE KEY uq_quiz_attempt_number (quiz_id, student_user_id, attempt_number),
    KEY idx_attempts_student_status (student_user_id, status),
    KEY idx_attempts_quiz_status (quiz_id, status),
    CONSTRAINT fk_attempts_quiz
        FOREIGN KEY (quiz_id) REFERENCES quizzes(quiz_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_attempts_student
        FOREIGN KEY (student_user_id) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_attempts_score CHECK (score IS NULL OR score >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS quiz_answers (
    answer_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    attempt_id BIGINT UNSIGNED NOT NULL,
    question_id BIGINT UNSIGNED NOT NULL,
    selected_option_id BIGINT UNSIGNED NULL,
    answer_text TEXT NULL,
    awarded_points DECIMAL(8,2) NULL,
    answered_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (answer_id),
    UNIQUE KEY uq_attempt_question_answer (attempt_id, question_id),
    KEY idx_answers_question (question_id),
    KEY idx_answers_selected_option (selected_option_id),
    CONSTRAINT fk_answers_attempt
        FOREIGN KEY (attempt_id) REFERENCES quiz_attempts(attempt_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_answers_question
        FOREIGN KEY (question_id) REFERENCES quiz_questions(question_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_answers_option
        FOREIGN KEY (selected_option_id) REFERENCES quiz_options(option_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_answers_awarded_points CHECK (awarded_points IS NULL OR awarded_points >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS announcements (
    announcement_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    created_by BIGINT UNSIGNED NOT NULL,
    subject_id INT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    audience ENUM('ALL', 'STUDENTS', 'SUBJECT') NOT NULL DEFAULT 'ALL',
    published_at DATETIME NULL,
    expires_at DATETIME NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (announcement_id),
    KEY idx_announcements_audience_published (audience, published_at),
    KEY idx_announcements_subject (subject_id),
    KEY idx_announcements_creator (created_by),
    CONSTRAINT fk_announcements_creator
        FOREIGN KEY (created_by) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_announcements_subject
        FOREIGN KEY (subject_id) REFERENCES subjects(subject_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT chk_announcements_dates CHECK (
        expires_at IS NULL OR published_at IS NULL OR expires_at >= published_at
    ),
    CONSTRAINT chk_announcements_subject_audience CHECK (
        (audience = 'SUBJECT' AND subject_id IS NOT NULL)
        OR (audience <> 'SUBJECT')
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ai_interactions (
    interaction_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    feature_type ENUM('STUDY_HELP', 'SUMMARY', 'QUIZ_HELP', 'OTHER') NOT NULL DEFAULT 'OTHER',
    provider VARCHAR(80) NULL,
    model_name VARCHAR(120) NULL,
    request_text MEDIUMTEXT NULL,
    response_text MEDIUMTEXT NULL,
    status ENUM('SUCCESS', 'FAILED', 'BLOCKED') NOT NULL DEFAULT 'SUCCESS',
    input_tokens INT UNSIGNED NULL,
    output_tokens INT UNSIGNED NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (interaction_id),
    KEY idx_ai_interactions_user_created (user_id, created_at),
    KEY idx_ai_interactions_status_created (status, created_at),
    CONSTRAINT fk_ai_interactions_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Analytics can initially be computed directly from study_sessions,
-- assignments/submissions and quiz_attempts. Add snapshots only if needed later.
-- Application must enforce that role=STUDENT for student activity tables and
-- role=TEACHER for created_by/graded_by; SQL FKs validate user existence, not role.
-- Application must also validate that quiz_answers.question_id belongs to the
-- attempt's quiz and selected_option_id belongs to that question.
