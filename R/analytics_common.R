# OpenStudy analytics - shared helpers (Member 4)
# Sourced by the four analysis scripts. Requires R >= 4.0 and the 'jsonlite' package.
#
# CLI contract for every script:
#   Rscript <script>.R <input.csv> <output.json> [key=value ...]
# Input CSV columns (header required):
#   student_key, assessment_type, subject_name, assessment_title, percentage, completed_at
# student_key is a per-request pseudonym (S001, S002 ...). No names, e-mails or ids are written.

suppressPackageStartupMessages(library(jsonlite))

parse_args <- function() {
  a <- commandArgs(trailingOnly = TRUE)
  if (length(a) < 2) stop("usage: <script> <input.csv> <output.json> [key=value ...]")
  opts <- list()
  if (length(a) > 2) for (kv in a[3:length(a)]) {
    p <- strsplit(kv, "=", fixed = TRUE)[[1]]
    if (length(p) == 2) opts[[p[1]]] <- p[2]
  }
  list(input = a[1], output = a[2], opts = opts)
}

opt_num <- function(opts, key, default) {
  v <- suppressWarnings(as.numeric(opts[[key]]))
  if (length(v) != 1 || is.na(v)) default else v
}

r2 <- function(x, d = 2) if (is.null(x) || length(x) == 0 || is.na(x)) NULL else round(as.numeric(x), d)
r4 <- function(x) r2(x, 4)
# p-values are kept with more precision
pv <- function(x) if (is.null(x) || is.na(x)) NULL else signif(as.numeric(x), 4)

write_result <- function(res, path) {
  json <- toJSON(res, auto_unbox = TRUE, na = "null", null = "null", digits = NA)
  # sprintf("%.3f") prints very small p-values as 0.000; show them as "< 0.001" instead.
  json <- gsub("p = 0\\.000", "p < 0.001", json)
  writeLines(json, path, useBytes = TRUE)
}

fail_with <- function(path, msg) {
  # Message is generic on purpose: never echo data values back.
  try(writeLines(toJSON(list(status = "error", message = msg), auto_unbox = TRUE), path), silent = TRUE)
  quit(status = 2, save = "no")
}

# Reads and validates the input. Returns list(df, warnings).
read_input <- function(path) {
  warnings <- character(0)
  if (!file.exists(path)) stop("input file not found")
  df <- tryCatch(read.csv(path, stringsAsFactors = FALSE, strip.white = TRUE),
                 error = function(e) stop("input CSV could not be read"))
  need <- c("student_key", "assessment_type", "subject_name", "assessment_title", "percentage", "completed_at")
  miss <- setdiff(need, names(df))
  if (length(miss) > 0) stop(paste("input CSV is missing columns:", paste(miss, collapse = ", ")))
  df$percentage <- suppressWarnings(as.numeric(df$percentage))
  bad <- is.na(df$percentage) | df$percentage < 0 | df$percentage > 100
  if (any(bad)) {
    warnings <- c(warnings, paste(sum(bad), "row(s) with missing or out-of-range percentage were ignored."))
    df <- df[!bad, , drop = FALSE]
  }
  df$when <- as.POSIXct(df$completed_at, format = "%Y-%m-%d %H:%M:%S", tz = "UTC")
  if (any(is.na(df$when))) {
    df$when <- ifelse(is.na(df$when), as.POSIXct(substr(df$completed_at, 1, 10), format = "%Y-%m-%d", tz = "UTC"), df$when)
    df$when <- as.POSIXct(df$when, origin = "1970-01-01", tz = "UTC")
  }
  list(df = df, warnings = warnings)
}

# Most frequent whole-number score; NULL unless it is genuinely informative.
safe_mode <- function(x) {
  if (length(x) < 10) return(NULL)
  t <- table(round(x))
  if (max(t) < 2) return(NULL)
  m <- as.numeric(names(t)[t == max(t)])
  if (length(m) > 3) return(NULL)   # many ties: mode is not meaningful
  as.list(m)
}

mean_ci <- function(x, level = 0.95) {
  x <- x[!is.na(x)]
  n <- length(x)
  if (n < 2 || sd(x) == 0) return(NULL)
  se <- sd(x) / sqrt(n)
  q <- qt(1 - (1 - level) / 2, df = n - 1)
  list(level = level, lower = r2(max(0, mean(x) - q * se)), upper = r2(min(100, mean(x) + q * se)))
}

describe <- function(x) {
  x <- x[!is.na(x)]
  n <- length(x)
  if (n == 0) return(list(n = 0))
  q <- as.numeric(quantile(x, c(0.25, 0.75), type = 7))
  bs <- boxplot.stats(x)
  list(
    n = n,
    mean = r2(mean(x)), median = r2(median(x)), mode = safe_mode(x),
    variance = if (n > 1) r2(var(x)) else NULL,
    sd = if (n > 1) r2(sd(x)) else NULL,
    min = r2(min(x)), max = r2(max(x)),
    q1 = r2(q[1]), q3 = r2(q[2]), iqr = r2(q[2] - q[1]),
    ci95 = mean_ci(x),
    boxplot = list(whisker_low = r2(bs$stats[1]), q1 = r2(bs$stats[2]), median = r2(bs$stats[3]),
                   q3 = r2(bs$stats[4]), whisker_high = r2(bs$stats[5]),
                   outliers = lapply(unname(bs$out), r2))
  )
}

# Runs expr; on error returns a status block instead of aborting the whole script.
guarded <- function(expr) {
  tryCatch(expr, error = function(e) list(status = "error", message = "This analysis could not be completed with the supplied data."))
}

insufficient <- function(reason) list(status = "insufficient_data", reason = reason)

run_main <- function(fn) {
  a <- parse_args()
  tryCatch({
    inp <- read_input(a$input)
    res <- fn(inp$df, a$opts)
    res$warnings <- c(as.list(inp$warnings), as.list(res$warnings))
    write_result(res, a$output)
  }, error = function(e) fail_with(a$output, conditionMessage(e)))
}
