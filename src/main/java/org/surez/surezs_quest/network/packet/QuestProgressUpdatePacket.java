package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

public record QuestProgressUpdatePacket(
    ResourceLocation questId,
    int objectiveIndex,
    int progress,
    int max
) implements CustomPacketPayload {

    public static final Type<QuestProgressUpdatePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "quest_progress_update"));


    public static final StreamCodec<FriendlyByteBuf, QuestProgressUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, QuestProgressUpdatePacket::questId,
        ByteBufCodecs.VAR_INT, QuestProgressUpdatePacket::objectiveIndex,
        ByteBufCodecs.VAR_INT, QuestProgressUpdatePacket::progress,
        ByteBufCodecs.VAR_INT, QuestProgressUpdatePacket::max,
        QuestProgressUpdatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
