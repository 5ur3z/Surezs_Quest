package org.surez.surezs_quest.api.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public sealed interface QuestReward {

    String type();

    /** Icon used to render this reward in GUI. Defaults vary by type. */
    ResourceLocation iconId();

    // -- ItemStackSpec ----------------------------------------------------------

    record ItemStackSpec(
        ResourceLocation id,
        int count,
        CompoundTag nbt
    ) {
        public static final MapCodec<ItemStackSpec> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(ItemStackSpec::id),
                Codec.INT.fieldOf("count").forGetter(ItemStackSpec::count),
                CompoundTag.CODEC.optionalFieldOf("nbt", new CompoundTag()).forGetter(ItemStackSpec::nbt)
            ).apply(instance, ItemStackSpec::new)
        );
    }

    // -- ItemReward: 物品奖励 ----------------------------------------------------

    record ItemReward(ItemStackSpec item, Optional<ResourceLocation> icon) implements QuestReward {
        public String type() { return "ITEM"; }

        public ResourceLocation iconId() { return icon.orElse(item.id()); }

        public static final MapCodec<ItemReward> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ItemStackSpec.CODEC.fieldOf("item").forGetter(ItemReward::item),
                ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(ItemReward::icon)
            ).apply(instance, ItemReward::new)
        );
    }

    // -- ExperienceReward: 经验奖励 ----------------------------------------------

    record ExperienceReward(int experience, Optional<ResourceLocation> icon) implements QuestReward {
        public String type() { return "EXPERIENCE"; }

        public ResourceLocation iconId() { return icon.orElse(ResourceLocation.withDefaultNamespace("experience_bottle")); }

        public static final MapCodec<ExperienceReward> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.INT.fieldOf("experience").forGetter(ExperienceReward::experience),
                ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(ExperienceReward::icon)
            ).apply(instance, ExperienceReward::new)
        );
    }

    // -- CommandReward: 命令奖励 --------------------------------------------------

    record CommandReward(String command, Optional<ResourceLocation> icon) implements QuestReward {
        public String type() { return "COMMAND"; }

        public ResourceLocation iconId() { return icon.orElse(ResourceLocation.withDefaultNamespace("command_block")); }

        public static final MapCodec<CommandReward> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.STRING.fieldOf("command").forGetter(CommandReward::command),
                ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(CommandReward::icon)
            ).apply(instance, CommandReward::new)
        );
    }

    // -- FunctionReward: 函数奖励 -------------------------------------------------

    record FunctionReward(ResourceLocation function, Optional<ResourceLocation> icon) implements QuestReward {
        public String type() { return "FUNCTION"; }

        public ResourceLocation iconId() { return icon.orElse(ResourceLocation.withDefaultNamespace("knowledge_book")); }

        public static final MapCodec<FunctionReward> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("function").forGetter(FunctionReward::function),
                ResourceLocation.CODEC.optionalFieldOf("icon").forGetter(FunctionReward::icon)
            ).apply(instance, FunctionReward::new)
        );
    }

    // -- Dispatch -----------------------------------------------------------------

    Codec<QuestReward> CODEC = Codec.STRING.dispatch(
        QuestReward::type,
        type -> switch (type) {
            case "ITEM"       -> ItemReward.CODEC;
            case "EXPERIENCE" -> ExperienceReward.CODEC;
            case "COMMAND"    -> CommandReward.CODEC;
            case "FUNCTION"   -> FunctionReward.CODEC;
            default -> throw new IllegalArgumentException("Unknown QuestReward type: " + type);
        }
    );
}
