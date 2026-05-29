package org.surez.surezs_quest.storage;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public class JsonPlayerDataStore implements IPlayerDataStore {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path dataDir;

    public JsonPlayerDataStore(Path configDir) {
        this.dataDir = configDir.resolve("player_data");
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create player_data directory: {}", dataDir, e);
        }
    }

    private Path fileFor(UUID uuid) {
        return dataDir.resolve(uuid.toString() + ".json");
    }

    @Override
    public PlayerQuestData load(UUID playerUuid) {
        Path file = fileFor(playerUuid);
        if (!Files.exists(file)) {
            return new PlayerQuestData();
        }
        try {
            String json = Files.readString(file);
            return QuestDataCodec.decodePlayer(json);
        } catch (IOException e) {
            LOGGER.error("Failed to load player data for {}: {}", playerUuid, e.getMessage());
            return new PlayerQuestData();
        }
    }

    private final Object saveLock = new Object();

    @Override
    public void save(UUID playerUuid, PlayerQuestData data) {
        Path file = fileFor(playerUuid);
        synchronized (saveLock) {
            try {
                Files.writeString(file, QuestDataCodec.encodePlayer(data));
            } catch (IOException e) {
                LOGGER.error("Failed to save player data for {}: {}", playerUuid, e.getMessage());
            }
        }
    }

}
