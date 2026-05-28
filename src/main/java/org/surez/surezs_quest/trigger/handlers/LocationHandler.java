package org.surez.surezs_quest.trigger.handlers;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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

public class LocationHandler implements ITriggerHandler<QuestTrigger.PlayerTick> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CHECK_INTERVAL = 20;
    private final Map<UUID, Integer> tickCounters = new HashMap<>();

    @Override
    public Class<? extends Event> listenedEvent() {
        return PlayerTickEvent.Post.class;
    }

    @Override
    public List<QuestTrigger.PlayerTick> match(Event event) {
        if (!(event instanceof PlayerTickEvent.Post)) return List.of();
        return List.of(new QuestTrigger.PlayerTick(CHECK_INTERVAL));
    }

    @Override
    public void handle(QuestTrigger.PlayerTick trigger, Player player, PlayerQuestData data) {
        UUID uuid = player.getUUID();
        int count = tickCounters.merge(uuid, 1, Integer::sum);
        if (count % CHECK_INTERVAL != 0) return;

        if (!(player instanceof ServerPlayer sp)) return;

        BlockPos pos = sp.blockPosition();
        String dim = sp.level().dimension().location().toString();
        int matched = 0;

        for (var questId : data.acceptedQuests()) {
            Quest quest = DataLoaders.QUESTS.get(questId);
            if (quest == null) continue;

            for (int i = 0; i < quest.objectives().size(); i++) {
                if (quest.objectives().get(i) instanceof QuestObjective.ReachLocation loc) {
                    if (!loc.dimension().toString().equals(dim)) continue;

                    int dx = pos.getX() - loc.x();
                    int dy = pos.getY() - loc.y();
                    int dz = pos.getZ() - loc.z();
                    if (dx * dx + dy * dy + dz * dz <= loc.radius() * loc.radius()) {
                        QuestProgressManager.updateProgress(player, data, questId, i, 1);
                        LOGGER.info("[Location] Quest {} obj {} reached", questId, i);
                        matched++;
                    }
                }
            }
        }
        if (matched > 0) LOGGER.info("[Location] {} objectives matched", matched);
    }

    @Override
    public void onPlayerLogout(UUID uuid) {
        tickCounters.remove(uuid);
    }
}
