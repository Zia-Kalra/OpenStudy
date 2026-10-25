# Teacher Analytics with Aiven data

Normal startup uses the existing Aiven DataSource and `openstudy.teacher-analytics.provider=jdbc`.
Login, shared database settings, dashboards and student modules are unchanged.
For the current AI provider configuration, see [Groq configuration](groq-configuration.md).
The explicit `mock` profile retains its original demo providers for database-independent development.

## Pages

| Route | Data |
| --- | --- |
| `/teacher/analytics` | Authorized enrolled roster, normalized grade aggregates, weekly trends and distribution |
| `/teacher/analytics/students` | Search, performance bands, sorting, subject filters and student drill-down |
| `/teacher/analytics/assessments` | Published/closed assignments and available quizzes, submission rates and graded question responses |
| `/teacher/analytics/reports` | Live descriptive summary; honest empty states for unavailable R outputs |

All routes use the existing teacher login session. Restart your normal backend to load these changes,
then open `/teacher/analytics`. No new login or public data endpoint is introduced.

## Verified database mapping

The shared schema was inspected through read-only information_schema queries.

- `users.user_id` supplies the existing authenticated identity. The analytics adapter rechecks
  `role=TEACHER` and `account_status=ACTIVE` before loading any subject assignments.
- `subject_teachers` supplies the authorized subjects. Every analytics query binds the verified
  teacher ID; request parameters can only narrow that scope.
- `subject_enrollments` with `enrollment_status=ENROLLED`, joined to active student users, supplies
  the roster. Legacy `subjects.student_id` points to a separate `students` table and is deliberately
  not guessed to be a `users.user_id` relationship.
- The inspected schema has **no class/cohort table or student-to-class relationship**. Class filters
  are disabled and the UI explains that results cover assigned subjects. Internal empty class IDs
  do not create or represent database classes.
- `assignments.max_score` supplies assignment maxima. Draft assessments and draft submissions
  are excluded. Returned/submitted work remains ungraded until a valid final `GRADED` score exists.
- Quiz maxima sum canonical `question_bank_questions.points` through the actual
  `quiz_questions.bank_question_id` relationship. Future unavailable quizzes are excluded.
- Retakes use the latest **submitted or graded** attempt by attempt number, then attempt ID.
  In-progress/abandoned attempts never replace a completed attempt.
- Question difficulty comes from the canonical bank. Correct-response counts use valid final
  awarded points from that same selected graded attempt, and require the answer's question to
  belong to the attempt's quiz. Ungraded answers are not counted as incorrect responses.
- No R engine, R script implementation, or R-report table is present. No R report or inferential
  result is synthesized. Unsupported R method/report controls are hidden on real data.

## Calculations and empty states

Scores are normalized as `score / maximum * 100`. Each valid final assessment result has equal
weight. Missing and ungraded work never becomes a zero grade. Averages, highest/lowest results,
quiz/assignment comparisons, student bands and weekly charts are calculated from the selected scope.
Dates use the database calendar dates; chart labels include the year.

Submission rates divide submitted/graded eligible work by expected work. With no eligible roster,
the rate is unavailable (an em dash), rather than a misleading 0%. Students enrolled after a closed
assessment deadline are excluded from its expected count unless an actual submission exists.
Current withdrawn/completed enrollments are excluded; this is a current-roster view, not a historical
roster reconstruction. Published assessments with no positive maximum are omitted with a notice.
Invalid/missing final scores are excluded with a notice; they cannot corrupt chart percentages.

Student search/bands/sort narrow the student table. They do not change the class/subject overview
denominator. Subject filters update assessments and all corresponding aggregates/charts. Direct
student links must also satisfy the selected filters. Unassigned subjects/classes return 403;
out-of-scope student/report IDs return 404. No teacher ID is accepted from the browser.

Advanced R Analytics includes an explicitly application-calculated descriptive summary of real
normalized grades: count, mean, median, sample standard deviation (n-1), and linearly interpolated
quartiles. It is not labeled as an R output or saved report. Empty data stays empty; standard deviation
requires at least two grades. t-tests, ANOVA and regression stay unavailable until an actual R
integration exists.

The schema stores no historical assessment-maximum snapshot. Percentages therefore use the current
stored assignment/bank maximum; historical changes to those values cannot be reconstructed here.

## Live observations and validation

Read-only recheck on 22 October 2026 confirmed the reported missing-data cause:
**Athul Mohan is active teacher user ID 10, with zero `subject_teachers` rows**.
Subjects 1 (Maths) and 2 (java) are both assigned to teacher user ID 3. There are
zero rows in `subject_enrollments`. The two existing submissions reference student
user ID 2 and subjects 1/2, but do not supply the missing enrollment relationships.
Login stores `users.user_id` in `loggedInUser`; analytics uses that exact ID.
The joins and filters match the schema. Removing these filters or inferring access
from another teacher's records would violate authorization, so no such change was
made. Legitimate subject assignments and enrollments require team confirmation
and an approved change through the owning workflow; no records were inserted.

The analytics header now shows the verified teacher name and user ID, and empty
states distinguish missing subject assignments from missing enrollments. Class
dropdowns/columns are hidden when no class mapping exists. Subject filters appear
only when they offer a meaningful choice; student controls appear when a roster
exists and include a scoped reset link. The real R tab always shows the available
descriptive panel, without the unsupported method selector, report search or
illustrative method cards. Existing tab layout and styles are retained.

Read-only inspection found two subjects, two subject-teacher assignments, four assignments,
two submissions, and **zero student enrollments or quizzes**. Three published/closed assignments
are visible to the assigned teacher. Unlinked submissions are not used to invent an enrolled roster.
Teachers with no assigned subjects receive an empty workspace.

Compilation and read-only provider previews were used for this change. No Maven test phase and
no new test files are part of this work. No INSERT, UPDATE, DELETE, schema change, migration, or
sample-data script has been executed. Real enrollment links and quiz data are still needed before
those statistics can populate. Any proposed sample inserts require a separate review and approval.

The follow-up compilation passed; a temporary default-profile backend started
with Groq selected and completed `SELECT 1`/`DATABASE()` against `openstudy_db`.
Direct read-only provider previews returned two subjects/three assessments for
teacher 3 and no subjects/students/assessments for teacher 10. Selecting subject 1
as teacher 10 returned 403. All four JSP tabs and representative filters rendered
with HTTP 200 through the existing mock login. Authenticated live browser rendering
for Athul requires his normal login after restarting; no login was bypassed.
