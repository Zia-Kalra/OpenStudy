# Member 4 - Student Performance Analytics & Statistical Intelligence

**This module is read-only. It creates no tables, views, indexes or migrations and never writes to the database.** Every query is a `SELECT` over existing tables, so the DB user can have `SELECT` only.

## How it works
Browser (JSP + Chart.js) -> `/api/analytics/**` (Spring) -> `AnalyticsRepository` (JDBC `SELECT`) -> Java builds a pseudonymised CSV -> `Rscript` -> JSON -> Java -> browser.
Simple figures (averages, min/max, counts, distribution, participation, question accuracy) come straight from SQL. R is used for median/SD/quartiles, confidence intervals, tests, regression and the trend test. If R is missing or slow, the dashboard still shows the SQL figures and the statistical sections say they are unavailable.

## Files
| Path | Purpose |
|---|---|
| `backend/.../config/AnalyticsProperties.java` | settings (`openstudy.analytics.*`) |
| `backend/.../repository/AnalyticsRepository.java` | all SQL (counting rules in `RESULTS`) |
| `backend/.../service/analytics/AnalyticsService.java` | orchestration, caching, pseudonymisation |
| `backend/.../service/analytics/RAnalyticsService.java`, `RProcessRunner.java`, `RScript.java`, `DatasetCsvWriter.java` | Java-R bridge |
| `backend/.../service/analytics/InsightEngine.java` | rule-based insights |
| `backend/.../service/analytics/AnalyticsUserContext.java`, `SessionAnalyticsUserContext.java` | who is signed in |
| `backend/.../controller/AnalyticsApiController.java`, `AnalyticsExceptionHandler.java`, `AnalyticsPageController.java` | endpoints and pages |
| `backend/.../dto/analytics/*` | filter, response records, envelope |
| `backend/src/main/webapp/WEB-INF/jsp/student/performance.jsp`, `student/fragments/sidebar.jsp`, `teacher/analytics.jsp` | pages |
| `backend/src/main/resources/static/{css/analytics.css, js/analytics-common.js, js/student-analytics.js, js/teacher-analytics.js, vendor/chart.umd.js}` | frontend (Chart.js 4.4.7 bundled, MIT) |
| `R/*.R`, `R/README.md` | statistics |
| `application.properties`, `.env.example` | modified: analytics settings; `openstudy.modules.analytics-path` now `/teacher/analytics` |
| `database/analytics-seed-data.sql` | OPTIONAL dummy data for a local DB (INSERTs only) |

Shared files touched: `application.properties` (appended + one value changed) and `.env.example` (appended). No other member's file was edited. Member 5's sidebar already links to `openstudy.modules.analytics-path`.

## Existing tables and columns used (read only)
- `users(user_id, role, account_status)`
- `subjects(subject_id, subject_name)`, `subject_teachers(subject_id, teacher_id)`, `subject_enrollments(subject_id, student_user_id, enrollment_status)`
- `quizzes(quiz_id, subject_id, created_by, title, status)`, `quiz_questions(question_id, quiz_id, question_text, points, position)`, `quiz_options(option_id, is_correct)`
- `quiz_attempts(attempt_id, quiz_id, student_user_id, attempt_number, status, score, started_at, submitted_at)`, `quiz_answers(attempt_id, question_id, selected_option_id, awarded_points)`
- `assignments(assignment_id, subject_id, created_by, title, max_score, status)`, `assignment_submissions(submission_id, assignment_id, student_user_id, status, score, submitted_at, graded_at, created_at)`

## Counting rules
- **Percentage** = `score / max_score * 100`, clamped to 0-100.
- **Quiz max score** = `SUM(quiz_questions.points)` (the quizzes table has no max column). **Assignment max score** = `assignments.max_score`.
- **Quiz result**: latest attempt (highest `attempt_number`) per student and quiz, status `SUBMITTED` or `GRADED`, score not NULL. `IN_PROGRESS`/`ABANDONED` are ignored.
- **Assignment result**: only `GRADED` or `RETURNED` with score not NULL. `DRAFT`/`SUBMITTED` (not finalized) are excluded.
- **Overall average** = mean of per-result percentages (each completed assessment counts once). It is not total marks / total max marks.
- **Class rank** = rank by average percentage among all students with at least one result. Only the caller's rank and the class size are returned.
- **Teacher scope**: subjects in `subject_teachers` for that teacher, plus anything the teacher created.
- **Question correct** = `awarded_points >= points` (falls back to `quiz_options.is_correct`). Unanswered = incorrect. Reliable only with 5+ attempts.

## Authentication (important)
The repository has no login yet. The analytics API never reads a user id from the request. It reads the signed-in `User` from the HTTP session attribute named by `openstudy.analytics.session-attribute` (default `currentUser`). When Member 1's login stores a `User` there (role `STUDENT` or `TEACHER`, `accountStatus` `ACTIVE`) everything works. If they use another key, change the property.
Until then, for local development only, set `ANALYTICS_DEV_IDENTITY=true` plus `ANALYTICS_DEV_STUDENT_ID` / `ANALYTICS_DEV_TEACHER_ID`. This is server config, not a browser parameter, and logs a warning at startup. Leave it `false` in production.

## API (all GET, JSON envelope `{success, data, message, warnings, generatedAt}`)
Common optional filters: `subjectId`, `type` (QUIZ|ASSIGNMENT|ALL), `from`, `to` (`yyyy-MM-dd`).
| Endpoint | Role | Notes |
|---|---|---|
| `/api/analytics/student/overview` | student | summary, rank, grade bands |
| `/api/analytics/student/subjects` | student | per-subject quiz/assignment averages |
| `/api/analytics/student/trends` | student | monthly series + R trend test |
| `/api/analytics/student/assessments` | student | own result rows (`type` filter for quiz/assignment tables) |
| `/api/analytics/student/insights` | student | rule-based insights and recommendations |
| `/api/analytics/teacher/filters` | teacher | subjects the teacher may filter by |
| `/api/analytics/teacher/class-overview` | teacher | summary, participation, per-assessment table, R descriptive stats |
| `/api/analytics/teacher/subjects` | teacher | subject-wise quiz vs assignment |
| `/api/analytics/teacher/distribution` | teacher | 10 histogram bins |
| `/api/analytics/teacher/trends` | teacher | monthly series |
| `/api/analytics/teacher/question-performance` | teacher | lowest-accuracy quiz questions |
| `/api/analytics/teacher/insights` | teacher | rule-based insights |
| `/api/analytics/teacher/statistical-report` | teacher | runs all four R scripts (cached 60 s) |

Status codes: 200 OK, 400 bad filter, 401 not signed in, 403 wrong role or subject not yours, 503 database unavailable, 500 unexpected. Each R section is `{status: OK|INSUFFICIENT_DATA|UNAVAILABLE|TIMEOUT|BUSY|ERROR, message, data}`; a failing R section never fails the whole request.
Pages: `/student/performance`, `/teacher/analytics`.

## Configuration
`openstudy.analytics.r.executable` (env `RSCRIPT_PATH`), `r.script-dir` (env `ANALYTICS_R_SCRIPT_DIR`, default `R`, also tries `../R`), `r.timeout-seconds` (30), `r.max-concurrent` (2), `r.enabled`, `pass-mark` (50), `min-results-for-insights` (3), `report-cache-seconds` (60).

## R and privacy safeguards
Fixed allow-list of four scripts, no shell, no user code. R receives pseudonyms (S001...) instead of ids/names/e-mails. DB_*, GROQ_*, password, secret and token variables are removed from R's environment. Temp files live in a private folder deleted after every run (also on timeout). Errors returned to the browser are generic; details go to the server log only.

## Statistical test selection (viva notes)
| Question | Test | Run only when |
|---|---|---|
| Is the class mean different from the 50% benchmark? | one-sample t-test + 95% CI on student means | 5+ students, not all identical |
| Do quizzes and assignments differ? | paired t-test on student means (Wilcoxon if differences are not normal) | 5+ students with both |
| Do subjects differ? | one-way ANOVA (+Tukey), Kruskal-Wallis fallback if Shapiro/Fligner fail | 2+ subjects with 3+ students each |
| Do quiz and assignment scores move together? | Pearson r (+Spearman) | 5+ students, neither constant |
| Predict assignment mean from quiz mean | simple linear regression | 5+ students |
| Is performance changing over time? | regression on days; multiple regression adds type and subject | 8+ results; 10 per predictor for multiple |
Limitations are returned with each result and shown in the UI (repeated measures, differing difficulty, small samples).

## Verification status
Verified in a sandbox: SQL against MySQL 8.0 with seeded data through a SELECT-only user; all four R scripts on the seeded data and on empty, one-row, constant, missing-column and missing-file inputs; the Java runner against real Rscript (success, missing R, missing script, script error, timeout, malformed JSON, secret stripping, temp cleanup); both pages' JavaScript in jsdom with real R output; 18 unit tests. **Not verified:** a full `mvnw` build and a running Spring Boot app, because Maven Central was unreachable in my sandbox. The Spring-dependent classes were type-checked against hand-written stubs only. Run the commands below on your machine.

## Run locally
```
./backend/mvnw -f backend/pom.xml test
./backend/mvnw -f backend/pom.xml spring-boot:run          # Windows: .\backend\mvnw.cmd ...
```
Needs the DB env vars from the README. Then open `/teacher/analytics` or `/student/performance` (with a session user or the dev identity). The `mock` profile does not load analytics (it has no database).
