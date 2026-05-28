package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

import java.util.List;

public record OpenSubmitScreenPacket(
    ResourceLocation questId,
    List<SlotItem> validItems
) implements CustomPacketPayload {

    public record SlotItem(int slotIndex, ResourceLocation itemId, int count) {
        private static final StreamCodec<FriendlyByteBuf, ResourceLocation> RL_STREAM =
            StreamCodec.of(FriendlyByteBuf::writeResourceLocation, FriendlyByteBuf::readResourceLocation);

        static final StreamCodec<FriendlyByteBuf, SlotItem> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SlotItem::slotIndex,
            RL_STREAM, SlotItem::itemId,
            ByteBufCodecs.VAR_INT, SlotItem::count,
            SlotItem::new
        );
    }

    public static final Type<OpenSubmitScreenPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "open_submit_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenSubmitScreenPacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, OpenSubmitScreenPacket::questId,
        SlotItem.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenSubmitScreenPacket::validItems,
        OpenSubmitScreenPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
