
        package com.mybotty.demo;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class MyJourneyBot implements CommandLineRunner {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    private final InformationService informationService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final PdfService pdfService;

    /*
     * Stores the current state of every Telegram user.
     */
    private final Map<Long, String> userState =
            new HashMap<>();

    /*
     * Category selected while adding information.
     */
    private final Map<Long, String> selectedCategory =
            new HashMap<>();

    /*
     * Category selected for PDF generation.
     */
    private final Map<Long, String> pdfCategory =
            new HashMap<>();

    /*
     * Category selected for rename/delete.
     */
    private final Map<Long, String> manageCategory =
            new HashMap<>();


    public MyJourneyBot(
            InformationService informationService,
            CategoryService categoryService,
            UserService userService,
            PdfService pdfService,
            ObjectMapper objectMapper
    ) {

        this.informationService = informationService;
        this.categoryService = categoryService;
        this.userService = userService;
        this.pdfService = pdfService;
        this.objectMapper = objectMapper;

        String token = System.getenv("TELEGRAM_BOT_TOKEN");

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
    // START BOT
    // =========================================================

    @Override
    public void run(String... args) {

        System.out.println(
                "🚀 MyJourney Telegram Bot started"
        );

        Thread pollingThread =
                new Thread(
                        this::startPolling,
                        "telegram-polling-thread"
                );

        pollingThread.start();
    }


    // =========================================================
    // TELEGRAM POLLING
    // =========================================================

    private void startPolling() {

        long offset = 0;

        while (true) {

            try {

                final long currentOffset = offset;

                String response =
                        restClient.get()
                                .uri(uriBuilder ->
                                        uriBuilder
                                                .path("getUpdates")
                                                .queryParam(
                                                        "offset",
                                                        currentOffset
                                                )
                                                .queryParam(
                                                        "timeout",
                                                        30
                                                )
                                                .build()
                                )
                                .retrieve()
                                .body(String.class);

                JsonNode root =
                        objectMapper.readTree(response);

                JsonNode updates =
                        root.get("result");

                if (updates == null ||
                        !updates.isArray()) {
                    continue;
                }

                for (JsonNode update : updates) {

                    if (update.has("update_id")) {

                        offset =
                                update.get("update_id")
                                        .asLong() + 1;
                    }

                    handleUpdate(update);
                }

            } catch (Exception e) {

                System.out.println(
                        "Polling error: "
                                + e.getMessage()
                );

                try {

                    Thread.sleep(3000);

                } catch (InterruptedException ex) {

                    Thread.currentThread()
                            .interrupt();

                    return;
                }
            }
        }
    }


    // =========================================================
    // UPDATE HANDLER
    // =========================================================

    private void handleUpdate(JsonNode update) {

        try {

            /*
             * We only process normal Telegram messages.
             */
            JsonNode message =
                    update.get("message");

            if (message == null) {
                return;
            }

            JsonNode chat =
                    message.get("chat");

            if (chat == null ||
                    !chat.has("id")) {
                return;
            }

            long chatId =
                    chat.get("id").asLong();


            /*
             * Save/update Telegram user.
             */
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


            /*
             * Ignore messages without text.
             */
            if (!message.has("text")) {
                return;
            }

            String text =
                    message.get("text")
                            .asText()
                            .trim();

            if (text.isBlank()) {
                return;
            }


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

            /*
             * GLOBAL COMMANDS
             */

            if (text.equals("/start")) {

                clearState(chatId);

                sendMainMenu(chatId);

                return;
            }


            /*
             * GLOBAL BACK
             */

            if (text.equals("⬅️ BACK")) {

                clearState(chatId);

                sendMainMenu(chatId);

                return;
            }


            /*
             * GLOBAL CANCEL
             */

            if (text.equals("❌ CANCEL")) {

                clearState(chatId);

                sendMainMenu(chatId);

                return;
            }


            /*
             * GET CURRENT STATE
             */

            String state =
                    userState.get(chatId);


            /*
             * =================================================
             * MAIN MENU ACTIONS
             * =================================================
             */

            if (text.equals("➕ ADD INFORMATION")) {

                showAddInformationMenu(chatId);

                return;
            }


            if (text.equals("📖 VIEW INFORMATION")) {

                showViewInformation(chatId);

                return;
            }


            if (text.equals("🔍 SEARCH INFORMATION")) {

                userState.put(
                        chatId,
                        "SEARCH"
                );

                sendMessage(
                        chatId,
                        "🔍 Enter the text you want to search:"
                );

                return;
            }


            if (text.equals("📄 GENERATE PDF")) {

                showPdfCategories(chatId);

                return;
            }


            if (text.equals("⚙️ MANAGE CATEGORIES")) {

                showManageCategories(chatId);

                return;
            }


            if (text.equals("📂 CREATE CATEGORY")) {

                userState.put(
                        chatId,
                        "CREATE_CATEGORY"
                );

                sendMessage(
                        chatId,
                        "📂 Enter the new category name:"
                );

                return;
            }


            /*
             * =================================================
             * STATE: CREATE CATEGORY
             * =================================================
             */

            if ("CREATE_CATEGORY".equals(state)) {

                createCategory(
                        chatId,
                        text
                );

                return;
            }


            /*
             * =================================================
             * STATE: SELECT CATEGORY
             * =================================================
             */

            if ("SELECT_CATEGORY".equals(state)) {

                handleCategorySelection(
                        chatId,
                        text
                );

                return;
            }


            /*
             * =================================================
             * STATE: ADD INFORMATION
             * =================================================
             */

            if ("ADD_INFORMATION".equals(state)) {

                addInformation(
                        chatId,
                        text
                );

                return;
            }


            /*
             * =================================================
             * STATE: VIEW CATEGORY
             * =================================================
             */

            if ("VIEW_CATEGORY".equals(state)) {

                viewInformation(
                        chatId,
                        text
                );

                return;
            }


            /*
             * =================================================
             * STATE: SEARCH
             * =================================================
             */

            if ("SEARCH".equals(state)) {

                searchInformation(
                        chatId,
                        text
                );

                return;
            }


            /*
             * =================================================
             * STATE: MANAGE CATEGORY
             * =================================================
             */

            if ("MANAGE_CATEGORY".equals(state)) {

                handleManageCategorySelection(
                        chatId,
                        text
                );

                return;
            }


            /*
             * =================================================
             * STATE: MANAGE ACTION
             * =================================================
             */

            if ("MANAGE_ACTION".equals(state)) {

                handleManageSelectedAction(
                        chatId,
                        text
                );

                return;
            }


            /*
             * =================================================
             * STATE: RENAME
             * =================================================
             */

            if ("RENAME_CATEGORY".equals(state)) {

                renameCategory(
                        chatId,
                        text
                );

                return;
            }


            /*
             * =================================================
             * STATE: DELETE
             * =================================================
             */

            if ("DELETE_CATEGORY".equals(state)) {

                deleteCategory(
                        chatId,
                        text
                );

                return;
            }


            /*
             * =================================================
             * STATE: PDF CATEGORY
             * =================================================
             */

            if ("PDF_CATEGORY".equals(state)) {

                selectPdfCategory(
                        chatId,
                        text
                );

                return;
            }


            /*
             * =================================================
             * STATE: PDF PERIOD
             * =================================================
             */

            if ("PDF_PERIOD".equals(state)) {

                generatePdfByPeriod(
                        chatId,
                        text
                );

                return;
            }


            /*
             * UNKNOWN INPUT
             */

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

    private void sendMainMenu(long chatId) {

        String keyboard =
                """
                {
                  "keyboard": [
                    ["➕ ADD INFORMATION"],
                    ["📖 VIEW INFORMATION"],
                    ["🔍 SEARCH INFORMATION"],
                    ["📄 GENERATE PDF"],
                    ["⚙️ MANAGE CATEGORIES"]
                  ],
                  "resize_keyboard": true,
                  "one_time_keyboard": false
                }
                """;

        sendKeyboardMessage(
                chatId,
                "Welcome to MyJourney 🚀\n\n"
                        + "Your personal journey information system.\n\n"
                        + "Choose an option:",
                keyboard
        );
    }


    // =========================================================
    // ADD INFORMATION MENU
    // =========================================================

    private void showAddInformationMenu(
            long chatId
    ) throws Exception {

        List<String> categories =
                categoryService.getCategories(chatId);

        if (categories == null ||
                categories.isEmpty()) {

            userState.put(
                    chatId,
                    "CREATE_CATEGORY"
            );

            sendMessage(
                    chatId,
                    "You don't have any categories yet.\n\n"
                            + "📂 Enter your first category name:"
            );

            return;
        }


        String keyboard =
                buildCategoryKeyboard(
                        categories,
                        true,
                        true
                );


        userState.put(
                chatId,
                "SELECT_CATEGORY"
        );


        sendKeyboardMessage(
                chatId,
                "📂 Select a category:",
                keyboard
        );
    }


    // =========================================================
    // CATEGORY SELECTION FOR ADD
    // =========================================================

    private void handleCategorySelection(
            long chatId,
            String text
    ) throws Exception {

        /*
         * CREATE CATEGORY from this screen.
         */

        if (text.equals("📂 CREATE CATEGORY")) {

            userState.put(
                    chatId,
                    "CREATE_CATEGORY"
            );

            sendMessage(
                    chatId,
                    "📂 Enter the new category name:"
            );

            return;
        }


        /*
         * MANAGE CATEGORIES from this screen.
         */

        if (text.equals("⚙️ MANAGE CATEGORIES")) {

            showManageCategories(chatId);

            return;
        }


        /*
         * Validate actual category.
         */

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
                "✅ Category selected:\n\n"
                        + "📂 "
                        + text
                        + "\n\n"
                        + "Now enter the information you want to save:"
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


        /*
         * Prevent category names from becoming
         * Telegram menu commands.
         */

        if (isReservedButton(categoryName)) {

            sendMessage(
                    chatId,
                    "Please choose a different category name."
            );

            return;
        }


        List<String> existing =
                categoryService.getCategories(chatId);


        if (existing != null) {

            for (String category : existing) {

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
        }


        categoryService.saveCategory(
                chatId,
                categoryName
        );


        /*
         * Immediately select the newly created category
         * so the user can enter information.
         */

        selectedCategory.put(
                chatId,
                categoryName
        );


        userState.put(
                chatId,
                "ADD_INFORMATION"
        );


        sendMessage(
                chatId,
                "✅ Category created successfully!\n\n"
                        + "📂 "
                        + categoryName
                        + "\n\n"
                        + "Now enter the information you want to save:"
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
                selectedCategory.get(chatId);


        if (category == null ||
                category.isBlank()) {

            showAddInformationMenu(chatId);

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
                "✅ Information stored successfully."
        );


        /*
         * Stay in a useful state:
         * ask whether the user wants to add another item.
         */

        userState.put(
                chatId,
                "ADD_MORE"
        );


        String keyboard =
                """
                {
                  "keyboard": [
                    ["➕ ADD MORE"],
                    ["📖 VIEW INFORMATION"],
                    ["⬅️ BACK"]
                  ],
                  "resize_keyboard": true,
                  "one_time_keyboard": false
                }
                """;


        sendKeyboardMessage(
                chatId,
                "What would you like to do next?",
                keyboard
        );
    }


    // =========================================================
    // VIEW INFORMATION
    // =========================================================

    private void showViewInformation(
            long chatId
    ) throws Exception {

        List<String> categories =
                categoryService.getCategories(chatId);


        if (categories == null ||
                categories.isEmpty()) {

            sendMessage(
                    chatId,
                    "You don't have any categories yet."
            );

            return;
        }


        String keyboard =
                buildCategoryKeyboard(
                        categories,
                        false,
                        false
                );


        userState.put(
                chatId,
                "VIEW_CATEGORY"
        );


        sendKeyboardMessage(
                chatId,
                "📖 Select a category to view:",
                keyboard
        );
    }


    private void viewInformation(
            long chatId,
            String category
    ) throws Exception {

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


        if (data == null ||
                data.isEmpty()) {

            sendMessage(
                    chatId,
                    "No information found in:\n\n"
                            + "📂 "
                            + category
            );

            clearState(chatId);
            sendMainMenu(chatId);

            return;
        }


        StringBuilder result =
                new StringBuilder();


        int count = 1;


        for (Map<String, Object> item : data) {

            result.append(
                    count
                            + ". "
                            + String.valueOf(
                            item.get("text")
                    )
                            + "\n"
            );

            result.append(
                    "📅 "
                            + String.valueOf(
                            item.get("createdAt")
                    )
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
        sendMainMenu(chatId);
    }


    // =========================================================
    // SEARCH
    // =========================================================

    private void searchInformation(
            long chatId,
            String searchText
    ) throws Exception {

        searchText =
                searchText.trim()
                        .toLowerCase();


        if (searchText.isBlank()) {

            sendMessage(
                    chatId,
                    "Please enter something to search."
            );

            return;
        }


        List<Map<String, Object>> data =
                informationService.getAllInformation(
                        chatId
                );


        StringBuilder result =
                new StringBuilder();


        int count = 0;


        if (data != null) {

            for (Map<String, Object> item : data) {

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
                                    + String.valueOf(
                                    item.get("category")
                            )
                                    + "\n"
                    );

                    result.append(
                            "📅 "
                                    + String.valueOf(
                                    item.get("createdAt")
                            )
                                    + "\n\n"
                    );
                }
            }
        }


        if (count == 0) {

            sendMessage(
                    chatId,
                    "🔍 No matching information found."
            );

        } else {

            sendMessage(
                    chatId,
                    "🔍 Search Results\n\n"
                            + result
            );
        }


        clearState(chatId);
        sendMainMenu(chatId);
    }


    // =========================================================
    // MANAGE CATEGORIES
    // =========================================================

    private void showManageCategories(
            long chatId
    ) throws Exception {

        List<String> categories =
                categoryService.getCategories(chatId);


        if (categories == null ||
                categories.isEmpty()) {

            sendMessage(
                    chatId,
                    "You don't have any categories to manage."
            );

            return;
        }


        String keyboard =
                buildCategoryKeyboard(
                        categories,
                        false,
                        false
                );


        userState.put(
                chatId,
                "MANAGE_CATEGORY"
        );


        sendKeyboardMessage(
                chatId,
                "⚙️ Select a category to manage:",
                keyboard
        );
    }


    // =========================================================
    // MANAGE CATEGORY SELECTION
    // =========================================================

    private void handleManageCategorySelection(
            long chatId,
            String text
    ) throws Exception {

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
                "MANAGE_ACTION"
        );


        String keyboard =
                """
                {
                  "keyboard": [
                    ["✏️ RENAME"],
                    ["🗑️ DELETE"],
                    ["⬅️ BACK"]
                  ],
                  "resize_keyboard": true,
                  "one_time_keyboard": false
                }
                """;


        sendKeyboardMessage(
                chatId,
                "⚙️ Manage category:\n\n"
                        + "📂 "
                        + text
                        + "\n\n"
                        + "Choose an action:",
                keyboard
        );
    }


    // =========================================================
    // MANAGE ACTION
    // =========================================================

    private void handleManageSelectedAction(
            long chatId,
            String text
    ) {

        String category =
                manageCategory.get(chatId);


        if (category == null) {

            showManageCategoriesSafely(chatId);

            return;
        }


        if (text.equals("✏️ RENAME")) {

            userState.put(
                    chatId,
                    "RENAME_CATEGORY"
            );

            sendMessage(
                    chatId,
                    "✏️ Enter the new category name for:\n\n"
                            + "📂 "
                            + category
            );

            return;
        }


        if (text.equals("🗑️ DELETE")) {

            userState.put(
                    chatId,
                    "DELETE_CATEGORY"
            );

            sendMessage(
                    chatId,
                    "⚠️ To confirm deletion, type the exact category name:\n\n"
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


    // =========================================================
    // RENAME CATEGORY
    // =========================================================

    private void renameCategory(
            long chatId,
            String newName
    ) throws Exception {

        String oldName =
                manageCategory.get(chatId);


        if (oldName == null) {

            showManageCategories(chatId);

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


        if (isReservedButton(newName)) {

            sendMessage(
                    chatId,
                    "Please choose a different category name."
            );

            return;
        }


        List<String> existing =
                categoryService.getCategories(chatId);


        if (existing != null) {

            for (String category : existing) {

                if (!category.equalsIgnoreCase(oldName)
                        && category.equalsIgnoreCase(newName)) {

                    sendMessage(
                            chatId,
                            "This category already exists."
                    );

                    return;
                }
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
                "✅ Category renamed successfully!\n\n"
                        + oldName
                        + " → "
                        + newName
        );


        sendMainMenu(chatId);
    }


    // =========================================================
    // DELETE CATEGORY
    // =========================================================

    private void deleteCategory(
            long chatId,
            String categoryName
    ) throws Exception {

        String selected =
                manageCategory.get(chatId);


        if (selected == null) {

            showManageCategories(chatId);

            return;
        }


        /*
         * Exact match required.
         */

        if (!categoryName.equals(selected)) {

            sendMessage(
                    chatId,
                    "❌ Category name does not match.\n\n"
                            + "Please type exactly:\n"
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
                "🗑️ Category deleted successfully:\n\n"
                        + selected
        );


        sendMainMenu(chatId);
    }


    // =========================================================
    // PDF CATEGORY
    // =========================================================

    private void showPdfCategories(
            long chatId
    ) throws Exception {

        List<String> categories =
                categoryService.getCategories(chatId);


        if (categories == null ||
                categories.isEmpty()) {

            sendMessage(
                    chatId,
                    "No categories available for PDF generation."
            );

            return;
        }


        String keyboard =
                buildPdfCategoryKeyboard(
                        categories
                );


        userState.put(
                chatId,
                "PDF_CATEGORY"
        );


        sendKeyboardMessage(
                chatId,
                "📄 Select category for PDF:",
                keyboard
        );
    }


    // =========================================================
    // SELECT PDF CATEGORY
    // =========================================================

    private void selectPdfCategory(
            long chatId,
            String category
    ) throws Exception {

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
                  "resize_keyboard": true,
                  "one_time_keyboard": false
                }
                """;


        sendKeyboardMessage(
                chatId,
                "📄 Select time period:",
                keyboard
        );
    }


    // =========================================================
    // GENERATE PDF
    // =========================================================

    private void generatePdfByPeriod(
            long chatId,
            String period
    ) throws Exception {

        String category =
                pdfCategory.get(chatId);


        if (category == null) {

            showPdfCategories(chatId);

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
            sendMainMenu(chatId);

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
    // FILTER PDF
    // =========================================================

    private List<Map<String, Object>>
    filterByCategoryAndPeriod(
            List<Map<String, Object>> data,
            String category,
            String period
    ) {

        List<Map<String, Object>> result =
                new ArrayList<>();


        if (data == null) {
            return result;
        }


        LocalDate today =
                LocalDate.now();


        for (Map<String, Object> item : data) {

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
                        Instant.parse(createdAt);


                LocalDate date =
                        instant.atZone(
                                        ZoneId.systemDefault()
                                )
                                .toLocalDate();


                boolean include;


                switch (period) {

                    case "TODAY" ->
                            include =
                                    date.equals(today);

                    case "THIS WEEK" ->
                            include =
                                    !date.isBefore(
                                            today.minusDays(6)
                                    )
                                            &&
                                            !date.isAfter(today);

                    case "THIS MONTH" ->
                            include =
                                    date.getYear()
                                            == today.getYear()
                                            &&
                                            date.getMonth()
                                                    == today.getMonth();

                    case "ALL" ->
                            include = true;

                    default ->
                            include = false;
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

        if (text == null ||
                text.isBlank()) {
            return false;
        }


        List<String> categories =
                categoryService.getCategories(chatId);


        if (categories == null) {
            return false;
        }


        for (String category : categories) {

            if (category.equals(text)) {
                return true;
            }
        }


        return false;
    }


    // =========================================================
    // BUILD CATEGORY KEYBOARD
    // =========================================================

    private String buildCategoryKeyboard(
            List<String> categories,
            boolean includeCreate,
            boolean includeManage
    ) {

        StringBuilder keyboard =
                new StringBuilder();

        keyboard.append(
                "{\"keyboard\":["
        );


        boolean first = true;


        for (String category : categories) {

            if (category == null ||
                    category.isBlank()) {
                continue;
            }


            if (!first) {
                keyboard.append(",");
            }


            keyboard.append(
                    "[\""
                            + escapeJson(category)
                            + "\"]"
            );


            first = false;
        }


        if (includeCreate) {

            if (!first) {
                keyboard.append(",");
            }

            keyboard.append(
                    "[\"📂 CREATE CATEGORY\"]"
            );

            first = false;
        }


        if (includeManage) {

            if (!first) {
                keyboard.append(",");
            }

            keyboard.append(
                    "[\"⚙️ MANAGE CATEGORIES\"]"
            );
        }


        keyboard.append(
                ",[\"⬅️ BACK\"]"
        );


        keyboard.append(
                "],\"resize_keyboard\":true,"
                        + "\"one_time_keyboard\":false}"
        );


        return keyboard.toString();
    }


    // =========================================================
    // BUILD PDF CATEGORY KEYBOARD
    // =========================================================

    private String buildPdfCategoryKeyboard(
            List<String> categories
    ) {

        StringBuilder keyboard =
                new StringBuilder();

        keyboard.append(
                "{\"keyboard\":["
        );


        for (String category : categories) {

            if (category == null ||
                    category.isBlank()) {
                continue;
            }

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
                "],\"resize_keyboard\":true,"
                        + "\"one_time_keyboard\":false}"
        );


        return keyboard.toString();
    }


    // =========================================================
    // RESERVED BUTTON CHECK
    // =========================================================

    private boolean isReservedButton(
            String text
    ) {

        return text.equals("➕ ADD INFORMATION")
                || text.equals("📖 VIEW INFORMATION")
                || text.equals("🔍 SEARCH INFORMATION")
                || text.equals("📄 GENERATE PDF")
                || text.equals("⚙️ MANAGE CATEGORIES")
                || text.equals("📂 CREATE CATEGORY")
                || text.equals("⬅️ BACK")
                || text.equals("❌ CANCEL")
                || text.equals("✏️ RENAME")
                || text.equals("🗑️ DELETE")
                || text.equals("📚 ALL")
                || text.equals("TODAY")
                || text.equals("THIS WEEK")
                || text.equals("THIS MONTH")
                || text.equals("ALL");
    }


    // =========================================================
    // SHOW MANAGE CATEGORIES SAFELY
    // =========================================================

    private void showManageCategoriesSafely(
            long chatId
    ) {

        try {

            showManageCategories(chatId);

        } catch (Exception e) {

            System.out.println(
                    "Manage categories error: "
                            + e.getMessage()
            );

            sendMessage(
                    chatId,
                    "Unable to load categories."
            );
        }
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

            LinkedMultiValueMap<String, Object>
                    body =
                    new LinkedMultiValueMap<>();


            body.add(
                    "chat_id",
                    String.valueOf(chatId)
            );


            body.add(
                    "document",
                    new ByteArrayResource(pdf) {

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
    // CLEAR STATE
    // =========================================================

    private void clearState(
            long chatId
    ) {

        userState.remove(chatId);

        selectedCategory.remove(chatId);

        pdfCategory.remove(chatId);

        manageCategory.remove(chatId);
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

