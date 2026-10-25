import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import java.util.Arrays;

/** Checks the local truststore without opening a database connection or changing the file. */
class ValidateTruststore {
    public static void main(String[] args) {
        String password = System.getenv("DB_TRUSTSTORE_PASSWORD");
        if (args.length != 1 || password == null || password.isEmpty()) {
            System.err.println("Truststore validation requires a file and DB_TRUSTSTORE_PASSWORD.");
            System.exit(1);
        }
        char[] secret = password.toCharArray();
        password = null;
        int exitCode = 0;
        try (var input = Files.newInputStream(Path.of(args[0]))) {
            var store = KeyStore.getInstance("JKS");
            store.load(input, secret);
            if (store.size() == 0) {
                System.err.println("The truststore contains no certificates.");
                exitCode = 1;
            } else {
                System.out.println("Truststore password and file: OK.");
            }
        } catch (IOException failure) {
            if (failure.getCause() instanceof UnrecoverableKeyException) {
                System.err.println("Cannot unlock aiven-truststore.jks. DB_TRUSTSTORE_PASSWORD is incorrect, or the file's integrity check failed. Use the password set when this JKS was created (the one used for your successful schema inspection).");
            } else {
                System.err.println("Cannot read the JKS truststore. Check the file path, permissions, and file format.");
            }
            exitCode = 1;
        } catch (Exception failure) {
            System.err.println("Truststore validation failed. Check the JKS file and Java installation.");
            exitCode = 1;
        } finally {
            Arrays.fill(secret, '\0');
        }
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }
}
