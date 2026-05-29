package org.surez.surezs_quest.storage;

public interface IServerDataStore {

    ServerQuestData load();

    void save(ServerQuestData data);
}
