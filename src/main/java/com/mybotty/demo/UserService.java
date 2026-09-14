package com.mybotty.demo;

import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final Firestore firestore;

    public UserService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void saveOrUpdateUser(
            long chatId,
            String firstName,
            String lastName,
            String username
    ) throws Exception {

        String displayName = firstName;

        if (lastName != null && !lastName.isBlank()) {
            displayName += " " + lastName;
        }

        Map<String, Object> data = new HashMap<>();

        data.put("chatId", chatId);
        data.put("displayName", displayName);
        data.put("firstName", firstName);
        data.put("lastName", lastName);
        data.put("username", username);
        data.put("updatedAt", Instant.now().toString());

        firestore.collection("users")
                .document(String.valueOf(chatId))
                .set(data)
                .get();
    }
}