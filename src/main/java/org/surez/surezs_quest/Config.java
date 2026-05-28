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

    private boolean serverQuestsEnabled = true;
    private boolean serverQuestsGuiVisible = true;
    private int autoSaveIntervalSeconds = 30;
    private int locationCheckIntervalTicks = 20;

    public boolean serverQuestsEnabled() { return serverQuestsEnabled; }
    public boolean serverQuestsGuiVisible() { return serverQuestsGuiVisible; }
    public int autoSaveIntervalSeconds() { return autoSaveIntervalSeconds; }
    public int locationCheckIntervalTicks() { return locationCheckIntervalTicks; }

    // -- load / save -----------------------------------------------------------

    public static Config load(Path configDir) {
        Config config = new Config();
        config.configFile = configDir.resolve("config.json");

        if (Files.exists(config.configFile)) {
            try {
                String json = Files.readString(config.configFile);
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                if (root.has("server_quests_enabled"))
                    config.serverQuestsEnabled = root.get("server_quests_enabled").getAsBoolean();
                if (root.has("server_quests_gui_visible"))
                    config.serverQuestsGuiVisible = root.get("server_quests_gui_visible").getAsBoolean();
                if (root.has("auto_save_interval_seconds"))
                    config.autoSaveIntervalSeconds = root.get("auto_save_interval_seconds").getAsInt();
                if (root.has("location_check_interval_ticks"))
                    config.locationCheckIntervalTicks = root.get("location_check_interval_ticks").getAsInt();
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
        root.addProperty("server_quests_enabled", serverQuestsEnabled);
        root.addProperty("server_quests_gui_visible", serverQuestsGuiVisible);
        root.addProperty("auto_save_interval_seconds", autoSaveIntervalSeconds);
        root.addProperty("location_check_interval_ticks", locationCheckIntervalTicks);

        try {
            Files.createDirectories(configFile.getParent());
            Files.writeString(configFile, GSON.toJson(root));
        } catch (IOException e) {
            LOGGER.error("Failed to save config: {}", e.getMessage());
        }
    }
}
