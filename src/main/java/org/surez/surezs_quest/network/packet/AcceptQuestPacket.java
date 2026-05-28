package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

public record AcceptQuestPacket(
    ResourceLocation questId
) implements CustomPacketPayload {

    public static final Type<AcceptQuestPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "accept_quest"));


    public static final StreamCodec<FriendlyByteBuf, AcceptQuestPacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, AcceptQuestPacket::questId,
        AcceptQuestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
