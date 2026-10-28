package com.openstudy.openstudy.service.analytics;

import static com.openstudy.openstudy.dto.analytics.AnalyticsResponse.Insight;
import static com.openstudy.openstudy.dto.analytics.AnalyticsResponse.InsightResult;
import static com.openstudy.openstudy.dto.analytics.AnalyticsResponse.Participation;
import static com.openstudy.openstudy.dto.analytics.AnalyticsResponse.QuestionStat;
import static com.openstudy.openstudy.dto.analytics.AnalyticsResponse.SubjectStat;
import static com.openstudy.openstudy.dto.analytics.AnalyticsResponse.SummaryStats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Rule-based, explainable insights. Every rule states its threshold in code, requires a minimum
 * amount of data, and quotes the numbers it used. No AI service is involved.
 *
 * <p>Percentages are always on a 0-100 scale. "Points" means percentage points.
 */
public final class InsightEngine {

    static final double SUBJECT_GAP = 8.0;      // subject this far below overall => attention
    static final double RECENT_CHANGE = 5.0;    // last-3 vs previous-3 change worth reporting
    static final double FORMAT_GAP = 6.0;       // quiz vs assignment gap worth reporting
    static final double HIGH_VARIATION_SD = 15; // class SD considered "substantial variation"
    static final double LOW_QUESTION_RATE = 40; // % correct below which a question is flagged
    static final int MIN_QUESTION_ATTEMPTS = 5;
    static final double LOW_PARTICIPATION = 60; // % of enrolled students

    private final int minResults;
    private final double passMark;

    public InsightEngine(int minResults, double passMark) {
        this.minResults = Math.max(1, minResults);
        this.passMark = passMark;
    }

    /** @param chronological percentages of the student's results, oldest first */
    public InsightResult forStudent(SummaryStats summary, List<SubjectStat> subjects, List<Double> chronological) {
        if (summary == null || summary.count() < minResults) {
            return notEnough(summary == null ? 0 : summary.count());
        }
        List<Insight> in = new ArrayList<>();
        List<String> rec = new ArrayList<>();
        double overall = summary.average();

        List<SubjectStat> usable = subjects.stream().filter(s -> s.count() >= 2 && s.average() != null).toList();
        if (usable.size() >= 2) {
            SubjectStat best = usable.stream().max(Comparator.comparingDouble(SubjectStat::average)).get();
            SubjectStat worst = usable.stream().min(Comparator.comparingDouble(SubjectStat::average)).get();
            in.add(new Insight("POSITIVE", "SUBJECT", String.format(Locale.ROOT,
                    "You perform best in %s with an average of %.1f%% across %d results.",
                    best.subjectName(), best.average(), best.count())));
            if (overall - worst.average() >= SUBJECT_GAP) {
                in.add(new Insight("ATTENTION", "SUBJECT", String.format(Locale.ROOT,
                        "%s has a lower average (%.1f%%) than your overall average (%.1f%%).",
                        worst.subjectName(), worst.average(), overall)));
                rec.add("Focus more on " + worst.subjectName() + " to raise your score; review the questions you missed there.");
            }
        }

        int n = chronological.size();
        if (n >= 6) {
            double recent = mean(chronological.subList(n - 3, n));
            double previous = mean(chronological.subList(n - 6, n - 3));
            double change = recent - previous;
            if (change >= RECENT_CHANGE) {
                in.add(new Insight("POSITIVE", "TREND", String.format(Locale.ROOT,
                        "Your last 3 results average %.1f%%, up %.1f points from the 3 before.", recent, change)));
            } else if (change <= -RECENT_CHANGE) {
                in.add(new Insight("ATTENTION", "TREND", String.format(Locale.ROOT,
                        "Your last 3 results average %.1f%%, down %.1f points from the 3 before.", recent, -change)));
                rec.add("Your recent scores dipped; revisit recent topics before the next assessment.");
            }
        }

        if (summary.quizCount() >= 2 && summary.assignmentCount() >= 2
                && summary.quizAverage() != null && summary.assignmentAverage() != null) {
            double gap = summary.assignmentAverage() - summary.quizAverage();
            if (gap >= FORMAT_GAP) {
                in.add(new Insight("INFO", "FORMAT", String.format(Locale.ROOT,
                        "Your assignment scores (%.1f%%) are higher than your quiz scores (%.1f%%).",
                        summary.assignmentAverage(), summary.quizAverage())));
                rec.add("Practice more timed quizzes to improve speed and accuracy.");
            } else if (gap <= -FORMAT_GAP) {
                in.add(new Insight("INFO", "FORMAT", String.format(Locale.ROOT,
                        "Your quiz scores (%.1f%%) are higher than your assignment scores (%.1f%%).",
                        summary.quizAverage(), summary.assignmentAverage())));
                rec.add("Spend more time structuring written answers and checking them against the rubric.");
            }
        }

        long below = chronological.stream().filter(p -> p < passMark).count();
        if (below >= 2) {
            in.add(new Insight("ATTENTION", "RESULTS", String.format(Locale.ROOT,
                    "%d of your %d results are below %.0f%%.", below, n, passMark)));
            rec.add("Review the feedback and incorrect answers from the assessments where you scored below " + (int) passMark + "%.");
        }
        if (n >= 5 && sd(chronological) <= 10) {
            in.add(new Insight("POSITIVE", "CONSISTENCY", String.format(Locale.ROOT,
                    "Your results are consistent (standard deviation %.1f points).", sd(chronological))));
        }
        return finish(in, rec);
    }

    /** @param percentages every result in the teacher's filtered scope */
    public InsightResult forTeacher(SummaryStats summary, List<Double> percentages, List<SubjectStat> subjects,
                                    Participation participation, List<QuestionStat> questions) {
        if (summary == null || summary.count() < minResults) {
            return notEnough(summary == null ? 0 : summary.count());
        }
        List<Insight> in = new ArrayList<>();
        List<String> rec = new ArrayList<>();

        if (summary.average() < passMark) {
            in.add(new Insight("ATTENTION", "CLASS", String.format(Locale.ROOT,
                    "The class average (%.1f%%) is below the %.0f%% pass mark.", summary.average(), passMark)));
        }
        if (percentages.size() >= 5) {
            double sd = sd(percentages);
            if (sd >= HIGH_VARIATION_SD) {
                in.add(new Insight("ATTENTION", "VARIATION", String.format(Locale.ROOT,
                        "Class scores show substantial variation (standard deviation %.1f points).", sd)));
                rec.add("Consider support sessions: results are spread widely, so some students may need extra help while others need extension work.");
            }
        }
        List<SubjectStat> usable = subjects.stream().filter(s -> s.count() >= 5 && s.average() != null).toList();
        if (usable.size() >= 2) {
            SubjectStat worst = usable.stream().min(Comparator.comparingDouble(SubjectStat::average)).get();
            SubjectStat best = usable.stream().max(Comparator.comparingDouble(SubjectStat::average)).get();
            if (best.average() - worst.average() >= 10) {
                in.add(new Insight("ATTENTION", "SUBJECT", String.format(Locale.ROOT,
                        "%s has the lowest average (%.1f%%), %.1f points below %s (%.1f%%).",
                        worst.subjectName(), worst.average(), best.average() - worst.average(),
                        best.subjectName(), best.average())));
                rec.add("Review teaching material and assessment difficulty in " + worst.subjectName() + ".");
            }
        }
        if (summary.quizCount() >= 5 && summary.assignmentCount() >= 5
                && summary.quizAverage() != null && summary.assignmentAverage() != null) {
            double gap = summary.assignmentAverage() - summary.quizAverage();
            if (Math.abs(gap) >= FORMAT_GAP) {
                in.add(new Insight("INFO", "FORMAT", String.format(Locale.ROOT,
                        "Students score %.1f points %s on assignments (%.1f%%) than on quizzes (%.1f%%).",
                        Math.abs(gap), gap > 0 ? "higher" : "lower", summary.assignmentAverage(), summary.quizAverage())));
            }
        }
        if (participation != null && participation.rate() != null && participation.enrolledStudents() >= 5
                && participation.rate() < LOW_PARTICIPATION) {
            in.add(new Insight("ATTENTION", "PARTICIPATION", String.format(Locale.ROOT,
                    "Only %.0f%% of enrolled students have completed work in this scope.", participation.rate())));
            rec.add("Send a reminder to students who have not yet completed any quiz or assignment.");
        }
        List<QuestionStat> weak = questions.stream()
                .filter(q -> q.reliable() && q.correctRate() < LOW_QUESTION_RATE).limit(3).toList();
        for (QuestionStat q : weak) {
            in.add(new Insight("ATTENTION", "QUESTION", String.format(Locale.ROOT,
                    "Question %d of \"%s\" has a low correct-response rate (%.0f%% of %d attempts).",
                    q.position(), q.quizTitle(), q.correctRate(), q.attempts())));
        }
        if (!weak.isEmpty()) {
            rec.add("Review the flagged questions: they may be unclear, mis-keyed, or cover a topic that needs re-teaching.");
        }
        return finish(in, rec);
    }

    private InsightResult finish(List<Insight> in, List<String> rec) {
        if (in.isEmpty()) {
            return new InsightResult(true, "No notable patterns were found in the current data.", in, rec);
        }
        return new InsightResult(true, null, in, rec);
    }

    private InsightResult notEnough(long count) {
        return new InsightResult(false, String.format(Locale.ROOT,
                "Insights need at least %d completed assessments; %d found so far.", minResults, count),
                List.of(), List.of());
    }

    static double mean(List<Double> xs) {
        return xs.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    static double sd(List<Double> xs) {
        if (xs.size() < 2) {
            return 0;
        }
        double m = mean(xs);
        double ss = xs.stream().mapToDouble(x -> (x - m) * (x - m)).sum();
        return Math.sqrt(ss / (xs.size() - 1));
    }
}
