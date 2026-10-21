# Teacher Analytics — Member 5

Teacher Analytics is separate from Nitya's Student Analytics. It monitors assigned
classes, students, assessments, and saved reports. No student routes, student services,
R scripts, or database files are replaced by this module.

## Local demo

From the repository root:

```powershell
.\backend\mvnw.cmd -f backend\pom.xml spring-boot:run "-Dspring-boot.run.profiles=mock"
```

Open `http://localhost:8080/teacher/analytics`. The sidebar and dashboard quick action
now lead to this teacher route. Fixtures cover two assigned classes, 20 students,
two subjects, 16 assessments, and four weeks. They require neither MySQL nor R.

## Routes

All routes are GET pages under `/teacher/analytics`:

| Route | View |
| --- | --- |
| `/teacher/analytics` | Class overview and aggregate charts |
| `/teacher/analytics/students` | Searchable, sortable performance table |
| `/teacher/analytics/students/{studentId}` | Authorized student drill-down |
| `/teacher/analytics/assessments` | Assessment/subject comparisons and question difficulty |
| `/teacher/analytics/reports` | Method-filtered saved report workspace |
| `/teacher/analytics/reports/{reportId}` | Authorized saved report preview |

Optional query parameters: `classId`, `subjectId`, `search` (100 characters), `band`,
`sort`, and `method`. Student search and performance bands narrow the table, not the
class overview denominator. IDs and filter choices are bounded/validated. Teacher IDs
and authorization scopes are never accepted from HTTP input. Responses use `no-store`.

## Architecture and authorization

`TeacherAnalyticsController → TeacherAnalyticsService → TeacherAnalyticsDataProvider`
preserves the established controller/service/provider design. `TeacherIdentityDTO`
and the existing teacher navigation/layout are reused. Analytics data and view DTOs
live in `dto.analytics`; providers live in `provider.analytics`.

`TeacherAnalyticsAccessProvider.resolve(Principal)` is the authentication integration
boundary. It returns an immutable `TeacherAnalyticsScope` containing a verified
teacher identity and server-derived class and subject assignments.

- Local demonstration access is opt-in through `openstudy.teacher-analytics.demo-access=true`.
  The default application properties enable it so Teacher Analytics works with Aiven on
  the default profile and with the database-free `mock` profile. Anonymous access is
  limited to the labeled demo teacher. Any other principal is rejected. This is not
  production login. The mock access bean is disabled on `prod`/`production` profiles.
- Without that flag, and whenever a real adapter is missing on production, the
  conditional fallback fails closed with HTTP 503 before data is loaded. Member 1 can
  supply a `TeacherAnalyticsAccessProvider` bean without modifying the controller. The
  adapter must verify login and TEACHER role and resolve assignments from trusted
  server-side identity; a principal name alone is insufficient.
- Unauthorized class/subject filters return 403. Student/report IDs outside the
  selected authorized scope return 404. Aggregates, feedback, question results, and
  reports are constrained by the same scope. Cross-class/subject reports require
  authorization to every included class and subject.
- The service intersects all adapter rows with the trusted scope as defense in depth.
  This does not replace authorization inside future database and R adapters.
- Demo records cannot be returned for a non-demo identity. A real-data adapter must
  return `demo=false` and use the authenticated access adapter.

## MySQL integration boundary

Implement `TeacherAnalyticsDataProvider.load(scope)` and select it with
`openstudy.teacher-analytics.provider`. The default provider is `mock`.

Return immutable normalized `TeacherAnalyticsData.Snapshot` rows. Each assessment
identifies its class, subject, type, maximum score, and date; each result references
a student and assessment and has a `GRADED`, `SUBMITTED`, or `MISSING` status.
Only graded results carry a numeric score. Scores must lie within the assessment
maximum. Unknown/no submitted rows are counted as missing expected work.

Database adapters must use parameterized queries and join trusted teacher assignments
and active student enrollments before loading results. The existing database defines
subject-teacher/enrollment relationships; the demo cohort IDs are not new database
tables. Agree cohort mapping and enrollment eligibility with the database owner before
writing the JDBC adapter. Do not add schema or migrations for the UI identifiers.
Question-response counts must be scoped by the same assignments, not global counts.
Production pagination and term/date selection should be added at this boundary when
the live data volume and eligibility rules are agreed.

## Metric definitions

- Every graded score is normalized as `score / assessment maximum × 100`.
- Class/type/subject means use equally weighted graded results. Highest/lowest are
  individual normalized assessment results, not student averages.
- Student averages use that student's applicable graded assessments.
- Submission rates count submitted or graded work divided by expected eligible work.
  Missing and submitted-but-ungraded work are not converted into zero grades.
- Distribution bands count students by their graded average, with a separate not-graded
  band. Search filters do not change these class totals.
- Weekly bars group by assessment date, starting Monday. Real adapters must map dates
  to the school's agreed timezone. Quiz questions use adapter-supplied correct/answered
  counts; author-assigned difficulty is displayed independently.

## R integration boundary

`TeacherRAnalyticsProvider.savedReports(scope, query)` returns immutable stored report
outputs. Select a future adapter with `openstudy.teacher-analytics.r-provider`.
Report metadata must identify every included class and subject and whether it is demo
data. Reports include metrics, interpretation text, and chart data.

The mock provider returns **precomputed illustrative fixtures**, not statistics derived
from dashboard records. Java performs only basic dashboard aggregations; it does not
calculate standard deviation, t-tests, ANOVA, regression, or p-values and never launches
R processes. A future adapter should reuse the team's existing R engine/report store,
preserving pairing, sample size, missing-data policy, assumptions and diagnostics.
Never construct shell commands or file paths from request parameters. Scope and
authorization must also be enforced by the R/report-store integration itself.

No report-generation POST endpoint or persistence is introduced in the demo. Production
login, MySQL mapping, R execution, and saved-report storage remain integration work.

## Checks

```powershell
.\backend\mvnw.cmd -f backend\pom.xml test
node --test backend\src\test\javascript\teacher-dashboard.test.cjs
```

Teacher tests cover mock fixtures, normalized scores, missing/ungraded work, filtering,
authorization denial, adapter-scope defense, fail-closed production access, immutable
contracts, and all six JSP routes on embedded Tomcat. JSP tests also check XSS escaping,
deep links, table filtering, assets, and the existing dashboard. No shared database,
live Gemini API, or R engine is used by these checks.

## File manifest

New files:

```text
backend/src/main/java/com/openstudy/openstudy/
  config/TeacherAnalyticsAccessConfiguration.java
  controller/TeacherAnalyticsController.java
  service/TeacherAnalyticsService.java
  dto/analytics/TeacherAnalyticsScope.java
  dto/analytics/TeacherAnalyticsQuery.java
  dto/analytics/TeacherAnalyticsData.java
  dto/analytics/TeacherAnalyticsDTO.java
  provider/analytics/TeacherAnalyticsAccessProvider.java
  provider/analytics/MockTeacherAnalyticsAccessProvider.java
  provider/analytics/UnavailableTeacherAnalyticsAccessProvider.java
  provider/analytics/TeacherAnalyticsDataProvider.java
  provider/analytics/MockTeacherAnalyticsDataProvider.java
  provider/analytics/TeacherRAnalyticsProvider.java
  provider/analytics/MockTeacherRAnalyticsProvider.java
backend/src/main/resources/static/css/teacher-analytics.css
backend/src/main/webapp/WEB-INF/jsp/teacher/
  analytics.jsp
  analytics/overview.jsp
  analytics/students.jsp
  analytics/assessments.jsp
  analytics/reports.jsp
backend/src/test/java/com/openstudy/openstudy/
  controller/TeacherAnalyticsControllerTest.java
  controller/TeacherAnalyticsPageTest.java
  provider/TeacherAnalyticsAccessTest.java
  service/TeacherAnalyticsServiceTest.java
docs/teacher-analytics.md
```

Existing files updated only to integrate teacher navigation/layout and its regression check:

```text
backend/src/main/java/com/openstudy/openstudy/config/TeacherModuleProperties.java
backend/src/main/java/com/openstudy/openstudy/controller/TeacherLayoutAdvice.java
backend/src/main/java/com/openstudy/openstudy/service/TeacherDashboardService.java
backend/src/main/resources/application.properties
backend/src/main/webapp/WEB-INF/jsp/teacher/fragments/sidebar.jsp
backend/src/main/webapp/WEB-INF/jsp/teacher/fragments/footer.jsp
backend/src/test/java/com/openstudy/openstudy/service/TeacherDashboardServiceTest.java
```
