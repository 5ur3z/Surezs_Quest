package org.surez.surezs_quest.trigger;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.data.DataLoaders;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.surez.surezs_quest.api.quest.Quest.Scope;
import org.surez.surezs_quest.network.packet.QuestCompletedPacket;
import org.surez.surezs_quest.network.packet.QuestProgressUpdatePacket;
import org.surez.surezs_quest.network.packet.ServerQuestProgressPacket;
import org.surez.surezs_quest.reward.QuestRewardDispatcher;
import org.surez.surezs_quest.storage.PlayerQuestData;
import org.surez.surezs_quest.storage.QuestDataManager;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class QuestProgressManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<Consumer<QuestCompletedEvent>> listeners = new ArrayList<>();

    public static void addListener(Consumer<QuestCompletedEvent> listener) {
        listeners.add(listener);
    }

    /** Force-complete a quest for a player (admin command use) */
    public static void forceComplete(Player player, PlayerQuestData data, Quest quest) {
        completeQuest(player, data, quest);
    }

    public static void updateProgress(Player player, PlayerQuestData data, ResourceLocation questId, int objIndex, int newValue) {
        Quest quest = DataLoaders.QUESTS.get(questId);
        if (quest == null) return;

        if (quest.scope() == Scope.SERVER) {
            updateServerProgress(player, quest, objIndex, newValue);
        } else {
            updatePlayerProgress(player, data, quest, objIndex, newValue);
        }
    }

    private static void updatePlayerProgress(Player player, PlayerQuestData data, Quest quest, int objIndex, int newValue) {
        data.setProgress(quest.id(), objIndex, newValue);

        if (player instanceof ServerPlayer sp) {
            int max = getObjectiveMax(quest, objIndex);
            PacketDistributor.sendToPlayer(sp, new QuestProgressUpdatePacket(quest.id(), objIndex, newValue, max));
        }

        boolean allDone = checkAllObjectives(quest, data, null);
        if (allDone) {
            completeQuest(player, data, quest);
        }
    }

    private static void updateServerProgress(Player player, Quest quest, int objIndex, int amount) {
        var serverData = QuestDataManager.INSTANCE.getServerData();
        boolean wasAlreadyMet = checkAllObjectives(quest, null, serverData);
        serverData.addProgress(quest.id(), objIndex, amount);
        serverData.addContribution(quest.id(), player.getUUID(), objIndex, amount);

        int newValue = serverData.getProgress(quest.id(), objIndex);
        int max = getObjectiveMax(quest, objIndex);

        // broadcast to all online players
        var packet = new ServerQuestProgressPacket(quest.id(), objIndex, newValue, max);
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (var sp : server.getPlayerList().getPlayers()) {
                PacketDistributor.sendToPlayer(sp, packet);
            }
        }

        boolean allDone = checkAllObjectives(quest, null, serverData);
        if (allDone && !wasAlreadyMet) {
            completeServerQuest(quest, serverData);
        }

        QuestDataManager.INSTANCE.saveServer();
    }

    private static boolean checkAllObjectives(Quest quest, PlayerQuestData playerData, org.surez.surezs_quest.storage.ServerQuestData serverData) {
        for (int i = 0; i < quest.objectives().size(); i++) {
            int required = getObjectiveMax(quest, i);
            int current = serverData != null
                ? serverData.getProgress(quest.id(), i)
                : playerData.getProgress(quest.id(), i);
            if (current < required) return false;
        }
        return true;
    }

    private static void completeServerQuest(Quest quest, org.surez.surezs_quest.storage.ServerQuestData serverData) {
        serverData.markDebugComplete(quest.id());
        LOGGER.info("Server quest completed: {}", quest.id());

        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        var completedPacket = new QuestCompletedPacket(quest.id());
        for (var sp : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(sp, completedPacket);

            if (quest.rewardMode() == Quest.RewardMode.ALL_ONLINE) {
                var pd = QuestDataManager.INSTANCE.getPlayerData(sp.getUUID());
                if (pd != null) {
                    QuestRewardDispatcher.grantRewards(quest, sp, pd, true);
                    serverData.markCompleted(quest.id(), sp.getUUID());
                }
            }
        }
        // PER_CONTRIBUTOR: each player claims manually via ClaimRewardPacket
        // acceptedPlayers cleared above; completed + !claimed → 可领取
    }

    private static void completeQuest(Player player, PlayerQuestData data, Quest quest) {
        // Don't auto-grant rewards — player must click [Claim] button
        // via ClaimRewardPacket, which calls QuestRewardDispatcher.grantRewards()
        QuestDataManager.INSTANCE.savePlayer(player.getUUID());
        LOGGER.info("Quest completed: {} by {}", quest.id(), player.getName().getString());

        if (player instanceof ServerPlayer sp) {
            PacketDistributor.sendToPlayer(sp, new QuestCompletedPacket(quest.id()));
        }

        var event = new QuestCompletedEvent(quest.id(), player);
        for (var listener : listeners) {
            try {
                listener.accept(event);
            } catch (Exception e) {
                LOGGER.error("QuestCompletedEvent listener failed: {}", e.getMessage());
            }
        }
    }

    public static int getObjectiveMax(Quest quest, int index) {
        if (index < 0 || index >= quest.objectives().size()) return 1;
        var obj = quest.objectives().get(index);
        return switch (obj) {
            case org.surez.surezs_quest.api.quest.QuestObjective.ReachLocation r -> 1;
            case org.surez.surezs_quest.api.quest.QuestObjective.FindItems f -> f.count();
            case org.surez.surezs_quest.api.quest.QuestObjective.SubmitItems s -> s.count();
            case org.surez.surezs_quest.api.quest.QuestObjective.KillEntity k -> k.count();
            case org.surez.surezs_quest.api.quest.QuestObjective.CraftItem c -> c.count();
        };
    }
}
