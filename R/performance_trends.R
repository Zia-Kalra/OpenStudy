#!/usr/bin/env Rscript
# Performance over time: monthly means, 3-point moving average and a linear trend test.
src <- sub("--file=", "", grep("--file=", commandArgs(FALSE), value = TRUE))
source(file.path(dirname(src), "analytics_common.R"))

main <- function(df, opts) {
  n <- nrow(df)
  if (n == 0) return(list(status = "insufficient_data", reason = "No completed assessments match the filters.", n = 0))
  df <- df[order(df$when), ]
  df$month <- format(df$when, "%Y-%m")

  monthly <- do.call(rbind, lapply(split(df, df$month), function(g) data.frame(
    month = g$month[1], n = nrow(g), mean = mean(g$percentage),
    quiz = if (any(g$assessment_type == "QUIZ")) mean(g$percentage[g$assessment_type == "QUIZ"]) else NA,
    assignment = if (any(g$assessment_type == "ASSIGNMENT")) mean(g$percentage[g$assessment_type == "ASSIGNMENT"]) else NA)))
  monthly_list <- lapply(seq_len(nrow(monthly)), function(i) list(
    month = monthly$month[i], n = monthly$n[i], mean = r2(monthly$mean[i]),
    quiz_mean = r2(monthly$quiz[i]), assignment_mean = r2(monthly$assignment[i])))

  ma <- if (n >= 3) as.numeric(stats::filter(df$percentage, rep(1 / 3, 3), sides = 1)) else rep(NA_real_, n)
  series <- lapply(seq_len(nrow(df)), function(i) list(
    date = format(df$when[i], "%Y-%m-%d"), type = df$assessment_type[i], percentage = r2(df$percentage[i]), moving_avg = r2(ma[i])))

  trend <- guarded({
    days <- as.numeric(difftime(df$when, min(df$when), units = "days"))
    if (n < 5) insufficient("At least 5 completed assessments are needed to test a trend.")
    else if (length(unique(days)) < 3 || sd(days) == 0) insufficient("Results span too short a period to test a trend.")
    else if (sd(df$percentage) == 0) insufficient("All scores are identical, so there is no trend to measure.")
    else {
      fit <- lm(percentage ~ days, data = data.frame(percentage = df$percentage, days = days))
      s <- summary(fit)
      p <- s$coefficients[2, 4]
      slope_month <- unname(coef(fit)[2]) * 30
      direction <- if (p < 0.05) (if (slope_month > 0) "improving" else "declining") else "no_significant_trend"
      list(status = "ok", method = "Simple linear regression of percentage on time",
           slope_per_month = r2(slope_month), p_value = pv(p), r_squared = r4(s$r.squared),
           direction = direction,
           interpretation = switch(direction,
             improving = sprintf("Scores rise by about %.1f percentage points per month (p = %.3f).", slope_month, p),
             declining = sprintf("Scores fall by about %.1f percentage points per month (p = %.3f).", abs(slope_month), p),
             sprintf("The estimated change of %.1f points per month is not statistically distinguishable from zero (p = %.3f).", slope_month, p)),
           limitations = list("Assumes a straight-line change; real progress may be uneven.",
                              "Assessments differ in difficulty, so a trend can reflect harder or easier papers."))
    }
  })

  recent <- NULL
  if (n >= 6) {
    k <- min(3, floor(n / 2))
    last <- tail(df$percentage, k); prev <- tail(head(df$percentage, n - k), k)
    recent <- list(window = k, recent_mean = r2(mean(last)), previous_mean = r2(mean(prev)), change = r2(mean(last) - mean(prev)))
  }

  list(status = "ok", n = n, monthly = monthly_list, series = series, trend = trend, recent_vs_previous = recent,
       warnings = if (n < 5) list("Fewer than 5 results: the chart is shown but no trend test was run.") else list())
}
run_main(main)
