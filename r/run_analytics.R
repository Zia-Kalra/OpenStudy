find_script_directory <- function() {
  args <- commandArgs(trailingOnly = FALSE)
  file_arg <- grep("^--file=", args, value = TRUE)
  if (length(file_arg)) return(dirname(normalizePath(sub("^--file=", "", file_arg[1]), winslash = "/", mustWork = TRUE)))
  normalizePath("r", winslash = "/", mustWork = TRUE)
}

script_dir <- find_script_directory()
for (file in c("config.R", "db_connection.R", "load_data.R", "analytics.R", "plots.R")) {
  source(file.path(script_dir, file), local = FALSE)
}

arguments <- commandArgs(trailingOnly = TRUE)
student_id <- NULL
if (length(arguments)) {
  if (length(arguments) != 2L || arguments[1] != "--student-id") {
    stop("Usage: Rscript r/run_analytics.R [--student-id <positive-id>]", call. = FALSE)
  }
  student_id <- arguments[2]
}

config <- openstudy_config(student_id)
connection <- openstudy_connect(config)
tryCatch({
  data <- load_openstudy_data(connection, config$student_id)
  analytics <- make_planner_analytics(data, config$timezone)
  summary_metrics <- make_summary_metrics(analytics)
  output_dir <- file.path(script_dir, "output")
  plot_result <- save_openstudy_plots(analytics, output_dir)
  utils::write.csv(summary_metrics, file.path(output_dir, "summary_metrics.csv"), row.names = FALSE)
  utils::write.csv(analytics$data_quality, file.path(output_dir, "data_quality.csv"), row.names = FALSE)

  cat("OpenStudy Planner Analytics\n")
  if (is.null(config$student_id)) cat("Scope: all students (no student ID filter)\n") else cat("Scope: student ", config$student_id, "\n", sep = "")
  cat("Analysis time: ", format(analytics$analysis_time, tz = config$timezone), " (", config$timezone, ")\n", sep = "")
  print(summary_metrics, row.names = FALSE)
  cat("\nData quality checks:\n")
  print(analytics$data_quality, row.names = FALSE)
  for (message in unique(c(analytics$messages, plot_result$messages))) cat("- ", message, "\n", sep = "")
  if (length(plot_result$files)) {
    cat("\nCharts saved to ", output_dir, ":\n", sep = "")
    for (file in plot_result$files) cat("- ", file, "\n", sep = "")
  }
}, finally = openstudy_disconnect(connection))
