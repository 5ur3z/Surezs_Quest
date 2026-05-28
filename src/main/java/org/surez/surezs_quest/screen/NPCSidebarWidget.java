package org.surez.surezs_quest.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public class NPCSidebarWidget {

    public static final int WIDTH = 120;
    private static final int ENTRY_HEIGHT = 24;
    private static final int PADDING = 4;

    private int scrollOffset;
    private final int x, y, height;

    public NPCSidebarWidget(int x, int y, int height) {
        this.x = x;
        this.y = y;
        this.height = height;
    }

    public void render(GuiGraphics gfx, int mouseX, int mouseY) {
        var npcs = ClientQuestDataCache.INSTANCE.npcIds();
        var activeId = ClientQuestDataCache.INSTANCE.activeNpcId();

        int maxScroll = Math.max(0, npcs.size() * ENTRY_HEIGHT - height);
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
        if (scrollOffset < 0) scrollOffset = 0;

        int startIndex = scrollOffset / ENTRY_HEIGHT;
        int offsetY = y - (scrollOffset % ENTRY_HEIGHT);

        for (int i = startIndex; i < npcs.size(); i++) {
            int entryY = offsetY + (i - startIndex) * ENTRY_HEIGHT;
            if (entryY + ENTRY_HEIGHT < y || entryY > y + height) continue;

            ResourceLocation npcId = npcs.get(i);
            boolean active = npcId.equals(activeId);
            boolean hasUnread = hasUnread(npcId);

            int bgColor = active ? 0xFF_555555 : 0xFF_2C2C2C;
            gfx.fill(x + PADDING, entryY, x + WIDTH - PADDING, entryY + ENTRY_HEIGHT - 2, bgColor);

            // avatar
            NPCAvatarRenderer.render(gfx, npcId, x + 6, entryY + 4);

            // name
            String name = npcId.getPath();
            gfx.drawString(mc().font, name, x + 24, entryY + 6, 0xFFFFFF);

            // unread dot
            if (hasUnread) {
                gfx.fill(x + WIDTH - 16, entryY + 8, x + WIDTH - 8, entryY + 16, 0xFF_FF4444);
            }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY) {
        if (mouseX < x || mouseX > x + WIDTH) return false;
        if (mouseY < y || mouseY > y + height) return false;

        var npcs = ClientQuestDataCache.INSTANCE.npcIds();
        int index = ((int) mouseY - y + scrollOffset) / ENTRY_HEIGHT;
        if (index >= 0 && index < npcs.size()) {
            ClientQuestDataCache.INSTANCE.setActiveNpcId(npcs.get(index));
            return true;
        }
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX < x || mouseX > x + WIDTH) return false;
        var npcs = ClientQuestDataCache.INSTANCE.npcIds();
        int maxScroll = Math.max(0, npcs.size() * ENTRY_HEIGHT - height);
        scrollOffset = (int) Math.clamp(scrollOffset - deltaY * 20, 0, maxScroll);
        return true;
    }

    private boolean hasUnread(ResourceLocation npcId) {
        // has pending quest offers or accepted quests from this NPC
        var cache = ClientQuestDataCache.INSTANCE;
        for (var qid : cache.visibleHiddenQuests()) {
            var q = ClientQuestData.get(qid);
            if (q != null && q.npcId().equals(npcId)) return true;
        }
        for (var q : ClientQuestData.getAll()) {
            if (q.npcId().equals(npcId) && cache.isAccepted(q.id())) return true;
        }
        return false;
    }

    private static net.minecraft.client.Minecraft mc() {
        return net.minecraft.client.Minecraft.getInstance();
    }
}
