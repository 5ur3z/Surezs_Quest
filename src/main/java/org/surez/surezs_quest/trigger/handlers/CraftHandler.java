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

public class CraftHandler implements ITriggerHandler<QuestTrigger.ItemCrafted> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHECK_INTERVAL = 20;
    private final TickGate tickGate = new TickGate(CHECK_INTERVAL);
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
        if (!tickGate.shouldRun(uuid)) return;

        Map<ResourceLocation, Integer> prev = lastInventory.getOrDefault(uuid, Map.of());
        Map<ResourceLocation, Integer> current = InventoryUtil.snapshot(player);

        final int[] matched = {0};
        AcceptedObjectiveWalker.forEach(data, match -> {
            if (match.objective() instanceof QuestObjective.CraftItem craft) {
                ResourceLocation itemKey = craft.item();
                int prevCount = prev.getOrDefault(itemKey, 0);
                int currCount = current.getOrDefault(itemKey, 0);
                if (currCount > prevCount) {
                    int progress = data.getProgress(match.questId(), match.objectiveIndex()) + (currCount - prevCount);
                    QuestProgressManager.updateProgress(player, data, match.questId(), match.objectiveIndex(), Math.min(progress, craft.count()));
                    LOGGER.info("[Craft] Quest {} obj {} matched: {}/{}",
                        match.questId(), match.objectiveIndex(), data.getProgress(match.questId(), match.objectiveIndex()), craft.count());
                    matched[0]++;
                }
            }
        });

        lastInventory.put(uuid, current);
        if (matched[0] > 0) LOGGER.info("[Craft] {} objectives matched", matched[0]);
    }

    @Override
    public void onPlayerLogout(UUID uuid) {
        tickGate.clear(uuid);
        lastInventory.remove(uuid);
    }
}
