import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.regex.Pattern;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.ResourcePropertySource;

/** Temporary read-only inspector. No Spring application context or SQL initializer is started. */
class InspectAnalyticsSchema {
    private static final String COLUMNS = """
            SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = ?
            ORDER BY TABLE_NAME, ORDINAL_POSITION
            """;
    private static final String FOREIGN_KEYS = """
            SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME, CONSTRAINT_NAME
            FROM information_schema.KEY_COLUMN_USAGE
            WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME IS NOT NULL
            ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION
            """;

    public static void main(String[] args) {
        // Suppress library configuration dumps, which can contain JDBC or truststore settings.
        if (LoggerFactory.getILoggerFactory() instanceof LoggerContext logging) {
            logging.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(Level.OFF);
        }
        try {
            inspect();
        } catch (Exception failure) {
            System.err.println("SCHEMA_INSPECTION_FAILED (no credential values are shown)");
            for (int depth = 0; failure != null && depth < 16; depth++) {
                String diagnostic = failure.getClass().getSimpleName();
                if (failure instanceof SQLException sql) {
                    String state = sql.getSQLState();
                    diagnostic += " SQLState=" + (state != null && state.matches("[A-Z0-9]{5}") ? state : "unknown")
                            + " errorCode=" + sql.getErrorCode();
                }
                System.err.println(diagnostic);
                failure = failure.getCause() instanceof Exception cause ? cause : null;
            }
            System.exit(1);
        }
    }

    private static void inspect() throws Exception {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new ResourcePropertySource("classpath:application.properties"));
        var binder = Binder.get(environment);
        var configuration = binder.bind("spring.datasource", Bindable.of(DataSourceProperties.class)).get();
        if (configuration.getUrl() == null || !configuration.getUrl().startsWith("jdbc:mysql://")) {
            throw new IllegalStateException();
        }
        var sslModes = Pattern.compile("(?i)[?&]sslMode=([^&#]*)").matcher(configuration.getUrl());
        if (sslModes.find() && (!"VERIFY_IDENTITY".equals(sslModes.group(1)) || sslModes.find())) {
            throw new IllegalStateException();
        }
        try (var pool = configuration.initializeDataSourceBuilder().type(HikariDataSource.class).build()) {
            binder.bind("spring.datasource.hikari", Bindable.ofInstance(pool));
            if (!"VERIFY_IDENTITY".equals(pool.getDataSourceProperties().getProperty("sslMode"))) {
                throw new IllegalStateException();
            }
            String truststore = pool.getDataSourceProperties().getProperty("trustCertificateKeyStoreUrl");
            if (truststore == null || !Files.isRegularFile(Path.of(URI.create(truststore)))) {
                throw new IllegalStateException();
            }
            pool.setMaximumPoolSize(1);
            pool.setMinimumIdle(0);
            pool.setReadOnly(true);
            pool.setAutoCommit(false);
            pool.setConnectionTimeout(15000);
            try (Connection connection = pool.getConnection()) {
                if (!"openstudy_db".equals(connection.getCatalog())) {
                    throw new IllegalStateException();
                }
                connection.setReadOnly(true);
                System.out.println("SCHEMA: openstudy_db (read-only)");
                print(connection, COLUMNS, "TABLE_NAME\tCOLUMN_NAME\tCOLUMN_TYPE\tIS_NULLABLE", 4);
                print(connection, FOREIGN_KEYS,
                        "TABLE_NAME\tCOLUMN_NAME\tREFERENCED_TABLE_NAME\tREFERENCED_COLUMN_NAME\tCONSTRAINT_NAME", 5);
                connection.rollback();
                System.out.println("READ_ONLY_SCHEMA_INSPECTION_COMPLETE");
            }
        }
    }

    private static void print(Connection connection, String sql, String header, int columns) throws SQLException {
        System.out.println();
        System.out.println(header);
        try (PreparedStatement query = connection.prepareStatement(sql)) {
            query.setString(1, "openstudy_db");
            query.setQueryTimeout(15);
            try (ResultSet rows = query.executeQuery()) {
                int count = 0;
                while (rows.next()) {
                    var output = new StringBuilder();
                    for (int column = 1; column <= columns; column++) {
                        if (column > 1) { output.append('\t'); }
                        String value = rows.getString(column);
                        output.append(value == null ? "" : value.replace('\t', ' ').replace('\n', ' ').replace('\r', ' '));
                    }
                    System.out.println(output);
                    count++;
                }
                System.out.println("ROWS: " + count);
            }
        }
    }
}
