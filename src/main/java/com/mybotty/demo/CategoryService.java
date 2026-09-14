package com.mybotty.demo;

import com.google.cloud.firestore.Firestore;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CategoryService {

    private final Firestore firestore;

    public CategoryService(Firestore firestore) {
        this.firestore = firestore;
    }

    public void saveCategory(long chatId, String categoryName) throws Exception {

        Map<String, Object> data = new HashMap<>();

        data.put("chatId", chatId);
        data.put("name", categoryName);
        data.put("createdAt", Instant.now().toString());

        firestore.collection("categories")
                .add(data)
                .get();
    }

    public List<String> getCategories(long chatId) throws Exception {

        return firestore.collection("categories")
                .whereEqualTo("chatId", chatId)
                .get()
                .get()
                .getDocuments()
                .stream()
                .map(document -> document.getString("name"))
                .toList();
    }
    public void deleteCategory(
            long chatId,
            String categoryName
    ) throws Exception {

        var documents = firestore.collection("categories")
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("name", categoryName)
                .get()
                .get()
                .getDocuments();

        for (var document : documents) {
            document.getReference()
                    .delete()
                    .get();
        }
    }

    public void renameCategory(
            long chatId,
            String oldName,
            String newName
    ) throws Exception {

        var documents = firestore.collection("categories")
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("name", oldName)
                .get()
                .get()
                .getDocuments();

        for (var document : documents) {
            Map<String, Object> updates = new HashMap<>();

            updates.put("name", newName);

            document.getReference()
                    .update(updates)
                    .get();
        }
    }
}