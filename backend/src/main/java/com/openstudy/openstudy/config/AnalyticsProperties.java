package com.openstudy.openstudy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings for the Member 4 analytics module ({@code openstudy.analytics.*}).
 * Nothing here is machine-specific: paths and executables come from the environment.
 */
@ConfigurationProperties(prefix = "openstudy.analytics")
public class AnalyticsProperties {

    private final R r = new R();
    private final DevIdentity devIdentity = new DevIdentity();

    /** HTTP session attribute that Member 1's login stores the authenticated User under. */
    private String sessionAttribute = "currentUser";
    /** Percentage at or above which a result counts as a pass (used for pass rate and insights). */
    private double passMark = 50.0;
    /** Minimum completed assessments before any insight or recommendation is produced. */
    private int minResultsForInsights = 3;
    /** Hard cap on rows loaded for one request (protects memory and R run time). */
    private int maxRows = 20000;
    /** Seconds a statistical report is cached per teacher and filter. 0 disables the cache. */
    private int reportCacheSeconds = 60;

    public R getR() { return r; }
    public DevIdentity getDevIdentity() { return devIdentity; }
    public String getSessionAttribute() { return sessionAttribute; }
    public void setSessionAttribute(String sessionAttribute) { this.sessionAttribute = sessionAttribute; }
    public double getPassMark() { return passMark; }
    public void setPassMark(double passMark) { this.passMark = passMark; }
    public int getMinResultsForInsights() { return minResultsForInsights; }
    public void setMinResultsForInsights(int v) { this.minResultsForInsights = Math.max(1, v); }
    public int getMaxRows() { return maxRows; }
    public void setMaxRows(int maxRows) { this.maxRows = Math.max(100, maxRows); }
    public int getReportCacheSeconds() { return reportCacheSeconds; }
    public void setReportCacheSeconds(int v) { this.reportCacheSeconds = Math.max(0, v); }

    public static class R {
        /** Set false to run without R: SQL-based figures still work, statistical sections report "unavailable". */
        private boolean enabled = true;
        /** Rscript executable name or absolute path (env RSCRIPT_PATH). */
        private String executable = "Rscript";
        /** Folder containing the analysis scripts. Relative paths are tried as-is and as ../R. */
        private String scriptDir = "R";
        /** Folder for temporary CSV/JSON files. Blank = the JVM temp directory. */
        private String tempDir = "";
        private int timeoutSeconds = 30;
        /** Maximum R processes running at once. */
        private int maxConcurrent = 2;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getExecutable() { return executable; }
        public void setExecutable(String executable) { this.executable = executable; }
        public String getScriptDir() { return scriptDir; }
        public void setScriptDir(String scriptDir) { this.scriptDir = scriptDir; }
        public String getTempDir() { return tempDir; }
        public void setTempDir(String tempDir) { this.tempDir = tempDir; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int v) { this.timeoutSeconds = Math.max(1, v); }
        public int getMaxConcurrent() { return maxConcurrent; }
        public void setMaxConcurrent(int v) { this.maxConcurrent = Math.max(1, v); }
    }

    /**
     * DEVELOPMENT ONLY. Until Member 1's login exists, lets the analytics APIs act as a fixed
     * student/teacher chosen on the SERVER (never by the browser). Must stay disabled in production.
     */
    public static class DevIdentity {
        private boolean enabled = false;
        private long studentId = 0;
        private long teacherId = 0;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public long getStudentId() { return studentId; }
        public void setStudentId(long studentId) { this.studentId = studentId; }
        public long getTeacherId() { return teacherId; }
        public void setTeacherId(long teacherId) { this.teacherId = teacherId; }
    }
}
