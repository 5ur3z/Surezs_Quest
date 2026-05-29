package org.surez.surezs_quest.trigger.handlers;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.surez.surezs_quest.Config;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.api.quest.QuestObjective;
import org.surez.surezs_quest.api.trigger.QuestTrigger;
import org.surez.surezs_quest.storage.PlayerQuestData;
import org.surez.surezs_quest.trigger.ITriggerHandler;
import org.surez.surezs_quest.trigger.QuestProgressManager;

import java.util.*;

public class LocationHandler implements ITriggerHandler<QuestTrigger.PlayerTick> {

    private static final Logger LOGGER = LogUtils.getLogger();
    private final TickGate tickGate = new TickGate(Config.INSTANCE.locationCheckIntervalTicks());

    @Override
    public Class<? extends Event> listenedEvent() {
        return PlayerTickEvent.Post.class;
    }

    @Override
    public List<QuestTrigger.PlayerTick> match(Event event) {
        if (!(event instanceof PlayerTickEvent.Post)) return List.of();
        return List.of(new QuestTrigger.PlayerTick(Config.INSTANCE.locationCheckIntervalTicks()));
    }

    @Override
    public void handle(QuestTrigger.PlayerTick trigger, Player player, PlayerQuestData data) {
        UUID uuid = player.getUUID();
        if (!tickGate.shouldRun(uuid)) return;

        if (!(player instanceof ServerPlayer sp)) return;

        BlockPos pos = sp.blockPosition();
        String dim = sp.level().dimension().location().toString();
        final int[] matched = {0};

        AcceptedObjectiveWalker.forEach(data, match -> {
            if (match.objective() instanceof QuestObjective.ReachLocation loc) {
                if (!loc.dimension().toString().equals(dim)) return;

                int dx = pos.getX() - loc.x();
                int dy = pos.getY() - loc.y();
                int dz = pos.getZ() - loc.z();
                if (dx * dx + dy * dy + dz * dz <= loc.radius() * loc.radius()) {
                    QuestProgressManager.updateProgress(player, data, match.questId(), match.objectiveIndex(), 1);
                    LOGGER.info("[Location] Quest {} obj {} reached", match.questId(), match.objectiveIndex());
                    matched[0]++;
                }
            }
        });
        if (matched[0] > 0) LOGGER.info("[Location] {} objectives matched", matched[0]);
    }

    @Override
    public void onPlayerLogout(UUID uuid) {
        tickGate.clear(uuid);
    }
}
