package org.surez.surezs_quest.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.api.quest.Quest;

import java.util.ArrayList;
import java.util.List;

public class QuestListPanel {

    private static final int GAP = 2;

    private final int x, y, width, height;
    private int scrollOffset;

    public QuestListPanel(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY) {
        var activeId = ClientQuestDataCache.INSTANCE.activeNpcId();
        if (activeId == null) return;

        List<Quest> npcQuests = getVisibleQuests(activeId);
        if (npcQuests.isEmpty()) {
            gfx.drawString(mc().font, "该 NPC 暂无可用任务", x + 4, y + 4, 0xFF_888888);
            return;
        }

        // calculate total height for scroll clamping
        int totalH = 0;
        for (Quest quest : npcQuests) {
            totalH += QuestCardWidget.getHeight(quest.id()) + GAP;
        }
        int maxScroll = Math.max(0, totalH - height + 12);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int cy = y - scrollOffset;
        for (Quest quest : npcQuests) {
            int cardH = QuestCardWidget.render(gfx, mc().font, x + 4, cy, width - 8, quest.id());
            cy += cardH + GAP;
            if (cy > y + height) break;
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false;

        var activeId = ClientQuestDataCache.INSTANCE.activeNpcId();
        if (activeId == null) return false;

        List<Quest> npcQuests = getVisibleQuests(activeId);
        // find which card was clicked
        int cy = y - scrollOffset;
        for (Quest quest : npcQuests) {
            int cardH = QuestCardWidget.getHeight(quest.id());
            if (mouseY >= cy && mouseY <= cy + cardH) {
                return QuestCardWidget.mouseClicked(mouseX, mouseY, x + 4, cy, width - 8, quest.id());
            }
            cy += cardH + GAP;
        }
        return false;
    }

    public void mouseMoved(double mouseX, double mouseY) {
        var activeId = ClientQuestDataCache.INSTANCE.activeNpcId();
        if (activeId == null) return;
        List<Quest> npcQuests = getVisibleQuests(activeId);
        int cy = y - scrollOffset;
        for (Quest quest : npcQuests) {
            int cardH = QuestCardWidget.getHeight(quest.id());
            if (mouseY >= cy && mouseY <= cy + cardH) {
                QuestCardWidget.updateHover(mouseX, mouseY, x + 4, cy, width - 8, quest.id());
                return;
            }
            cy += cardH + GAP;
        }
        QuestCardWidget.hoveredQuestId = null;
        QuestCardWidget.hoveredButton = 0;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX < x || mouseX > x + width) return false;
        scrollOffset = Math.max(0, scrollOffset - (int)(deltaY * 20)); // upper clamp done in render()
        return true;
    }

    private List<Quest> getVisibleQuests(ResourceLocation npcId) {
        List<Quest> result = new ArrayList<>();
        var cache = ClientQuestDataCache.INSTANCE;
        for (Quest q : ClientQuestData.getAll()) {
            if (!q.npcId().equals(npcId)) continue;
            // hide quests marked hidden, unless player has already interacted
            if (ClientQuestData.isHidden(q.id())
                && !cache.isAccepted(q.id())
                && !cache.isVisibleHidden(q.id())
                && !cache.isDeclined(q.id())
                && !cache.areObjectivesMet(q.id())) {
                continue;
            }
            result.add(q);
        }
        return result;
    }

    private boolean isCompletedLocally(Quest quest) {
        var cache = ClientQuestDataCache.INSTANCE;
        for (int i = 0; i < quest.objectives().size(); i++) {
            int max = QuestCardWidget.objectiveMax(quest.objectives().get(i));
            if (cache.getProgress(quest.id(), i) < max) return false;
        }
        return quest.objectives().isEmpty() ? false : cache.isAccepted(quest.id());
    }

    private String getProgressLine(Quest quest) {
        var cache = ClientQuestDataCache.INSTANCE;
        if (!cache.isAccepted(quest.id())) return "";
        var sb = new StringBuilder();
        for (int i = 0; i < quest.objectives().size(); i++) {
            if (i > 0) sb.append(" | ");
            int max = QuestCardWidget.objectiveMax(quest.objectives().get(i));
            int cur = cache.getProgress(quest.id(), i);
            String label = switch (quest.objectives().get(i)) {
                case org.surez.surezs_quest.api.quest.QuestObjective.FindItems f -> f.item().getPath();
                case org.surez.surezs_quest.api.quest.QuestObjective.SubmitItems s -> s.item().getPath();
                case org.surez.surezs_quest.api.quest.QuestObjective.KillEntity k -> k.entityType().getPath();
                case org.surez.surezs_quest.api.quest.QuestObjective.CraftItem c -> c.item().getPath();
                case org.surez.surezs_quest.api.quest.QuestObjective.ReachLocation r -> "到达";
            };
            sb.append(label).append(" ").append(cur).append("/").append(max);
        }
        return sb.toString();
    }

    private static net.minecraft.client.Minecraft mc() {
        return net.minecraft.client.Minecraft.getInstance();
    }
}
