# R scripts (statistical analysis)

Used by the analytics module (Member 4). Java writes a temporary CSV, runs one script with `Rscript`, and reads the JSON the script writes. Nothing is stored in R.

## Install
1. Install R 4.0 or newer (https://cran.r-project.org). On Windows the default install is fine.
2. Install the one required package (once): `Rscript -e "install.packages('jsonlite', repos='https://cloud.r-project.org')"`
3. Check: `Rscript --version`. If `Rscript` is not on PATH, set `RSCRIPT_PATH` to its full path (see `.env.example`).

Only base R and `jsonlite` are used.

## Scripts
| Script | What it computes |
|---|---|
| `descriptive_analysis.R` | mean, median, mode (only when meaningful), variance, SD, min/max, Q1/Q3, IQR, 95% CI, box-plot summary, 10-point frequency distribution, grade bands, per-type and per-subject summaries |
| `performance_trends.R` | monthly means, 3-point moving average, linear trend test (needs 5+ results) |
| `hypothesis_testing.R` | one-sample t-test vs benchmark, paired t-test quiz vs assignment (Wilcoxon fallback), one-way ANOVA across subjects (Shapiro/Fligner checks, Kruskal-Wallis fallback, Tukey post hoc), Pearson/Spearman correlation |
| `regression_analysis.R` | simple regression (assignment mean ~ quiz mean; score ~ time), multiple regression (score ~ time + type + subject) with R-squared, adjusted R-squared, F-test, residual analysis, VIF |
| `analytics_common.R` | shared input validation and JSON output helpers |

A test is skipped (not forced) when its data requirements are not met. Examples: fewer than 5 students, constant scores, fewer than 2 subjects with 3+ students, fewer than 10 observations per regression predictor. Skipped tests return `"status":"insufficient_data"` with the reason.

## Running a script by hand
```
Rscript R/descriptive_analysis.R input.csv output.json pass_mark=50
Rscript R/hypothesis_testing.R input.csv output.json alpha=0.05 benchmark=50
```
Input CSV header: `student_key,assessment_type,subject_name,assessment_title,percentage,completed_at`
(`assessment_type` is QUIZ or ASSIGNMENT, `completed_at` is `yyyy-MM-dd HH:mm:ss`, `student_key` is any label such as S001).

Exit code 0 = JSON written. Exit code 2 = JSON with `"status":"error"` written. Scripts never print student data.
