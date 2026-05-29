package org.surez.surezs_quest.trigger.handlers;

import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.api.quest.QuestObjective;
import org.surez.surezs_quest.data.DataLoaders;
import org.surez.surezs_quest.storage.PlayerQuestData;

import java.util.function.Consumer;

final class AcceptedObjectiveWalker {

    private AcceptedObjectiveWalker() {}

    record Match(ResourceLocation questId, Quest quest, int objectiveIndex, QuestObjective objective) {}

    static void forEach(PlayerQuestData data, Consumer<Match> consumer) {
        for (var questId : data.acceptedQuests()) {
            Quest quest = DataLoaders.QUESTS.get(questId);
            if (quest == null) continue;

            for (int i = 0; i < quest.objectives().size(); i++) {
                consumer.accept(new Match(questId, quest, i, quest.objectives().get(i)));
            }
        }
    }
}
