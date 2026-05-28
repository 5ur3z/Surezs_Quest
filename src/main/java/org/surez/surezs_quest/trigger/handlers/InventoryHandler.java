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

public class InventoryHandler implements ITriggerHandler<QuestTrigger.InventoryChanged> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHECK_INTERVAL = 20;
    private final Map<UUID, Integer> tickCounters = new HashMap<>();
    private final Map<UUID, Map<ResourceLocation, Integer>> lastSnapshots = new HashMap<>();

    @Override
    public Class<? extends Event> listenedEvent() {
        return PlayerTickEvent.Post.class;
    }

    @Override
    public List<QuestTrigger.InventoryChanged> match(Event event) {
        if (!(event instanceof PlayerTickEvent.Post)) return List.of();
        return List.of(new QuestTrigger.InventoryChanged());
    }

    @Override
    public void handle(QuestTrigger.InventoryChanged trigger, Player player, PlayerQuestData data) {
        UUID uuid = player.getUUID();
        int count = tickCounters.merge(uuid, 1, Integer::sum);
        if (count % CHECK_INTERVAL != 0) return;

        Map<ResourceLocation, Integer> current = snapshot(player);
        Map<ResourceLocation, Integer> prev = lastSnapshots.get(uuid);

        if (prev != null && prev.equals(current)) return; // no change, skip
        lastSnapshots.put(uuid, current);

        int matched = 0;
        for (var questId : data.acceptedQuests()) {
            Quest quest = DataLoaders.QUESTS.get(questId);
            if (quest == null) continue;

            for (int i = 0; i < quest.objectives().size(); i++) {
                QuestObjective obj = quest.objectives().get(i);
                ResourceLocation target = targetItem(obj);
                if (target == null) continue;

                int held = current.getOrDefault(target, 0);
                int previous = data.getProgress(questId, i);

                if (held >= requiredCount(obj) && held != previous) {
                    int capped = Math.min(held, requiredCount(obj));
                    QuestProgressManager.updateProgress(player, data, questId, i, capped);
                    LOGGER.info("[Inventory] Quest {} obj {} matched: {}/{}", questId, i, held, requiredCount(obj));
                    matched++;
                }
            }
        }
        if (matched > 0) LOGGER.info("[Inventory] {} objectives matched", matched);
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

    private static int requiredCount(QuestObjective obj) {
        return switch (obj) {
            case QuestObjective.FindItems f -> f.count();
            // SUBMIT_ITEMS is NOT auto-progressed — player must use Submit GUI
            default -> 0;
        };
    }

    @Override
    public void onPlayerLogout(UUID uuid) {
        tickCounters.remove(uuid);
        lastSnapshots.remove(uuid);
    }

    private static ResourceLocation targetItem(QuestObjective obj) {
        return switch (obj) {
            case QuestObjective.FindItems f -> f.item();
            default -> null;
        };
    }
}
