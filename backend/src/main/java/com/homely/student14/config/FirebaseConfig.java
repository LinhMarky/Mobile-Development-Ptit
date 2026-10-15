package com.homely.student14.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "student14.push-enabled", havingValue = "true")
public class FirebaseConfig {
    @Bean(name = "student14FirebaseApp", destroyMethod = "delete")
    FirebaseApp firebaseApp(
            @Value("${student14.firebase-project-id:}") String projectId,
            @Value("${student14.firebase-credentials:}") String credentialsFile) {
        if (projectId.isBlank() || credentialsFile.isBlank()) {
            throw new IllegalStateException(
                    "Bật push cần student14.firebase-project-id và"
                            + " student14.firebase-credentials.");
        }

        Path credentialsPath = Path.of(credentialsFile).toAbsolutePath().normalize();
        if (!Files.isRegularFile(credentialsPath) || !Files.isReadable(credentialsPath)) {
            throw new IllegalStateException(
                    "Không đọc được tệp cấu hình Firebase bên ngoài project.");
        }

        try (InputStream credentialStream = Files.newInputStream(credentialsPath)) {
            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setProjectId(projectId.trim())
                            .setCredentials(GoogleCredentials.fromStream(credentialStream))
                            .setConnectTimeout(10000)
                            .setReadTimeout(20000)
                            .build();
            return FirebaseApp.initializeApp(options, "student14-push");
        } catch (Exception exception) {
            // Không đưa nội dung credential hoặc khóa riêng vào log lỗi.
            throw new IllegalStateException(
                    "Tệp cấu hình Firebase không hợp lệ hoặc không đọc được.");
        }
    }

    @Bean
    FirebaseMessaging firebaseMessaging(
            @Qualifier("student14FirebaseApp") FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
