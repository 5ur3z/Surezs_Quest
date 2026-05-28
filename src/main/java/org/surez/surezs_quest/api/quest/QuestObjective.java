package org.surez.surezs_quest.api.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public sealed interface QuestObjective {

    String type();

    // -- SubmitItems: 上交物品 -------------------------------------------------

    record SubmitItems(
        ResourceLocation item,
        int count
    ) implements QuestObjective {
        public String type() { return "submit_items"; }

        public static final MapCodec<SubmitItems> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(SubmitItems::item),
                Codec.INT.fieldOf("count").forGetter(SubmitItems::count)
            ).apply(instance, SubmitItems::new)
        );
    }

    // -- FindItems: 找到物品（不上交） -----------------------------------------

    record FindItems(
        ResourceLocation item,
        int count
    ) implements QuestObjective {
        public String type() { return "find_items"; }

        public static final MapCodec<FindItems> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(FindItems::item),
                Codec.INT.fieldOf("count").forGetter(FindItems::count)
            ).apply(instance, FindItems::new)
        );
    }

    // -- ReachLocation: 位置踩点 -----------------------------------------------

    record ReachLocation(
        ResourceLocation dimension,
        int x,
        int y,
        int z,
        int radius
    ) implements QuestObjective {
        public String type() { return "reach_location"; }

        public static final MapCodec<ReachLocation> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("dimension").forGetter(ReachLocation::dimension),
                Codec.INT.fieldOf("x").forGetter(ReachLocation::x),
                Codec.INT.fieldOf("y").forGetter(ReachLocation::y),
                Codec.INT.fieldOf("z").forGetter(ReachLocation::z),
                Codec.INT.optionalFieldOf("radius", 5).forGetter(ReachLocation::radius)
            ).apply(instance, ReachLocation::new)
        );
    }

    // -- KillEntity: 击杀实体 -------------------------------------------------

    record KillEntity(
        ResourceLocation entityType,
        int count
    ) implements QuestObjective {
        public String type() { return "kill_entity"; }

        public static final MapCodec<KillEntity> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("entity_type_id").forGetter(KillEntity::entityType),
                Codec.INT.fieldOf("count").forGetter(KillEntity::count)
            ).apply(instance, KillEntity::new)
        );
    }

    // -- CraftItem: 合成物品 ---------------------------------------------------

    record CraftItem(
        ResourceLocation item,
        int count
    ) implements QuestObjective {
        public String type() { return "craft_item"; }

        public static final MapCodec<CraftItem> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("item").forGetter(CraftItem::item),
                Codec.INT.fieldOf("count").forGetter(CraftItem::count)
            ).apply(instance, CraftItem::new)
        );
    }

    // -- 多态 Dispatch Codec ---------------------------------------------------

    Codec<QuestObjective> CODEC = Codec.STRING.dispatch(
        QuestObjective::type,
        type -> switch (type) {
            case "submit_items"   -> SubmitItems.CODEC;
            case "find_items"     -> FindItems.CODEC;
            case "reach_location" -> ReachLocation.CODEC;
            case "kill_entity"    -> KillEntity.CODEC;
            case "craft_item"     -> CraftItem.CODEC;
            default -> throw new IllegalArgumentException("Unknown QuestObjective type: " + type);
        }
    );
}
