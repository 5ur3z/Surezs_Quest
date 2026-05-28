package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

public record RequestOpenQuestScreenPacket() implements CustomPacketPayload {

    public static final Type<RequestOpenQuestScreenPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "request_open_quest_screen"));

    public static final StreamCodec<FriendlyByteBuf, RequestOpenQuestScreenPacket> STREAM_CODEC =
        StreamCodec.unit(new RequestOpenQuestScreenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
