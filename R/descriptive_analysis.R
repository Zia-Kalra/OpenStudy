#!/usr/bin/env Rscript
# Descriptive statistics: mean, median, mode, variance, SD, min/max, quartiles, IQR,
# 95% CI of the mean, box-plot summary and frequency distributions.
src <- sub("--file=", "", grep("--file=", commandArgs(FALSE), value = TRUE))
source(file.path(dirname(src), "analytics_common.R"))

main <- function(df, opts) {
  n <- nrow(df)
  if (n == 0) return(list(status = "insufficient_data", reason = "No completed assessments match the filters.", n = 0))
  pass_mark <- opt_num(opts, "pass_mark", 50)

  group_stats <- function(col) {
    keys <- sort(unique(df[[col]]))
    lapply(keys, function(k) c(list(group = k), describe(df$percentage[df[[col]] == k])))
  }

  breaks <- seq(0, 100, by = 10)
  cut10 <- cut(df$percentage, breaks = breaks, include.lowest = TRUE, right = FALSE)
  # the value 100 belongs to the last bin
  cut10[df$percentage == 100] <- levels(cut10)[10]
  tab <- table(cut10)
  bins <- lapply(seq_along(tab), function(i) list(
    label = if (i < 10) sprintf("%d-%d", breaks[i], breaks[i + 1] - 1) else "90-100",
    from = breaks[i], to = breaks[i + 1], count = as.integer(tab[i]), percent = r2(100 * tab[i] / n)))

  bands <- list(
    list(label = "Above 90%", count = sum(df$percentage > 90)),
    list(label = "70% - 90%", count = sum(df$percentage > 70 & df$percentage <= 90)),
    list(label = "50% - 70%", count = sum(df$percentage > 50 & df$percentage <= 70)),
    list(label = "Below 50%", count = sum(df$percentage <= 50)))
  bands <- lapply(bands, function(b) { b$percent <- r2(100 * b$count / n); b })

  warnings <- list()
  if (n < 5) warnings <- c(warnings, "Fewer than 5 results: summary statistics are shown but are unstable.")
  overall <- describe(df$percentage)
  if (!is.null(overall$sd) && overall$sd == 0) warnings <- c(warnings, "All scores are identical, so no variation can be measured.")

  interp <- sprintf("Across %d completed assessments the mean score is %.1f%% (median %.1f%%).", n, mean(df$percentage), median(df$percentage))
  if (n > 1 && sd(df$percentage) > 0) {
    s <- sd(df$percentage)
    interp <- paste(interp, sprintf("The standard deviation is %.1f points, which indicates %s.", s,
      if (s >= 15) "substantial variation between results" else if (s >= 8) "moderate variation between results" else "fairly consistent results"))
  }

  list(status = "ok", n = n, pass_mark = pass_mark,
       overall = overall,
       pass_rate = r2(100 * mean(df$percentage >= pass_mark)),
       by_type = group_stats("assessment_type"),
       by_subject = group_stats("subject_name"),
       distribution = bins, grade_bands = bands,
       interpretation = interp,
       limitations = list("Statistics describe the filtered results only; they are not predictions.",
                          "Each completed quiz or assignment counts once, so students with more results weigh more."),
       warnings = warnings)
}
run_main(main)
