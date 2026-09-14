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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.project-id}")
    private String projectId;

    @Value("${firebase.private-key-id}")
    private String privateKeyId;

    @Value("${firebase.private-key}")
    private String privateKey;

    @Value("${firebase.client-email}")
    private String clientEmail;

    @Value("${firebase.client-id}")
    private String clientId;

    @Bean
    public Firestore firestore() throws IOException {

        if (FirebaseApp.getApps().isEmpty()) {

            Map<String, Object> serviceAccount =
                    new HashMap<>();

            serviceAccount.put(
                    "type",
                    "service_account"
            );

            serviceAccount.put(
                    "project_id",
                    projectId
            );

            serviceAccount.put(
                    "private_key_id",
                    privateKeyId
            );

            serviceAccount.put(
                    "private_key",
                    privateKey.replace(
                            "\\n",
                            "\n"
                    )
            );

            serviceAccount.put(
                    "client_email",
                    clientEmail
            );

            serviceAccount.put(
                    "client_id",
                    clientId
            );

            serviceAccount.put(
                    "auth_uri",
                    "https://accounts.google.com/o/oauth2/auth"
            );

            serviceAccount.put(
                    "token_uri",
                    "https://oauth2.googleapis.com/token"
            );

            serviceAccount.put(
                    "auth_provider_x509_cert_url",
                    "https://www.googleapis.com/oauth2/v1/certs"
            );

            ObjectMapper mapper =
                    new ObjectMapper();

            String json =
                    mapper.writeValueAsString(
                            serviceAccount
                    );

            GoogleCredentials credentials =
                    GoogleCredentials.fromStream(
                            new ByteArrayInputStream(
                                    json.getBytes(
                                            StandardCharsets.UTF_8
                                    )
                            )
                    );

            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setCredentials(
                                    credentials
                            )
                            .setProjectId(
                                    projectId
                            )
                            .build();

            FirebaseApp.initializeApp(
                    options
            );
        }

        return FirestoreClient.getFirestore();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
