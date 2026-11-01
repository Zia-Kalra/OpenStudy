import java.sql.*;
import java.util.Properties;

class DbProbe {
    public static void main(String[] args) {
        Properties p = new Properties();
        p.setProperty("user", System.getenv("DB_USERNAME"));
        p.setProperty("password", System.getenv("DB_PASSWORD"));
        p.setProperty("sslMode", "VERIFY_IDENTITY");
        p.setProperty("trustCertificateKeyStoreUrl", System.getenv("DB_TRUSTSTORE_URL"));
        p.setProperty("trustCertificateKeyStoreType", "JKS");
        p.setProperty("trustCertificateKeyStorePassword", System.getenv("DB_TRUSTSTORE_PASSWORD"));

        try (Connection c = DriverManager.getConnection(System.getenv("DB_URL"), p)) {
            System.out.println("SUCCESS: Direct JDBC connection worked!");
            System.out.println("Database: " + c.getCatalog());
        } catch (SQLException e) {
            System.out.println("SQLState: " + e.getSQLState());
            System.out.println("Error code: " + e.getErrorCode());
            System.out.println("Message: " + e.getMessage());
        }
    }
}
