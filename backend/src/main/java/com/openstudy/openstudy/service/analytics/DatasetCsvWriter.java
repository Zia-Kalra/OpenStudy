package com.openstudy.openstudy.service.analytics;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Writes the R input CSV (RFC 4180 quoting, spreadsheet-formula neutralised, UTF-8). */
public final class DatasetCsvWriter {

    static final String HEADER = "student_key,assessment_type,subject_name,assessment_title,percentage,completed_at";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DatasetCsvWriter() {
    }

    public static void write(Path file, List<DatasetRow> rows) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(toCsv(rows));
        }
    }

    public static String toCsv(List<DatasetRow> rows) {
        StringBuilder sb = new StringBuilder(HEADER).append('\n');
        for (DatasetRow r : rows) {
            sb.append(field(r.studentKey())).append(',')
              .append(field(r.assessmentType())).append(',')
              .append(field(r.subjectName())).append(',')
              .append(field(r.assessmentTitle())).append(',')
              .append(String.format(Locale.ROOT, "%.4f", r.percentage())).append(',')
              .append(r.completedAt() == null ? "" : TS.format(r.completedAt())).append('\n');
        }
        return sb.toString();
    }

    static String field(String value) {
        if (value == null) {
            return "";
        }
        String v = value.replace("\r", " ").replace("\n", " ");
        if (!v.isEmpty() && "=+-@\t".indexOf(v.charAt(0)) >= 0) {
            v = "'" + v;   // neutralise formula injection if the file is ever opened in a spreadsheet
        }
        if (v.indexOf(',') >= 0 || v.indexOf('"') >= 0) {
            v = '"' + v.replace("\"", "\"\"") + '"';
        }
        return v;
    }
}
