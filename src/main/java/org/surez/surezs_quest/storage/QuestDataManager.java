package org.surez.surezs_quest.storage;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.data.DataLoaders;
import org.surez.surezs_quest.trigger.TriggerRegistry;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class QuestDataManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    public static final QuestDataManager INSTANCE = new QuestDataManager();

    private final ConcurrentHashMap<UUID, PlayerQuestData> playerDataMap = new ConcurrentHashMap<>();
    private ServerQuestData serverData = new ServerQuestData();

    private IPlayerDataStore playerStore;
    private IServerDataStore serverStore;

    private QuestDataManager() {}

    public void init(Path configDir) {
        this.playerStore = new JsonPlayerDataStore(configDir);
        this.serverStore = new JsonServerDataStore(configDir);
        this.serverData = serverStore.load();
        LOGGER.info("QuestDataManager initialized — config: {}", configDir);
    }

    // -- accessors ------------------------------------------------------------

    public PlayerQuestData getPlayerData(UUID uuid) {
        return playerDataMap.get(uuid);
    }

    public ServerQuestData getServerData() {
        return serverData;
    }

    // -- events ---------------------------------------------------------------

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        UUID uuid = event.getEntity().getUUID();
        PlayerQuestData data = playerStore.load(uuid);
        playerDataMap.put(uuid, data);
        LOGGER.debug("Loaded quest data for player {}", uuid);

        // auto-accept quests with no prerequisites and auto_accept=true
        boolean serverModified = false;
        for (Quest q : DataLoaders.QUESTS.getAll()) {
            if (!q.autoAccept() || !q.prerequisites().isEmpty()) continue;

            if (q.scope() == Quest.Scope.SERVER) {
                if (serverData.areObjectivesMet(q.id(), q)
                    || serverData.isAccepted(q.id(), uuid)
                    || serverData.isDeclined(q.id(), uuid)
                    || serverData.hasPlayerCompleted(q.id(), uuid)) continue;
                serverData.accept(q.id(), uuid);
                serverModified = true;
            } else {
                if (data.isAccepted(q.id()) || data.isDeclined(q.id())
                    || data.isCompleted(q.id())) continue;
                data.accept(q.id());
                playerStore.save(uuid, data);
            }
        }
        if (serverModified) saveServer();
    }

    @SubscribeEvent
    public void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID uuid = event.getEntity().getUUID();
        PlayerQuestData data = playerDataMap.remove(uuid);
        if (data != null) {
            playerStore.save(uuid, data);
            LOGGER.debug("Saved quest data for player {}", uuid);
        }
        TriggerRegistry.notifyLogout(uuid);
    }

    @SubscribeEvent
    public void onServerStop(ServerStoppingEvent event) {
        flushAll();
    }

    @SubscribeEvent
    public void onServerStart(ServerStartedEvent event) {
        serverData = serverStore.load();
        LOGGER.info("Server quest data loaded");
    }

    // -- helpers --------------------------------------------------------------

    public void savePlayer(UUID uuid) {
        PlayerQuestData data = playerDataMap.get(uuid);
        if (data != null) playerStore.save(uuid, data);
    }

    public void saveServer() {
        serverStore.save(serverData);
    }

    public void flushAll() {
        for (var entry : playerDataMap.entrySet()) {
            playerStore.save(entry.getKey(), entry.getValue());
        }
        saveServer();
        LOGGER.info("All quest data flushed to disk");
    }
}
