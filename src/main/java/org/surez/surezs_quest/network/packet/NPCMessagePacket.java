package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

import java.util.Optional;

public record NPCMessagePacket(
    ResourceLocation npcId,
    String messageText,
    Optional<ResourceLocation> attachedQuestId
) implements CustomPacketPayload {

    public static final Type<NPCMessagePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "npc_message"));


    public static final StreamCodec<FriendlyByteBuf, NPCMessagePacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, NPCMessagePacket::npcId,
        ByteBufCodecs.STRING_UTF8, NPCMessagePacket::messageText,
        ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs::optional), NPCMessagePacket::attachedQuestId,
        NPCMessagePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
