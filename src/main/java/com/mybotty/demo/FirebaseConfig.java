package com.mybotty.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials:}")
    private String credentialsPath;

    @Value("${FIREBASE_CREDENTIALS:}")
    private String firebaseCredentials;

    @Bean
    public Firestore firestore() throws IOException {

        if (FirebaseApp.getApps().isEmpty()) {

            GoogleCredentials credentials;

            if (firebaseCredentials != null
                    && !firebaseCredentials.isBlank()) {

                credentials = GoogleCredentials.fromStream(
                        new ByteArrayInputStream(
                                firebaseCredentials.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        )
                );

            } else {

                FileInputStream serviceAccount =
                        new FileInputStream(credentialsPath);

                credentials =
                        GoogleCredentials.fromStream(
                                serviceAccount
                        );

                serviceAccount.close();
            }

            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setCredentials(credentials)
                            .build();

            FirebaseApp.initializeApp(options);
        }

        return FirestoreClient.getFirestore();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
