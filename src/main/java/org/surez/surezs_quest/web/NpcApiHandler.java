package org.surez.surezs_quest.web;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public class NpcApiHandler implements HttpHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int MAX_BODY = 100_000;
    private final Path npcDir;

    public NpcApiHandler(Path npcDir) {
        this.npcDir = npcDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equals(method)) {
            handleGet(exchange);
        } else if ("PUT".equals(method)) {
            handlePut(exchange);
        } else {
            String body = "{\"error\":\"Method not allowed\"}";
            exchange.sendResponseHeaders(405, body.length());
            try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
        }
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        var items = new JsonArray();
        try (var stream = Files.list(npcDir)) {
            for (Path f : stream.sorted().toList()) {
                String name = f.getFileName().toString();
                if (!name.endsWith(".json")) continue;
                try {
                    String raw = Files.readString(f);
                    JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
                    items.add(obj);
                } catch (Exception ignored) {}
            }
        }
        String body = GSON.toJson(items);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private void handlePut(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String prefix = "/api/npcs/";
        if (path.length() <= prefix.length() || !path.startsWith(prefix)) {
            String body = "{\"error\":\"NPC id required in URL, e.g. /api/npcs/my_npc\"}";
            exchange.sendResponseHeaders(400, body.length());
            try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            return;
        }
        String npcId = java.net.URLDecoder.decode(path.substring(prefix.length()), StandardCharsets.UTF_8);
        if (npcId.isEmpty()) {
            String body = "{\"error\":\"NPC id required in URL\"}";
            exchange.sendResponseHeaders(400, body.length());
            try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            return;
        }

        byte[] raw = exchange.getRequestBody().readNBytes(MAX_BODY);
        String json = new String(raw, StandardCharsets.UTF_8);

        try {
            JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            String body = "{\"error\":\"Invalid JSON: " + e.getMessage() + "\"}";
            exchange.sendResponseHeaders(400, body.length());
            try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            return;
        }

        String fileName = npcId;
        if (npcId.contains(":")) fileName = npcId.substring(npcId.lastIndexOf(':') + 1);
        if (!fileName.matches("[a-zA-Z0-9_-]+")) {
            String body = "{\"error\":\"Invalid NPC id\"}";
            exchange.sendResponseHeaders(400, body.length());
            try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            return;
        }

        Path file = npcDir.resolve(fileName + ".json").normalize();
        if (!file.startsWith(npcDir.normalize())) {
            String body = "{\"error\":\"Path traversal blocked\"}";
            exchange.sendResponseHeaders(403, body.length());
            try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            return;
        }

        Files.createDirectories(npcDir);

        // id immutability check: reject changes to existing NPC id
        JsonObject savedObj = JsonParser.parseString(json).getAsJsonObject();
        String newNpcId = savedObj.get("id").getAsString();
        if (newNpcId.isEmpty()) {
            String body = "{\"error\":\"NPC id cannot be empty\"}";
            exchange.sendResponseHeaders(400, body.length());
            try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            return;
        }
        if (Files.exists(file)) {
            try {
                String existingJson = Files.readString(file);
                JsonObject existingObj = JsonParser.parseString(existingJson).getAsJsonObject();
                String existingId = existingObj.get("id").getAsString();
                if (!existingId.equals(newNpcId)) {
                    String body = "{\"error\":\"NPC id is immutable once created\"}";
                    exchange.sendResponseHeaders(400, body.length());
                    try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
                    return;
                }
            } catch (Exception e) {
                // file exists but unreadable — treat as error
                String body = "{\"error\":\"Failed to read existing NPC file\"}";
                exchange.sendResponseHeaders(500, body.length());
                try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
                return;
            }
        }

        Files.writeString(file, GSON.toJson(savedObj));
        String body = "{\"ok\":true}";
        exchange.sendResponseHeaders(200, body.length());
        try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
    }
}
