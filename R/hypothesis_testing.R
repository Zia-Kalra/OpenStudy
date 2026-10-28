#!/usr/bin/env Rscript
# Inferential tests. Each test runs only when its data requirements are met, and every
# result carries its assumptions and limitations.
#   1. Confidence interval + one-sample t-test of student mean scores vs a benchmark
#   2. Paired t-test (quiz vs assignment) on student-level means (Wilcoxon fallback)
#   3. One-way ANOVA of subject means (Kruskal-Wallis fallback, Tukey HSD post hoc)
#   4. Pearson / Spearman correlation between quiz and assignment means
src <- sub("--file=", "", grep("--file=", commandArgs(FALSE), value = TRUE))
source(file.path(dirname(src), "analytics_common.R"))

shapiro_p <- function(x) {
  if (length(x) < 3 || length(x) > 5000 || sd(x) == 0) return(NA_real_)
  shapiro.test(x)$p.value
}

main <- function(df, opts) {
  alpha <- opt_num(opts, "alpha", 0.05)
  bench <- opt_num(opts, "benchmark", 50)
  warnings <- list()
  if (nrow(df) == 0) return(list(status = "insufficient_data", reason = "No completed assessments match the filters.", n = 0))

  # Student-level means: one value per student keeps observations independent.
  stu <- aggregate(percentage ~ student_key, df, mean)
  wide <- reshape(aggregate(percentage ~ student_key + assessment_type, df, mean),
                  idvar = "student_key", timevar = "assessment_type", direction = "wide")
  both <- if (all(c("percentage.QUIZ", "percentage.ASSIGNMENT") %in% names(wide)))
            wide[complete.cases(wide[, c("percentage.QUIZ", "percentage.ASSIGNMENT")]), ] else NULL

  # 1. one-sample t-test -------------------------------------------------------
  one_sample <- guarded({
    x <- stu$percentage
    if (length(x) < 5) insufficient("At least 5 students with results are needed.")
    else if (sd(x) == 0) insufficient("All student means are identical.")
    else {
      tt <- t.test(x, mu = bench, conf.level = 1 - alpha)
      sw <- shapiro_p(x)
      list(status = "ok", method = "One-sample t-test on student mean scores",
           question = sprintf("Is the average student mean different from the benchmark of %.0f%%?", bench),
           n = length(x), mean = r2(mean(x)), benchmark = bench,
           ci = list(level = 1 - alpha, lower = r2(tt$conf.int[1]), upper = r2(tt$conf.int[2])),
           t = r4(tt$statistic), df = r2(tt$parameter), p_value = pv(tt$p.value),
           normality_shapiro_p = pv(sw),
           interpretation = sprintf("The average student mean is %.1f%% (%.0f%% CI %.1f to %.1f). It %s the %.0f%% benchmark (p = %.3f).",
              mean(x), 100 * (1 - alpha), tt$conf.int[1], tt$conf.int[2],
              if (tt$p.value < alpha) (if (mean(x) > bench) "is significantly above" else "is significantly below") else "does not differ significantly from",
              bench, tt$p.value),
           limitations = list("Assumes student means are roughly normal; with fewer than 30 students this matters.",
                              if (!is.na(sw) && sw < alpha) "Shapiro-Wilk suggests the means are not normal, so treat the p-value cautiously." else "No strong evidence against normality."))
    }
  })

  # 2. paired t-test ------------------------------------------------------------
  paired <- guarded({
    if (is.null(both) || nrow(both) < 5) insufficient("At least 5 students with both quiz and assignment results are needed.")
    else {
      q <- both$percentage.QUIZ; a <- both$percentage.ASSIGNMENT; d <- a - q
      if (sd(d) == 0) insufficient("Quiz and assignment means are identical for every student.")
      else {
        tt <- t.test(a, q, paired = TRUE, conf.level = 1 - alpha)
        sw <- shapiro_p(d)
        out <- list(status = "ok", method = "Paired t-test (assignment minus quiz) on student means",
             question = "Do students score differently on assignments than on quizzes?",
             n = nrow(both), mean_quiz = r2(mean(q)), mean_assignment = r2(mean(a)), mean_difference = r2(mean(d)),
             ci = list(level = 1 - alpha, lower = r2(tt$conf.int[1]), upper = r2(tt$conf.int[2])),
             t = r4(tt$statistic), df = r2(tt$parameter), p_value = pv(tt$p.value),
             normality_of_differences_shapiro_p = pv(sw),
             interpretation = sprintf("Assignment scores average %.1f points %s than quiz scores (p = %.3f), which is %s.",
                abs(mean(d)), if (mean(d) >= 0) "higher" else "lower", tt$p.value,
                if (tt$p.value < alpha) "statistically significant" else "not statistically significant"),
             limitations = list("Assignments and quizzes cover different material and are marked differently, so a difference is not proof that one format is easier.",
                                "Only students with both kinds of result are included."))
        if (!is.na(sw) && sw < alpha) {
          wt <- suppressWarnings(wilcox.test(a, q, paired = TRUE, exact = FALSE))
          out$fallback <- list(method = "Wilcoxon signed-rank test (used because differences are not normal)", p_value = pv(wt$p.value))
        }
        out
      }
    }
  })

  # 3. ANOVA across subjects ------------------------------------------------------
  anova_res <- guarded({
    ss <- aggregate(percentage ~ student_key + subject_name, df, mean)
    cnt <- table(ss$subject_name)
    keep <- names(cnt)[cnt >= 3]
    ss <- ss[ss$subject_name %in% keep, , drop = FALSE]
    if (length(keep) < 2) insufficient("At least 2 subjects with 3 or more students each are needed.")
    else if (sd(ss$percentage) == 0) insufficient("All values are identical.")
    else {
      ss$subject_name <- factor(ss$subject_name)
      fit <- aov(percentage ~ subject_name, data = ss)
      sm <- summary(fit)[[1]]
      p <- sm[["Pr(>F)"]][1]
      sw <- shapiro_p(residuals(fit))
      lev <- tryCatch(fligner.test(percentage ~ subject_name, data = ss)$p.value, error = function(e) NA_real_)
      assumptions_ok <- (!is.na(sw) && sw >= alpha) && (!is.na(lev) && lev >= alpha)
      groups <- lapply(levels(ss$subject_name), function(g) list(subject = g, n = sum(ss$subject_name == g), mean = r2(mean(ss$percentage[ss$subject_name == g]))))
      out <- list(status = "ok", method = "One-way ANOVA of student-by-subject mean scores",
        question = "Do average scores differ between subjects?",
        groups = groups, f = r4(sm[["F value"]][1]), df_between = sm$Df[1], df_within = sm$Df[2], p_value = pv(p),
        eta_squared = r4(sm[["Sum Sq"]][1] / sum(sm[["Sum Sq"]])),
        assumptions = list(residual_normality_shapiro_p = pv(sw), equal_variance_fligner_p = pv(lev), satisfied = assumptions_ok),
        interpretation = sprintf("Subject means %s (F = %.2f, p = %.3f).",
            if (p < alpha) "differ significantly" else "do not differ significantly", sm[["F value"]][1], p),
        limitations = list("The same students appear in several subjects, so observations are not fully independent; treat this as exploratory.",
                           "ANOVA only says that some difference exists, not which subject is responsible."))
      if (p < alpha && length(levels(ss$subject_name)) > 2) {
        tk <- TukeyHSD(fit)[[1]]
        out$post_hoc <- lapply(seq_len(nrow(tk)), function(i) list(comparison = rownames(tk)[i], diff = r2(tk[i, "diff"]), p_adj = pv(tk[i, "p adj"])))
      }
      if (!assumptions_ok) {
        kw <- kruskal.test(percentage ~ subject_name, data = ss)
        out$fallback <- list(method = "Kruskal-Wallis test (assumptions not clearly met)", p_value = pv(kw$p.value))
        out$warnings <- list("ANOVA assumptions (normal residuals, equal variances) are not clearly satisfied; prefer the Kruskal-Wallis result.")
      }
      out
    }
  })

  # 4. correlation -----------------------------------------------------------------
  corr <- guarded({
    if (is.null(both) || nrow(both) < 5) insufficient("At least 5 students with both quiz and assignment results are needed.")
    else if (sd(both$percentage.QUIZ) == 0 || sd(both$percentage.ASSIGNMENT) == 0) insufficient("One of the variables is constant, so correlation is undefined.")
    else {
      x <- both$percentage.QUIZ; y <- both$percentage.ASSIGNMENT
      ct <- cor.test(x, y, method = "pearson", conf.level = 1 - alpha)
      sp <- suppressWarnings(cor.test(x, y, method = "spearman", exact = FALSE))
      strength <- function(r) { a <- abs(r); if (a >= 0.7) "strong" else if (a >= 0.4) "moderate" else if (a >= 0.2) "weak" else "very weak" }
      list(status = "ok", method = "Pearson correlation (Spearman as robustness check)",
        question = "Do students who do well on quizzes also do well on assignments?",
        n = length(x), pearson_r = r4(ct$estimate), p_value = pv(ct$p.value),
        ci = list(level = 1 - alpha, lower = r4(ct$conf.int[1]), upper = r4(ct$conf.int[2])),
        spearman_rho = r4(sp$estimate), spearman_p = pv(sp$p.value),
        points = lapply(seq_along(x), function(i) list(x = r2(x[i]), y = r2(y[i]))),
        interpretation = sprintf("There is a %s %s relationship between quiz and assignment means (r = %.2f, p = %.3f).",
            strength(ct$estimate), if (ct$estimate >= 0) "positive" else "negative", ct$estimate, ct$p.value),
        limitations = list("Correlation does not show that one score causes the other.",
                           "With few students a single unusual student can change r noticeably."))
    }
  })

  list(status = "ok", alpha = alpha, students = nrow(stu),
       one_sample_t_test = one_sample, paired_t_test = paired, anova = anova_res, correlation = corr,
       notes = list("Tests are run only when their minimum data requirements are met; 'insufficient_data' means the test was skipped deliberately.",
                    "P-values below the significance level (alpha) indicate evidence against the null hypothesis, not the size or importance of an effect."),
       warnings = warnings)
}
run_main(main)
