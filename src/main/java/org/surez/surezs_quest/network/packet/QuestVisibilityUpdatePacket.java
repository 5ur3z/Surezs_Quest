package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

import java.util.List;

public record QuestVisibilityUpdatePacket(
    List<ResourceLocation> questIds,
    List<ResourceLocation> removedIds
) implements CustomPacketPayload {

    public QuestVisibilityUpdatePacket(List<ResourceLocation> questIds) {
        this(questIds, List.of());
    }

    public static final Type<QuestVisibilityUpdatePacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "quest_visibility_update"));

    public static final StreamCodec<FriendlyByteBuf, QuestVisibilityUpdatePacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), QuestVisibilityUpdatePacket::questIds,
        ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), QuestVisibilityUpdatePacket::removedIds,
        QuestVisibilityUpdatePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
