package org.surez.surezs_quest.trigger.handlers;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.slf4j.Logger;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.api.quest.QuestObjective;
import org.surez.surezs_quest.api.trigger.QuestTrigger;
import org.surez.surezs_quest.storage.PlayerQuestData;
import org.surez.surezs_quest.trigger.ITriggerHandler;
import org.surez.surezs_quest.trigger.QuestProgressManager;

import java.util.List;

public class KillEntityHandler implements ITriggerHandler<QuestTrigger.EntityKill> {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public Class<? extends Event> listenedEvent() {
        return LivingDeathEvent.class;
    }

    @Override
    public List<QuestTrigger.EntityKill> match(Event event) {
        if (!(event instanceof LivingDeathEvent deathEvent)) return List.of();
        if (!(deathEvent.getSource().getEntity() instanceof Player)) return List.of();

        LivingEntity victim = deathEvent.getEntity();
        String entityId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType()).toString();
        return List.of(new QuestTrigger.EntityKill(
            net.minecraft.resources.ResourceLocation.parse(entityId), 1));
    }

    @Override
    public void handle(QuestTrigger.EntityKill trigger, Player player, PlayerQuestData data) {
        final int[] matched = {0};
        AcceptedObjectiveWalker.forEach(data, match -> {
            if (match.objective() instanceof QuestObjective.KillEntity kill) {
                if (kill.entityType().equals(trigger.entityType())) {
                    int current = data.getProgress(match.questId(), match.objectiveIndex()) + 1;
                    QuestProgressManager.updateProgress(player, data, match.questId(), match.objectiveIndex(), current);
                    LOGGER.info("[Kill] Quest {} obj {} matched: {}/{}",
                        match.questId(), match.objectiveIndex(), current, kill.count());
                    matched[0]++;
                }
            }
        });
        if (matched[0] > 0) LOGGER.info("[Kill] {} objectives matched", matched[0]);
    }
}
