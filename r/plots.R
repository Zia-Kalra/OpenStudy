save_openstudy_plots <- function(analytics, output_dir) {
  if (!requireNamespace("ggplot2", quietly = TRUE)) {
    stop("R package 'ggplot2' is required to generate charts. Install it with install.packages('ggplot2').", call. = FALSE)
  }
  dir.create(output_dir, recursive = TRUE, showWarnings = FALSE)
  plots_saved <- character()
  messages <- character()

  save_plot <- function(plot, filename, width = 9, height = 5) {
    ggplot2::ggsave(file.path(output_dir, filename), plot = plot, width = width, height = height, dpi = 150)
    plots_saved <<- c(plots_saved, filename)
  }

  if (nrow(analytics$daily_study) >= 2L) {
    plot <- ggplot2::ggplot(analytics$daily_study, ggplot2::aes(x = date, y = study_hours)) +
      ggplot2::geom_line(color = "#315C9B", linewidth = 0.9) +
      ggplot2::geom_point(color = "#315C9B", size = 2) +
      ggplot2::scale_x_date(date_labels = "%b %d", date_breaks = "1 week") +
      ggplot2::labs(title = "Daily Study Time", x = "Study date", y = "Study hours") +
      ggplot2::theme_minimal(base_size = 12) +
      ggplot2::theme(axis.text.x = ggplot2::element_text(angle = 35, hjust = 1))
    save_plot(plot, "daily_study_hours.png")
  } else messages <- c(messages, "Skipped daily chart: at least two study dates are needed for a trend line.")

  if (nrow(analytics$weekly_study) >= 2L) {
    plot <- ggplot2::ggplot(analytics$weekly_study, ggplot2::aes(x = week_start, y = study_hours)) +
      ggplot2::geom_line(color = "#6A4C93", linewidth = 0.9) +
      ggplot2::geom_point(color = "#6A4C93", size = 2) +
      ggplot2::scale_x_date(date_labels = "%b %d", date_breaks = "1 week") +
      ggplot2::labs(title = "Weekly Study Time (weeks start Monday)", x = "Week starting", y = "Study hours") +
      ggplot2::theme_minimal(base_size = 12) +
      ggplot2::theme(axis.text.x = ggplot2::element_text(angle = 35, hjust = 1))
    save_plot(plot, "weekly_study_hours.png")
  } else messages <- c(messages, "Skipped weekly chart: at least two calendar weeks are needed.")

  if (nrow(analytics$subject_study)) {
    plot <- ggplot2::ggplot(analytics$subject_study,
      ggplot2::aes(x = reorder(subject, study_hours), y = study_hours)) +
      ggplot2::geom_col(fill = "#3B8C78") + ggplot2::coord_flip() +
      ggplot2::labs(title = "Study Time by Subject", x = "Subject", y = "Study hours") +
      ggplot2::theme_minimal(base_size = 12)
    save_plot(plot, "subject_study_hours.png")
  } else messages <- c(messages, "Skipped subject chart: there are no valid study sessions.")

  if (analytics$assignment_summary$total_assignments > 0L) {
    plot <- ggplot2::ggplot(analytics$assignment_status,
      ggplot2::aes(x = reorder(status, -count), y = count)) +
      ggplot2::geom_col(fill = "#4F6D9A") +
      ggplot2::labs(title = "Assignments by Status", x = "Status", y = "Assignment count") +
      ggplot2::theme_minimal(base_size = 12)
    save_plot(plot, "assignment_status.png")
  } else messages <- c(messages, "Skipped assignment status chart: there are no assignments.")

  if (analytics$assignment_summary$total_assignments > 0L) {
    plot <- ggplot2::ggplot(analytics$assignment_priority,
      ggplot2::aes(x = reorder(priority, -count), y = count)) +
      ggplot2::geom_col(fill = "#D08B3E") +
      ggplot2::labs(title = "Assignments by Priority", x = "Priority", y = "Assignment count") +
      ggplot2::theme_minimal(base_size = 12)
    save_plot(plot, "assignment_priority.png")
  } else messages <- c(messages, "Skipped assignment priority chart: there are no assignments.")

  list(files = plots_saved, messages = messages)
}
