package com.example.project.customer.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Configuration
public class FirebaseConfig {

    @Value("${firebase.project-id:${FIREBASE_PROJECT_ID:}}")
    private String projectId;

    @Value("${firebase.client-email:${FIREBASE_CLIENT_EMAIL:}}")
    private String clientEmail;

    @Value("${firebase.private-key:${FIREBASE_PRIVATE_KEY:}}")
    private String privateKey;

    @Value("${firebase.credentials-json:${FIREBASE_CREDENTIALS_JSON:}}")
    private String credentialsJson;

    @Value("${firebase.credentials-base64:${FIREBASE_CREDENTIALS_BASE64:}}")
    private String credentialsBase64;

    @Value("${firebase.config-path:${FIREBASE_CONFIG_PATH:}}")
    private String configPath;

    @Bean
    public FirebaseApp firebaseApp() {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        try {
            GoogleCredentials credentials = resolveCredentials();
            if (credentials == null) {
                // Fallback token to allow FirebaseApp initialization for public key ID token verification
                credentials = GoogleCredentials.create(new com.google.auth.oauth2.AccessToken("public-verify-token", new java.util.Date(System.currentTimeMillis() + 86400000000L)));
            }

            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(credentials);

            if (projectId != null && !projectId.isBlank()) {
                optionsBuilder.setProjectId(projectId.trim());
            }

            FirebaseApp app = FirebaseApp.initializeApp(optionsBuilder.build());
            log.info("Firebase Admin SDK successfully initialized (Project ID: {})", projectId);
            return app;
        } catch (Exception e) {
            log.warn("Firebase Admin SDK initialization skipped or encountered warning: {}. Running with default/fallback credentials.", e.getMessage());
            try {
                // Fallback attempt with empty/default options for local dev
                if (FirebaseApp.getApps().isEmpty()) {
                    return FirebaseApp.initializeApp();
                }
                return FirebaseApp.getInstance();
            } catch (Exception ex) {
                log.error("Unable to initialize FirebaseApp: {}", ex.getMessage());
                return null;
            }
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        if (firebaseApp != null) {
            return FirebaseAuth.getInstance(firebaseApp);
        }
        try {
            return FirebaseAuth.getInstance();
        } catch (Exception e) {
            log.warn("FirebaseAuth instance could not be retrieved directly: {}", e.getMessage());
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

            // 3. File path from environment or properties
            if (configPath != null && !configPath.isBlank()) {
                log.info("Initializing Firebase using config file path: {}", configPath);
                try (InputStream stream = new FileInputStream(configPath)) {
                    return GoogleCredentials.fromStream(stream);
                }
            }

            // 4. Classpath resource: firebase-service-account.json
            InputStream cpStream = getClass().getClassLoader().getResourceAsStream("firebase-service-account.json");
            if (cpStream != null) {
                log.info("Initializing Firebase using classpath:firebase-service-account.json");
                return GoogleCredentials.fromStream(cpStream);
            }

            // 5. Discrete Environment Variables: Project ID, Client Email, Private Key
            if (clientEmail != null && !clientEmail.isBlank() && privateKey != null && !privateKey.isBlank()) {
                log.info("Initializing Firebase using discrete environment variables (Client Email: {})", clientEmail);
                String formattedKey = privateKey.replace("\\n", "\n").trim();
                String jsonCredentials = String.format(
                        "{\n" +
                                "  \"type\": \"service_account\",\n" +
                                "  \"project_id\": \"%s\",\n" +
                                "  \"client_email\": \"%s\",\n" +
                                "  \"private_key\": \"%s\"\n" +
                                "}",
                        projectId != null ? projectId.trim() : "",
                        clientEmail.trim(),
                        formattedKey.replace("\n", "\\n")
                );
                InputStream stream = new ByteArrayInputStream(jsonCredentials.getBytes(StandardCharsets.UTF_8));
                return GoogleCredentials.fromStream(stream);
            }

            // 6. Try Google Application Default Credentials (e.g., AWS EC2 with GCP connector or local gcloud auth)
            log.info("Attempting to initialize Firebase using Application Default Credentials");
            return GoogleCredentials.getApplicationDefault();
        } catch (Exception e) {
            log.warn("Could not load GoogleCredentials from configured sources: {}", e.getMessage());
            return null;
        }
    }
}
