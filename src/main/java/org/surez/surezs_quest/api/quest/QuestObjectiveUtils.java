package org.surez.surezs_quest.api.quest;

import net.minecraft.resources.ResourceLocation;

import java.util.Optional;

public final class QuestObjectiveUtils {

    private QuestObjectiveUtils() {}

    public static int maxProgress(Quest quest, int index) {
        if (index < 0 || index >= quest.objectives().size()) return 1;
        return maxProgress(quest.objectives().get(index));
    }

    public static int maxProgress(QuestObjective objective) {
        return switch (objective) {
            case QuestObjective.ReachLocation ignored -> 1;
            case QuestObjective.FindItems findItems -> findItems.count();
            case QuestObjective.SubmitItems submitItems -> submitItems.count();
            case QuestObjective.KillEntity killEntity -> killEntity.count();
            case QuestObjective.CraftItem craftItem -> craftItem.count();
        };
    }

    public static Optional<ResourceLocation> targetId(QuestObjective objective) {
        return switch (objective) {
            case QuestObjective.FindItems findItems -> Optional.of(findItems.item());
            case QuestObjective.SubmitItems submitItems -> Optional.of(submitItems.item());
            case QuestObjective.KillEntity killEntity -> Optional.of(killEntity.entityType());
            case QuestObjective.CraftItem craftItem -> Optional.of(craftItem.item());
            case QuestObjective.ReachLocation ignored -> Optional.empty();
        };
    }

    public static boolean requiresManualSubmit(QuestObjective objective) {
        return objective instanceof QuestObjective.SubmitItems;
    }
}
