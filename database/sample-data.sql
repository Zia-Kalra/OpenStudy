-- OpenStudy sample data (safe to review; run ONCE against a schema that has V2-V6 applied).
-- Everything here is tied to users with an @sample.openstudy email or subjects whose description is
-- 'SAMPLE-DATA', so database/sample-data-remove.sql can delete exactly these rows.
-- Logins (password for all: Sample@123):
--   teacher  : teacher@sample.openstudy
--   students : aarav@, diya@, kabir@, meera@, rohan@ sample.openstudy
START TRANSACTION;

INSERT INTO users (full_name, email, password_hash, role, account_status) VALUES
  ('Dr. Sample Teacher', 'teacher@sample.openstudy', '$2y$10$gIoWO/Uz1Btqvy6B9iY.WuHM0iT8b89bAntVvTuRoD2SQ5Xhu5RKy', 'TEACHER', 'ACTIVE'),
  ('Aarav Sharma', 'aarav@sample.openstudy', '$2y$10$gIoWO/Uz1Btqvy6B9iY.WuHM0iT8b89bAntVvTuRoD2SQ5Xhu5RKy', 'STUDENT', 'ACTIVE'),
  ('Diya Patel', 'diya@sample.openstudy', '$2y$10$gIoWO/Uz1Btqvy6B9iY.WuHM0iT8b89bAntVvTuRoD2SQ5Xhu5RKy', 'STUDENT', 'ACTIVE'),
  ('Kabir Singh', 'kabir@sample.openstudy', '$2y$10$gIoWO/Uz1Btqvy6B9iY.WuHM0iT8b89bAntVvTuRoD2SQ5Xhu5RKy', 'STUDENT', 'ACTIVE'),
  ('Meera Iyer', 'meera@sample.openstudy', '$2y$10$gIoWO/Uz1Btqvy6B9iY.WuHM0iT8b89bAntVvTuRoD2SQ5Xhu5RKy', 'STUDENT', 'ACTIVE'),
  ('Rohan Gupta', 'rohan@sample.openstudy', '$2y$10$gIoWO/Uz1Btqvy6B9iY.WuHM0iT8b89bAntVvTuRoD2SQ5Xhu5RKy', 'STUDENT', 'ACTIVE');
SET @teacher := (SELECT user_id FROM users WHERE email='teacher@sample.openstudy');
SET @aarav := (SELECT user_id FROM users WHERE email='aarav@sample.openstudy');
SET @diya := (SELECT user_id FROM users WHERE email='diya@sample.openstudy');
SET @kabir := (SELECT user_id FROM users WHERE email='kabir@sample.openstudy');
SET @meera := (SELECT user_id FROM users WHERE email='meera@sample.openstudy');
SET @rohan := (SELECT user_id FROM users WHERE email='rohan@sample.openstudy');

INSERT INTO subjects (subject_name, description) VALUES ('Java Programming','SAMPLE-DATA'), ('Database Systems','SAMPLE-DATA');
SET @java := (SELECT subject_id FROM subjects WHERE subject_name='Java Programming' AND description='SAMPLE-DATA' ORDER BY subject_id DESC LIMIT 1);
SET @db := (SELECT subject_id FROM subjects WHERE subject_name='Database Systems' AND description='SAMPLE-DATA' ORDER BY subject_id DESC LIMIT 1);
INSERT INTO subject_teachers (subject_id, teacher_id) VALUES (@java,@teacher),(@db,@teacher);
INSERT INTO subject_enrollments (subject_id, student_user_id) VALUES
  (@java,@aarav),
  (@java,@diya),
  (@java,@kabir),
  (@java,@meera),
  (@java,@rohan),
  (@db,@aarav),
  (@db,@diya),
  (@db,@kabir),
  (@db,@meera),
  (@db,@rohan);

INSERT INTO question_bank_questions (created_by, subject_id, question_text, question_type, points, difficulty, source, approval_status, topic) VALUES (@teacher, @java, 'Which keyword is used to handle an exception in Java?', 'SINGLE_CHOICE', 1.00, 'EASY', 'MANUAL', 'APPROVED', 'Exception Handling');
SET @bq1 := LAST_INSERT_ID();
INSERT INTO question_bank_options (bank_question_id, option_text, is_correct, position) VALUES
  (@bq1, 'try-catch', 1, 1),
  (@bq1, 'throw-only', 0, 2),
  (@bq1, 'finally-only', 0, 3),
  (@bq1, 'assert', 0, 4);
SET @bo1_1 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq1 AND position=1);
SET @bo1_2 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq1 AND position=2);
SET @bo1_3 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq1 AND position=3);
SET @bo1_4 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq1 AND position=4);
INSERT INTO question_bank_questions (created_by, subject_id, question_text, question_type, points, difficulty, source, approval_status, topic) VALUES (@teacher, @java, 'Which collection does not allow duplicate elements?', 'SINGLE_CHOICE', 1.00, 'MEDIUM', 'MANUAL', 'APPROVED', 'Collections');
SET @bq2 := LAST_INSERT_ID();
INSERT INTO question_bank_options (bank_question_id, option_text, is_correct, position) VALUES
  (@bq2, 'ArrayList', 0, 1),
  (@bq2, 'LinkedList', 0, 2),
  (@bq2, 'HashSet', 1, 3),
  (@bq2, 'Vector', 0, 4);
SET @bo2_1 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq2 AND position=1);
SET @bo2_2 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq2 AND position=2);
SET @bo2_3 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq2 AND position=3);
SET @bo2_4 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq2 AND position=4);
INSERT INTO question_bank_questions (created_by, subject_id, question_text, question_type, points, difficulty, source, approval_status, topic) VALUES (@teacher, @java, 'Which concept lets a subclass provide its own version of a parent method?', 'SINGLE_CHOICE', 1.00, 'EASY', 'MANUAL', 'APPROVED', 'OOP');
SET @bq3 := LAST_INSERT_ID();
INSERT INTO question_bank_options (bank_question_id, option_text, is_correct, position) VALUES
  (@bq3, 'Overloading', 0, 1),
  (@bq3, 'Overriding', 1, 2),
  (@bq3, 'Encapsulation', 0, 3),
  (@bq3, 'Abstraction', 0, 4);
SET @bo3_1 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq3 AND position=1);
SET @bo3_2 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq3 AND position=2);
SET @bo3_3 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq3 AND position=3);
SET @bo3_4 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq3 AND position=4);
INSERT INTO question_bank_questions (created_by, subject_id, question_text, question_type, points, difficulty, source, approval_status, topic) VALUES (@teacher, @java, 'What is the size of an int in Java?', 'SINGLE_CHOICE', 1.00, 'EASY', 'MANUAL', 'APPROVED', 'Basics');
SET @bq4 := LAST_INSERT_ID();
INSERT INTO question_bank_options (bank_question_id, option_text, is_correct, position) VALUES
  (@bq4, '16 bits', 0, 1),
  (@bq4, '32 bits', 1, 2),
  (@bq4, '64 bits', 0, 3),
  (@bq4, 'Depends on the OS', 0, 4);
SET @bo4_1 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq4 AND position=1);
SET @bo4_2 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq4 AND position=2);
SET @bo4_3 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq4 AND position=3);
SET @bo4_4 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq4 AND position=4);
INSERT INTO question_bank_questions (created_by, subject_id, question_text, question_type, points, difficulty, source, approval_status, topic) VALUES (@teacher, @db, 'Which SQL clause filters rows after grouping?', 'SINGLE_CHOICE', 1.00, 'EASY', 'MANUAL', 'APPROVED', 'SQL');
SET @bq5 := LAST_INSERT_ID();
INSERT INTO question_bank_options (bank_question_id, option_text, is_correct, position) VALUES
  (@bq5, 'WHERE', 0, 1),
  (@bq5, 'HAVING', 1, 2),
  (@bq5, 'ORDER BY', 0, 3),
  (@bq5, 'LIMIT', 0, 4);
SET @bo5_1 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq5 AND position=1);
SET @bo5_2 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq5 AND position=2);
SET @bo5_3 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq5 AND position=3);
SET @bo5_4 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq5 AND position=4);
INSERT INTO question_bank_questions (created_by, subject_id, question_text, question_type, points, difficulty, source, approval_status, topic) VALUES (@teacher, @db, 'A table is in 2NF when it is in 1NF and has no...', 'SINGLE_CHOICE', 1.00, 'MEDIUM', 'MANUAL', 'APPROVED', 'Normalization');
SET @bq6 := LAST_INSERT_ID();
INSERT INTO question_bank_options (bank_question_id, option_text, is_correct, position) VALUES
  (@bq6, 'Transitive dependency', 0, 1),
  (@bq6, 'Partial dependency', 1, 2),
  (@bq6, 'Multivalued attribute', 0, 3),
  (@bq6, 'Foreign key', 0, 4);
SET @bo6_1 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq6 AND position=1);
SET @bo6_2 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq6 AND position=2);
SET @bo6_3 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq6 AND position=3);
SET @bo6_4 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq6 AND position=4);
INSERT INTO question_bank_questions (created_by, subject_id, question_text, question_type, points, difficulty, source, approval_status, topic) VALUES (@teacher, @db, 'Which join returns all rows from the left table?', 'SINGLE_CHOICE', 1.00, 'MEDIUM', 'MANUAL', 'APPROVED', 'Joins');
SET @bq7 := LAST_INSERT_ID();
INSERT INTO question_bank_options (bank_question_id, option_text, is_correct, position) VALUES
  (@bq7, 'INNER JOIN', 0, 1),
  (@bq7, 'RIGHT JOIN', 0, 2),
  (@bq7, 'LEFT JOIN', 1, 3),
  (@bq7, 'CROSS JOIN', 0, 4);
SET @bo7_1 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq7 AND position=1);
SET @bo7_2 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq7 AND position=2);
SET @bo7_3 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq7 AND position=3);
SET @bo7_4 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq7 AND position=4);
INSERT INTO question_bank_questions (created_by, subject_id, question_text, question_type, points, difficulty, source, approval_status, topic) VALUES (@teacher, @db, 'Which ACID property guarantees committed data survives a crash?', 'SINGLE_CHOICE', 1.00, 'HARD', 'MANUAL', 'APPROVED', 'Transactions');
SET @bq8 := LAST_INSERT_ID();
INSERT INTO question_bank_options (bank_question_id, option_text, is_correct, position) VALUES
  (@bq8, 'Atomicity', 0, 1),
  (@bq8, 'Consistency', 0, 2),
  (@bq8, 'Isolation', 0, 3),
  (@bq8, 'Durability', 1, 4);
SET @bo8_1 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq8 AND position=1);
SET @bo8_2 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq8 AND position=2);
SET @bo8_3 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq8 AND position=3);
SET @bo8_4 := (SELECT bank_option_id FROM question_bank_options WHERE bank_question_id=@bq8 AND position=4);

INSERT INTO quizzes (subject_id, created_by, title, instructions, available_from, available_until, time_limit_minutes, status) VALUES (@java, @teacher, 'Java Basics Quiz', 'Answer all questions. One attempt per student.', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 20, 'PUBLISHED');
SET @quiz1 := LAST_INSERT_ID();
INSERT INTO quiz_questions (quiz_id, bank_question_id, question_text, question_type, position, points) VALUES (@quiz1, @bq1, 'Which keyword is used to handle an exception in Java?', 'SINGLE_CHOICE', 1, 1.00);
SET @qq1_1 := LAST_INSERT_ID();
INSERT INTO quiz_options (question_id, bank_question_id, bank_option_id, option_text, is_correct, position) VALUES
  (@qq1_1, @bq1, @bo1_1, 'try-catch', 1, 1),
  (@qq1_1, @bq1, @bo1_2, 'throw-only', 0, 2),
  (@qq1_1, @bq1, @bo1_3, 'finally-only', 0, 3),
  (@qq1_1, @bq1, @bo1_4, 'assert', 0, 4);
SET @qo1_1_1 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_1 AND position=1);
SET @qo1_1_2 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_1 AND position=2);
SET @qo1_1_3 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_1 AND position=3);
SET @qo1_1_4 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_1 AND position=4);
INSERT INTO quiz_questions (quiz_id, bank_question_id, question_text, question_type, position, points) VALUES (@quiz1, @bq2, 'Which collection does not allow duplicate elements?', 'SINGLE_CHOICE', 2, 1.00);
SET @qq1_2 := LAST_INSERT_ID();
INSERT INTO quiz_options (question_id, bank_question_id, bank_option_id, option_text, is_correct, position) VALUES
  (@qq1_2, @bq2, @bo2_1, 'ArrayList', 0, 1),
  (@qq1_2, @bq2, @bo2_2, 'LinkedList', 0, 2),
  (@qq1_2, @bq2, @bo2_3, 'HashSet', 1, 3),
  (@qq1_2, @bq2, @bo2_4, 'Vector', 0, 4);
SET @qo1_2_1 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_2 AND position=1);
SET @qo1_2_2 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_2 AND position=2);
SET @qo1_2_3 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_2 AND position=3);
SET @qo1_2_4 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_2 AND position=4);
INSERT INTO quiz_questions (quiz_id, bank_question_id, question_text, question_type, position, points) VALUES (@quiz1, @bq3, 'Which concept lets a subclass provide its own version of a parent method?', 'SINGLE_CHOICE', 3, 1.00);
SET @qq1_3 := LAST_INSERT_ID();
INSERT INTO quiz_options (question_id, bank_question_id, bank_option_id, option_text, is_correct, position) VALUES
  (@qq1_3, @bq3, @bo3_1, 'Overloading', 0, 1),
  (@qq1_3, @bq3, @bo3_2, 'Overriding', 1, 2),
  (@qq1_3, @bq3, @bo3_3, 'Encapsulation', 0, 3),
  (@qq1_3, @bq3, @bo3_4, 'Abstraction', 0, 4);
SET @qo1_3_1 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_3 AND position=1);
SET @qo1_3_2 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_3 AND position=2);
SET @qo1_3_3 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_3 AND position=3);
SET @qo1_3_4 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_3 AND position=4);
INSERT INTO quiz_questions (quiz_id, bank_question_id, question_text, question_type, position, points) VALUES (@quiz1, @bq4, 'What is the size of an int in Java?', 'SINGLE_CHOICE', 4, 1.00);
SET @qq1_4 := LAST_INSERT_ID();
INSERT INTO quiz_options (question_id, bank_question_id, bank_option_id, option_text, is_correct, position) VALUES
  (@qq1_4, @bq4, @bo4_1, '16 bits', 0, 1),
  (@qq1_4, @bq4, @bo4_2, '32 bits', 1, 2),
  (@qq1_4, @bq4, @bo4_3, '64 bits', 0, 3),
  (@qq1_4, @bq4, @bo4_4, 'Depends on the OS', 0, 4);
SET @qo1_4_1 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_4 AND position=1);
SET @qo1_4_2 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_4 AND position=2);
SET @qo1_4_3 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_4 AND position=3);
SET @qo1_4_4 := (SELECT option_id FROM quiz_options WHERE question_id=@qq1_4 AND position=4);
INSERT INTO quizzes (subject_id, created_by, title, instructions, available_from, available_until, time_limit_minutes, status) VALUES (@db, @teacher, 'SQL Fundamentals Quiz', 'Pick the best answer for each question.', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), 15, 'PUBLISHED');
SET @quiz2 := LAST_INSERT_ID();
INSERT INTO quiz_questions (quiz_id, bank_question_id, question_text, question_type, position, points) VALUES (@quiz2, @bq5, 'Which SQL clause filters rows after grouping?', 'SINGLE_CHOICE', 1, 1.00);
SET @qq2_1 := LAST_INSERT_ID();
INSERT INTO quiz_options (question_id, bank_question_id, bank_option_id, option_text, is_correct, position) VALUES
  (@qq2_1, @bq5, @bo5_1, 'WHERE', 0, 1),
  (@qq2_1, @bq5, @bo5_2, 'HAVING', 1, 2),
  (@qq2_1, @bq5, @bo5_3, 'ORDER BY', 0, 3),
  (@qq2_1, @bq5, @bo5_4, 'LIMIT', 0, 4);
SET @qo2_1_1 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_1 AND position=1);
SET @qo2_1_2 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_1 AND position=2);
SET @qo2_1_3 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_1 AND position=3);
SET @qo2_1_4 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_1 AND position=4);
INSERT INTO quiz_questions (quiz_id, bank_question_id, question_text, question_type, position, points) VALUES (@quiz2, @bq6, 'A table is in 2NF when it is in 1NF and has no...', 'SINGLE_CHOICE', 2, 1.00);
SET @qq2_2 := LAST_INSERT_ID();
INSERT INTO quiz_options (question_id, bank_question_id, bank_option_id, option_text, is_correct, position) VALUES
  (@qq2_2, @bq6, @bo6_1, 'Transitive dependency', 0, 1),
  (@qq2_2, @bq6, @bo6_2, 'Partial dependency', 1, 2),
  (@qq2_2, @bq6, @bo6_3, 'Multivalued attribute', 0, 3),
  (@qq2_2, @bq6, @bo6_4, 'Foreign key', 0, 4);
SET @qo2_2_1 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_2 AND position=1);
SET @qo2_2_2 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_2 AND position=2);
SET @qo2_2_3 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_2 AND position=3);
SET @qo2_2_4 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_2 AND position=4);
INSERT INTO quiz_questions (quiz_id, bank_question_id, question_text, question_type, position, points) VALUES (@quiz2, @bq7, 'Which join returns all rows from the left table?', 'SINGLE_CHOICE', 3, 1.00);
SET @qq2_3 := LAST_INSERT_ID();
INSERT INTO quiz_options (question_id, bank_question_id, bank_option_id, option_text, is_correct, position) VALUES
  (@qq2_3, @bq7, @bo7_1, 'INNER JOIN', 0, 1),
  (@qq2_3, @bq7, @bo7_2, 'RIGHT JOIN', 0, 2),
  (@qq2_3, @bq7, @bo7_3, 'LEFT JOIN', 1, 3),
  (@qq2_3, @bq7, @bo7_4, 'CROSS JOIN', 0, 4);
SET @qo2_3_1 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_3 AND position=1);
SET @qo2_3_2 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_3 AND position=2);
SET @qo2_3_3 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_3 AND position=3);
SET @qo2_3_4 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_3 AND position=4);
INSERT INTO quiz_questions (quiz_id, bank_question_id, question_text, question_type, position, points) VALUES (@quiz2, @bq8, 'Which ACID property guarantees committed data survives a crash?', 'SINGLE_CHOICE', 4, 1.00);
SET @qq2_4 := LAST_INSERT_ID();
INSERT INTO quiz_options (question_id, bank_question_id, bank_option_id, option_text, is_correct, position) VALUES
  (@qq2_4, @bq8, @bo8_1, 'Atomicity', 0, 1),
  (@qq2_4, @bq8, @bo8_2, 'Consistency', 0, 2),
  (@qq2_4, @bq8, @bo8_3, 'Isolation', 0, 3),
  (@qq2_4, @bq8, @bo8_4, 'Durability', 1, 4);
SET @qo2_4_1 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_4 AND position=1);
SET @qo2_4_2 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_4 AND position=2);
SET @qo2_4_3 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_4 AND position=3);
SET @qo2_4_4 := (SELECT option_id FROM quiz_options WHERE question_id=@qq2_4 AND position=4);
INSERT INTO quizzes (subject_id, created_by, title, instructions, available_from, available_until, time_limit_minutes, status) VALUES (@java, @teacher, 'OOP Concepts (draft)', 'Draft quiz - not visible to students yet.', DATE_SUB(NOW(), INTERVAL 7 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), NULL, 'DRAFT');
SET @quiz3 := LAST_INSERT_ID();
INSERT INTO quiz_questions (quiz_id, bank_question_id, question_text, question_type, position, points) VALUES (@quiz3, @bq3, 'Which concept lets a subclass provide its own version of a parent method?', 'SINGLE_CHOICE', 1, 1.00);
SET @qq3_1 := LAST_INSERT_ID();
INSERT INTO quiz_options (question_id, bank_question_id, bank_option_id, option_text, is_correct, position) VALUES
  (@qq3_1, @bq3, @bo3_1, 'Overloading', 0, 1),
  (@qq3_1, @bq3, @bo3_2, 'Overriding', 1, 2),
  (@qq3_1, @bq3, @bo3_3, 'Encapsulation', 0, 3),
  (@qq3_1, @bq3, @bo3_4, 'Abstraction', 0, 4);
SET @qo3_1_1 := (SELECT option_id FROM quiz_options WHERE question_id=@qq3_1 AND position=1);
SET @qo3_1_2 := (SELECT option_id FROM quiz_options WHERE question_id=@qq3_1 AND position=2);
SET @qo3_1_3 := (SELECT option_id FROM quiz_options WHERE question_id=@qq3_1 AND position=3);
SET @qo3_1_4 := (SELECT option_id FROM quiz_options WHERE question_id=@qq3_1 AND position=4);
INSERT INTO quiz_questions (quiz_id, bank_question_id, question_text, question_type, position, points) VALUES (@quiz3, @bq4, 'What is the size of an int in Java?', 'SINGLE_CHOICE', 2, 1.00);
SET @qq3_2 := LAST_INSERT_ID();
INSERT INTO quiz_options (question_id, bank_question_id, bank_option_id, option_text, is_correct, position) VALUES
  (@qq3_2, @bq4, @bo4_1, '16 bits', 0, 1),
  (@qq3_2, @bq4, @bo4_2, '32 bits', 1, 2),
  (@qq3_2, @bq4, @bo4_3, '64 bits', 0, 3),
  (@qq3_2, @bq4, @bo4_4, 'Depends on the OS', 0, 4);
SET @qo3_2_1 := (SELECT option_id FROM quiz_options WHERE question_id=@qq3_2 AND position=1);
SET @qo3_2_2 := (SELECT option_id FROM quiz_options WHERE question_id=@qq3_2 AND position=2);
SET @qo3_2_3 := (SELECT option_id FROM quiz_options WHERE question_id=@qq3_2 AND position=3);
SET @qo3_2_4 := (SELECT option_id FROM quiz_options WHERE question_id=@qq3_2 AND position=4);

INSERT INTO quiz_attempts (quiz_id, student_user_id, attempt_number, started_at, submitted_at, score, status) VALUES (@quiz1, @aarav, 1, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 12 MINUTE), 2.00, 'GRADED');
SET @att1 := LAST_INSERT_ID();
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att1, @qq1_1, @qo1_1_1, 1.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att1, @qq1_2, @qo1_2_2, 0.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att1, @qq1_3, @qo1_3_3, 0.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att1, @qq1_4, @qo1_4_2, 1.00);
INSERT INTO quiz_attempts (quiz_id, student_user_id, attempt_number, started_at, submitted_at, score, status) VALUES (@quiz1, @diya, 1, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 4 DAY), INTERVAL 12 MINUTE), 3.00, 'GRADED');
SET @att2 := LAST_INSERT_ID();
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att2, @qq1_1, @qo1_1_1, 1.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att2, @qq1_2, @qo1_2_3, 1.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att2, @qq1_3, @qo1_3_3, 0.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att2, @qq1_4, @qo1_4_2, 1.00);
INSERT INTO quiz_attempts (quiz_id, student_user_id, attempt_number, started_at, submitted_at, score, status) VALUES (@quiz1, @kabir, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 12 MINUTE), 1.00, 'GRADED');
SET @att3 := LAST_INSERT_ID();
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att3, @qq1_1, @qo1_1_2, 0.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att3, @qq1_2, @qo1_2_2, 0.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att3, @qq1_3, @qo1_3_1, 0.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att3, @qq1_4, @qo1_4_2, 1.00);
INSERT INTO quiz_attempts (quiz_id, student_user_id, attempt_number, started_at, submitted_at, score, status) VALUES (@quiz1, @meera, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 12 MINUTE), 1.00, 'GRADED');
SET @att4 := LAST_INSERT_ID();
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att4, @qq1_1, @qo1_1_1, 1.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att4, @qq1_2, @qo1_2_2, 0.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att4, @qq1_3, @qo1_3_3, 0.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att4, @qq1_4, @qo1_4_1, 0.00);
INSERT INTO quiz_attempts (quiz_id, student_user_id, attempt_number, started_at, submitted_at, score, status) VALUES (@quiz2, @aarav, 1, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 12 MINUTE), 4.00, 'GRADED');
SET @att5 := LAST_INSERT_ID();
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att5, @qq2_1, @qo2_1_2, 1.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att5, @qq2_2, @qo2_2_2, 1.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att5, @qq2_3, @qo2_3_3, 1.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att5, @qq2_4, @qo2_4_4, 1.00);
INSERT INTO quiz_attempts (quiz_id, student_user_id, attempt_number, started_at, submitted_at, score, status) VALUES (@quiz2, @diya, 1, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 12 MINUTE), 3.00, 'GRADED');
SET @att6 := LAST_INSERT_ID();
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att6, @qq2_1, @qo2_1_1, 0.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att6, @qq2_2, @qo2_2_2, 1.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att6, @qq2_3, @qo2_3_3, 1.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att6, @qq2_4, @qo2_4_4, 1.00);
INSERT INTO quiz_attempts (quiz_id, student_user_id, attempt_number, started_at, submitted_at, score, status) VALUES (@quiz2, @rohan, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 12 MINUTE), 3.00, 'GRADED');
SET @att7 := LAST_INSERT_ID();
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att7, @qq2_1, @qo2_1_2, 1.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att7, @qq2_2, @qo2_2_1, 0.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att7, @qq2_3, @qo2_3_3, 1.00);
INSERT INTO quiz_answers (attempt_id, question_id, selected_option_id, awarded_points) VALUES (@att7, @qq2_4, @qo2_4_4, 1.00);

INSERT INTO assignments (subject_id, created_by, title, instructions, due_at, max_score, status, attempt_duration_minutes) VALUES (@java, @teacher, 'Java Collections Report', 'Write short answers. Cite examples from the lecture.', DATE_ADD(NOW(), INTERVAL 7 DAY), 10.00, 'PUBLISHED', 45);
SET @asg1 := LAST_INSERT_ID();
INSERT INTO assignment_questions (assignment_id, question_text, question_type, model_answer, rubric, points, position) VALUES (@asg1, 'Explain the difference between ArrayList and LinkedList.', 'LONG_TEXT', 'ArrayList is array-backed with O(1) random access; LinkedList is node-based with O(1) insertion at known nodes.', 'Accuracy 3, examples 2', 5.00, 1);
SET @aq1_1 := LAST_INSERT_ID();
INSERT INTO assignment_questions (assignment_id, question_text, question_type, model_answer, rubric, points, position) VALUES (@asg1, 'When would you choose a HashSet over a TreeSet?', 'SHORT_TEXT', 'HashSet for O(1) lookups without ordering; TreeSet when sorted iteration is needed.', 'Correct trade-off 3', 5.00, 2);
SET @aq1_2 := LAST_INSERT_ID();
INSERT INTO assignments (subject_id, created_by, title, instructions, due_at, max_score, status, attempt_duration_minutes) VALUES (@db, @teacher, 'SQL Joins Worksheet', 'Answer using the sample schema from class.', DATE_ADD(NOW(), INTERVAL 14 DAY), 10.00, 'PUBLISHED', 60);
SET @asg2 := LAST_INSERT_ID();
INSERT INTO assignment_questions (assignment_id, question_text, question_type, model_answer, rubric, points, position) VALUES (@asg2, 'Describe INNER JOIN versus LEFT JOIN with an example.', 'LONG_TEXT', 'INNER returns matching rows only; LEFT keeps every left row, filling NULLs where no match exists.', 'Definition 3, example 2', 5.00, 1);
SET @aq2_1 := LAST_INSERT_ID();
INSERT INTO assignment_questions (assignment_id, question_text, question_type, model_answer, rubric, points, position) VALUES (@asg2, 'What is a primary key and why must it be unique?', 'SHORT_TEXT', 'It uniquely identifies each row so rows can be referenced without ambiguity.', 'Definition 2, reason 1', 3.00, 2);
SET @aq2_2 := LAST_INSERT_ID();
INSERT INTO assignment_questions (assignment_id, question_text, question_type, model_answer, rubric, points, position) VALUES (@asg2, 'Why do we normalise tables?', 'SHORT_TEXT', 'To reduce redundancy and update anomalies.', 'Reason 2', 2.00, 3);
SET @aq2_3 := LAST_INSERT_ID();
INSERT INTO assignments (subject_id, created_by, title, instructions, due_at, max_score, status, attempt_duration_minutes) VALUES (@java, @teacher, 'Draft: Inheritance Essay', 'Draft - not published yet.', DATE_ADD(NOW(), INTERVAL 21 DAY), 5.00, 'DRAFT', 60);
SET @asg3 := LAST_INSERT_ID();
INSERT INTO assignment_questions (assignment_id, question_text, question_type, model_answer, rubric, points, position) VALUES (@asg3, 'Discuss inheritance versus composition.', 'LONG_TEXT', 'Prefer composition for flexibility; use inheritance for true is-a relationships.', 'Balanced discussion 5', 5.00, 1);
SET @aq3_1 := LAST_INSERT_ID();

INSERT INTO assignment_submissions (assignment_id, student_user_id, submitted_at, score, graded_by, graded_at, status, evaluation_status, attempt_started_at, attempt_expires_at) VALUES (@asg1, @aarav, DATE_SUB(NOW(), INTERVAL 3 DAY), 9.00, @teacher, DATE_SUB(NOW(), INTERVAL 2 DAY), 'GRADED', 'EVALUATED', DATE_SUB(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 30 MINUTE), DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 30 MINUTE));
SET @sub1 := LAST_INSERT_ID();
INSERT INTO assignment_answers (submission_id, question_id, answer_text, ai_score, ai_feedback, final_score, final_feedback, evaluated_by, evaluated_at) VALUES (@sub1, @aq1_1, 'ArrayList stores items in an array so get(i) is O(1), while LinkedList must walk nodes. LinkedList wins for inserts in the middle.', 4.00, 'Suggested score based on the model answer.', 4.00, 'Clear and accurate.', @teacher, DATE_SUB(NOW(), INTERVAL 2 DAY));
INSERT INTO assignment_answers (submission_id, question_id, answer_text, ai_score, ai_feedback, final_score, final_feedback, evaluated_by, evaluated_at) VALUES (@sub1, @aq1_2, 'HashSet is faster for lookups; use TreeSet if you need sorted order.', 5.00, 'Suggested score based on the model answer.', 5.00, 'Clear and accurate.', @teacher, DATE_SUB(NOW(), INTERVAL 2 DAY));
INSERT INTO assignment_submissions (assignment_id, student_user_id, submitted_at, score, graded_by, graded_at, status, evaluation_status, attempt_started_at, attempt_expires_at) VALUES (@asg1, @diya, DATE_SUB(NOW(), INTERVAL 2 DAY), NULL, NULL, NULL, 'SUBMITTED', 'EVALUATED', DATE_SUB(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 30 MINUTE), DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 30 MINUTE));
SET @sub2 := LAST_INSERT_ID();
INSERT INTO assignment_answers (submission_id, question_id, answer_text, ai_score, ai_feedback, final_score, final_feedback, evaluated_by, evaluated_at) VALUES (@sub2, @aq1_1, 'ArrayList is faster to read, LinkedList is faster to insert.', 3.00, 'Suggested score based on the model answer.', NULL, NULL, NULL, NULL);
INSERT INTO assignment_answers (submission_id, question_id, answer_text, ai_score, ai_feedback, final_score, final_feedback, evaluated_by, evaluated_at) VALUES (@sub2, @aq1_2, 'TreeSet keeps items sorted.', 3.00, 'Suggested score based on the model answer.', NULL, NULL, NULL, NULL);
INSERT INTO assignment_submissions (assignment_id, student_user_id, submitted_at, score, graded_by, graded_at, status, evaluation_status, attempt_started_at, attempt_expires_at) VALUES (@asg2, @kabir, DATE_SUB(NOW(), INTERVAL 1 DAY), NULL, NULL, NULL, 'SUBMITTED', 'PENDING', DATE_SUB(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 30 MINUTE), DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 30 MINUTE));
SET @sub3 := LAST_INSERT_ID();
INSERT INTO assignment_answers (submission_id, question_id, answer_text, ai_score, ai_feedback, final_score, final_feedback, evaluated_by, evaluated_at) VALUES (@sub3, @aq2_1, 'INNER JOIN only returns rows that match, LEFT JOIN returns all left rows and NULL for the rest.', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO assignment_answers (submission_id, question_id, answer_text, ai_score, ai_feedback, final_score, final_feedback, evaluated_by, evaluated_at) VALUES (@sub3, @aq2_2, 'A primary key identifies a row and cannot repeat.', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO assignment_answers (submission_id, question_id, answer_text, ai_score, ai_feedback, final_score, final_feedback, evaluated_by, evaluated_at) VALUES (@sub3, @aq2_3, 'To avoid duplicate data.', NULL, NULL, NULL, NULL, NULL, NULL);
INSERT INTO assignment_submissions (assignment_id, student_user_id, submitted_at, score, graded_by, graded_at, status, evaluation_status, attempt_started_at, attempt_expires_at) VALUES (@asg2, @meera, DATE_SUB(NOW(), INTERVAL 2 DAY), 8.00, @teacher, DATE_SUB(NOW(), INTERVAL 1 DAY), 'GRADED', 'EVALUATED', DATE_SUB(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 30 MINUTE), DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 30 MINUTE));
SET @sub4 := LAST_INSERT_ID();
INSERT INTO assignment_answers (submission_id, question_id, answer_text, ai_score, ai_feedback, final_score, final_feedback, evaluated_by, evaluated_at) VALUES (@sub4, @aq2_1, 'INNER gives only common rows. LEFT gives everything from the left table plus matches.', 4.50, 'Suggested score based on the model answer.', 4.50, 'Clear and accurate.', @teacher, DATE_SUB(NOW(), INTERVAL 1 DAY));
INSERT INTO assignment_answers (submission_id, question_id, answer_text, ai_score, ai_feedback, final_score, final_feedback, evaluated_by, evaluated_at) VALUES (@sub4, @aq2_2, 'Primary key is a unique id for each row; uniqueness lets other tables point to it.', 2.00, 'Suggested score based on the model answer.', 2.00, 'Clear and accurate.', @teacher, DATE_SUB(NOW(), INTERVAL 1 DAY));
INSERT INTO assignment_answers (submission_id, question_id, answer_text, ai_score, ai_feedback, final_score, final_feedback, evaluated_by, evaluated_at) VALUES (@sub4, @aq2_3, 'Normalisation removes repeated data and prevents update anomalies.', 1.50, 'Suggested score based on the model answer.', 1.50, 'Clear and accurate.', @teacher, DATE_SUB(NOW(), INTERVAL 1 DAY));

COMMIT;
