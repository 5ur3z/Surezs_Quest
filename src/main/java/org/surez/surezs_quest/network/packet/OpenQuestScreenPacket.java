package org.surez.surezs_quest.network.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Surezs_quest;

import java.util.List;

public record OpenQuestScreenPacket(
    List<ResourceLocation> npcIds,
    List<String> npcNames,
    StatusLists status,
    List<QuestInfo> questInfos
) implements CustomPacketPayload {

    /** Bundled player status lists (avoids exceeding StreamCodec.composite 6-field limit) */
    public record StatusLists(
        List<ResourceLocation> accepted,
        List<ResourceLocation> completed,
        List<ResourceLocation> declined,
        List<ResourceLocation> visibleHidden
    ) {
        static final StreamCodec<FriendlyByteBuf, StatusLists> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), StatusLists::accepted,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), StatusLists::completed,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), StatusLists::declined,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), StatusLists::visibleHidden,
            StatusLists::new
        );
    }

    /** Bundled display strings (avoids exceeding 6-field StreamCodec limit) */
    public record TextData(String rewardText, String description) {
        static final StreamCodec<FriendlyByteBuf, TextData> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, TextData::rewardText,
            ByteBufCodecs.STRING_UTF8, TextData::description,
            TextData::new
        );
    }

    /** Lightweight quest summary sent from server to client */
    public record QuestInfo(
        ResourceLocation id,
        ResourceLocation npcId,
        List<ObjectiveInfo> objectives,
        List<ResourceLocation> prerequisites,
        byte flags, // bit 0: canReject
        TextData texts
    ) {
        public boolean canReject() { return (flags & 1) != 0; }

        static final StreamCodec<FriendlyByteBuf, QuestInfo> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, QuestInfo::id,
            ResourceLocation.STREAM_CODEC, QuestInfo::npcId,
            ObjectiveInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), QuestInfo::objectives,
            ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), QuestInfo::prerequisites,
            ByteBufCodecs.BYTE, QuestInfo::flags,
            TextData.STREAM_CODEC, QuestInfo::texts,
            QuestInfo::new
        );
    }

    public record ObjectiveInfo(String type, ResourceLocation item, int count) {
        static final StreamCodec<FriendlyByteBuf, ObjectiveInfo> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, ObjectiveInfo::type,
            ResourceLocation.STREAM_CODEC, ObjectiveInfo::item,
            ByteBufCodecs.VAR_INT, ObjectiveInfo::count,
            ObjectiveInfo::new
        );
    }

    public static final Type<OpenQuestScreenPacket> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "open_quest_screen"));

    public static final StreamCodec<FriendlyByteBuf, OpenQuestScreenPacket> STREAM_CODEC = StreamCodec.composite(
        ResourceLocation.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenQuestScreenPacket::npcIds,
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), OpenQuestScreenPacket::npcNames,
        StatusLists.STREAM_CODEC, OpenQuestScreenPacket::status,
        QuestInfo.STREAM_CODEC.apply(ByteBufCodecs.list()), OpenQuestScreenPacket::questInfos,
        OpenQuestScreenPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
