openstudy_read_table <- function(connection, table, columns, student_id = NULL) {
  allowed <- list(
    assignments = c("assignment_id", "student_id", "subject_id", "due_date", "priority", "status"),
    study_sessions = c("session_id", "student_id", "subject_id", "assignment_id", "start_time", "end_time"),
    subjects = c("subject_id", "student_id", "subject_name")
  )
  if (!table %in% names(allowed) || !all(columns %in% allowed[[table]])) {
    stop("Requested analytics table or columns are not allow-listed.", call. = FALSE)
  }

  sql <- sprintf("SELECT %s FROM %s", paste(columns, collapse = ", "), table)
  params <- NULL
  if (!is.null(student_id)) {
    sql <- paste(sql, "WHERE student_id = ?")
    params <- list(student_id)
  }
  if (is.null(params)) DBI::dbGetQuery(connection, sql) else DBI::dbGetQuery(connection, sql, params = params)
}

load_openstudy_data <- function(connection, student_id = NULL) {
  list(
    assignments = openstudy_read_table(
      connection, "assignments",
      c("assignment_id", "student_id", "subject_id", "due_date", "priority", "status"),
      student_id
    ),
    study_sessions = openstudy_read_table(
      connection, "study_sessions",
      c("session_id", "student_id", "subject_id", "assignment_id", "start_time", "end_time"),
      student_id
    ),
    subjects = openstudy_read_table(
      connection, "subjects", c("subject_id", "student_id", "subject_name"), student_id
    )
  )
}
