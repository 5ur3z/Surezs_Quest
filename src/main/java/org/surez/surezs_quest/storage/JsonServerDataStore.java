package org.surez.surezs_quest.storage;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonServerDataStore implements IServerDataStore {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Path filePath;

    public JsonServerDataStore(Path configDir) {
        this.filePath = configDir.resolve("server_data.json");
    }

    @Override
    public ServerQuestData load() {
        if (!Files.exists(filePath)) {
            return new ServerQuestData();
        }
        try {
            String json = Files.readString(filePath);
            return QuestDataCodec.decodeServer(json);
        } catch (IOException e) {
            LOGGER.error("Failed to load server data: {}", e.getMessage());
            return new ServerQuestData();
        }
    }

    private final Object saveLock = new Object();

    @Override
    public void save(ServerQuestData data) {
        synchronized (saveLock) {
            try {
                Files.writeString(filePath, QuestDataCodec.encodeServer(data));
            } catch (IOException e) {
                LOGGER.error("Failed to save server data: {}", e.getMessage());
            }
        }
    }

    @Override
    public void saveAsync(ServerQuestData data) {
        new Thread(() -> save(data), "QuestServerDataSave").start();
    }
}
