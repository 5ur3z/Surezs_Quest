package org.surez.surezs_quest.network;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.surez.surezs_quest.Surezs_quest;
import org.surez.surezs_quest.network.packet.*;
import org.surez.surezs_quest.screen.ClientQuestData;
import org.surez.surezs_quest.screen.ClientQuestDataCache;
import org.surez.surezs_quest.screen.SubmitItemScreen;

import java.util.HashSet;

@EventBusSubscriber(modid = Surezs_quest.MODID, value = Dist.CLIENT)
public class ClientNetworkHandler {

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(NetworkHandler.PROTOCOL_VERSION);

        // Server quest broadcast
        registrar.playToClient(
            ServerQuestProgressPacket.TYPE, ServerQuestProgressPacket.STREAM_CODEC,
            (packet, ctx) -> ctx.enqueueWork(() ->
                ClientQuestDataCache.INSTANCE.updateProgress(
                    packet.questId(), packet.objectiveIndex(), packet.progress()))
        );

        // Open quest screen
        registrar.playToClient(
            OpenQuestScreenPacket.TYPE, OpenQuestScreenPacket.STREAM_CODEC,
            (packet, ctx) -> ctx.enqueueWork(() -> {
                ClientQuestData.loadFromServer(packet.questInfos());
                ClientQuestDataCache.INSTANCE.clearStatusFlags();
                ClientQuestDataCache.INSTANCE.setNpcIds(packet.npcIds());
                ClientQuestDataCache.INSTANCE.setNpcNames(packet.npcNames());
                ClientQuestDataCache.INSTANCE.setAcceptedQuests(new HashSet<>(packet.status().accepted()));
                ClientQuestDataCache.INSTANCE.setCompletedQuests(new HashSet<>(packet.status().completed()));
                ClientQuestDataCache.INSTANCE.setDeclinedQuests(new HashSet<>(packet.status().declined()));
                ClientQuestDataCache.INSTANCE.setVisibleHiddenQuests(new HashSet<>(packet.status().visibleHidden()));
            })
        );

        // Progress update
        registrar.playToClient(
            QuestProgressUpdatePacket.TYPE, QuestProgressUpdatePacket.STREAM_CODEC,
            (packet, ctx) -> ctx.enqueueWork(() ->
                ClientQuestDataCache.INSTANCE.updateProgress(
                    packet.questId(), packet.objectiveIndex(), packet.progress()))
        );

        // Quest visibility update
        registrar.playToClient(
            QuestVisibilityUpdatePacket.TYPE, QuestVisibilityUpdatePacket.STREAM_CODEC,
            (packet, ctx) -> ctx.enqueueWork(() -> {
                ClientQuestDataCache.INSTANCE.addVisibleHiddenQuests(packet.questIds());
                packet.removedIds().forEach(id -> {
                    ClientQuestDataCache.INSTANCE.removeVisibleHiddenQuest(id);
                    ClientQuestDataCache.INSTANCE.clearQuestState(id);
                });
            })
        );

        // Open submit screen
        registrar.playToClient(
            OpenSubmitScreenPacket.TYPE, OpenSubmitScreenPacket.STREAM_CODEC,
            (packet, ctx) -> ctx.enqueueWork(() -> {
                ClientQuestDataCache.INSTANCE.setPendingSubmit(packet.questId(), packet.validItems());
                Minecraft.getInstance().setScreen(new SubmitItemScreen(packet.questId(), packet.validItems()));
            })
        );
    }
}
