# Teacher Analytics and Groq insights

Member 5 uses the existing Aiven DataSource. Authentication, assignment APIs,
shared Groq configuration, database schema and existing records are unchanged.

## Access and real-data mapping

Analytics verifies the existing `loggedInUser` session against an active TEACHER
in `users`. The integrated Teacher Submissions path is:
`/teacher/submissions` -> `teacher/submissions.jsp` -> teacher-assignment-evaluation.js
-> `GET /api/assignments/teacher/submissions` -> `AssignmentController.teacherSubmissions`.
There is no intervening repository/service for that list: the controller validates
an active TEACHER and queries SUBMITTED/GRADED work across assignment owners.

`TeacherAnalyticsVisibility` now defines the shared read-only review predicate.
An active teacher sees their own visible assessments plus PUBLISHED/CLOSED
assignments with actual SUBMITTED/GRADED work, matching the legitimate Submissions
review permission. Subject scope includes those assignments' subjects. Both access
and data providers use the same predicate. Every shared query joins the bound
active TEACHER reviewer, and the data provider independently rechecks the account.
Foreign drafts remain private. Quiz access remains ownership-scoped because the
integrated Submissions module supplies no shared quiz-review permission.

Editing/grading permissions and teammates' endpoints are unchanged. No account is
assigned subjects or granted an additional database role. For shared review,
Analytics includes only actual submitted/graded participants and never infers a
complete roster or missing work from someone else's enrollments.

Student records come from current ENROLLED subject links and actual completed
submissions on reviewable assignments and completed attempts on owned quizzes. Legacy
`students.student_id` values are not equated to authenticated `users.user_id`.
Recorded participants are not represented as enrollment rows. Historical work
remains visible regardless of the student's current account status; the teacher
must still have an active account.

Draft assessments/work are excluded. Assignments must be PUBLISHED/CLOSED;
quizzes must also have reached their availability date. Invalid/nonpositive
maxima are omitted with a notice. Only valid final GRADED scores enter averages;
AI suggestions and ungraded submissions never become final grades or zero marks.
Quiz maxima and question difficulty use canonical bank relationships. Retakes
select the latest completed attempt; question counts require final awarded points
on that same selected graded attempt.

## Pages and controls

- `/teacher/analytics`: Java-calculated normalized averages, ranges, distributions,
  weekly trends, subject comparisons and an **AI Class Insights** section.
- `/teacher/analytics/students`: case-insensitive name/user-ID search, meaningful
  performance/name/score controls, scoped reset and student selection. A student's
  page includes real grade history, submitted/missing statuses, subject performance,
  teacher feedback and **AI Student Summary**.
- `/teacher/analytics/assessments`: case-insensitive `assessmentSearch` for assignment
  and quiz titles. Search narrows the list and corresponding aggregates/charts.
  Selecting `/teacher/analytics/assessments/{id}` shows actual submitted/graded
  counts, marks, normalized grades, feedback and links to student details.
- `/teacher/analytics/reports`: existing Java descriptive statistics and honest
  empty states for unavailable R outputs. No R engine/report store is connected.

Subject filters only narrow authorized scope. Foreign subjects return 403;
unknown/out-of-scope students and assessments return 404, including selections
outside current title/name filters. No teacher ID is taken from browser input.
Class controls stay hidden because no class/cohort relationship exists. The
unsupported submission-rate sort was removed from Student Explorer. Existing
layout, CSS and navigation remain in use.

## Participation and statistics

Normalized grades are `score / maximum * 100`, with equal weight per valid final
assessment result. Missing/ungraded work is excluded. Descriptive statistics are
Java-calculated count, mean, median, sample standard deviation and quartiles.

Eligible enrolled students are determined using enrollment/deadline timestamps;
actual recorded submissions are included even without enrollment links. Missing
work is inferred only for known eligible enrollments. If enrollment coverage is
unavailable (or a participant has no enrollment link), expected-work counts and
submission rates show unavailable, while actual submission/grade counts remain
visible. Student rows then show `N recorded`, not a fabricated 100% rate.
This is a current-enrollment/recorded-participant view, not historical roster
reconstruction. Historical changes to assessment maxima cannot be reconstructed.

## AI facts and request handling

AI runs only on an explicit authenticated POST, never during ordinary page load.
The existing shared `GroqAIService` receives anonymous calculated facts and a small
set of eligible next steps. It selects up to three fact IDs and one recommendation
ID. Both provider and analytics service validate that selection. Java renders only
original server-calculated sentences and advice; generated prose/numbers cannot
invent grades. Names, IDs, answers and free-text feedback are not sent to Groq.

Forms use a session token, and requests are limited to one per 30 seconds per
session. Each request rechecks authorization and reloads the selected data.
Missing credentials, rate limits and provider failures leave normal statistics
available and show a safe message. Nothing is persisted. Demo data is never sent
to Groq. See [Groq configuration](groq-configuration.md).
