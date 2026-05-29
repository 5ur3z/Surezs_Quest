package org.surez.surezs_quest.storage;

import net.minecraft.resources.ResourceLocation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerQuestData {

    private final Set<ResourceLocation> acceptedQuests = ConcurrentHashMap.newKeySet();
    private final Set<ResourceLocation> declinedQuests = ConcurrentHashMap.newKeySet();
    private final Set<ResourceLocation> completedQuests = ConcurrentHashMap.newKeySet();
    private final Map<ResourceLocation, Map<Integer, Integer>> objectiveProgress = new ConcurrentHashMap<>();
    // -- accepted ------------------------------------------------------------

    public Set<ResourceLocation> acceptedQuests() { return acceptedQuests; }
    public boolean isAccepted(ResourceLocation id) { return acceptedQuests.contains(id); }
    public void accept(ResourceLocation id) { acceptedQuests.add(id); }

    // -- declined ------------------------------------------------------------

    public Set<ResourceLocation> declinedQuests() { return declinedQuests; }
    public boolean isDeclined(ResourceLocation id) { return declinedQuests.contains(id); }
    public void decline(ResourceLocation id) { declinedQuests.add(id); }

    // -- completed -----------------------------------------------------------

    public Set<ResourceLocation> completedQuests() { return completedQuests; }
    public boolean isCompleted(ResourceLocation id) { return completedQuests.contains(id); }
    public void markCompleted(ResourceLocation id) { completedQuests.add(id); }

    // -- objective progress --------------------------------------------------

    public Map<ResourceLocation, Map<Integer, Integer>> objectiveProgress() { return objectiveProgress; }

    public int getProgress(ResourceLocation questId, int objectiveIndex) {
        return objectiveProgress
            .getOrDefault(questId, Map.of())
            .getOrDefault(objectiveIndex, 0);
    }

    public void setProgress(ResourceLocation questId, int objectiveIndex, int value) {
        objectiveProgress
            .computeIfAbsent(questId, k -> new ConcurrentHashMap<>())
            .put(objectiveIndex, value);
    }

    // -- helpers -------------------------------------------------------------

    public void clearQuest(ResourceLocation id) {
        acceptedQuests.remove(id);
        declinedQuests.remove(id);
        completedQuests.remove(id);
        objectiveProgress.remove(id);
    }

    public void clear() {
        acceptedQuests.clear();
        declinedQuests.clear();
        completedQuests.clear();
        objectiveProgress.clear();
    }
}
