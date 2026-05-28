package org.surez.surezs_quest.api.trigger;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

public sealed interface QuestTrigger {

    String type();

    // -- 进度类触发器 --------------------------------------------------------

    record InventoryChanged() implements QuestTrigger {
        public String type() { return "inventory_changed"; }
        public static final MapCodec<InventoryChanged> CODEC = MapCodec.unit(new InventoryChanged());
    }

    record PlayerTick(int intervalTicks) implements QuestTrigger {
        public String type() { return "player_tick"; }
        public static final MapCodec<PlayerTick> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.INT.optionalFieldOf("interval_ticks", 20).forGetter(PlayerTick::intervalTicks)
            ).apply(instance, PlayerTick::new)
        );
    }

    record EntityKill(ResourceLocation entityType, int count) implements QuestTrigger {
        public String type() { return "entity_kill"; }
        public static final MapCodec<EntityKill> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("entity_type").forGetter(EntityKill::entityType),
                Codec.INT.fieldOf("count").forGetter(EntityKill::count)
            ).apply(instance, EntityKill::new)
        );
    }

    record ItemCrafted(ResourceLocation itemId, int count) implements QuestTrigger {
        public String type() { return "item_crafted"; }
        public static final MapCodec<ItemCrafted> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("item_id").forGetter(ItemCrafted::itemId),
                Codec.INT.fieldOf("count").forGetter(ItemCrafted::count)
            ).apply(instance, ItemCrafted::new)
        );
    }

    // -- 消息类触发器 --------------------------------------------------------

    record QuestCompleted(ResourceLocation questId) implements QuestTrigger {
        public String type() { return "quest_completed"; }
        public static final MapCodec<QuestCompleted> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("quest_id").forGetter(QuestCompleted::questId)
            ).apply(instance, QuestCompleted::new)
        );
    }

    record LocationEnter(
        ResourceLocation dimension, int x, int y, int z, int radius
    ) implements QuestTrigger {
        public String type() { return "location_enter"; }
        public static final MapCodec<LocationEnter> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("dimension").forGetter(LocationEnter::dimension),
                Codec.INT.fieldOf("x").forGetter(LocationEnter::x),
                Codec.INT.fieldOf("y").forGetter(LocationEnter::y),
                Codec.INT.fieldOf("z").forGetter(LocationEnter::z),
                Codec.INT.optionalFieldOf("radius", 5).forGetter(LocationEnter::radius)
            ).apply(instance, LocationEnter::new)
        );
    }

    // -- Dispatch ------------------------------------------------------------

    Codec<QuestTrigger> CODEC = Codec.STRING.dispatch(
        QuestTrigger::type,
        type -> switch (type) {
            case "inventory_changed" -> InventoryChanged.CODEC;
            case "player_tick"       -> PlayerTick.CODEC;
            case "entity_kill"       -> EntityKill.CODEC;
            case "item_crafted"      -> ItemCrafted.CODEC;
            case "quest_completed"   -> QuestCompleted.CODEC;
            case "location_enter"    -> LocationEnter.CODEC;
            default -> throw new IllegalArgumentException("Unknown QuestTrigger type: " + type);
        }
    );
}
