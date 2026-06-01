package org.surez.surezs_quest.web;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class QuestLineApiHandler implements HttpHandler {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_BODY = 100_000;
    private static final int MAX_NAME_LENGTH = 64;

    private final Path configDir;
    private final Object lock = new Object();

    public QuestLineApiHandler(Path configDir) {
        this.configDir = configDir;
    }

    private Path questLineFile() {
        return configDir.resolve("questlines.json");
    }

    private Map<String, List<String>> loadAll() {
        Path file = questLineFile();
        if (!Files.exists(file)) {
            // auto-create default line with all quests
            Map<String, List<String>> result = new LinkedHashMap<>();
            List<String> allQuestIds = scanQuestIds();
            result.put("default", allQuestIds);
            saveAll(result);
            return result;
        }
        try {
            String raw = Files.readString(file);
            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
            Map<String, List<String>> result = new LinkedHashMap<>();
            for (var entry : obj.entrySet()) {
                List<String> quests = new ArrayList<>();
                for (JsonElement e : entry.getValue().getAsJsonArray()) {
                    quests.add(e.getAsString());
                }
                result.put(entry.getKey(), quests);
            }
            // ensure default always exists
            if (!result.containsKey("default")) {
                result.put("default", scanQuestIds());
            }
            return result;
        } catch (Exception e) {
            LOGGER.error("Failed to load questlines: {}", e.getMessage());
            Map<String, List<String>> fallback = new LinkedHashMap<>();
            fallback.put("default", scanQuestIds());
            return fallback;
        }
    }

    private List<String> scanQuestIds() {
        Path questDir = configDir.resolve("quests");
        List<String> ids = new ArrayList<>();
        if (Files.exists(questDir)) {
            try (var stream = Files.list(questDir)) {
                stream.filter(f -> f.getFileName().toString().endsWith(".json"))
                    .sorted().forEach(f -> {
                        try {
                            String raw = Files.readString(f);
                            JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
                            if (obj.has("id")) {
                                ids.add(obj.get("id").getAsString());
                            }
                        } catch (Exception e) {
                            LOGGER.warn("Failed to read quest id from {}: {}", f.getFileName(), e.getMessage());
                        }
                    });
            } catch (IOException e) {
                LOGGER.warn("Failed to scan quest dir: {}", e.getMessage());
            }
        }
        return ids;
    }

    private void saveAll(Map<String, List<String>> lines) {
        JsonObject obj = new JsonObject();
        for (var entry : lines.entrySet()) {
            JsonArray arr = new JsonArray();
            for (String q : entry.getValue()) arr.add(q);
            obj.add(entry.getKey(), arr);
        }
        try {
            Files.createDirectories(configDir);
            Files.writeString(questLineFile(), GSON.toJson(obj));
        } catch (IOException e) {
            LOGGER.error("Failed to save questlines: {}", e.getMessage());
            throw new RuntimeException("Failed to save questlines", e);
        }
    }

    private static boolean validName(String name) {
        return name != null && !name.isEmpty()
            && name.length() <= MAX_NAME_LENGTH
            && !name.contains("/") && !name.contains("\\") && !name.contains("..");
    }

    private static String jsonError(String message) {
        return GSON.toJson(Map.of("error", message));
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method) && "/api/questlines".equals(path)) {
                listAll(exchange);
                return;
            }

            if ("POST".equals(method) && "/api/questlines".equals(path)) {
                create(exchange);
                return;
            }

            // /api/questlines/{name} and /api/questlines/{name}/quests
            if (path.startsWith("/api/questlines/")) {
                String sub = path.substring("/api/questlines/".length());
                if (sub.isEmpty() || sub.equals("/")) {
                    send(exchange, 405, jsonError("Method not allowed"));
                    return;
                }

                if (sub.endsWith("/quests")) {
                    String name = sub.substring(0, sub.length() - "/quests".length());
                    if (!validName(name)) { send(exchange, 400, jsonError("Invalid name")); return; }
                    if ("PUT".equals(method)) {
                        updateQuests(exchange, name);
                        return;
                    }
                } else {
                    String name = sub;
                    if (!validName(name)) { send(exchange, 400, jsonError("Invalid name")); return; }
                    if ("PUT".equals(method)) {
                        rename(exchange, name);
                        return;
                    }
                    if ("DELETE".equals(method)) {
                        delete(exchange, name);
                        return;
                    }
                }
            }

            send(exchange, 405, jsonError("Method not allowed"));
        } catch (Exception e) {
            LOGGER.error("QuestLine API error: {}", e.getMessage());
            send(exchange, 500, jsonError(e.getMessage()));
        }
    }

    private void listAll(HttpExchange exchange) throws IOException {
        Map<String, List<String>> lines;
        synchronized (lock) { lines = loadAll(); }
        JsonArray arr = new JsonArray();
        for (var entry : lines.entrySet()) {
            JsonObject obj = new JsonObject();
            obj.addProperty("name", entry.getKey());
            JsonArray quests = new JsonArray();
            for (String q : entry.getValue()) quests.add(q);
            obj.add("quests", quests);
            arr.add(obj);
        }
        send(exchange, 200, GSON.toJson(arr));
    }

    private void create(HttpExchange exchange) throws IOException {
        byte[] raw = exchange.getRequestBody().readAllBytes();
        if (raw.length > MAX_BODY) { send(exchange, 413, jsonError("Payload too large")); return; }
        JsonObject obj = JsonParser.parseString(new String(raw)).getAsJsonObject();
        String name = obj.get("name").getAsString();
        if (!validName(name)) { send(exchange, 400, jsonError("Invalid or missing name")); return; }

        List<String> quests = new ArrayList<>();
        if (obj.has("quests")) {
            for (JsonElement e : obj.getAsJsonArray("quests")) quests.add(e.getAsString());
        }

        synchronized (lock) {
            Map<String, List<String>> lines = loadAll();
            if (lines.containsKey(name)) { send(exchange, 409, jsonError("Already exists")); return; }
            lines.put(name, quests);
            saveAll(lines);
        }
        send(exchange, 200, "{\"ok\":true}");
    }

    private void rename(HttpExchange exchange, String oldName) throws IOException {
        byte[] raw = exchange.getRequestBody().readAllBytes();
        if (raw.length > MAX_BODY) { send(exchange, 413, jsonError("Payload too large")); return; }
        JsonObject obj = JsonParser.parseString(new String(raw)).getAsJsonObject();
        String newName = obj.get("name").getAsString();
        if (!validName(newName)) { send(exchange, 400, jsonError("Invalid or missing name")); return; }

        synchronized (lock) {
            Map<String, List<String>> lines = loadAll();
            if (!lines.containsKey(oldName)) { send(exchange, 404, jsonError("Not found")); return; }
            if (lines.containsKey(newName)) { send(exchange, 409, jsonError("Name already exists")); return; }
            lines.put(newName, lines.remove(oldName));
            saveAll(lines);
        }
        send(exchange, 200, "{\"ok\":true}");
    }

    private void delete(HttpExchange exchange, String name) throws IOException {
        synchronized (lock) {
            Map<String, List<String>> lines = loadAll();
            if (!lines.containsKey(name)) { send(exchange, 404, jsonError("Not found")); return; }
            lines.remove(name);
            saveAll(lines);
        }
        send(exchange, 200, "{\"ok\":true}");
    }

    private void updateQuests(HttpExchange exchange, String name) throws IOException {
        byte[] raw = exchange.getRequestBody().readAllBytes();
        if (raw.length > MAX_BODY) { send(exchange, 413, jsonError("Payload too large")); return; }
        JsonObject obj = JsonParser.parseString(new String(raw)).getAsJsonObject();

        List<String> quests = new ArrayList<>();
        if (obj.has("quests")) {
            for (JsonElement e : obj.getAsJsonArray("quests")) quests.add(e.getAsString());
        }

        synchronized (lock) {
            Map<String, List<String>> lines = loadAll();
            if (!lines.containsKey(name)) {
                // auto-create the line if it doesn't exist (e.g. frontend's default line)
                lines.put(name, quests);
                saveAll(lines);
                send(exchange, 200, "{\"ok\":true}");
                return;
            }
            lines.put(name, quests);
            saveAll(lines);
        }
        send(exchange, 200, "{\"ok\":true}");
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
