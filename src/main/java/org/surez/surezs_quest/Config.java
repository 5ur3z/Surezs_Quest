package org.surez.surezs_quest;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static Config INSTANCE;

    private Path configFile;

    // -- config values ---------------------------------------------------------

    private int autoSaveIntervalSeconds = 30;
    private int locationCheckIntervalTicks = 20;
    private int webEditorPort = 0;  // 0 = disabled
    private String language = "en_us";

    public int autoSaveIntervalSeconds() { return autoSaveIntervalSeconds; }
    public int locationCheckIntervalTicks() { return locationCheckIntervalTicks; }
    public int webEditorPort() { return webEditorPort; }
    public String language() { return language != null && !language.isEmpty() ? language : "en_us"; }

    // -- load / save -----------------------------------------------------------

    public static Config load(Path configDir) {
        Config config = new Config();
        config.configFile = configDir.resolve("config.json");

        if (Files.exists(config.configFile)) {
            try {
                String json = Files.readString(config.configFile);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                if (root.has("auto_save_interval_seconds"))
                    config.autoSaveIntervalSeconds = root.get("auto_save_interval_seconds").getAsInt();
                if (root.has("location_check_interval_ticks"))
                    config.locationCheckIntervalTicks = root.get("location_check_interval_ticks").getAsInt();
                if (root.has("web_editor_port"))
                    config.webEditorPort = root.get("web_editor_port").getAsInt();
                if (root.has("language"))
                    config.language = root.get("language").getAsString();
            } catch (IOException e) {
                LOGGER.warn("Failed to load config, using defaults: {}", e.getMessage());
            }
        }

        config.save();
        INSTANCE = config;
        return config;
    }

    public void save() {
        JsonObject root = new JsonObject();
        root.addProperty("auto_save_interval_seconds", autoSaveIntervalSeconds);
        root.addProperty("location_check_interval_ticks", locationCheckIntervalTicks);
        root.addProperty("web_editor_port", webEditorPort);
        root.addProperty("language", language);

        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, GSON.toJson(root));
        } catch (IOException e) {
            LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }
}
