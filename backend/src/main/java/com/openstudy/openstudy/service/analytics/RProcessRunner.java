package com.openstudy.openstudy.service.analytics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Runs one allow-listed R script in a private temporary folder and returns its JSON output.
 *
 * <p>Safety properties: no shell is involved (argument list, not a command string); only
 * {@link RScript} constants are runnable; the child process gets an environment with database,
 * API-key and password variables removed; it is killed after the timeout; the temporary folder is
 * always deleted. This class has no Spring dependency, so the execution mechanism can be replaced
 * (for example by Rserve) without touching callers.
 */
public final class RProcessRunner {

    private static final long MAX_OUTPUT_BYTES = 5L * 1024 * 1024;

    private final String executable;
    private final Path scriptDir;
    private final Path tempBase;
    private final Duration timeout;

    public RProcessRunner(String executable, Path scriptDir, Path tempBase, Duration timeout) {
        this.executable = executable;
        this.scriptDir = scriptDir;
        this.tempBase = tempBase;
        this.timeout = timeout;
    }

    public String run(RScript script, List<DatasetRow> rows, List<String> options) throws RExecutionException {
        Path scriptFile = scriptDir.resolve(script.fileName()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(scriptFile)) {
            throw new RExecutionException(RExecutionException.Kind.SCRIPT_MISSING,
                    "R script not found: " + script.fileName() + " (check openstudy.analytics.r.script-dir)");
        }
        Path work = null;
        try {
            work = tempBase == null ? Files.createTempDirectory("openstudy-r-")
                                    : Files.createTempDirectory(tempBase, "openstudy-r-");
            restrict(work);
            Path in = work.resolve("input.csv");
            Path out = work.resolve("output.json");
            Path err = work.resolve("stderr.txt");
            DatasetCsvWriter.write(in, rows);

            List<String> cmd = new ArrayList<>();
            cmd.add(executable);
            cmd.add("--default-packages=methods,stats,utils,grDevices");
            cmd.add(scriptFile.toString());
            cmd.add(in.toString());
            cmd.add(out.toString());
            cmd.addAll(options);

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.directory(work.toFile());
            pb.redirectInput(ProcessBuilder.Redirect.from(nullDevice(work).toFile()));
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(err.toFile());
            pb.environment().keySet().removeIf(RProcessRunner::isSensitive);

            Process process;
            try {
                process = pb.start();
            } catch (IOException e) {
                throw new RExecutionException(RExecutionException.Kind.NOT_INSTALLED,
                        "Rscript could not be started. Install R and set openstudy.analytics.r.executable / RSCRIPT_PATH.", e);
            }
            boolean finished;
            try {
                finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw new RExecutionException(RExecutionException.Kind.IO, "Interrupted while waiting for R.", e);
            }
            if (!finished) {
                process.descendants().forEach(ProcessHandle::destroyForcibly);
                process.destroyForcibly();
                throw new RExecutionException(RExecutionException.Kind.TIMEOUT,
                        script.fileName() + " exceeded " + timeout.toSeconds() + "s and was stopped.");
            }
            int exit = process.exitValue();
            if (!Files.isRegularFile(out)) {
                throw new RExecutionException(RExecutionException.Kind.SCRIPT_FAILED,
                        script.fileName() + " exited with code " + exit + " and produced no output. " + tail(err));
            }
            if (Files.size(out) > MAX_OUTPUT_BYTES) {
                throw new RExecutionException(RExecutionException.Kind.BAD_OUTPUT, "R output was unexpectedly large.");
            }
            String json = Files.readString(out, StandardCharsets.UTF_8);
            if (exit != 0) {
                // The script writes {"status":"error","message":...} before exiting non-zero.
                throw new RExecutionException(RExecutionException.Kind.SCRIPT_FAILED,
                        script.fileName() + " failed (exit " + exit + "): " + abbreviate(json));
            }
            return json;
        } catch (IOException e) {
            throw new RExecutionException(RExecutionException.Kind.IO, "Temporary file handling failed.", e);
        } finally {
            deleteQuietly(work);
        }
    }

    static boolean isSensitive(String name) {
        String n = name.toUpperCase(Locale.ROOT);
        return n.startsWith("DB_") || n.startsWith("GROQ") || n.contains("PASSWORD") || n.contains("SECRET")
                || n.contains("TOKEN") || n.contains("API_KEY") || n.startsWith("SPRING_DATASOURCE");
    }

    private static Path nullDevice(Path work) throws IOException {
        Path empty = work.resolve("stdin.txt");
        Files.writeString(empty, "");
        return empty;
    }

    private static void restrict(Path dir) {
        try {
            Files.setPosixFilePermissions(dir, PosixFilePermissions.fromString("rwx------"));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Non-POSIX file system (Windows): the per-user temp directory is already private.
        }
    }

    private static String tail(Path err) {
        try {
            return abbreviate(Files.readString(err, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return "";
        }
    }

    private static String abbreviate(String s) {
        String t = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        return t.length() > 300 ? t.substring(0, 300) + "..." : t;
    }

    static void deleteQuietly(Path dir) {
        if (dir == null) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException ignored) {
                    // best effort; the OS temp cleaner removes leftovers
                }
            });
        } catch (IOException ignored) {
            // nothing to clean up
        }
    }
}
