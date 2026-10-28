#!/usr/bin/env Rscript
# Regression models with R-squared, adjusted R-squared, F-test and residual analysis.
#   model 1 (paired):  student assignment mean ~ student quiz mean   (simple linear)
#   model 2 (trend):   percentage ~ days  (simple) and, when enough data,
#                      percentage ~ days + assessment_type (+ subject)  (multiple)
src <- sub("--file=", "", grep("--file=", commandArgs(FALSE), value = TRUE))
source(file.path(dirname(src), "analytics_common.R"))

summarize_lm <- function(fit, label, formula_txt, alpha) {
  s <- summary(fit)
  cf <- s$coefficients
  res <- residuals(fit); fv <- fitted(fit); n <- length(res)
  fs <- s$fstatistic
  fp <- if (!is.null(fs)) pf(fs[1], fs[2], fs[3], lower.tail = FALSE) else NA
  sw <- if (n >= 3 && n <= 5000 && sd(res) > 0) shapiro.test(res)$p.value else NA
  het <- tryCatch(suppressWarnings(cor.test(abs(res), fv, method = "spearman", exact = FALSE)$p.value), error = function(e) NA)
  cook <- tryCatch(cooks.distance(fit), error = function(e) rep(0, n))
  # variance inflation factors (only meaningful with 2+ predictors)
  vif <- NULL
  mm <- model.matrix(fit)[, -1, drop = FALSE]
  if (ncol(mm) >= 2) {
    vif <- lapply(colnames(mm), function(v) {
      r <- tryCatch(summary(lm(mm[, v] ~ mm[, colnames(mm) != v, drop = FALSE]))$r.squared, error = function(e) NA)
      list(term = v, vif = r2(if (is.na(r) || r >= 1) NA else 1 / (1 - r)))
    })
  }
  keep <- if (n > 200) sort(sample(n, 200)) else seq_len(n)
  list(status = "ok", model = label, formula = formula_txt, n = n,
       coefficients = lapply(seq_len(nrow(cf)), function(i) list(term = rownames(cf)[i], estimate = r4(cf[i, 1]),
                               std_error = r4(cf[i, 2]), t = r4(cf[i, 3]), p_value = pv(cf[i, 4]))),
       r_squared = r4(s$r.squared), adj_r_squared = r4(s$adj.r.squared),
       f_statistic = r4(fs[1]), f_df1 = unname(fs[2]), f_df2 = unname(fs[3]), f_p_value = pv(fp),
       residual_std_error = r4(s$sigma),
       residual_analysis = list(mean = r4(mean(res)), sd = r4(sd(res)),
                                normality_shapiro_p = pv(sw), heteroscedasticity_spearman_p = pv(het),
                                influential_points = sum(cook > 4 / n, na.rm = TRUE),
                                plot = lapply(keep, function(i) list(fitted = r2(fv[i]), residual = r2(res[i])))),
       vif = vif,
       interpretation = sprintf("The model explains %.1f%% of the variation (adjusted R-squared %.1f%%); overall it is %s (F-test p = %.3f).",
          100 * s$r.squared, 100 * s$adj.r.squared, if (!is.na(fp) && fp < alpha) "statistically significant" else "not statistically significant", fp))
}

main <- function(df, opts) {
  alpha <- opt_num(opts, "alpha", 0.05)
  if (nrow(df) == 0) return(list(status = "insufficient_data", reason = "No completed assessments match the filters.", n = 0))

  paired <- guarded({
    wide <- reshape(aggregate(percentage ~ student_key + assessment_type, df, mean),
                    idvar = "student_key", timevar = "assessment_type", direction = "wide")
    if (!all(c("percentage.QUIZ", "percentage.ASSIGNMENT") %in% names(wide))) insufficient("Both quiz and assignment results are required.")
    else {
      d <- wide[complete.cases(wide[, c("percentage.QUIZ", "percentage.ASSIGNMENT")]), ]
      if (nrow(d) < 5) insufficient("At least 5 students with both quiz and assignment results are needed.")
      else if (sd(d$percentage.QUIZ) == 0) insufficient("Quiz means are constant, so no slope can be estimated.")
      else {
        fit <- lm(percentage.ASSIGNMENT ~ percentage.QUIZ, data = d)
        out <- summarize_lm(fit, "Simple linear regression", "assignment_mean ~ quiz_mean", alpha)
        xs <- range(d$percentage.QUIZ)
        out$fitted_line <- list(list(x = r2(xs[1]), y = r2(predict(fit, data.frame(percentage.QUIZ = xs[1])))),
                                list(x = r2(xs[2]), y = r2(predict(fit, data.frame(percentage.QUIZ = xs[2])))))
        out$points <- lapply(seq_len(nrow(d)), function(i) list(x = r2(d$percentage.QUIZ[i]), y = r2(d$percentage.ASSIGNMENT[i])))
        out$limitations <- list("Student means are averages of different numbers of assessments.",
                                "Predicting one student's assignment mean from the quiz mean is only an approximation.")
        out
      }
    }
  })

  trend_df <- df[order(df$when), ]
  trend_df$days <- as.numeric(difftime(trend_df$when, min(trend_df$when), units = "days"))

  simple_trend <- guarded({
    if (nrow(trend_df) < 8) insufficient("At least 8 completed assessments are needed.")
    else if (sd(trend_df$days) == 0 || sd(trend_df$percentage) == 0) insufficient("Time or score is constant.")
    else {
      fit <- lm(percentage ~ days, data = trend_df)
      out <- summarize_lm(fit, "Simple linear regression", "percentage ~ days", alpha)
      out$limitations <- list("Assumes a constant rate of change over time.")
      out
    }
  })

  multiple <- guarded({
    n <- nrow(trend_df)
    types <- length(unique(trend_df$assessment_type))
    subs <- length(unique(trend_df$subject_name))
    terms <- c("days"); preds <- 1
    if (types > 1) { terms <- c(terms, "assessment_type"); preds <- preds + 1 }
    if (subs > 1 && n >= 10 * (preds + subs - 1)) { terms <- c(terms, "subject_name"); preds <- preds + subs - 1 }
    if (length(terms) < 2) insufficient("A multiple regression needs at least two predictors with variation (e.g. both quizzes and assignments).")
    else if (n < 10 * preds) insufficient(sprintf("A multiple regression with %d predictors needs at least %d results (10 per predictor).", preds, 10 * preds))
    else if (sd(trend_df$days) == 0 || sd(trend_df$percentage) == 0) insufficient("Time or score is constant.")
    else {
      trend_df$assessment_type <- factor(trend_df$assessment_type)
      trend_df$subject_name <- factor(trend_df$subject_name)
      fml <- as.formula(paste("percentage ~", paste(terms, collapse = " + ")))
      fit <- lm(fml, data = trend_df)
      out <- summarize_lm(fit, "Multiple linear regression", paste("percentage ~", paste(terms, collapse = " + ")), alpha)
      if (any(is.na(coef(fit)))) out$warnings <- list("Some predictors are perfectly collinear and were dropped by R.")
      out$limitations <- list("Associations only; the model does not prove that type or subject causes score differences.",
                              "Repeated results from the same student are treated as independent, which understates uncertainty.")
      out
    }
  })

  list(status = "ok", alpha = alpha, quiz_vs_assignment = paired, trend_simple = simple_trend, trend_multiple = multiple,
       notes = list("Models are fitted only when sample-size rules are satisfied; otherwise 'insufficient_data' is returned with the reason."))
}
run_main(main)
