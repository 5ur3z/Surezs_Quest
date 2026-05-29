package org.surez.surezs_quest.storage;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlayerQuestDataTest {

    @Test
    void clearQuestRemovesOnlyOneQuestState() {
        PlayerQuestData data = new PlayerQuestData();
        ResourceLocation first = ResourceLocation.parse("surezs_quest:first");
        ResourceLocation second = ResourceLocation.parse("surezs_quest:second");

        data.accept(first);
        data.decline(first);
        data.markCompleted(first);
        data.setProgress(first, 0, 3);
        data.accept(second);
        data.setProgress(second, 0, 9);

        data.clearQuest(first);

        assertFalse(data.isAccepted(first));
        assertFalse(data.isDeclined(first));
        assertFalse(data.isCompleted(first));
        assertEquals(0, data.getProgress(first, 0));
        assertTrue(data.isAccepted(second));
        assertEquals(9, data.getProgress(second, 0));
    }

    @Test
    void clearRemovesAllQuestState() {
        PlayerQuestData data = new PlayerQuestData();
        ResourceLocation questId = ResourceLocation.parse("surezs_quest:any");

        data.accept(questId);
        data.decline(questId);
        data.markCompleted(questId);
        data.setProgress(questId, 0, 3);

        data.clear();

        assertTrue(data.acceptedQuests().isEmpty());
        assertTrue(data.declinedQuests().isEmpty());
        assertTrue(data.completedQuests().isEmpty());
        assertTrue(data.objectiveProgress().isEmpty());
    }
}
