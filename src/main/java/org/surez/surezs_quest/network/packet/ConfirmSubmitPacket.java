package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

import java.util.List;

public record ConfirmSubmitPacket(
    ResourceLocation questId,
    List<Integer> slotIndices
) implements CustomPacketPayload {

    public static final Type<ConfirmSubmitPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "confirm_submit"));


    public static final StreamCodec<FriendlyByteBuf, ConfirmSubmitPacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, ConfirmSubmitPacket::questId,
        ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), ConfirmSubmitPacket::slotIndices,
        ConfirmSubmitPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
