package org.surez.surezs_quest.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public class StaticFileHandler implements HttpHandler {
    private static final String WEB_ROOT = "/web/";
    private static final Map<String, String> MIME = Map.of(
        ".html", "text/html; charset=utf-8",
        ".css", "text/css; charset=utf-8",
        ".js", "application/javascript; charset=utf-8",
        ".json", "application/json"
    );

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path)) path = "/index.html";

        String resourcePath = WEB_ROOT + path.substring(1);
        InputStream in = getClass().getResourceAsStream(resourcePath);
        if (in == null) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        String ext = path.substring(path.lastIndexOf('.'));
        String mime = MIME.getOrDefault(ext, "application/octet-stream");
        exchange.getResponseHeaders().set("Content-Type", mime);

        byte[] bytes = in.readAllBytes();
        in.close();
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
