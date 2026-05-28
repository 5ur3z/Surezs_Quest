package org.surez.surezs_quest.trigger;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
import org.surez.surezs_quest.network.packet.NPCMessagePacket;
import org.surez.surezs_quest.storage.QuestDataManager;

import java.util.Optional;

public class NPCMessageDispatcher {

    private static final Logger LOGGER = LogUtils.getLogger();

    public static void sendMessage(Player player, ResourceLocation npcId,
                                    String text, ResourceLocation attachQuest) {
        if (!(player instanceof ServerPlayer sp)) return;

        var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) return;

        var packet = new NPCMessagePacket(npcId, text,
            attachQuest != null ? Optional.of(attachQuest) : Optional.empty());

        PacketDistributor.sendToPlayer(sp, packet);

        LOGGER.debug("NPC {} sent message to {}: {}", npcId, player.getName().getString(), text);
    }
}
