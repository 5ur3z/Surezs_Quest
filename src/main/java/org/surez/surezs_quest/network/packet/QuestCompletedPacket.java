package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

public record QuestCompletedPacket(
    ResourceLocation questId
) implements CustomPacketPayload {

    public static final Type<QuestCompletedPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "quest_completed"));


    public static final StreamCodec<FriendlyByteBuf, QuestCompletedPacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, QuestCompletedPacket::questId,
        QuestCompletedPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
