openstudy_config <- function(student_id = NULL) {
  get_setting <- function(name, default = NULL, required = FALSE) {
    value <- Sys.getenv(name, unset = "")
    if (!nzchar(value)) {
      if (required) {
        stop(sprintf("Set the %s environment variable before running analytics.", name), call. = FALSE)
      }
      return(default)
    }
    value
  }

  if (!is.null(student_id)) {
    student_id <- trimws(as.character(student_id))
    if (length(student_id) != 1L || is.na(student_id) ||
        !grepl("^[0-9]+$", student_id) || suppressWarnings(as.numeric(student_id)) <= 0) {
      stop("student_id must be one positive whole-number ID.", call. = FALSE)
    }
  }

  timezone <- get_setting("OPENSTUDY_ANALYTICS_TZ", Sys.timezone())
  if (is.na(timezone) || !nzchar(timezone)) timezone <- "UTC"
  if (!(timezone %in% OlsonNames()) && timezone != "UTC") {
    stop("OPENSTUDY_ANALYTICS_TZ must be a valid Olson time zone, such as Asia/Kolkata.", call. = FALSE)
  }

  port <- suppressWarnings(as.integer(get_setting("OPENSTUDY_DB_PORT", "3306")))
  if (is.na(port) || port < 1L || port > 65535L) {
    stop("OPENSTUDY_DB_PORT must be a whole number from 1 to 65535.", call. = FALSE)
  }

  list(
    host = get_setting("OPENSTUDY_DB_HOST", "localhost"),
    port = port,
    database = get_setting("OPENSTUDY_DB_NAME", "openstudy_db"),
    user = get_setting("OPENSTUDY_DB_USER", required = TRUE),
    password = get_setting("OPENSTUDY_DB_PASSWORD", required = TRUE),
    timezone = timezone,
    student_id = student_id
  )
}
