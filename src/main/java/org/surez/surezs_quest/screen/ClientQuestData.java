package org.surez.surezs_quest.screen;

import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.api.quest.QuestObjective;
import org.surez.surezs_quest.network.packet.OpenQuestScreenPacket;

import java.util.*;

public class ClientQuestData {

    private static final Map<ResourceLocation, Quest> quests = new HashMap<>();
    private static final Map<ResourceLocation, String> rewardTexts = new HashMap<>();
    private static final Map<ResourceLocation, List<ItemRewardInfo>> rewardItems = new HashMap<>();

    public record ItemRewardInfo(ResourceLocation itemId, int count) {}
    private static final Map<ResourceLocation, Boolean> hiddenQuests = new HashMap<>();
    private static final Map<ResourceLocation, String[]> descriptions = new HashMap<>();

    /** Called by the client when it receives quest data from the server */
    public static void loadFromServer(List<OpenQuestScreenPacket.QuestInfo> infos) {
        quests.clear();
        rewardTexts.clear();
        hiddenQuests.clear();
        for (var info : infos) {
            List<QuestObjective> objectives = new ArrayList<>();
            for (var objInfo : info.objectives()) {
                QuestObjective obj = switch (objInfo.type()) {
                    case "find_items" -> new QuestObjective.FindItems(objInfo.item(), objInfo.count());
                    case "submit_items" -> new QuestObjective.SubmitItems(objInfo.item(), objInfo.count());
                    case "kill_entity" -> new QuestObjective.KillEntity(objInfo.item(), objInfo.count());
                    case "craft_item" -> new QuestObjective.CraftItem(objInfo.item(), objInfo.count());
                    default -> new QuestObjective.ReachLocation(objInfo.item(), 0, 0, 0, 5);
                };
                objectives.add(obj);
            }
            Quest quest = new Quest(
                info.id(), info.npcId(), objectives, List.of(),
                Quest.PrerequisiteMode.ALL,
                Quest.Scope.PLAYER,
                List.of(),
                Quest.RewardMode.PER_CONTRIBUTOR,
                info.canReject(), false, false, true, 0,
                new Quest.Dialogue("", "", "", "", "")
            );
            quests.put(quest.id(), quest);

            // Parse reward text for display + item icons
            String raw = info.rewardText();
            String displayText;
            List<ItemRewardInfo> items = new ArrayList<>();

            // Try || separator first ("emeraldx2, 50经验||minecraft:emerald:2")
            int sep = raw.indexOf("||");
            String itemsPart = sep >= 0 ? raw.substring(sep + 2) : "";
            displayText = sep >= 0 ? raw.substring(0, sep) : raw;

            // Parse items from || part or from display text fallback
            if (!itemsPart.isEmpty()) {
                for (String part : itemsPart.split(",")) {
                    int lastColon = part.lastIndexOf(':');
                    if (lastColon < 0) continue;
                    try {
                        String id = part.substring(0, lastColon);
                        int count = Integer.parseInt(part.substring(lastColon + 1));
                        items.add(new ItemRewardInfo(ResourceLocation.parse(id), count));
                    } catch (Exception ignored) {}
                }
            }
            // Fallback: parse item names from display text "iron_swordx1, 100经验"
            if (items.isEmpty()) {
                for (String part : displayText.split(",")) {
                    part = part.trim();
                    if (!part.contains("x") || part.startsWith("[")) continue;
                    int xIdx = part.lastIndexOf('x');
                    try {
                        String id = part.substring(0, xIdx).trim();
                        int count = Integer.parseInt(part.substring(xIdx + 1).trim());
                        ResourceLocation rl = id.contains(":") ? ResourceLocation.parse(id) : ResourceLocation.withDefaultNamespace(id);
                        items.add(new ItemRewardInfo(rl, count));
                    } catch (Exception ignored) {}
                }
            }

            rewardTexts.put(quest.id(), displayText);
            rewardItems.put(quest.id(), items);
            if (items.size() > 0) System.out.println("[RewardParse] " + quest.id() + " items=" + items.size() + " raw=" + info.rewardText());
            hiddenQuests.put(quest.id(), info.hidden());
            String[] parts = info.description().split("\0", -1);
            descriptions.put(quest.id(), parts.length >= 4 ? parts : new String[]{"", "", "", ""});
        }
    }

    public static Quest get(ResourceLocation id) { return quests.get(id); }
    public static Collection<Quest> getAll() { return quests.values(); }
    public static String getRewardText(ResourceLocation id) { return rewardTexts.getOrDefault(id, ""); }
    public static boolean isHidden(ResourceLocation id) { return hiddenQuests.getOrDefault(id, false); }
    public static List<ItemRewardInfo> getRewardItems(ResourceLocation id) { return rewardItems.getOrDefault(id, List.of()); }

    /** dialogue.give — quest description shown in expanded card */
    public static String getDescription(ResourceLocation id) {
        String[] parts = descriptions.get(id);
        return parts != null && parts.length > 0 ? parts[0] : "";
    }
    /** dialogue.accept — shown when player accepts */
    public static String getAcceptText(ResourceLocation id) {
        String[] parts = descriptions.get(id);
        return parts != null && parts.length > 1 ? parts[1] : "";
    }
    /** dialogue.decline — shown when player declines */
    public static String getDeclineText(ResourceLocation id) {
        String[] parts = descriptions.get(id);
        return parts != null && parts.length > 2 ? parts[2] : "";
    }
    /** dialogue.complete — shown when player claims reward */
    public static String getCompleteText(ResourceLocation id) {
        String[] parts = descriptions.get(id);
        return parts != null && parts.length > 3 ? parts[3] : "";
    }
}
