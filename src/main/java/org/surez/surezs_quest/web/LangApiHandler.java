package org.surez.surezs_quest.web;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.surez.surezs_quest.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LangApiHandler implements HttpHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            String body = "{\"error\":\"Method not allowed\"}";
            exchange.sendResponseHeaders(405, body.length());
            try (var os = exchange.getResponseBody()) { os.write(body.getBytes(StandardCharsets.UTF_8)); }
            return;
        }

        String locale = Config.INSTANCE.language();
        String content = loadLangFile(locale);
        if (content == null) {
            content = loadLangFile("en_us");
        }
        if (content == null) {
            content = "{}";
        }

        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("lang", locale);
        wrapper.add("strings", JsonParser.parseString(content));

        String body = GSON.toJson(wrapper);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        try (var os = exchange.getResponseBody()) { os.write(bytes); }
    }

    private String loadLangFile(String locale) {
        String path = "/web/lang/" + locale + ".json";
        try (InputStream in = getClass().getResourceAsStream(path)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
