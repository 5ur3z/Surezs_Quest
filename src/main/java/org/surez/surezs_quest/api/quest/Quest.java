package org.surez.surezs_quest.api.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record Quest(
    ResourceLocation id,
    ResourceLocation npcId,
    List<QuestObjective> objectives,
    List<ResourceLocation> prerequisites,
    PrerequisiteMode prerequisiteMode,
    Scope scope,
    List<QuestReward> rewards,
    RewardMode rewardMode,
    boolean canReject,
    boolean hidden,
    boolean repeatable,
    boolean autoAccept,
    int timeLimitTicks,
    Dialogue dialogue
) {
    // -- 内嵌类型 ------------------------------------------------------------

    public enum Scope {
        PLAYER, SERVER;

        public static final Codec<Scope> CODEC = Codec.STRING.xmap(
            s -> Scope.valueOf(s.toUpperCase()),
            Scope::name
        );
    }

    public enum PrerequisiteMode {
        ALL, ANY;

        public static final Codec<PrerequisiteMode> CODEC = Codec.STRING.xmap(
            s -> PrerequisiteMode.valueOf(s.toUpperCase()),
            PrerequisiteMode::name
        );
    }

    public enum RewardMode {
        PER_CONTRIBUTOR, ALL_ONLINE;

        public static final Codec<RewardMode> CODEC = Codec.STRING.xmap(
            s -> RewardMode.valueOf(s.toUpperCase()),
            RewardMode::name
        );
    }

    public record Dialogue(
        String give,
        String accept,
        String decline,
        String inProgress,
        String complete
    ) {
        public static final Codec<Dialogue> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                Codec.STRING.fieldOf("give").forGetter(Dialogue::give),
                Codec.STRING.fieldOf("accept").forGetter(Dialogue::accept),
                Codec.STRING.optionalFieldOf("decline", "").forGetter(Dialogue::decline),
                Codec.STRING.fieldOf("in_progress").forGetter(Dialogue::inProgress),
                Codec.STRING.fieldOf("complete").forGetter(Dialogue::complete)
            ).apply(instance, Dialogue::new)
        );
    }

    // -- Codec ---------------------------------------------------------------

    public static final Codec<Quest> CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(Quest::id),
            ResourceLocation.CODEC.fieldOf("npc_id").forGetter(Quest::npcId),
            QuestObjective.CODEC.listOf().fieldOf("objectives").forGetter(Quest::objectives),
            ResourceLocation.CODEC.listOf().optionalFieldOf("prerequisites", List.of()).forGetter(Quest::prerequisites),
            PrerequisiteMode.CODEC.optionalFieldOf("prerequisite_mode", PrerequisiteMode.ALL).forGetter(Quest::prerequisiteMode),
            Scope.CODEC.optionalFieldOf("scope", Scope.PLAYER).forGetter(Quest::scope),
            QuestReward.CODEC.listOf().fieldOf("rewards").forGetter(Quest::rewards),
            RewardMode.CODEC.optionalFieldOf("reward_mode", RewardMode.PER_CONTRIBUTOR).forGetter(Quest::rewardMode),
            Codec.BOOL.optionalFieldOf("can_reject", true).forGetter(Quest::canReject),
            Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Quest::hidden),
            Codec.BOOL.optionalFieldOf("repeatable", false).forGetter(Quest::repeatable),
            Codec.BOOL.optionalFieldOf("auto_accept", false).forGetter(Quest::autoAccept),
            Codec.INT.optionalFieldOf("time_limit_ticks", 0).forGetter(Quest::timeLimitTicks),
            Dialogue.CODEC.fieldOf("dialogue").forGetter(Quest::dialogue)
        ).apply(instance, Quest::new)
    );
}
