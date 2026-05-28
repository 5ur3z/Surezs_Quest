package org.surez.surezs_quest.storage;

import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.trigger.QuestProgressManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerQuestData {

    private final ConcurrentHashMap<ResourceLocation, ServerQuestEntry> quests = new ConcurrentHashMap<>();

    public ConcurrentHashMap<ResourceLocation, ServerQuestEntry> quests() { return quests; }

    /** Per-quest server data — JSON and Java share the same structure */
    public record ServerQuestEntry(
        ConcurrentHashMap<Integer, Integer> progress,
        Set<UUID> acceptedPlayers,
        Set<UUID> declinedPlayers,
        Set<UUID> completedPlayers,
        ConcurrentHashMap<UUID, ConcurrentHashMap<Integer, Integer>> contributors,
        boolean completedDebug
    ) {
        static ServerQuestEntry empty() {
            return new ServerQuestEntry(
                new ConcurrentHashMap<>(), ConcurrentHashMap.newKeySet(),
                ConcurrentHashMap.newKeySet(), ConcurrentHashMap.newKeySet(), new ConcurrentHashMap<>(), false);
        }
    }

    // -- server progress -----------------------------------------------------

    public int getProgress(ResourceLocation questId, int objectiveIndex) {
        var e = quests.get(questId);
        return e != null ? e.progress().getOrDefault(objectiveIndex, 0) : 0;
    }

    public void addProgress(ResourceLocation questId, int objectiveIndex, int amount) {
        quests.computeIfAbsent(questId, k -> ServerQuestEntry.empty())
            .progress().merge(objectiveIndex, amount, Integer::sum);
    }

    // -- completion check (computed from progress) ---------------------------

    public boolean areObjectivesMet(ResourceLocation id, Quest quest) {
        var e = quests.get(id);
        if (e == null || quest.objectives().isEmpty()) return false;
        for (int i = 0; i < quest.objectives().size(); i++) {
            if (e.progress().getOrDefault(i, 0) < getObjectiveMax(quest, i)) return false;
        }
        return true;
    }

    private static int getObjectiveMax(Quest quest, int i) {
        return QuestProgressManager.getObjectiveMax(quest, i);
    }

    // -- accepted players ----------------------------------------------------

    public void accept(ResourceLocation questId, UUID uuid) {
        quests.computeIfAbsent(questId, k -> ServerQuestEntry.empty())
            .acceptedPlayers().add(uuid);
    }

    public boolean isAccepted(ResourceLocation questId, UUID uuid) {
        var e = quests.get(questId);
        return e != null && e.acceptedPlayers().contains(uuid);
    }

    // -- declined players ----------------------------------------------------

    public void decline(ResourceLocation questId, UUID uuid) {
        quests.computeIfAbsent(questId, k -> ServerQuestEntry.empty())
            .declinedPlayers().add(uuid);
    }

    public boolean isDeclined(ResourceLocation questId, UUID uuid) {
        var e = quests.get(questId);
        return e != null && e.declinedPlayers().contains(uuid);
    }

    // -- claimed players -----------------------------------------------------

    public void markCompleted(ResourceLocation questId, UUID playerId) {
        quests.computeIfAbsent(questId, k -> ServerQuestEntry.empty())
            .completedPlayers().add(playerId);
    }

    public boolean hasPlayerCompleted(ResourceLocation questId, UUID playerId) {
        var e = quests.get(questId);
        return e != null && e.completedPlayers().contains(playerId);
    }

    // -- contributors --------------------------------------------------------

    public void clearAcceptedPlayers(ResourceLocation questId) {
        quests.computeIfPresent(questId, (k, old) -> new ServerQuestEntry(
            old.progress(), ConcurrentHashMap.newKeySet(),
            old.declinedPlayers(), old.completedPlayers(), old.contributors(), old.completedDebug()));
    }

    public void markDebugComplete(ResourceLocation questId) {
        quests.compute(questId, (k, old) -> {
            var base = old != null ? old : ServerQuestEntry.empty();
            return new ServerQuestEntry(base.progress(), base.acceptedPlayers(), base.declinedPlayers(),
                base.completedPlayers(), base.contributors(), true);
        });
    }

    public boolean hasContributed(ResourceLocation questId, UUID uuid) {
        var e = quests.get(questId);
        return e != null && e.contributors().containsKey(uuid);
    }

    public void addContribution(ResourceLocation questId, UUID playerId, int objIndex, int amount) {
        quests.computeIfAbsent(questId, k -> ServerQuestEntry.empty())
            .contributors()
            .computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
            .merge(objIndex, amount, Integer::sum);
    }

    // -- reset ---------------------------------------------------------------

    public void resetProgress(ResourceLocation questId) {
        quests.remove(questId);
    }
}
