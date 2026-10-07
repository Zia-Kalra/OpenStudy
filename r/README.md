# OpenStudy R Analytics

This standalone module reads planner records from the existing MySQL database, calculates descriptive assignment and study-session metrics, and saves summary CSV files and charts. Spring Boot remains responsible for planner CRUD and application behavior. The R module does not authenticate users and does not write to the database.

## Schema used

The schema is defined in `database/schema.sql` and `database/assignments_planner.sql`; the Spring datasource config confirms the database name `openstudy_db`.

- `study_sessions`: `session_id`, `student_id`, nullable `subject_id`, nullable `assignment_id`, `start_time`, `end_time`.
- `assignments`: `assignment_id`, `student_id`, nullable `subject_id`, `due_date`, `priority`, `status`.
- `subjects`: `subject_id`, `student_id`, `subject_name`.

No student table columns are queried. In particular, password hashes are never selected. Subject names are joined in R by `subject_id`; missing references are labeled and counted in the data-quality output.

## Requirements

- R 4.1 or newer is recommended. The module was authored and checked with R 4.6.1.
- MySQL reachable with a dedicated account that has **SELECT-only** privileges on `assignments`, `study_sessions`, and `subjects`.
- R packages `DBI`, `RMariaDB`, and `ggplot2`.

Install packages once from an R console:

```r
install.packages(c("DBI", "RMariaDB", "ggplot2"))
```

The module does not install packages automatically. `DBI` and `RMariaDB` are required to connect and read the database; `ggplot2` is required to save charts.

## Configuration

Set the connection values in the environment. Host, port, and database have non-sensitive defaults (`localhost`, `3306`, `openstudy_db`). Database user and password are required, with no password fallback. The timezone defaults to the R system timezone; set it explicitly to interpret MySQL `DATETIME` values consistently. The configured timezone controls returned timestamp display and date grouping.

PowerShell example (replace the user and placeholder locally; do not save a real password in source control):

```powershell
$env:OPENSTUDY_DB_HOST = "localhost"
$env:OPENSTUDY_DB_PORT = "3306"
$env:OPENSTUDY_DB_NAME = "openstudy_db"
$env:OPENSTUDY_DB_USER = "<select-only-analytics-user>"
$env:OPENSTUDY_DB_PASSWORD = "<your-local-password>"
$env:OPENSTUDY_ANALYTICS_TZ = "Asia/Kolkata"
```

Use a dedicated MySQL account limited to `SELECT` on those three tables rather than the application administrator or root account. The code only issues static `SELECT` queries. The optional student filter is bound as a query parameter and is not SQL string interpolation.

## Run

From the repository root, for an overall report:

```powershell
Rscript r/run_analytics.R
```

For a single student's records:

```powershell
Rscript r/run_analytics.R --student-id 1
```

With no filter, results summarize all students visible to the configured database account. A supplied positive `student_id` filters all three tables. R does not authenticate or authorize that ID; the caller is responsible for choosing a permitted scope.

Run the calculation and chart checks (uses temporary in-memory test fixtures only; it does not connect to MySQL or modify database records):

```powershell
Rscript r/tests/test_analytics.R
```

## Analytics and interpretation

- **Study sessions:** count all rows; calculate duration in hours from `end_time - start_time`; report total, average, minimum, and maximum among valid sessions. Missing/unparseable timestamps and non-positive durations are excluded from duration and trend calculations and counted in data quality.
- **Subject study time:** sum valid session hours by subject. Nullable subjects appear as `(No subject)`; unresolved IDs are labeled and counted.
- **Daily and weekly trends:** daily totals use the session start date. Weeks begin Monday and end Sunday. The weekly chart requires at least two represented weeks; daily trend requires at least two dates. Otherwise the report prints a skip message and creates no misleading trend chart.
- **Assignment status and priority:** counts include unexpected or missing database values as visible categories, while the headline completed/pending/in-progress counts use only the existing enum values. Unexpected values are reported in data quality.
- **Completion:** completed assignments divided by all assignment rows, times 100; returns 0% for an empty assignment table.
- **Overdue:** due timestamp is earlier than the runtime analysis timestamp and status is not `COMPLETED`. Invalid/missing due dates are not considered overdue and are counted.
- **Subject assignment summaries:** total, completed, pending, completion percentage, and overdue count by subject, including no-subject rows.
- **Study and assignment association:** sums valid session hours with a non-NULL `assignment_id`, and counts assignments with or without a matching session. This is descriptive only and implies no causal effect.
- **Data quality:** reports invalid rows, missing IDs, unknown subject/assignment references, and unexpected enum values without modifying source records.

## Outputs

Generated files are written under `r/output/` and ignored by Git:

- `summary_metrics.csv` — headline report metrics.
- `data_quality.csv` — row counts and excluded/unexpected data counts.
- `daily_study_hours.png` — daily study hours when two or more dates exist.
- `weekly_study_hours.png` — weekly study hours when two or more weeks exist.
- `subject_study_hours.png` — subject totals when valid sessions exist.
- `assignment_status.png` — status counts when assignments exist.
- `assignment_priority.png` — priority counts when assignments exist.

CSV and chart outputs reflect actual database records in a regular run. No demo data is inserted or used by the entry point. Test fixtures exist only in `r/tests/test_analytics.R` to verify calculations and empty-data behavior without touching MySQL.

## Read-only guarantee

The query loader permits only three internal table names and approved column lists; it constructs `SELECT` statements and binds optional student filters using DBI parameters. There are no data-changing SQL statements in this module. Also enforce read-only access with MySQL privileges on the analytics account; R-side conventions alone are not a database security boundary.
