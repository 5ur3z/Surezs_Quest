package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

public record RequestSubmitItemsPacket(
    ResourceLocation questId
) implements CustomPacketPayload {

    public static final Type<RequestSubmitItemsPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "request_submit_items"));


    public static final StreamCodec<FriendlyByteBuf, RequestSubmitItemsPacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, RequestSubmitItemsPacket::questId,
        RequestSubmitItemsPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
