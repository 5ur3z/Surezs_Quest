package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

public record DeclineQuestPacket(
    ResourceLocation questId
) implements CustomPacketPayload {

    public static final Type<DeclineQuestPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "decline_quest"));


    public static final StreamCodec<FriendlyByteBuf, DeclineQuestPacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, DeclineQuestPacket::questId,
        DeclineQuestPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
