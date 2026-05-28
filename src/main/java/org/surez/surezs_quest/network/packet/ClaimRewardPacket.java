package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

public record ClaimRewardPacket(
    ResourceLocation questId
) implements CustomPacketPayload {

    public static final Type<ClaimRewardPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "claim_reward"));


    public static final StreamCodec<FriendlyByteBuf, ClaimRewardPacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, ClaimRewardPacket::questId,
        ClaimRewardPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
