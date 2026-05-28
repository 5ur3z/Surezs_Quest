package org.surez.surezs_quest.storage;

public interface IServerDataStore {

    ServerQuestData load();

    void save(ServerQuestData data);

    default void saveAsync(ServerQuestData data) {
        save(data);
    }

    default void flushAll() {}
}
