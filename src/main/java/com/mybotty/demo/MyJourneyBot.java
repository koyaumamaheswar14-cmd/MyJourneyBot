
        package com.mybotty.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class MyJourneyBot {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final InformationService informationService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final PdfService pdfService;

    private final Map<Long, String> userState =
            new HashMap<>();

    private final Map<Long, String> selectedCategory =
            new HashMap<>();

    private final Map<Long, String> pdfCategory =
            new HashMap<>();

    private final Map<Long, String> manageCategory =
            new HashMap<>();


    public MyJourneyBot(
            InformationService informationService,
            CategoryService categoryService,
            UserService userService,
            PdfService pdfService,
            ObjectMapper objectMapper
    ) {

        this.informationService =
                informationService;

        this.categoryService =
                categoryService;

        this.userService =
                userService;

        this.pdfService =
                pdfService;

        this.objectMapper =
                objectMapper;


        String token =
                System.getenv(
                        "TELEGRAM_BOT_TOKEN"
                );

        if (token == null || token.isBlank()) {

            throw new IllegalStateException(
                    "TELEGRAM_BOT_TOKEN environment variable is not set"
            );
        }


        this.restClient =
                RestClient.builder()
                        .baseUrl(
                                "https://api.telegram.org/bot"
                                        + token
                                        + "/"
                        )
                        .build();
    }


    // =========================================================
    // TELEGRAM WEBHOOK
    // =========================================================

    @PostMapping("/telegram/webhook")
    public ResponseEntity<String> telegramWebhook(
            @RequestBody JsonNode update
    ) {

        try {

            System.out.println(
                    "📩 Telegram webhook received"
            );

            handleUpdate(update);

            return ResponseEntity.ok("OK");

        } catch (Exception e) {

            System.out.println(
                    "Webhook error: "
                            + e.getMessage()
            );

            return ResponseEntity.ok("ERROR");
        }
    }


    // =========================================================
    // UPDATE HANDLER
    // =========================================================

    private void handleUpdate(
            JsonNode update
    ) {

        try {

            JsonNode message =
                    update.get("message");


            if (message == null) {
                return;
            }


            JsonNode chat =
                    message.get("chat");


            if (chat == null) {
                return;
            }


            long chatId =
                    chat.get("id").asLong();


            JsonNode from =
                    message.get("from");


            String firstName =
                    from != null &&
                            from.has("first_name")
                            ? from.get("first_name")
                            .asText()
                            : "";


            String lastName =
                    from != null &&
                            from.has("last_name")
                            ? from.get("last_name")
                            .asText()
                            : null;


            String username =
                    from != null &&
                            from.has("username")
                            ? from.get("username")
                            .asText()
                            : null;


            try {

                userService.saveOrUpdateUser(
                        chatId,
                        firstName,
                        lastName,
                        username
                );

            } catch (Exception e) {

                System.out.println(
                        "User save error: "
                                + e.getMessage()
                );
            }


            if (!message.has("text")) {
                return;
            }


            String text =
                    message.get("text")
                            .asText();


            handleMessage(
                    chatId,
                    text
            );


        } catch (Exception e) {

            System.out.println(
                    "Update error: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // MESSAGE HANDLER
    // =========================================================

    private void handleMessage(
            long chatId,
            String text
    ) {

        try {

            if (text.equals("/start")) {

                clearState(chatId);

                sendMainMenu(
                        chatId
                );

                return;
            }


            if (text.equals("➕ ADD INFORMATION")) {

                showAddInformationMenu(
                        chatId
                );

                return;
            }


            if (text.equals("📂 CREATE CATEGORY")) {

                userState.put(
                        chatId,
                        "CREATE_CATEGORY"
                );

                sendMessage(
                        chatId,
                        "Enter the new category name:"
                );

                return;
            }


            if (text.equals("⚙️ MANAGE CATEGORIES")) {

                showManageCategories(
                        chatId
                );

                return;
            }


            if (text.equals("📖 VIEW INFORMATION")) {

                showViewInformation(
                        chatId
                );

                return;
            }


            if (text.equals("🔍 SEARCH INFORMATION")) {

                userState.put(
                        chatId,
                        "SEARCH"
                );

                sendMessage(
                        chatId,
                        "Enter the text you want to search:"
                );

                return;
            }


            if (text.equals("📄 GENERATE PDF")) {

                showPdfCategories(
                        chatId
                );

                return;
            }


            if (text.equals("⬅️ BACK")) {

                clearState(chatId);

                sendMainMenu(
                        chatId
                );

                return;
            }


            if (text.equals("❌ CANCEL")) {

                clearState(chatId);

                sendMainMenu(
                        chatId
                );

                return;
            }


            String state =
                    userState.get(chatId);


            // =================================================
            // CREATE CATEGORY
            // =================================================

            if ("CREATE_CATEGORY".equals(state)) {

                createCategory(
                        chatId,
                        text
                );

                return;
            }


            // =================================================
            // ADD INFORMATION
            // =================================================

            if ("ADD_INFORMATION".equals(state)) {

                addInformation(
                        chatId,
                        text
                );

                return;
            }


            // =================================================
            // VIEW INFORMATION
            // =================================================

            if ("VIEW_CATEGORY".equals(state)) {

                viewInformation(
                        chatId,
                        text
                );

                return;
            }


            // =================================================
            // SEARCH
            // =================================================

            if ("SEARCH".equals(state)) {

                searchInformation(
                        chatId,
                        text
                );

                return;
            }


            // =================================================
            // MANAGE CATEGORY SELECTION
            // =================================================

            if ("MANAGE_ACTION".equals(state)) {

                handleManageAction(
                        chatId,
                        text
                );

                return;
            }


            // =================================================
            // MANAGE SELECTED ACTION
            // =================================================

            if ("MANAGE_ACTION_SELECTED".equals(state)) {

                handleManageSelectedAction(
                        chatId,
                        text
                );

                return;
            }


            // =================================================
            // RENAME
            // =================================================

            if ("RENAME_CATEGORY".equals(state)) {

                renameCategory(
                        chatId,
                        text
                );

                return;
            }


            // =================================================
            // DELETE
            // =================================================

            if ("DELETE_CATEGORY".equals(state)) {

                deleteCategory(
                        chatId,
                        text
                );

                return;
            }


            // =================================================
            // PDF CATEGORY
            // =================================================

            if ("PDF_CATEGORY".equals(state)) {

                selectPdfCategory(
                        chatId,
                        text
                );

                return;
            }


            // =================================================
            // PDF PERIOD
            // =================================================

            if ("PDF_PERIOD".equals(state)) {

                generatePdfByPeriod(
                        chatId,
                        text
                );

                return;
            }


            // =================================================
            // DYNAMIC CATEGORY SELECTION
            // =================================================

            if (isUserCategory(
                    chatId,
                    text
            )) {

                selectedCategory.put(
                        chatId,
                        text
                );

                userState.put(
                        chatId,
                        "ADD_INFORMATION"
                );

                sendMessage(
                        chatId,
                        "Enter the information you want to save under:\n\n"
                                + "📂 "
                                + text
                );

                return;
            }


            sendMessage(
                    chatId,
                    "Please select an option from the menu."
            );


        } catch (Exception e) {

            System.out.println(
                    "Message handling error: "
                            + e.getMessage()
            );

            sendMessage(
                    chatId,
                    "Something went wrong. Please try again."
            );
        }
    }


    // =========================================================
    // MAIN MENU
    // =========================================================

    private void sendMainMenu(
            long chatId
    ) {

        String keyboard =
                """
                {
                  "keyboard": [
                    ["➕ ADD INFORMATION"],
                    ["📖 VIEW INFORMATION"],
                    ["🔍 SEARCH INFORMATION"],
                    ["📄 GENERATE PDF"],
                    ["⬅️ BACK"]
                  ],
                  "resize_keyboard": true,
                  "one_time_keyboard": false
                }
                """;

        sendKeyboardMessage(
                chatId,
                "Welcome to MyJourney 🚀\n\n"
                        + "Your personal journey information system.",
                keyboard
        );
    }


    // =========================================================
    // ADD INFORMATION
    // =========================================================

    private void showAddInformationMenu(
            long chatId
    ) throws Exception {

        List<String> categories =
                categoryService.getCategories(
                        chatId
                );


        if (categories.isEmpty()) {

            sendMessage(
                    chatId,
                    "You don't have any categories yet.\n\n"
                            + "Create your first category."
            );

            userState.put(
                    chatId,
                    "CREATE_CATEGORY"
            );

            sendMessage(
                    chatId,
                    "Enter the new category name:"
            );

            return;
        }


        StringBuilder keyboard =
                new StringBuilder();

        keyboard.append(
                "{\"keyboard\":["
        );


        for (String category :
                categories) {

            keyboard.append(
                    "[\""
            );

            keyboard.append(
                    escapeJson(category)
            );

            keyboard.append(
                    "\"],"
            );
        }


        keyboard.append(
                "[\"📂 CREATE CATEGORY\"],"
        );

        keyboard.append(
                "[\"⚙️ MANAGE CATEGORIES\"],"
        );

        keyboard.append(
                "[\"⬅️ BACK\"]"
        );

        keyboard.append(
                "],\"resize_keyboard\":true}"
        );


        userState.put(
                chatId,
                "ADD_INFORMATION"
        );


        sendKeyboardMessage(
                chatId,
                "Select a category:",
                keyboard.toString()
        );
    }


    // =========================================================
    // CREATE CATEGORY
    // =========================================================

    private void createCategory(
            long chatId,
            String categoryName
    ) throws Exception {

        categoryName =
                categoryName.trim();


        if (categoryName.isBlank()) {

            sendMessage(
                    chatId,
                    "Category name cannot be empty."
            );

            return;
        }


        List<String> existing =
                categoryService.getCategories(
                        chatId
                );


        for (String category :
                existing) {

            if (category.equalsIgnoreCase(
                    categoryName
            )) {

                sendMessage(
                        chatId,
                        "This category already exists."
                );

                return;
            }
        }


        categoryService.saveCategory(
                chatId,
                categoryName
        );


        clearState(chatId);


        sendMessage(
                chatId,
                "✅ Category created successfully:\n\n"
                        + "📂 "
                        + categoryName
        );


        showAddInformationMenu(
                chatId
        );
    }


    // =========================================================
    // ADD INFORMATION
    // =========================================================

    private void addInformation(
            long chatId,
            String text
    ) throws Exception {

        String category =
                selectedCategory.get(
                        chatId
                );


        if (category == null) {

            showAddInformationMenu(
                    chatId
            );

            return;
        }


        if (text.isBlank()) {

            sendMessage(
                    chatId,
                    "Information cannot be empty."
            );

            return;
        }


        informationService.saveInformation(
                chatId,
                category,
                text
        );


        sendMessage(
                chatId,
                "✅ Information saved.\n\n"
                        + "📂 Category: "
                        + category
                        + "\n\n"
                        + "📝 "
                        + text
        );


        clearState(chatId);


        sendMainMenu(
                chatId
        );
    }


    // =========================================================
    // VIEW INFORMATION
    // =========================================================

    private void showViewInformation(
            long chatId
    ) throws Exception {

        List<String> categories =
                categoryService.getCategories(
                        chatId
                );


        if (categories.isEmpty()) {

            sendMessage(
                    chatId,
                    "You don't have any categories yet."
            );

            return;
        }


        StringBuilder keyboard =
                new StringBuilder();

        keyboard.append(
                "{\"keyboard\":["
        );


        for (String category :
                categories) {

            keyboard.append(
                    "[\""
                            + escapeJson(category)
                            + "\"],"
            );
        }


        keyboard.append(
                "[\"⬅️ BACK\"]"
        );

        keyboard.append(
                "],\"resize_keyboard\":true}"
        );


        userState.put(
                chatId,
                "VIEW_CATEGORY"
        );


        sendKeyboardMessage(
                chatId,
                "Select a category to view:",
                keyboard.toString()
        );
    }


    private void viewInformation(
            long chatId,
            String category
    ) throws Exception {

        if (category.equals("⬅️ BACK")) {

            clearState(chatId);

            sendMainMenu(
                    chatId
            );

            return;
        }


        if (!isUserCategory(
                chatId,
                category
        )) {

            sendMessage(
                    chatId,
                    "Please select a valid category."
            );

            return;
        }


        List<Map<String, Object>> data =
                informationService.getInformation(
                        chatId,
                        category
                );


        if (data.isEmpty()) {

            sendMessage(
                    chatId,
                    "No information found in:\n\n"
                            + "📂 "
                            + category
            );

            clearState(chatId);

            sendMainMenu(
                    chatId
            );

            return;
        }


        StringBuilder result =
                new StringBuilder();


        int count = 1;


        for (Map<String, Object> item :
                data) {

            result.append(
                    count
                            + ". "
                            + item.get("text")
                            + "\n"
            );

            result.append(
                    "📅 "
                            + item.get("createdAt")
                            + "\n\n"
            );

            count++;
        }


        sendMessage(
                chatId,
                "📖 "
                        + category
                        + "\n\n"
                        + result
        );


        clearState(chatId);

        sendMainMenu(
                chatId
        );
    }


    // =========================================================
    // SEARCH INFORMATION
    // =========================================================

    private void searchInformation(
            long chatId,
            String searchText
    ) throws Exception {

        searchText =
                searchText.toLowerCase();


        List<Map<String, Object>> data =
                informationService.getAllInformation(
                        chatId
                );


        StringBuilder result =
                new StringBuilder();


        int count = 0;


        for (Map<String, Object> item :
                data) {

            String text =
                    String.valueOf(
                            item.get("text")
                    );


            if (text.toLowerCase()
                    .contains(searchText)) {

                count++;


                result.append(
                        count
                                + ". "
                                + text
                                + "\n"
                );

                result.append(
                        "📂 Category: "
                                + item.get("category")
                                + "\n"
                );

                result.append(
                        "📅 "
                                + item.get("createdAt")
                                + "\n\n"
                );
            }
        }


        if (count == 0) {

            sendMessage(
                    chatId,
                    "No matching information found."
            );

        } else {

            sendMessage(
                    chatId,
                    "🔍 Search Results\n\n"
                            + result
            );
        }


        clearState(chatId);


        sendMainMenu(
                chatId
        );
    }


    // =========================================================
    // MANAGE CATEGORIES
    // =========================================================

    private void showManageCategories(
            long chatId
    ) throws Exception {

        List<String> categories =
                categoryService.getCategories(
                        chatId
                );


        if (categories.isEmpty()) {

            sendMessage(
                    chatId,
                    "You don't have any categories to manage."
            );

            return;
        }


        StringBuilder keyboard =
                new StringBuilder();

        keyboard.append(
                "{\"keyboard\":["
        );


        for (String category :
                categories) {

            keyboard.append(
                    "[\""
                            + escapeJson(category)
                            + "\"],"
            );
        }


        keyboard.append(
                "[\"⬅️ BACK\"]"
        );

        keyboard.append(
                "],\"resize_keyboard\":true}"
        );


        userState.put(
                chatId,
                "MANAGE_ACTION"
        );


        sendKeyboardMessage(
                chatId,
                "Select a category to manage:",
                keyboard.toString()
        );
    }


    private void handleManageAction(
            long chatId,
            String text
    ) throws Exception {

        if (text.equals("⬅️ BACK")) {

            clearState(chatId);

            sendMainMenu(
                    chatId
            );

            return;
        }


        if (!isUserCategory(
                chatId,
                text
        )) {

            sendMessage(
                    chatId,
                    "Please select a valid category."
            );

            return;
        }


        manageCategory.put(
                chatId,
                text
        );


        userState.put(
                chatId,
                "MANAGE_ACTION_SELECTED"
        );


        String keyboard =
                """
                {
                  "keyboard": [
                    ["✏️ RENAME"],
                    ["🗑️ DELETE"],
                    ["⬅️ BACK"]
                  ],
                  "resize_keyboard": true
                }
                """;


        sendKeyboardMessage(
                chatId,
                "Manage category:\n\n"
                        + "📂 "
                        + text,
                keyboard
        );
    }


    private void handleManageSelectedAction(
            long chatId,
            String text
    ) {

        if (text.equals("⬅️ BACK")) {

            clearState(chatId);

            sendMainMenu(
                    chatId
            );

            return;
        }


        if (text.equals("✏️ RENAME")) {

            userState.put(
                    chatId,
                    "RENAME_CATEGORY"
            );

            sendMessage(
                    chatId,
                    "Enter the new category name:"
            );

            return;
        }


        if (text.equals("🗑️ DELETE")) {

            String category =
                    manageCategory.get(
                            chatId
                    );

            if (category == null) {

                showManageCategoriesSafely(
                        chatId
                );

                return;
            }


            userState.put(
                    chatId,
                    "DELETE_CATEGORY"
            );

            sendMessage(
                    chatId,
                    "Type the category name to confirm deletion:\n\n"
                            + "📂 "
                            + category
            );

            return;
        }


        sendMessage(
                chatId,
                "Please select RENAME, DELETE, or BACK."
        );
    }


    private void showManageCategoriesSafely(
            long chatId
    ) {

        try {

            showManageCategories(
                    chatId
            );

        } catch (Exception e) {

            sendMessage(
                    chatId,
                    "Unable to load categories."
            );
        }
    }


    // =========================================================
    // RENAME CATEGORY
    // =========================================================

    private void renameCategory(
            long chatId,
            String newName
    ) throws Exception {

        String oldName =
                manageCategory.get(
                        chatId
                );


        if (oldName == null) {

            showManageCategories(
                    chatId
            );

            return;
        }


        newName =
                newName.trim();


        if (newName.isBlank()) {

            sendMessage(
                    chatId,
                    "Category name cannot be empty."
            );

            return;
        }


        List<String> existing =
                categoryService.getCategories(
                        chatId
                );


        for (String category :
                existing) {

            if (category.equalsIgnoreCase(
                    newName
            )) {

                sendMessage(
                        chatId,
                        "This category already exists."
                );

                return;
            }
        }


        categoryService.renameCategory(
                chatId,
                oldName,
                newName
        );


        clearState(chatId);


        sendMessage(
                chatId,
                "✅ Category renamed.\n\n"
                        + oldName
                        + " → "
                        + newName
        );


        sendMainMenu(
                chatId
        );
    }


    // =========================================================
    // DELETE CATEGORY
    // =========================================================

    private void deleteCategory(
            long chatId,
            String categoryName
    ) throws Exception {

        String selected =
                manageCategory.get(
                        chatId
                );


        if (selected == null) {

            showManageCategories(
                    chatId
            );

            return;
        }


        if (!categoryName.equals(selected)) {

            sendMessage(
                    chatId,
                    "The category name does not match.\n\n"
                            + "Please type:\n"
                            + selected
            );

            return;
        }


        categoryService.deleteCategory(
                chatId,
                selected
        );


        clearState(chatId);


        sendMessage(
                chatId,
                "🗑️ Category deleted:\n\n"
                        + selected
        );


        sendMainMenu(
                chatId
        );
    }


    // =========================================================
    // PDF
    // =========================================================

    private void showPdfCategories(
            long chatId
    ) throws Exception {

        List<String> categories =
                categoryService.getCategories(
                        chatId
                );


        if (categories.isEmpty()) {

            sendMessage(
                    chatId,
                    "No categories available for PDF generation."
            );

            return;
        }


        StringBuilder keyboard =
                new StringBuilder();

        keyboard.append(
                "{\"keyboard\":["
        );


        for (String category :
                categories) {

            keyboard.append(
                    "[\""
                            + escapeJson(category)
                            + "\"],"
            );
        }


        keyboard.append(
                "[\"📚 ALL\"],"
        );

        keyboard.append(
                "[\"⬅️ BACK\"]"
        );

        keyboard.append(
                "],\"resize_keyboard\":true}"
        );


        userState.put(
                chatId,
                "PDF_CATEGORY"
        );


        sendKeyboardMessage(
                chatId,
                "Select category for PDF:",
                keyboard.toString()
        );
    }


    private void selectPdfCategory(
            long chatId,
            String category
    ) throws Exception {

        if (category.equals("⬅️ BACK")) {

            clearState(chatId);

            sendMainMenu(
                    chatId
            );

            return;
        }


        if (!category.equals("📚 ALL")
                && !isUserCategory(
                chatId,
                category
        )) {

            sendMessage(
                    chatId,
                    "Please select a valid category."
            );

            return;
        }


        pdfCategory.put(
                chatId,
                category
        );


        userState.put(
                chatId,
                "PDF_PERIOD"
        );


        String keyboard =
                """
                {
                  "keyboard": [
                    ["TODAY"],
                    ["THIS WEEK"],
                    ["THIS MONTH"],
                    ["ALL"],
                    ["⬅️ BACK"]
                  ],
                  "resize_keyboard": true
                }
                """;


        sendKeyboardMessage(
                chatId,
                "Select time period:",
                keyboard
        );
    }


    private void generatePdfByPeriod(
            long chatId,
            String period
    ) throws Exception {

        if (period.equals("⬅️ BACK")) {

            clearState(chatId);

            sendMainMenu(
                    chatId
            );

            return;
        }


        String category =
                pdfCategory.get(
                        chatId
                );


        if (category == null) {

            showPdfCategories(
                    chatId
            );

            return;
        }


        if (!period.equals("TODAY")
                && !period.equals("THIS WEEK")
                && !period.equals("THIS MONTH")
                && !period.equals("ALL")) {

            sendMessage(
                    chatId,
                    "Please select a valid time period."
            );

            return;
        }


        List<Map<String, Object>> data =
                informationService.getAllInformation(
                        chatId
                );


        List<Map<String, Object>> filtered =
                filterByCategoryAndPeriod(
                        data,
                        category,
                        period
                );


        if (filtered.isEmpty()) {

            sendMessage(
                    chatId,
                    "No information found for the selected category and period."
            );

            clearState(chatId);

            sendMainMenu(
                    chatId
            );

            return;
        }


        String title =
                "MyJourney - "
                        + category
                        + " - "
                        + period;


        byte[] pdf =
                pdfService.generatePdf(
                        title,
                        filtered
                );


        String safeCategory =
                category.replaceAll(
                        "[^a-zA-Z0-9-_]",
                        "_"
                );


        String safePeriod =
                period.replaceAll(
                        "[^a-zA-Z0-9-_]",
                        "_"
                );


        String fileName =
                "MyJourney_"
                        + safeCategory
                        + "_"
                        + safePeriod
                        + ".pdf";


        sendPdf(
                chatId,
                pdf,
                fileName
        );


        clearState(chatId);
    }


    // =========================================================
    // FILTER PDF DATA
    // =========================================================

    private List<Map<String, Object>>
    filterByCategoryAndPeriod(
            List<Map<String, Object>> data,
            String category,
            String period
    ) {

        List<Map<String, Object>> result =
                new ArrayList<>();


        LocalDate today =
                LocalDate.now();


        for (Map<String, Object> item :
                data) {

            String itemCategory =
                    String.valueOf(
                            item.get("category")
                    );


            if (!category.equals("📚 ALL")
                    && !category.equals("ALL")
                    && !category.equals(itemCategory)) {

                continue;
            }


            String createdAt =
                    String.valueOf(
                            item.get("createdAt")
                    );


            try {

                Instant instant =
                        Instant.parse(
                                createdAt
                        );


                LocalDate date =
                        instant.atZone(
                                        ZoneId.systemDefault()
                                )
                                .toLocalDate();


                boolean include = false;


                switch (period) {

                    case "TODAY" ->
                            include =
                                    date.equals(
                                            today
                                    );


                    case "THIS WEEK" ->
                            include =
                                    !date.isBefore(
                                            today.minusDays(6)
                                    )
                                            &&
                                            !date.isAfter(
                                                    today
                                            );


                    case "THIS MONTH" ->
                            include =
                                    date.getYear()
                                            == today.getYear()
                                            &&
                                            date.getMonth()
                                                    == today.getMonth();


                    case "ALL" ->
                            include = true;
                }


                if (include) {
                    result.add(item);
                }


            } catch (Exception ignored) {
            }
        }


        return result;
    }


    // =========================================================
    // CHECK USER CATEGORY
    // =========================================================

    private boolean isUserCategory(
            long chatId,
            String text
    ) throws Exception {

        List<String> categories =
                categoryService.getCategories(
                        chatId
                );


        for (String category :
                categories) {

            if (category.equals(text)) {
                return true;
            }
        }


        return false;
    }


    // =========================================================
    // SEND NORMAL MESSAGE
    // =========================================================

    private void sendMessage(
            long chatId,
            String text
    ) {

        try {

            Map<String, Object> body =
                    new HashMap<>();


            body.put(
                    "chat_id",
                    chatId
            );


            body.put(
                    "text",
                    text
            );


            restClient.post()
                    .uri("sendMessage")
                    .contentType(
                            MediaType.APPLICATION_JSON
                    )
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();


        } catch (Exception e) {

            System.out.println(
                    "Message error: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // SEND KEYBOARD MESSAGE
    // =========================================================

    private void sendKeyboardMessage(
            long chatId,
            String text,
            String keyboardJson
    ) {

        try {

            Map<String, Object> body =
                    new HashMap<>();


            body.put(
                    "chat_id",
                    chatId
            );


            body.put(
                    "text",
                    text
            );


            Map<String, Object> replyMarkup =
                    objectMapper.readValue(
                            keyboardJson,
                            new TypeReference<Map<String, Object>>() {
                            }
                    );


            body.put(
                    "reply_markup",
                    replyMarkup
            );


            restClient.post()
                    .uri("sendMessage")
                    .contentType(
                            MediaType.APPLICATION_JSON
                    )
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();


        } catch (Exception e) {

            System.out.println(
                    "Keyboard message error: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // SEND PDF
    // =========================================================

    private void sendPdf(
            long chatId,
            byte[] pdf,
            String fileName
    ) {

        try {

            org.springframework.util.LinkedMultiValueMap<String, Object>
                    body =
                    new org.springframework.util.LinkedMultiValueMap<>();


            body.add(
                    "chat_id",
                    String.valueOf(chatId)
            );


            body.add(
                    "document",
                    new org.springframework.core.io.ByteArrayResource(
                            pdf
                    ) {

                        @Override
                        public String getFilename() {
                            return fileName;
                        }
                    }
            );


            restClient.post()
                    .uri("sendDocument")
                    .contentType(
                            MediaType.MULTIPART_FORM_DATA
                    )
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();


        } catch (Exception e) {

            System.out.println(
                    "PDF send error: "
                            + e.getMessage()
            );
        }
    }


    // =========================================================
    // CLEAR USER STATE
    // =========================================================

    private void clearState(
            long chatId
    ) {

        userState.remove(
                chatId
        );

        selectedCategory.remove(
                chatId
        );

        pdfCategory.remove(
                chatId
        );

        manageCategory.remove(
                chatId
        );
    }


    // =========================================================
    // JSON ESCAPE
    // =========================================================

    private String escapeJson(
            String value
    ) {

        if (value == null) {
            return "";
        }


        return value
                .replace(
                        "\\",
                        "\\\\"
                )
                .replace(
                        "\"",
                        "\\\""
                )
                .replace(
                        "\n",
                        "\\n"
                )
                .replace(
                        "\r",
                        "\\r"
                );
    }
}


