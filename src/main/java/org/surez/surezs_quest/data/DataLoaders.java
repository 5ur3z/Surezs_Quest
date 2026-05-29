package org.surez.surezs_quest.data;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;
import org.surez.surezs_quest.api.npc.NPC;
import org.surez.surezs_quest.api.quest.Quest;

import java.nio.file.Path;

public class DataLoaders {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DefinitionManager<Quest> QUESTS =
        new DefinitionManager<>(Quest.CODEC, Quest::id, "quest", LogUtils.getLogger());
    public static final DefinitionManager<NPC> NPCS =
        new DefinitionManager<>(NPC.CODEC, NPC::id, "NPC", LogUtils.getLogger());
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
