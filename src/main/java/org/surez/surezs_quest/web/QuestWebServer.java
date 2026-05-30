package org.surez.surezs_quest.web;

import com.mojang.logging.LogUtils;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;

public class QuestWebServer {
    private static final Logger LOGGER = LogUtils.getLogger();
    private HttpServer server;

    public void start(int port, Path questDir, Path npcDir) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/", new StaticFileHandler());
        var questHandler = new QuestApiHandler(questDir);
        server.createContext("/api/quests", questHandler);
        server.createContext("/api/quests/", questHandler);
        server.createContext("/api/npcs", new NpcApiHandler(npcDir));
        server.setExecutor(null);
        server.start();
        LOGGER.info("Web editor started on http://localhost:{}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            LOGGER.info("Web editor stopped");
        }
    }

    public boolean isRunning() {
        return server != null;
    }
}
