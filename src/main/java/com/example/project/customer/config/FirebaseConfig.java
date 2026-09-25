package com.example.project.customer.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Configuration
public class FirebaseConfig {

    private final Environment environment;

    @Value("${firebase.project-id:${FIREBASE_PROJECT_ID:}}")
    private String projectId;

    @Value("${firebase.client-email:${FIREBASE_CLIENT_EMAIL:}}")
    private String clientEmail;

    @Value("${firebase.client-id:${FIREBASE_CLIENT_ID:}}")
    private String clientId;

    @Value("${firebase.private-key:${FIREBASE_PRIVATE_KEY:}}")
    private String privateKey;

    @Value("${firebase.private-key-id:${FIREBASE_PRIVATE_KEY_ID:}}")
    private String privateKeyId;

    @Value("${firebase.credentials-json:${FIREBASE_CREDENTIALS_JSON:}}")
    private String credentialsJson;

    @Value("${firebase.credentials-base64:${FIREBASE_CREDENTIALS_BASE64:}}")
    private String credentialsBase64;

    @Value("${firebase.config-path:${FIREBASE_CONFIG_PATH:}}")
    private String configPath;

    public FirebaseConfig(Environment environment) {
        this.environment = environment;
    }

    /**
     * Returns true if the current active profile is the 'dev' profile, where Firebase
     * credentials are optional (H2 in-memory DB is used and token verification is skipped).
     */
    private boolean isDevProfile() {
        return environment.acceptsProfiles(Profiles.of("dev"));
    }

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try {
            GoogleCredentials credentials = resolveCredentials();

            if (credentials == null) {
                if (isDevProfile()) {
                    // In dev mode, Firebase is optional. Return null — the filter handles a null FirebaseApp gracefully.
                    log.warn("[DEV] No Firebase credentials configured. Firebase token verification is DISABLED. "
                            + "Set FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL, FIREBASE_CLIENT_ID, FIREBASE_PRIVATE_KEY, FIREBASE_PRIVATE_KEY_ID to enable it.");
                    return null;
                }
                // In production, missing credentials is a fatal misconfiguration.
                throw new IllegalStateException(
                    "Firebase credentials are required in production but none were found. "
                    + "Ensure the following GitHub Secrets are set and injected via CI/CD: "
                    + "FIREBASE_PROJECT_ID, FIREBASE_CLIENT_EMAIL, FIREBASE_CLIENT_ID, FIREBASE_PRIVATE_KEY, FIREBASE_PRIVATE_KEY_ID "
                    + "(or FIREBASE_CREDENTIALS_JSON / FIREBASE_CREDENTIALS_BASE64)."
                );
            }

            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(credentials);

            if (projectId != null && !projectId.isBlank()) {
                optionsBuilder.setProjectId(projectId.trim());
            }

            FirebaseApp app = FirebaseApp.initializeApp(optionsBuilder.build());
            log.info("Firebase Admin SDK successfully initialized (Project ID: {})", projectId);
            return app;

        } catch (IllegalStateException e) {
            // Re-throw fatal configuration errors immediately — do NOT swallow.
            throw e;
        } catch (Exception e) {
            if (isDevProfile()) {
                log.warn("[DEV] Firebase Admin SDK initialization failed: {}. Firebase token verification is DISABLED.", e.getMessage());
                return null;
            }
            // In production, any initialization failure is fatal.
            throw new IllegalStateException(
                "Failed to initialize Firebase Admin SDK. Check FIREBASE_* environment variables injected from GitHub Secrets. Cause: " + e.getMessage(), e
            );
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        if (firebaseApp == null) {
            log.warn("FirebaseApp is null — FirebaseAuth bean will be null. Token verification is disabled.");
            return null;
        }
        try {
            return FirebaseAuth.getInstance(firebaseApp);
        } catch (Exception e) {
            log.warn("FirebaseAuth instance could not be retrieved: {}", e.getMessage());
            return null;
        }
    }

    private GoogleCredentials resolveCredentials() {
        try {
            // 1. Direct Service Account JSON string
            if (credentialsJson != null && !credentialsJson.isBlank()) {
                log.info("Initializing Firebase using FIREBASE_CREDENTIALS_JSON");
                InputStream stream = new ByteArrayInputStream(credentialsJson.getBytes(StandardCharsets.UTF_8));
                return GoogleCredentials.fromStream(stream);
            }

            // 2. Base64 encoded Service Account JSON
            if (credentialsBase64 != null && !credentialsBase64.isBlank()) {
                log.info("Initializing Firebase using FIREBASE_CREDENTIALS_BASE64");
                byte[] decoded = Base64.getDecoder().decode(credentialsBase64.trim());
                InputStream stream = new ByteArrayInputStream(decoded);
                return GoogleCredentials.fromStream(stream);
            }

            // 3. File path
            if (configPath != null && !configPath.isBlank()) {
                log.info("Initializing Firebase using config file path: {}", configPath);
                try (InputStream stream = new FileInputStream(configPath)) {
                    return GoogleCredentials.fromStream(stream);
                }
            }

            // 4. Classpath resource
            InputStream cpStream = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
            if (cpStream != null) {
                log.info("Initializing Firebase using classpath:firebase-service-account.json");
                return GoogleCredentials.fromStream(cpStream);
            }

            // 5. Discrete environment variables: FIREBASE_CLIENT_EMAIL + FIREBASE_PRIVATE_KEY + FIREBASE_CLIENT_ID + FIREBASE_PRIVATE_KEY_ID
            log.info(
                "Firebase credential inputs: projectIdPresent={}, clientEmailPresent={}, clientIdPresent={}, privateKeyIdPresent={}, privateKeyLength={}",
                projectId != null && !projectId.isBlank(),
                clientEmail != null && !clientEmail.isBlank(),
                clientId != null && !clientId.isBlank(),
                privateKeyId != null && !privateKeyId.isBlank(),
                privateKey == null ? 0 : privateKey.length()
            );

            if (clientEmail != null && !clientEmail.isBlank()
                    && privateKey != null && !privateKey.isBlank()
                    && clientId != null && !clientId.isBlank()
                    && privateKeyId != null && !privateKeyId.isBlank()) {
                log.info("Initializing Firebase using discrete environment variables");
                String formattedKey = privateKey.replace("\\n", "\n").replace("\r", "").trim();
                String jsonCredentials = String.format(
                        "{\n"
                        + "  \"type\": \"service_account\",\n"
                        + "  \"project_id\": \"%s\",\n"
                        + "  \"private_key_id\": \"%s\",\n"
                        + "  \"private_key\": \"%s\",\n"
                        + "  \"client_email\": \"%s\",\n"
                        + "  \"client_id\": \"%s\",\n"
                        + "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n"
                        + "  \"token_uri\": \"https://oauth2.googleapis.com/token\"\n"
                        + "}",
                        projectId != null ? projectId.trim() : "",
                        privateKeyId.trim(),
                        formattedKey.replace("\n", "\\n"),
                        clientEmail.trim(),
                        clientId.trim()
                );
                InputStream stream = new ByteArrayInputStream(jsonCredentials.getBytes(StandardCharsets.UTF_8));
                return GoogleCredentials.fromStream(stream);
            }

            // 6. Application Default Credentials (GCP-hosted environments)
            log.info("Attempting to initialize Firebase using Application Default Credentials");
            return GoogleCredentials.getApplicationDefault();

        } catch (Exception e) {
            log.error("Could not load GoogleCredentials from configured sources", e);
            return null;
        }
    }
}
