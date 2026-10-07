script_dir <- normalizePath(file.path(dirname(sub("^--file=", "", grep("^--file=", commandArgs(FALSE), value = TRUE)[1])), ".."), winslash = "/")
source(file.path(script_dir, "analytics.R"))
source(file.path(script_dir, "plots.R"))

check_enum_count_shape <- function(result, expected_values) {
  stopifnot(identical(names(result), c("status", "count")))
  stopifnot(is.character(result$status), is.integer(result$count))
  stopifnot(identical(result$status, expected_values))
}

# Regression: the former implementation rbound data.frame(value, 0L) to a
# data frame named status/count when an unexpected value was present and a
# standard enum was absent. The generated names differed and rbind failed.
check_enum_count_shape(
  count_enum_values("UNEXPECTED", c("PENDING", "IN_PROGRESS", "COMPLETED"), "status"),
  c("PENDING", "IN_PROGRESS", "COMPLETED", "UNEXPECTED")
)
check_enum_count_shape(
  count_enum_values(character(), c("PENDING", "IN_PROGRESS", "COMPLETED"), "status"),
  c("PENDING", "IN_PROGRESS", "COMPLETED")
)
check_enum_count_shape(
  count_enum_values(NULL, c("PENDING", "IN_PROGRESS", "COMPLETED"), "status"),
  c("PENDING", "IN_PROGRESS", "COMPLETED")
)
check_enum_count_shape(
  count_enum_values("PENDING", c("PENDING", "IN_PROGRESS", "COMPLETED"), "status"),
  c("PENDING", "IN_PROGRESS", "COMPLETED")
)
check_enum_count_shape(
  count_enum_values(c("COMPLETED", "PENDING", "COMPLETED"), c("PENDING", "IN_PROGRESS", "COMPLETED"), "status"),
  c("PENDING", "IN_PROGRESS", "COMPLETED")
)
check_enum_count_shape(
  count_enum_values(c(NA_character_, "PENDING"), c("PENDING", "IN_PROGRESS", "COMPLETED"), "status"),
  c("PENDING", "IN_PROGRESS", "COMPLETED", "(Missing)")
)
missing_and_unexpected <- count_enum_values(
  c(NA_character_, "ODD"), c("LOW", "MEDIUM", "HIGH"), "priority"
)
stopifnot(identical(names(missing_and_unexpected), c("priority", "count")))
stopifnot(identical(missing_and_unexpected$priority, c("LOW", "MEDIUM", "HIGH", "(Missing)", "ODD")))
stopifnot(identical(missing_and_unexpected$count, c(0L, 0L, 0L, 1L, 1L)))

subjects <- data.frame(
  subject_id = c(11L, 12L),
  subject_name = c("Java", "DBMS"),
  stringsAsFactors = FALSE
)
sessions <- data.frame(
  session_id = 1:7,
  subject_id = c(11L, 12L, NA, 11L, 11L, 12L, NA),
  assignment_id = c(101L, 102L, NA, 999L, NA, NA, NA),
  start_time = c(
    "2026-10-05 09:00:00", "2026-10-06 10:00:00", "2026-10-12 08:00:00",
    "2026-10-12 12:00:00", "not-a-time", "2026-10-13 09:00:00", NA
  ),
  end_time = c(
    "2026-10-05 10:30:00", "2026-10-06 12:00:00", "2026-10-12 09:00:00",
    "2026-10-12 11:00:00", "2026-10-12 15:00:00", "2026-10-13 09:00:00",
    "2026-10-14 09:00:00"
  ),
  stringsAsFactors = FALSE
)
assignments <- data.frame(
  assignment_id = 101:104,
  subject_id = c(11L, 12L, NA, 11L),
  due_date = c(
    "2026-10-01 12:00:00", "2026-10-01 12:00:00",
    "2026-11-01 12:00:00", "invalid-date"
  ),
  priority = c("HIGH", "MEDIUM", "LOW", "OTHER"),
  status = c("COMPLETED", "PENDING", "IN_PROGRESS", "UNKNOWN"),
  stringsAsFactors = FALSE
)

result <- make_planner_analytics(
  list(assignments = assignments, study_sessions = sessions, subjects = subjects),
  timezone = "UTC", analysis_time = as.POSIXct("2026-10-07 12:00:00", tz = "UTC")
)
stopifnot(nrow(result$valid_sessions) == 3L)
stopifnot(result$session_summary$total_study_sessions == 7L)
stopifnot(isTRUE(all.equal(result$session_summary$total_study_hours, 4.5)))
stopifnot(isTRUE(all.equal(result$session_summary$average_session_hours, 1.5)))
stopifnot(nrow(result$daily_study) == 3L, nrow(result$weekly_study) == 2L)
stopifnot(result$weekly_study$week_start[1] == as.Date("2026-10-05"))
stopifnot(result$subject_study$subject[1] == "DBMS")
stopifnot(result$assignment_summary$total_assignments == 4L)
stopifnot(result$assignment_summary$completed_assignments == 1L)
stopifnot(result$assignment_summary$pending_assignments == 1L)
stopifnot(result$assignment_summary$overdue_assignments == 1L)
stopifnot(result$assignment_summary$completion_percentage == 25)
stopifnot(!any(result$overdue_assignments$assignment_id == 101L))
stopifnot(result$assignment_insights$assignments_with_study_sessions == 2L)
stopifnot(result$assignment_insights$assignments_without_study_sessions == 2L)
stopifnot(any(result$data_quality$issue == "rows excluded: invalid start or end timestamp" & result$data_quality$count == 1L))
stopifnot(any(result$data_quality$issue == "rows excluded: missing start or end time" & result$data_quality$count == 1L))
stopifnot(any(result$data_quality$issue == "rows excluded: non-positive/invalid duration" & result$data_quality$count == 2L))
stopifnot(any(result$data_quality$issue == "rows with unexpected status" & result$data_quality$count == 1L))
stopifnot(any(result$data_quality$issue == "rows with unknown assignment_id" & result$data_quality$count == 1L))

empty_result <- make_planner_analytics(list(
  assignments = assignments[0, ], study_sessions = sessions[0, ], subjects = subjects[0, ]
), timezone = "UTC", analysis_time = as.POSIXct("2026-10-07 12:00:00", tz = "UTC"))
stopifnot(empty_result$session_summary$total_study_hours == 0)
stopifnot(empty_result$assignment_summary$completion_percentage == 0)
stopifnot(empty_result$assignment_summary$overdue_assignments == 0)
stopifnot(nrow(empty_result$daily_study) == 0L, nrow(empty_result$subject_assignments) == 0L)
stopifnot(any(grepl("No valid study sessions", empty_result$messages)))
empty_chart_dir <- tempfile("openstudy-empty-charts-")
empty_charts <- save_openstudy_plots(empty_result, empty_chart_dir)
stopifnot(length(empty_charts$files) == 0L)
stopifnot(any(grepl("no assignments", empty_charts$messages)))
unlink(empty_chart_dir, recursive = TRUE)

chart_dir <- tempfile("openstudy-charts-")
chart_result <- save_openstudy_plots(result, chart_dir)
stopifnot(length(chart_result$files) == 5L)
stopifnot(all(file.exists(file.path(chart_dir, chart_result$files))))
summary_path <- file.path(chart_dir, "summary_metrics.csv")
quality_path <- file.path(chart_dir, "data_quality.csv")
utils::write.csv(make_summary_metrics(result), summary_path, row.names = FALSE)
utils::write.csv(result$data_quality, quality_path, row.names = FALSE)
stopifnot(file.exists(summary_path), file.exists(quality_path))
stopifnot(identical(names(utils::read.csv(summary_path)), c("metric", "value")))
stopifnot(identical(names(utils::read.csv(quality_path)), c("source", "issue", "count")))
unlink(chart_dir, recursive = TRUE)

message("OpenStudy analytics calculations, empty-data behavior, and chart generation passed.")
