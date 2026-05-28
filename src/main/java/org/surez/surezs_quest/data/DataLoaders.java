package org.surez.surezs_quest.data;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.nio.file.Path;

public class DataLoaders {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final QuestManager QUESTS = new QuestManager();
    public static final NPCManager NPCS = new NPCManager();
    private static Path configDir;

    public static void init(Path configDir) {
        DataLoaders.configDir = configDir;
        DefaultDataCopier.copyDefaults(configDir);
        reload();
        LOGGER.info("DataLoaders initialized — config: {}", configDir);
    }

    public static void reload() {
        QUESTS.load(configDir.resolve("quests"));
        NPCS.load(configDir.resolve("npcs"));
        LOGGER.info("DataLoaders reloaded");
    }
}
