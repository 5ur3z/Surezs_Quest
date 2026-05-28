package org.surez.surezs_quest.trigger.handlers;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.api.quest.QuestObjective;
import org.surez.surezs_quest.api.trigger.QuestTrigger;
import org.surez.surezs_quest.data.DataLoaders;
import org.surez.surezs_quest.storage.PlayerQuestData;
import org.surez.surezs_quest.trigger.ITriggerHandler;
import org.surez.surezs_quest.trigger.QuestProgressManager;

import java.util.*;

public class CraftHandler implements ITriggerHandler<QuestTrigger.ItemCrafted> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHECK_INTERVAL = 20;
    private final Map<UUID, Integer> tickCounters = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Integer>> lastInventory = new HashMap<>();

    @Override
    public Class<? extends Event> listenedEvent() {
        return PlayerTickEvent.Post.class;
    }

    @Override
    public List<QuestTrigger.ItemCrafted> match(Event event) {
        if (!(event instanceof PlayerTickEvent.Post)) return List.of();
        return List.of(new QuestTrigger.ItemCrafted(
            net.minecraft.resources.ResourceLocation.parse("minecraft:air"), 0));
    }

    @Override
    public void handle(QuestTrigger.ItemCrafted trigger, Player player, PlayerQuestData data) {
        UUID uuid = player.getUUID();
        int count = tickCounters.merge(uuid, 1, Integer::sum);
        if (count % CHECK_INTERVAL != 0) return;

        Map<ResourceLocation, Integer> prev = lastInventory.getOrDefault(uuid, Map.of());
        Map<ResourceLocation, Integer> current = snapshot(player);

        int matched = 0;
        for (var questId : data.acceptedQuests()) {
            Quest quest = DataLoaders.QUESTS.get(questId);
            if (quest == null) continue;

            for (int i = 0; i < quest.objectives().size(); i++) {
                if (quest.objectives().get(i) instanceof QuestObjective.CraftItem craft) {
                    ResourceLocation itemKey = craft.item();
                    int prevCount = prev.getOrDefault(itemKey, 0);
                    int currCount = current.getOrDefault(itemKey, 0);
                    if (currCount > prevCount) {
                        int progress = data.getProgress(questId, i) + (currCount - prevCount);
                        QuestProgressManager.updateProgress(player, data, questId, i, Math.min(progress, craft.count()));
                        LOGGER.info("[Craft] Quest {} obj {} matched: {}/{}",
                            questId, i, data.getProgress(questId, i), craft.count());
                        matched++;
                    }
                }
            }
        }

        lastInventory.put(uuid, current);
        if (matched > 0) LOGGER.info("[Craft] {} objectives matched", matched);
    }

    @Override
    public void onPlayerLogout(UUID uuid) {
        tickCounters.remove(uuid);
        lastInventory.remove(uuid);
    }

    private static Map<ResourceLocation, Integer> snapshot(Player player) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        for (var stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                counts.merge(id, stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }
}
