package org.surez.surezs_quest.storage;

import java.util.UUID;

public interface IPlayerDataStore {

    PlayerQuestData load(UUID playerUuid);

    void save(UUID playerUuid, PlayerQuestData data);
}
