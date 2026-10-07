openstudy_connect <- function(config) {
  if (!requireNamespace("DBI", quietly = TRUE)) {
    stop("R package 'DBI' is required. Install it with install.packages('DBI').", call. = FALSE)
  }
  if (!requireNamespace("RMariaDB", quietly = TRUE)) {
    stop("R package 'RMariaDB' is required. Install it with install.packages('RMariaDB').", call. = FALSE)
  }

  connection <- tryCatch(
    DBI::dbConnect(
      RMariaDB::MariaDB(),
      host = config$host,
      port = config$port,
      dbname = config$database,
      username = config$user,
      password = config$password,
      timezone = "+00:00",
      timezone_out = config$timezone
    ),
    error = function(error) {
      stop("Could not connect to OpenStudy MySQL. Check the database service, connection settings, and credentials.", call. = FALSE)
    }
  )

  # This module issues SELECT statements only. Use a MySQL account granted
  # SELECT on the analytics tables and no write privileges for enforcement.
  connection
}

openstudy_disconnect <- function(connection) {
  if (!is.null(connection) && DBI::dbIsValid(connection)) {
    DBI::dbDisconnect(connection)
  }
}
