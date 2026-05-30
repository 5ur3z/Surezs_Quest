package org.surez.surezs_quest.web;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;

public class QuestApiHandler implements HttpHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_BODY = 100_000;
    private static final Set<String> STRIP_FIELDS = Set.of("objectives", "rewards", "dialogue");

    private final Path questDir;

    public QuestApiHandler(Path questDir) {
        this.questDir = questDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();
        String id = path.length() > "/api/quests/".length()
            ? path.substring("/api/quests/".length()) : null;

        try {
            if ("GET".equals(method)) {
                if (id == null || id.isEmpty()) listQuests(exchange);
                else getQuest(exchange, id);
            } else if ("PUT".equals(method) && id != null && !id.isEmpty()) {
                saveQuest(exchange, id);
            } else {
                send(exchange, 405, "{\"error\":\"Method not allowed\"}");
            }
        } catch (Exception e) {
            LOGGER.error("Quest API error: {}", e.getMessage());
            send(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void listQuests(HttpExchange exchange) throws IOException {
        var items = new JsonArray();
        try (var stream = Files.list(questDir)) {
            for (Path f : stream.sorted().toList()) {
                String name = f.getFileName().toString();
                if (!name.endsWith(".json")) continue;
                try {
                    String raw = Files.readString(f);
                    JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
                    for (String key : STRIP_FIELDS) obj.remove(key);
                    items.add(obj);
                } catch (Exception ignored) {
                    // skip broken files
                }
            }
        }
        send(exchange, 200, GSON.toJson(items));
    }

    private void getQuest(HttpExchange exchange, String id) throws IOException {
        if (!validId(id)) {
            send(exchange, 400, "{\"error\":\"Invalid quest ID\"}");
            return;
        }
        Path file = resolveQuestFile(id);
        if (file == null || !Files.exists(file)) {
            send(exchange, 404, "{\"error\":\"Not found\"}");
            return;
        }
        String content = Files.readString(file);
        JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
        send(exchange, 200, GSON.toJson(obj));
    }

    private void saveQuest(HttpExchange exchange, String id) throws IOException {
        if (!validId(id)) {
            send(exchange, 400, "{\"error\":\"Invalid quest ID\"}");
            return;
        }
        byte[] raw = exchange.getRequestBody().readAllBytes();
        if (raw.length > MAX_BODY) {
            send(exchange, 413, "{\"error\":\"Payload too large\"}");
            return;
        }
        String body = new String(raw);
        try {
            JsonObject obj = JsonParser.parseString(body).getAsJsonObject();
            if (!obj.has("id")) obj.addProperty("id", id);

            // Save to existing file or create new with namespace-stripped name
            Path file = resolveQuestFile(id);
            if (file == null) {
                int colon = id.lastIndexOf(':');
                String name = colon > 0 ? id.substring(colon + 1) : id;
                file = questDir.resolve(name + ".json");
            }
            Files.createDirectories(questDir);
            Files.writeString(file, GSON.toJson(obj));
            send(exchange, 200, "{\"ok\":true}");
        } catch (JsonParseException e) {
            send(exchange, 400, "{\"error\":\"Invalid JSON\"}");
        }
    }

    private boolean validId(String id) {
        return id != null && !id.isEmpty()
            && !id.contains("/") && !id.contains("\\") && !id.contains("..");
    }

    /** Resolve a quest ID to its file, trying full ID first then namespace-stripped */
    private Path resolveQuestFile(String id) {
        Path file = questDir.resolve(id + ".json");
        if (Files.exists(file)) return file;
        // strip namespace: "surezs_quest:collect_iron" → "collect_iron"
        int colon = id.lastIndexOf(':');
        if (colon > 0) {
            file = questDir.resolve(id.substring(colon + 1) + ".json");
            if (Files.exists(file)) return file;
        }
        return null;
    }

    private void send(HttpExchange exchange, int code, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(code, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
