package org.surez.surezs_quest.trigger.handlers;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.api.quest.QuestObjective;
import org.surez.surezs_quest.api.trigger.QuestTrigger;
import org.surez.surezs_quest.storage.PlayerQuestData;
import org.surez.surezs_quest.trigger.ITriggerHandler;
import org.surez.surezs_quest.trigger.QuestProgressManager;
import org.surez.surezs_quest.util.InventoryUtil;

import java.util.*;

public class InventoryHandler implements ITriggerHandler<QuestTrigger.InventoryChanged> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHECK_INTERVAL = 20;
    private final TickGate tickGate = new TickGate(CHECK_INTERVAL);
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
        if (!tickGate.shouldRun(uuid)) return;

        Map<ResourceLocation, Integer> current = InventoryUtil.snapshot(player);
        Map<ResourceLocation, Integer> prev = lastSnapshots.get(uuid);

        if (prev != null && prev.equals(current)) return;
        lastSnapshots.put(uuid, current);

        final int[] matched = {0};
        AcceptedObjectiveWalker.forEach(data, match -> {
            QuestObjective obj = match.objective();
            ResourceLocation target = targetItem(obj);
            if (target == null) return;

            int held = current.getOrDefault(target, 0);
            int previous = data.getProgress(match.questId(), match.objectiveIndex());

            if (held >= requiredCount(obj) && held != previous) {
                int capped = Math.min(held, requiredCount(obj));
                QuestProgressManager.updateProgress(player, data, match.questId(), match.objectiveIndex(), capped);
                LOGGER.info("[Inventory] Quest {} obj {} matched: {}/{}", match.questId(), match.objectiveIndex(), held, requiredCount(obj));
                matched[0]++;
            }
        });
        if (matched[0] > 0) LOGGER.info("[Inventory] {} objectives matched", matched[0]);
    }

    private static int requiredCount(QuestObjective obj) {
        return switch (obj) {
            case QuestObjective.FindItems f -> f.count();
            default -> 0;
        };
    }

    @Override
    public void onPlayerLogout(UUID uuid) {
        tickGate.clear(uuid);
        lastSnapshots.remove(uuid);
    }

    private static ResourceLocation targetItem(QuestObjective obj) {
        return switch (obj) {
            case QuestObjective.FindItems f -> f.item();
            default -> null;
        };
    }
}
