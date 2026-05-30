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
    private final Path npcDir;

    public NpcApiHandler(Path npcDir) {
        this.npcDir = npcDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            String body = "{\"error\":\"Method not allowed\"}";
            exchange.sendResponseHeaders(405, body.length());
            try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            return;
        }
        var items = new JsonArray();
        try (var stream = Files.list(npcDir)) {
            for (Path f : stream.sorted().toList()) {
                String name = f.getFileName().toString();
                if (!name.endsWith(".json")) continue;
                try {
                    String raw = Files.readString(f);
                    JsonObject obj = JsonParser.parseString(raw).getAsJsonObject();
                    items.add(obj);
                } catch (Exception ignored) {
                    // skip broken files
                }
            }
        }
        String body = GSON.toJson(items);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var os = exchange.getResponseBody()) { os.write(bytes); }
    }
}
