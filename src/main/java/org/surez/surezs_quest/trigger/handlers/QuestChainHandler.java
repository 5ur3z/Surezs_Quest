package org.surez.surezs_quest.trigger.handlers;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.data.DataLoaders;
import org.surez.surezs_quest.storage.PlayerQuestData;
import org.surez.surezs_quest.storage.QuestDataManager;
import org.surez.surezs_quest.network.packet.QuestVisibilityUpdatePacket;
import org.surez.surezs_quest.trigger.QuestCompletedEvent;
import org.surez.surezs_quest.trigger.QuestProgressManager;

import java.util.ArrayList;

public class QuestChainHandler {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void register() {
        QuestProgressManager.addListener(QuestChainHandler::onQuestCompleted);
    }

    private static void onQuestCompleted(QuestCompletedEvent event) {
        Player player = event.player();
        ResourceLocation completedId = event.questId();
        var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) return;

        for (Quest q : DataLoaders.QUESTS.getAll()) {
            if (!q.prerequisites().contains(completedId)) continue;
            if (!prerequisitesMet(q, data)) continue;

            if (q.autoAccept()) {
                data.accept(q.id());
                LOGGER.info("[Chain] Quest {} auto-unlocked for {}", q.id(), player.getName().getString());
            } else {
                LOGGER.info("[Chain] Branch quest {} offered to {}", q.id(), player.getName().getString());
            }
        }

        // notify client about newly visible hidden quests
        var newlyVisible = new ArrayList<ResourceLocation>();
        for (Quest q : DataLoaders.QUESTS.getAll()) {
            if (q.prerequisites().isEmpty()) continue;
            if (!q.prerequisites().contains(completedId)) continue;
            if (data.isAccepted(q.id()) || data.isDeclined(q.id()) || data.isCompleted(q.id())) continue;
            if (!prerequisitesMet(q, data)) continue;
            newlyVisible.add(q.id());
        }
        if (!newlyVisible.isEmpty() && player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new QuestVisibilityUpdatePacket(newlyVisible));
            LOGGER.debug("[Chain] Visibility update for {}: {}", player.getName().getString(), newlyVisible);
        }
    }

    private static boolean prerequisitesMet(Quest quest, PlayerQuestData data) {
        if (quest.prerequisites().isEmpty()) return true;

        long met = quest.prerequisites().stream()
            .filter(prereqId -> isQuestCompleted(prereqId, data))
            .count();

        return switch (quest.prerequisiteMode()) {
            case ALL -> met == quest.prerequisites().size();
            case ANY -> met > 0;
        };
    }

    private static boolean isQuestCompleted(ResourceLocation questId, PlayerQuestData data) {
        Quest q = DataLoaders.QUESTS.get(questId);
        if (q == null) return false;
        for (int i = 0; i < q.objectives().size(); i++) {
            int max = QuestProgressManager.getObjectiveMax(q, i);
            if (data.getProgress(questId, i) < max) return false;
        }
        return !q.objectives().isEmpty();
    }
}
