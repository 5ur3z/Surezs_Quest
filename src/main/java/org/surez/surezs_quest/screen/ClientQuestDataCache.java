package org.surez.surezs_quest.screen;

import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.network.packet.OpenSubmitScreenPacket;

import java.util.*;

public class ClientQuestDataCache {

    public static final ClientQuestDataCache INSTANCE = new ClientQuestDataCache();

    private List<ResourceLocation> npcIds = List.of();
    private final Set<ResourceLocation> acceptedQuests = new HashSet<>();
    private final Map<ResourceLocation, Map<Integer, Integer>> progress = new HashMap<>();
    private ResourceLocation activeNpcId;

    private ClientQuestDataCache() {}

    // -- npc list ------------------------------------------------------------

    private final Map<ResourceLocation, String> npcNames = new HashMap<>();

    public List<ResourceLocation> npcIds() { return npcIds; }
    public void setNpcIds(List<ResourceLocation> ids) { this.npcIds = List.copyOf(ids); }
    public void setNpcNames(List<String> names) {
        npcNames.clear();
        for (int i = 0; i < npcIds.size() && i < names.size(); i++)
            npcNames.put(npcIds.get(i), names.get(i));
    }
    public String getNpcName(ResourceLocation id) { return npcNames.getOrDefault(id, id.getPath()); }

    // -- accepted quests -----------------------------------------------------

    public Set<ResourceLocation> acceptedQuests() { return acceptedQuests; }
    public void setAcceptedQuests(Collection<ResourceLocation> ids) {
        for (var old : new HashSet<>(acceptedQuests)) {
            if (!ids.contains(old)) progress.remove(old);
        }
        acceptedQuests.clear();
        acceptedQuests.addAll(ids);
    }
    public boolean isAccepted(ResourceLocation id) { return acceptedQuests.contains(id); }
    public void addAccepted(ResourceLocation id) { acceptedQuests.add(id); }

    public boolean areObjectivesMet(ResourceLocation questId) {
        var q = ClientQuestData.get(questId);
        if (q == null) return false;
        for (int i = 0; i < q.objectives().size(); i++) {
            int max = QuestCardWidget.objectiveMax(q.objectives().get(i));
            if (getProgress(questId, i) < max) return false;
        }
        return !q.objectives().isEmpty();
    }

    // -- progress ------------------------------------------------------------

    public Map<ResourceLocation, Map<Integer, Integer>> progress() { return progress; }

    public int getProgress(ResourceLocation questId, int objIndex) {
        return progress.getOrDefault(questId, Map.of()).getOrDefault(objIndex, 0);
    }

    public void updateProgress(ResourceLocation questId, int objIndex, int newValue) {
        progress.computeIfAbsent(questId, k -> new HashMap<>()).put(objIndex, newValue);
    }

    // -- active npc ----------------------------------------------------------

    public ResourceLocation activeNpcId() { return activeNpcId; }
    public void setActiveNpcId(ResourceLocation id) { this.activeNpcId = id; }

    // -- submit items --------------------------------------------------------

    private ResourceLocation pendingSubmitQuestId;
    private List<OpenSubmitScreenPacket.SlotItem> pendingSubmitItems = List.of();

    public void setPendingSubmit(ResourceLocation questId, List<OpenSubmitScreenPacket.SlotItem> items) {
        this.pendingSubmitQuestId = questId;
        this.pendingSubmitItems = List.copyOf(items);
    }

    // -- pending quest cards (offered but not yet accepted) -------------------

    private final Set<ResourceLocation> completedQuests = new HashSet<>();
    public boolean isCompleted(ResourceLocation id) { return completedQuests.contains(id); }
    public void markCompleted(ResourceLocation id) { completedQuests.add(id); }
    public void setCompletedQuests(Set<ResourceLocation> ids) { completedQuests.clear(); completedQuests.addAll(ids); }

    private final Set<ResourceLocation> declinedQuests = new HashSet<>();
    public boolean isDeclined(ResourceLocation id) { return declinedQuests.contains(id); }
    public void markDeclined(ResourceLocation id) { declinedQuests.add(id); }
    public void setDeclinedQuests(Set<ResourceLocation> ids) { declinedQuests.clear(); declinedQuests.addAll(ids); }

    private final Set<ResourceLocation> visibleHiddenQuests = new HashSet<>();
    public boolean isVisibleHidden(ResourceLocation id) { return visibleHiddenQuests.contains(id); }
    public void setVisibleHiddenQuests(Set<ResourceLocation> ids) { visibleHiddenQuests.clear(); visibleHiddenQuests.addAll(ids); }
    public Set<ResourceLocation> visibleHiddenQuests() { return visibleHiddenQuests; }
    public void addVisibleHiddenQuests(List<ResourceLocation> ids) { visibleHiddenQuests.addAll(ids); }
    public void removeVisibleHiddenQuest(ResourceLocation id) { visibleHiddenQuests.remove(id); }

    public void clearQuestState(ResourceLocation id) {
        acceptedQuests.remove(id);
        declinedQuests.remove(id);
        progress.remove(id);
    }

    // -- clear ---------------------------------------------------------------

    /** Clear only status flags (claimed/declined), preserving progress and acceptedQuests */
    public void clearStatusFlags() {
        completedQuests.clear();
        declinedQuests.clear();
    }

    public void clear() {
        npcIds = List.of();
        acceptedQuests.clear();
        progress.clear();
        activeNpcId = null;
    }

}
