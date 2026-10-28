package com.openstudy.openstudy.service.analytics;

import static com.openstudy.openstudy.dto.analytics.AnalyticsResponse.RResult;

import com.openstudy.openstudy.config.AnalyticsProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Runs the R analyses and turns every failure into an {@link RResult} (never an exception), so the
 * rest of the dashboard keeps working when R is missing, slow or broken.
 */
@Service
@Profile("!mock")
public class RAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(RAnalyticsService.class);

    private final AnalyticsProperties props;
    private final ObjectMapper mapper;
    private final Semaphore slots;
    private final RProcessRunner runner;

    public RAnalyticsService(AnalyticsProperties props, ObjectMapper mapper) {
        this.props = props;
        this.mapper = mapper;
        this.slots = new Semaphore(props.getR().getMaxConcurrent());
        var r = props.getR();
        Path temp = r.getTempDir() == null || r.getTempDir().isBlank() ? null : Path.of(r.getTempDir());
        this.runner = new RProcessRunner(r.getExecutable(), resolveScriptDir(r.getScriptDir()), temp,
                Duration.ofSeconds(r.getTimeoutSeconds()));
    }

    public boolean isEnabled() {
        return props.getR().isEnabled();
    }

    /** @param options validated key=value arguments built by the server (never raw request text) */
    public RResult run(RScript script, List<DatasetRow> rows, List<String> options) {
        if (!props.getR().isEnabled()) {
            return RResult.failed("UNAVAILABLE", "Statistical analysis (R) is disabled on this server.");
        }
        if (rows.isEmpty()) {
            return RResult.failed("INSUFFICIENT_DATA", "No completed assessments match the selected filters.");
        }
        boolean acquired = false;
        try {
            acquired = slots.tryAcquire(5, TimeUnit.SECONDS);
            if (!acquired) {
                return RResult.failed("BUSY", "The statistics engine is busy. Please try again in a moment.");
            }
            String json = runner.run(script, rows, options);
            JsonNode tree = mapper.readTree(json);
            String status = tree.path("status").asText("");
            if ("error".equals(status)) {
                log.warn("R script {} reported an error: {}", script.fileName(), tree.path("message").asText(""));
                return RResult.failed("ERROR", "The statistical analysis could not process this data.");
            }
            if ("insufficient_data".equals(status)) {
                return RResult.failed("INSUFFICIENT_DATA", tree.path("reason").asText("Not enough data."));
            }
            return RResult.ok(tree);
        } catch (RExecutionException e) {
            log.warn("R execution failed ({}): {}", e.kind(), e.getMessage());
            return switch (e.kind()) {
                case NOT_INSTALLED, SCRIPT_MISSING, DISABLED -> RResult.failed("UNAVAILABLE",
                        "Statistical analysis is unavailable because R is not installed or configured on the server.");
                case TIMEOUT -> RResult.failed("TIMEOUT", "The statistical analysis took too long and was stopped.");
                case BUSY -> RResult.failed("BUSY", "The statistics engine is busy. Please try again in a moment.");
                default -> RResult.failed("ERROR", "The statistical analysis failed. Please try again later.");
            };
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RResult.failed("ERROR", "The statistical analysis was interrupted.");
        } catch (RuntimeException e) {   // malformed JSON from R (Jackson 3 exceptions are unchecked)
            log.warn("R output for {} could not be parsed: {}", script.fileName(), e.getClass().getSimpleName());
            return RResult.failed("ERROR", "The statistical analysis returned an unreadable result.");
        } finally {
            if (acquired) {
                slots.release();
            }
        }
    }

    /** Tries the configured folder, then ../R, so it works from the repo root or from backend/. */
    static Path resolveScriptDir(String configured) {
        String c = configured == null || configured.isBlank() ? "R" : configured;
        Path p = Path.of(c);
        if (Files.isDirectory(p)) {
            return p;
        }
        Path up = Path.of("..").resolve(c);
        if (!p.isAbsolute() && Files.isDirectory(up)) {
            return up;
        }
        return p;
    }
}
