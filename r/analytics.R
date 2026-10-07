empty_frame <- function(columns) {
  values <- lapply(columns, function(type) switch(type,
    character = character(), numeric = numeric(), integer = integer(),
    logical = logical(), date = as.Date(character()),
    timestamp = as.POSIXct(character(), tz = "UTC")
  ))
  names(values) <- names(columns)
  as.data.frame(values, stringsAsFactors = FALSE)
}

parse_openstudy_time <- function(value, timezone) {
  if (inherits(value, "POSIXt")) return(as.POSIXct(value, tz = timezone))
  text <- as.character(value)
  result <- as.POSIXct(rep(NA_real_, length(text)), origin = "1970-01-01", tz = timezone)
  formats <- c("%Y-%m-%d %H:%M:%OS", "%Y-%m-%dT%H:%M:%OS", "%Y-%m-%d")
  for (format in formats) {
    pending <- which(!is.na(text) & !is.na(trimws(text)) & nzchar(trimws(text)) & is.na(result))
    if (!length(pending)) break
    parsed <- suppressWarnings(as.POSIXct(text[pending], format = format, tz = timezone))
    valid <- !is.na(parsed)
    result[pending[valid]] <- parsed[valid]
  }
  result
}

subject_labels <- function(ids, subjects) {
  labels <- rep("(No subject)", length(ids))
  has_id <- !is.na(ids)
  if (!any(has_id)) return(labels)
  index <- match(as.character(ids[has_id]), as.character(subjects$subject_id))
  found <- !is.na(index)
  resolved <- rep(NA_character_, sum(has_id))
  resolved[found] <- as.character(subjects$subject_name[index[found]])
  unresolved_ids <- as.character(ids[has_id])
  resolved[!found] <- paste0("(Unknown subject #", unresolved_ids[!found], ")")
  missing_name <- is.na(resolved) | !nzchar(trimws(resolved))
  resolved[missing_name] <- paste0("(Unnamed subject #", unresolved_ids[missing_name], ")")
  labels[has_id] <- resolved
  labels
}

count_by_value <- function(values, column_name) {
  if (!length(values)) {
    result <- data.frame(value = character(), count = integer(), stringsAsFactors = FALSE)
  } else {
    display <- as.character(values)
    display[is.na(display) | !nzchar(trimws(display))] <- "(Missing)"
    counts <- table(display, useNA = "no")
    result <- data.frame(value = names(counts), count = as.integer(counts), stringsAsFactors = FALSE)
  }
  names(result)[1] <- column_name
  result
}

count_enum_values <- function(values, allowed_values, column_name) {
  observed <- count_by_value(values, column_name)
  allowed_values <- as.character(allowed_values)
  observed_values <- as.character(observed[[column_name]])
  result_values <- unique(c(allowed_values, observed_values))
  result_counts <- integer(length(result_values))

  if (nrow(observed)) {
    result_counts[match(observed_values, result_values)] <- as.integer(observed$count)
  }

  result <- data.frame(value = result_values, count = result_counts, stringsAsFactors = FALSE)
  names(result)[1] <- column_name
  result
}

make_planner_analytics <- function(data, timezone = "UTC", analysis_time = Sys.time()) {
  assignments <- data$assignments
  sessions <- data$study_sessions
  subjects <- data$subjects

  if (is.null(assignments)) assignments <- data.frame()
  if (is.null(sessions)) sessions <- data.frame()
  if (is.null(subjects)) subjects <- data.frame(subject_id = character(), subject_name = character())

  # Empty inputs from a DBI query retain their database columns. These guards
  # also make the calculations safe for deliberately minimal test inputs.
  for (name in c("session_id", "subject_id", "assignment_id", "start_time", "end_time")) {
    if (!name %in% names(sessions)) sessions[[name]] <- if (name %in% c("start_time", "end_time")) as.POSIXct(character(), tz = timezone) else character()
  }
  for (name in c("assignment_id", "subject_id", "due_date", "priority", "status")) {
    if (!name %in% names(assignments)) assignments[[name]] <- if (name == "due_date") as.POSIXct(character(), tz = timezone) else character()
  }
  for (name in c("subject_id", "subject_name")) if (!name %in% names(subjects)) subjects[[name]] <- character()

  raw_start <- as.character(sessions$start_time)
  raw_end <- as.character(sessions$end_time)
  sessions$start_time <- parse_openstudy_time(sessions$start_time, timezone)
  sessions$end_time <- parse_openstudy_time(sessions$end_time, timezone)
  start_absent <- is.na(raw_start) | !nzchar(trimws(raw_start))
  end_absent <- is.na(raw_end) | !nzchar(trimws(raw_end))
  missing_timestamp <- start_absent | end_absent
  invalid_timestamp <- (!start_absent & is.na(sessions$start_time)) |
    (!end_absent & is.na(sessions$end_time))
  missing_time <- missing_timestamp | invalid_timestamp
  duration_hours <- as.numeric(difftime(sessions$end_time, sessions$start_time, units = "hours"))
  invalid_duration <- !missing_time & (!is.finite(duration_hours) | duration_hours <= 0)
  session_valid <- !missing_time & !invalid_duration

  sessions$duration_hours <- duration_hours
  valid_sessions <- sessions[session_valid, , drop = FALSE]
  valid_sessions$study_date <- as.Date(valid_sessions$start_time, tz = timezone)
  valid_sessions$subject <- subject_labels(valid_sessions$subject_id, subjects)

  total_sessions <- nrow(sessions)
  session_durations <- valid_sessions$duration_hours
  total_hours <- sum(session_durations, na.rm = TRUE)
  session_summary <- data.frame(
    total_study_sessions = as.integer(total_sessions),
    valid_study_sessions = as.integer(nrow(valid_sessions)),
    total_study_hours = total_hours,
    average_session_hours = if (length(session_durations)) mean(session_durations) else 0,
    minimum_session_hours = if (length(session_durations)) min(session_durations) else 0,
    maximum_session_hours = if (length(session_durations)) max(session_durations) else 0
  )

  daily <- empty_frame(c(date = "date", study_hours = "numeric"))
  weekly <- empty_frame(c(week_start = "date", study_hours = "numeric"))
  if (nrow(valid_sessions)) {
    daily_sum <- aggregate(valid_sessions$duration_hours,
      by = list(date = valid_sessions$study_date), FUN = sum)
    names(daily_sum)[2] <- "study_hours"
    daily <- daily_sum[order(daily_sum$date), , drop = FALSE]

    weekdays_from_monday <- (as.POSIXlt(valid_sessions$study_date)$wday + 6L) %% 7L
    week_start <- valid_sessions$study_date - weekdays_from_monday
    weekly_sum <- aggregate(valid_sessions$duration_hours,
      by = list(week_start = week_start), FUN = sum)
    names(weekly_sum)[2] <- "study_hours"
    weekly <- weekly_sum[order(weekly_sum$week_start), , drop = FALSE]
  }

  subject_study <- empty_frame(c(subject = "character", study_hours = "numeric", session_count = "integer"))
  if (nrow(valid_sessions)) {
    subject_study <- aggregate(valid_sessions$duration_hours,
      by = list(subject = valid_sessions$subject), FUN = sum)
    names(subject_study)[2] <- "study_hours"
    subject_counts <- aggregate(valid_sessions$duration_hours,
      by = list(subject = valid_sessions$subject), FUN = length)
    names(subject_counts)[2] <- "session_count"
    subject_study <- merge(subject_study, subject_counts, by = "subject", sort = FALSE)
    subject_study <- subject_study[order(-subject_study$study_hours, subject_study$subject), , drop = FALSE]
  }

  raw_due_date <- as.character(assignments$due_date)
  assignments$due_date <- parse_openstudy_time(assignments$due_date, timezone)
  missing_due_date <- is.na(raw_due_date) | !nzchar(trimws(raw_due_date))
  invalid_due_date <- !missing_due_date & is.na(assignments$due_date)
  status <- trimws(as.character(assignments$status))
  priority <- trimws(as.character(assignments$priority))
  known_status <- c("PENDING", "IN_PROGRESS", "COMPLETED")
  known_priority <- c("LOW", "MEDIUM", "HIGH")
  completed <- !is.na(status) & status == "COMPLETED"
  pending <- !is.na(status) & status == "PENDING"
  in_progress <- !is.na(status) & status == "IN_PROGRESS"
  overdue <- !is.na(assignments$due_date) & assignments$due_date < as.POSIXct(analysis_time, tz = timezone) & !completed
  assignments$subject <- subject_labels(assignments$subject_id, subjects)
  assignments$overdue <- overdue
  assignments$status_clean <- status
  assignments$priority_clean <- priority

  assignment_count <- nrow(assignments)
  assignment_summary <- data.frame(
    total_assignments = as.integer(assignment_count),
    completed_assignments = as.integer(sum(completed)),
    pending_assignments = as.integer(sum(pending)),
    in_progress_assignments = as.integer(sum(in_progress)),
    overdue_assignments = as.integer(sum(overdue)),
    completion_percentage = if (assignment_count) 100 * sum(completed) / assignment_count else 0
  )

  status_counts <- count_enum_values(assignments$status, known_status, "status")
  priority_counts <- count_enum_values(assignments$priority, known_priority, "priority")
  overdue_assignments <- assignments[overdue, , drop = FALSE]
  overdue_by_priority <- count_by_value(overdue_assignments$priority, "priority")
  names(overdue_by_priority)[2] <- "overdue_count"
  overdue_by_subject <- count_by_value(overdue_assignments$subject, "subject")
  names(overdue_by_subject)[2] <- "overdue_count"

  subject_assignments <- empty_frame(c(
    subject = "character", total_assignments = "integer", completed_assignments = "integer",
    pending_assignments = "integer", completion_percentage = "numeric", overdue_assignments = "integer"
  ))
  if (assignment_count) {
    assignment_counts <- aggregate(rep(1L, assignment_count), by = list(subject = assignments$subject), FUN = sum)
    names(assignment_counts)[2] <- "total_assignments"
    complete_counts <- aggregate(as.integer(completed), by = list(subject = assignments$subject), FUN = sum)
    names(complete_counts)[2] <- "completed_assignments"
    pending_counts <- aggregate(as.integer(pending), by = list(subject = assignments$subject), FUN = sum)
    names(pending_counts)[2] <- "pending_assignments"
    overdue_counts <- aggregate(as.integer(overdue), by = list(subject = assignments$subject), FUN = sum)
    names(overdue_counts)[2] <- "overdue_assignments"
    subject_assignments <- Reduce(function(left, right) merge(left, right, by = "subject", all = TRUE, sort = FALSE),
      list(assignment_counts, complete_counts, pending_counts, overdue_counts))
    subject_assignments[is.na(subject_assignments)] <- 0L
    subject_assignments$completion_percentage <- ifelse(
      subject_assignments$total_assignments > 0,
      100 * subject_assignments$completed_assignments / subject_assignments$total_assignments, 0
    )
    subject_assignments <- subject_assignments[order(subject_assignments$subject), , drop = FALSE]
  }

  session_assignment_counts <- empty_frame(c(assignment_id = "character", session_count = "integer", study_hours = "numeric"))
  if (nrow(valid_sessions)) {
    associated <- !is.na(valid_sessions$assignment_id)
    if (any(associated)) {
      session_assignment_counts <- aggregate(valid_sessions$duration_hours[associated],
        by = list(assignment_id = as.character(valid_sessions$assignment_id[associated])), FUN = sum)
      names(session_assignment_counts)[2] <- "study_hours"
      counts <- aggregate(valid_sessions$duration_hours[associated],
        by = list(assignment_id = as.character(valid_sessions$assignment_id[associated])), FUN = length)
      names(counts)[2] <- "session_count"
      session_assignment_counts <- merge(session_assignment_counts, counts, by = "assignment_id", sort = FALSE)
    }
  }
  assignment_insights <- data.frame(
    assignments_with_study_sessions = as.integer(sum(as.character(assignments$assignment_id) %in% session_assignment_counts$assignment_id)),
    assignments_without_study_sessions = as.integer(sum(!(as.character(assignments$assignment_id) %in% session_assignment_counts$assignment_id))),
    study_hours_linked_to_assignments = sum(session_assignment_counts$study_hours, na.rm = TRUE)
  )

  subject_ids <- as.character(subjects$subject_id)
  quality <- data.frame(
    source = c("study_sessions", "study_sessions", "study_sessions", "study_sessions", "study_sessions", "assignments", "assignments", "assignments", "assignments", "subjects"),
    issue = c("rows loaded", "rows excluded: missing start or end time", "rows excluded: invalid start or end timestamp", "rows excluded: non-positive/invalid duration", "rows with no subject_id", "rows with missing due_date", "rows with invalid due_date", "rows with unexpected status", "rows with unexpected priority", "rows loaded"),
    count = c(
      nrow(sessions), sum(missing_timestamp), sum(invalid_timestamp), sum(invalid_duration), sum(is.na(sessions$subject_id)),
      sum(missing_due_date), sum(invalid_due_date), sum(is.na(status) | !status %in% known_status),
      sum(is.na(priority) | !priority %in% known_priority), nrow(subjects)
    ),
    stringsAsFactors = FALSE
  )
  quality <- rbind(quality, data.frame(
    source = "study_sessions", issue = "rows with missing assignment_id", count = sum(is.na(sessions$assignment_id)),
    stringsAsFactors = FALSE
  ))
  if (nrow(assignments)) {
    has_subject <- !is.na(assignments$subject_id)
    quality <- rbind(quality, data.frame(
      source = "assignments", issue = "rows with missing subject_id", count = sum(!has_subject),
      stringsAsFactors = FALSE
    ))
    missing_subject_ref <- has_subject & !(as.character(assignments$subject_id) %in% subject_ids)
    quality <- rbind(quality, data.frame(
      source = "assignments", issue = "rows with unknown subject_id", count = sum(missing_subject_ref),
      stringsAsFactors = FALSE
    ))
  }
  if (nrow(sessions)) {
    has_subject <- !is.na(sessions$subject_id)
    missing_subject_ref <- has_subject & !(as.character(sessions$subject_id) %in% subject_ids)
    quality <- rbind(quality, data.frame(
      source = "study_sessions", issue = "rows with unknown subject_id", count = sum(missing_subject_ref),
      stringsAsFactors = FALSE
    ))
    known_assignments <- as.character(assignments$assignment_id)
    has_assignment <- !is.na(sessions$assignment_id)
    missing_assignment_ref <- has_assignment & !(as.character(sessions$assignment_id) %in% known_assignments)
    quality <- rbind(quality, data.frame(
      source = "study_sessions", issue = "rows with unknown assignment_id", count = sum(missing_assignment_ref),
      stringsAsFactors = FALSE
    ))
  }

  messages <- character()
  if (!nrow(valid_sessions)) messages <- c(messages, "No valid study sessions available for analysis.")
  if (!assignment_count) messages <- c(messages, "No assignments available for analysis.")
  if (nrow(valid_sessions) && nrow(unique(daily["date"])) < 2L) messages <- c(messages, "Daily trend chart needs at least two study dates.")
  if (nrow(weekly) < 2L) messages <- c(messages, "Weekly trend chart needs at least two calendar weeks (weeks start Monday).")

  list(
    analysis_time = as.POSIXct(analysis_time, tz = timezone),
    session_summary = session_summary,
    assignment_summary = assignment_summary,
    assignment_insights = assignment_insights,
    valid_sessions = valid_sessions,
    assignments = assignments,
    daily_study = daily,
    weekly_study = weekly,
    subject_study = subject_study,
    assignment_status = status_counts,
    assignment_priority = priority_counts,
    overdue_assignments = overdue_assignments,
    overdue_by_priority = overdue_by_priority,
    overdue_by_subject = overdue_by_subject,
    subject_assignments = subject_assignments,
    assignment_session_hours = session_assignment_counts,
    data_quality = quality,
    messages = messages
  )
}

make_summary_metrics <- function(analytics) {
  subject <- "Unavailable"
  named_subject <- !grepl("^\\(", analytics$subject_study$subject)
  if (nrow(analytics$subject_study) && any(named_subject)) {
    subject <- analytics$subject_study$subject[which(named_subject)[1]]
  }
  data.frame(
    metric = c(
      "Total study hours", "Total study sessions", "Valid study sessions",
      "Average session duration (hours)", "Total assignments", "Completed assignments",
      "Pending assignments", "Overdue assignments", "Assignment completion (%)",
      "Most-studied subject"
    ),
    value = c(
      analytics$session_summary$total_study_hours,
      analytics$session_summary$total_study_sessions,
      analytics$session_summary$valid_study_sessions,
      analytics$session_summary$average_session_hours,
      analytics$assignment_summary$total_assignments,
      analytics$assignment_summary$completed_assignments,
      analytics$assignment_summary$pending_assignments,
      analytics$assignment_summary$overdue_assignments,
      analytics$assignment_summary$completion_percentage,
      subject
    ),
    stringsAsFactors = FALSE
  )
}
