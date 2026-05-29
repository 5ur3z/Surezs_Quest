package org.surez.surezs_quest.api.quest;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QuestObjectiveUtilsTest {

    @Test
    void maxProgressReturnsConfiguredCounts() {
        assertEquals(3, QuestObjectiveUtils.maxProgress(
            new QuestObjective.SubmitItems(ResourceLocation.parse("minecraft:iron_ingot"), 3)));
        assertEquals(4, QuestObjectiveUtils.maxProgress(
            new QuestObjective.FindItems(ResourceLocation.parse("minecraft:emerald"), 4)));
        assertEquals(5, QuestObjectiveUtils.maxProgress(
            new QuestObjective.KillEntity(ResourceLocation.parse("minecraft:zombie"), 5)));
        assertEquals(6, QuestObjectiveUtils.maxProgress(
            new QuestObjective.CraftItem(ResourceLocation.parse("minecraft:furnace"), 6)));
        assertEquals(1, QuestObjectiveUtils.maxProgress(
            new QuestObjective.ReachLocation(ResourceLocation.parse("minecraft:overworld"), 0, 64, 0, 8)));
    }

    @Test
    void maxProgressReturnsOneForInvalidQuestIndex() {
        Quest quest = new Quest(
            ResourceLocation.parse("surezs_quest:test"),
            ResourceLocation.parse("surezs_quest:aleksei"),
            List.of(new QuestObjective.FindItems(ResourceLocation.parse("minecraft:iron_ingot"), 2)),
            List.of(),
            Quest.PrerequisiteMode.ALL,
            Quest.Scope.PLAYER,
            List.of(),
            Quest.RewardMode.PER_CONTRIBUTOR,
            true,
            false,
            false,
            0,
            new Quest.Dialogue("give", "accept", "", "progress", "complete")
        );

        assertEquals(1, QuestObjectiveUtils.maxProgress(quest, -1));
        assertEquals(1, QuestObjectiveUtils.maxProgress(quest, 99));
        assertEquals(2, QuestObjectiveUtils.maxProgress(quest, 0));
    }

    @Test
    void targetIdIsAvailableForItemAndEntityObjectivesOnly() {
        assertEquals(ResourceLocation.parse("minecraft:iron_ingot"), QuestObjectiveUtils.targetId(
            new QuestObjective.SubmitItems(ResourceLocation.parse("minecraft:iron_ingot"), 3)).orElseThrow());
        assertEquals(ResourceLocation.parse("minecraft:zombie"), QuestObjectiveUtils.targetId(
            new QuestObjective.KillEntity(ResourceLocation.parse("minecraft:zombie"), 1)).orElseThrow());
        assertTrue(QuestObjectiveUtils.targetId(
            new QuestObjective.ReachLocation(ResourceLocation.parse("minecraft:overworld"), 0, 64, 0, 8)).isEmpty());
    }
}
