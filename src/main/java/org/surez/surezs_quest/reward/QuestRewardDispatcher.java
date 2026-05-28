package org.surez.surezs_quest.reward;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.api.quest.QuestReward;
import org.surez.surezs_quest.storage.PlayerQuestData;

public class QuestRewardDispatcher {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void grantRewards(Quest quest, Player player, PlayerQuestData data) {
        grantRewards(quest, player, data, false);
    }

    public static void grantRewards(Quest quest, Player player, PlayerQuestData data, boolean isServerQuest) {
        if (!isServerQuest && data.isCompleted(quest.id())) {
            LOGGER.warn("Player {} already claimed reward for {}", player.getName().getString(), quest.id());
            return;
        }

        if (!(player instanceof ServerPlayer sp)) return;
        MinecraftServer server = sp.getServer();
        if (server == null) return;

        for (QuestReward reward : quest.rewards()) {
            try {
                grant(reward, sp, server);
            } catch (Exception e) {
                LOGGER.error("Failed to grant reward {} for quest {}: {}", reward.type(), quest.id(), e.getMessage());
            }
        }

        if (!isServerQuest) {
            data.markCompleted(quest.id());
        }
        LOGGER.info("Rewards granted for quest {} to {}", quest.id(), player.getName().getString());
    }

    private static void grant(QuestReward reward, ServerPlayer player, MinecraftServer server) {
        switch (reward) {
            case QuestReward.ItemReward item -> {
                var spec = item.item();
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(spec.id()), spec.count());
                // NBT apply deferred — MC 1.21.1 uses data components
                if (!player.addItem(stack)) {
                    player.drop(stack, false); // inventory full, drop on ground
                }
            }
            case QuestReward.ExperienceReward exp -> {
                player.giveExperiencePoints(exp.experience());
            }
            case QuestReward.CommandReward cmd -> {
                server.getCommands().performPrefixedCommand(
                    player.createCommandSourceStack(), cmd.command());
            }
            case QuestReward.FunctionReward func -> {
                server.getFunctions().get(func.function()).ifPresent(fn ->
                    server.getFunctions().execute(fn, player.createCommandSourceStack()));
            }
        }
    }
}
