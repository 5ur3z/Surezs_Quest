package org.surez.surezs_quest.network;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.api.quest.Quest.Scope;
import org.surez.surezs_quest.api.quest.QuestObjective;
import org.surez.surezs_quest.api.quest.QuestReward;
import org.surez.surezs_quest.data.DataLoaders;
import org.surez.surezs_quest.network.packet.*;
import org.surez.surezs_quest.storage.QuestDataManager;
import org.surez.surezs_quest.trigger.QuestProgressManager;

import java.util.ArrayList;
import java.util.List;

public class NetworkHandler {

    public static final String PROTOCOL_VERSION = "1";
    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        // All S→C registrations are in ClientNetworkHandler (client-only)

        // -- C→S packets ----------------------------------------------------------
        registrar.playToServer(
            RequestOpenQuestScreenPacket.TYPE, RequestOpenQuestScreenPacket.STREAM_CODEC,
            (packet, ctx) -> ctx.enqueueWork(() -> handleOpenScreen(packet, ctx.player()))
        );
        registrar.playToServer(
            AcceptQuestPacket.TYPE, AcceptQuestPacket.STREAM_CODEC,
            (packet, ctx) -> ctx.enqueueWork(() -> handleAccept(packet, ctx.player()))
        );
        registrar.playToServer(
            DeclineQuestPacket.TYPE, DeclineQuestPacket.STREAM_CODEC,
            (packet, ctx) -> ctx.enqueueWork(() -> handleDecline(packet, ctx.player()))
        );
        registrar.playToServer(
            ClaimRewardPacket.TYPE, ClaimRewardPacket.STREAM_CODEC,
            (packet, ctx) -> ctx.enqueueWork(() -> handleClaim(packet, ctx.player()))
        );
        registrar.playToServer(
            RequestSubmitItemsPacket.TYPE, RequestSubmitItemsPacket.STREAM_CODEC,
            (packet, ctx) -> ctx.enqueueWork(() -> handleRequestSubmit(packet, ctx.player()))
        );
        registrar.playToServer(
            ConfirmSubmitPacket.TYPE, ConfirmSubmitPacket.STREAM_CODEC,
            (packet, ctx) -> ctx.enqueueWork(() -> handleConfirmSubmit(packet, ctx.player()))
        );
    }

    // -- Screen handler ---------------------------------------------------------

    private static void handleOpenScreen(RequestOpenQuestScreenPacket packet, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) return;

        var npcIds = new ArrayList<>(DataLoaders.NPCS.getAll().stream().map(npc -> npc.id()).toList());
        var npcNames = new ArrayList<>(DataLoaders.NPCS.getAll().stream().map(npc -> npc.id().getPath()).toList());
        var accepted = new ArrayList<ResourceLocation>();
        var completed = new ArrayList<ResourceLocation>();
        var declined = new ArrayList<ResourceLocation>();

        // only include PLAYER scope quests from PlayerQuestData
        for (var id : data.acceptedQuests()) {
            if (!DataLoaders.QUESTS.exists(id) || DataLoaders.QUESTS.get(id).scope() == Scope.PLAYER)
                accepted.add(id);
        }
        for (var id : data.completedQuests()) {
            if (!DataLoaders.QUESTS.exists(id) || DataLoaders.QUESTS.get(id).scope() == Scope.PLAYER)
                completed.add(id);
        }
        for (var id : data.declinedQuests()) {
            if (!DataLoaders.QUESTS.exists(id) || DataLoaders.QUESTS.get(id).scope() == Scope.PLAYER)
                declined.add(id);
        }

        // merge SERVER quest state from ServerQuestData
        var serverData = QuestDataManager.INSTANCE.getServerData();
        for (Quest q : DataLoaders.QUESTS.getAll()) {
            if (q.scope() != Scope.SERVER) continue;
            var sid = q.id();
            if (serverData.hasPlayerCompleted(sid, player.getUUID())) {
                completed.add(sid);
            } else if (serverData.isDeclined(sid, player.getUUID())) {
                declined.add(sid);
            } else if (serverData.areObjectivesMet(sid, q)
                && (serverData.isAccepted(sid, player.getUUID())
                    || serverData.hasContributed(sid, player.getUUID()))) {
                // objectives met and player participated → GUI shows "可领取"
            } else if (serverData.isAccepted(sid, player.getUUID())) {
                accepted.add(sid);
            } // else: not in any state → GUI shows "可接取" by default
        }

        var questInfos = new ArrayList<OpenQuestScreenPacket.QuestInfo>();
        for (Quest q : DataLoaders.QUESTS.getAll()) {
            var objs = new ArrayList<OpenQuestScreenPacket.ObjectiveInfo>();
            for (var obj : q.objectives()) {
                String type = obj.type();
                ResourceLocation item = switch (obj) {
                    case QuestObjective.FindItems f -> f.item();
                    case QuestObjective.SubmitItems s -> s.item();
                    case QuestObjective.KillEntity k -> k.entityType();
                    case QuestObjective.CraftItem c -> c.item();
                    case QuestObjective.ReachLocation r -> ResourceLocation.parse("minecraft:air");
                };
                int count = switch (obj) {
                    case QuestObjective.FindItems f -> f.count();
                    case QuestObjective.SubmitItems s -> s.count();
                    case QuestObjective.KillEntity k -> k.count();
                    case QuestObjective.CraftItem c -> c.count();
                    case QuestObjective.ReachLocation r -> 1;
                };
                objs.add(new OpenQuestScreenPacket.ObjectiveInfo(type, item, count));
            }
            var rewardText = new StringBuilder();
            var rewardItems = new StringBuilder();
            for (var rw : q.rewards()) {
                if (!rewardText.isEmpty()) rewardText.append(", ");
                switch (rw) {
                    case QuestReward.ItemReward ir -> {
                        rewardText.append(ir.item().id().getPath()).append("x").append(ir.item().count());
                        appendItem(rewardItems, ir.iconId(), ir.item().count());
                    }
                    case QuestReward.ExperienceReward er -> {
                        rewardText.append(er.experience()).append(Component.translatable("surezs_quest.reward.exp").getString());
                        appendItem(rewardItems, er.iconId(), er.experience());
                    }
                    case QuestReward.CommandReward cr -> {
                        rewardText.append(Component.translatable("surezs_quest.reward.command").getString());
                        appendItem(rewardItems, cr.iconId(), 1);
                    }
                    case QuestReward.FunctionReward fr -> {
                        rewardText.append(Component.translatable("surezs_quest.reward.function").getString());
                        appendItem(rewardItems, fr.iconId(), 1);
                    }
                }
            }
            byte flags = (byte)((q.canReject() ? 1 : 0) | (q.hidden() ? 2 : 0));
            String fullDesc = q.dialogue().give() + "\0"
                + q.dialogue().accept() + "\0"
                + q.dialogue().decline() + "\0"
                + q.dialogue().complete();
            String rewardStr = rewardText + (rewardItems.isEmpty() ? "" : "||" + rewardItems);
            questInfos.add(new OpenQuestScreenPacket.QuestInfo(
                q.id(), q.npcId(), objs, flags, rewardStr, fullDesc));
        }

        // hidden quests whose prerequisites are all met → visible
        var visibleHidden = new ArrayList<ResourceLocation>();
        for (Quest q : DataLoaders.QUESTS.getAll()) {
            if (q.hidden() && !q.prerequisites().isEmpty()
                && q.prerequisites().stream().allMatch(data::isCompleted)) {
                visibleHidden.add(q.id());
            }
        }
        var status = new OpenQuestScreenPacket.StatusLists(accepted, completed, declined, visibleHidden);
        PacketDistributor.sendToPlayer(sp, new OpenQuestScreenPacket(npcIds, npcNames, status, questInfos));

        // send current server quest progress so new players see correct values
        for (Quest q : DataLoaders.QUESTS.getAll()) {
            if (q.scope() != Scope.SERVER) continue;
            for (int i = 0; i < q.objectives().size(); i++) {
                int progress = serverData.getProgress(q.id(), i);
                int max = QuestProgressManager.getObjectiveMax(q, i);
                PacketDistributor.sendToPlayer(sp, new ServerQuestProgressPacket(q.id(), i, progress, max));
            }
        }
    }

    // -- Quest action handlers --------------------------------------------------

    private static void handleAccept(AcceptQuestPacket packet, net.minecraft.world.entity.player.Player player) {
        var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) return;
        Quest quest = DataLoaders.QUESTS.get(packet.questId());
        if (quest == null) return;

        if (quest.scope() == Scope.SERVER) {
            var serverData = QuestDataManager.INSTANCE.getServerData();
            if (serverData.areObjectivesMet(packet.questId(), quest)
                || serverData.isAccepted(packet.questId(), player.getUUID())
                || serverData.isDeclined(packet.questId(), player.getUUID())
                || serverData.hasPlayerCompleted(packet.questId(), player.getUUID())) return;
            serverData.accept(packet.questId(), player.getUUID());
            QuestDataManager.INSTANCE.saveServer();
        } else {
            if (data.isAccepted(packet.questId())) return;
            data.accept(packet.questId());
            QuestDataManager.INSTANCE.savePlayer(player.getUUID());

            for (int i = 0; i < quest.objectives().size(); i++) {
                if (quest.objectives().get(i) instanceof QuestObjective.FindItems fi) {
                    int held = 0;
                    for (var stack : player.getInventory().items) {
                        if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(fi.item())) {
                            held += stack.getCount();
                        }
                    }
                    if (held > 0) {
                        int capped = Math.min(held, fi.count());
                        QuestProgressManager.updateProgress(player, data, packet.questId(), i, capped);
                    }
                }
            }
        }

        LOGGER.debug("Player {} accepted quest {}", player.getName().getString(), packet.questId());
    }

    private static void handleDecline(DeclineQuestPacket packet, net.minecraft.world.entity.player.Player player) {
        var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) return;
        Quest quest = DataLoaders.QUESTS.get(packet.questId());
        if (quest == null) return;

        if (quest.scope() == Scope.SERVER) {
            var serverData = QuestDataManager.INSTANCE.getServerData();
            if (serverData.isDeclined(packet.questId(), player.getUUID())
                || serverData.areObjectivesMet(packet.questId(), quest)
                || serverData.hasPlayerCompleted(packet.questId(), player.getUUID())) return;
            serverData.decline(packet.questId(), player.getUUID());
            QuestDataManager.INSTANCE.saveServer();
        } else {
            data.decline(packet.questId());
            QuestDataManager.INSTANCE.savePlayer(player.getUUID());
        }

        LOGGER.debug("Player {} declined quest {}", player.getName().getString(), packet.questId());
    }

    private static void handleClaim(ClaimRewardPacket packet, net.minecraft.world.entity.player.Player player) {
        var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) return;
        Quest quest = DataLoaders.QUESTS.get(packet.questId());
        if (quest == null) return;

        if (quest.scope() == Scope.SERVER) {
            var serverData = QuestDataManager.INSTANCE.getServerData();
            if (!serverData.areObjectivesMet(packet.questId(), quest)) return;
            if (serverData.hasPlayerCompleted(packet.questId(), player.getUUID())) return;
            if (quest.rewardMode() == Quest.RewardMode.PER_CONTRIBUTOR
                && !serverData.isAccepted(packet.questId(), player.getUUID())
                && !serverData.hasContributed(packet.questId(), player.getUUID())) return;
            org.surez.surezs_quest.reward.QuestRewardDispatcher.grantRewards(quest, player, data, true);
            serverData.markCompleted(packet.questId(), player.getUUID());
            QuestDataManager.INSTANCE.saveServer();
            LOGGER.debug("Player {} claimed server quest {}", player.getName().getString(), packet.questId());
        } else {
            if (data.isCompleted(packet.questId())) return;
            org.surez.surezs_quest.reward.QuestRewardDispatcher.grantRewards(quest, player, data);
            data.acceptedQuests().remove(packet.questId());
            QuestDataManager.INSTANCE.savePlayer(player.getUUID());
        }
    }

    private static void appendItem(StringBuilder sb, ResourceLocation iconId, int count) {
        if (!sb.isEmpty()) sb.append(",");
        sb.append(iconId.toString()).append(":").append(count);
    }

    // -- Submit handlers --------------------------------------------------------

    private static void handleRequestSubmit(RequestSubmitItemsPacket packet, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) return;

        Quest quest = DataLoaders.QUESTS.get(packet.questId());
        if (quest == null) return;

        if (quest.scope() == Scope.SERVER) {
            var serverData = QuestDataManager.INSTANCE.getServerData();
            if (!serverData.isAccepted(packet.questId(), player.getUUID())) return;
        } else {
            if (!data.isAccepted(packet.questId())) return;
        }

        List<OpenSubmitScreenPacket.SlotItem> valid = new ArrayList<>();
        for (var obj : quest.objectives()) {
            if (obj instanceof QuestObjective.SubmitItems submit) {
                ResourceLocation target = submit.item();
                for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
                    ItemStack stack = player.getInventory().items.get(slot);
                    if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(target)) {
                        valid.add(new OpenSubmitScreenPacket.SlotItem(slot, target, stack.getCount()));
                    }
                }
            }
        }

        PacketDistributor.sendToPlayer(sp, new OpenSubmitScreenPacket(packet.questId(), valid));
        LOGGER.debug("Sent submit screen for {} with {} matching slots", packet.questId(), valid.size());
    }

    private static void handleConfirmSubmit(ConfirmSubmitPacket packet, net.minecraft.world.entity.player.Player player) {
        if (!(player instanceof ServerPlayer sp)) return;
        var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) return;

        Quest quest = DataLoaders.QUESTS.get(packet.questId());
        if (quest == null) return;

        if (quest.scope() == Scope.SERVER) {
            var serverData = QuestDataManager.INSTANCE.getServerData();
            if (!serverData.isAccepted(packet.questId(), player.getUUID())) return;
        } else {
            if (!data.isAccepted(packet.questId())) return;
        }

        for (int slot : packet.slotIndices()) {
            if (slot < 0 || slot >= player.getInventory().items.size()) continue;
            ItemStack stack = player.getInventory().items.get(slot);
            if (stack.isEmpty()) continue;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());

            for (int i = 0; i < quest.objectives().size(); i++) {
                if (quest.objectives().get(i) instanceof QuestObjective.SubmitItems submit) {
                    if (submit.item().equals(itemId)) {
                        if (quest.scope() == Scope.SERVER) {
                            var serverData = QuestDataManager.INSTANCE.getServerData();
                            int serverProgress = serverData.getProgress(packet.questId(), i);
                            int toTake = Math.min(stack.getCount(), submit.count() - serverProgress);
                            if (toTake > 0) {
                                stack.shrink(toTake);
                                QuestProgressManager.updateProgress(player, data, packet.questId(), i, toTake);
                                LOGGER.debug("Submitted {}x {} for server quest {}", toTake, itemId, packet.questId());
                            }
                        } else {
                            int toTake = Math.min(stack.getCount(), submit.count() - data.getProgress(packet.questId(), i));
                            if (toTake > 0) {
                                stack.shrink(toTake);
                                int newValue = data.getProgress(packet.questId(), i) + toTake;
                                QuestProgressManager.updateProgress(player, data, packet.questId(), i, newValue);
                                LOGGER.debug("Submitted {}x {} for quest {}", toTake, itemId, packet.questId());
                            }
                        }
                    }
                }
            }
        }
        QuestDataManager.INSTANCE.savePlayer(player.getUUID());
    }
}
