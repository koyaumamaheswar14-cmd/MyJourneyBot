package com.mybotty.demo;
import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InformationService {

    private final Firestore firestore;

    public InformationService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void saveInformation(
            long chatId,
            String category,
            String text
    ) {

        Map<String, Object> data = new HashMap<>();

        data.put("chatId", chatId);
        data.put("category", category);
        data.put("text", text);
        data.put("createdAt", Instant.now().toString());

        firestore.collection("information")
                .add(data);
    }

    public List<Map<String, Object>> getInformation(
            long chatId,
            String category
    ) throws Exception {

        return firestore.collection("information")
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("category", category)
                .get()
                .get()
                .getDocuments()
                .stream()
                .map(document -> document.getData())
                .toList();
    }

    public List<Map<String, Object>> getAllInformation(
            long chatId
    ) throws Exception {

        return firestore.collection("information")
                .whereEqualTo("chatId", chatId)
                .get()
                .get()
                .getDocuments()
                .stream()
                .map(document -> document.getData())
                .toList();
    }
}
