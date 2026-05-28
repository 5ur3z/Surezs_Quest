package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

public record ServerQuestProgressPacket(
    ResourceLocation questId,
    int objectiveIndex,
    int progress,
    int max
) implements CustomPacketPayload {

    public static final Type<ServerQuestProgressPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "server_quest_progress"));

    public static final StreamCodec<FriendlyByteBuf, ServerQuestProgressPacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, ServerQuestProgressPacket::questId,
        ByteBufCodecs.VAR_INT, ServerQuestProgressPacket::objectiveIndex,
        ByteBufCodecs.VAR_INT, ServerQuestProgressPacket::progress,
        ByteBufCodecs.VAR_INT, ServerQuestProgressPacket::max,
        ServerQuestProgressPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
