
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

import java.io.FileInputStream;
import java.io.IOException;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.credentials}")
    private String credentialsPath;


    @Bean
    public Firestore firestore() throws IOException {

        if (FirebaseApp.getApps().isEmpty()) {

            FileInputStream serviceAccount =
                    new FileInputStream(credentialsPath);

            FirebaseOptions options =
                    FirebaseOptions.builder()
                            .setCredentials(
                                    GoogleCredentials.fromStream(
                                            serviceAccount
                                    )
                            )
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

