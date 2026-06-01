package org.surez.surezs_quest.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.surez.surezs_quest.Translation;
import org.surez.surezs_quest.network.packet.RequestOpenQuestScreenPacket;

public class QuestScreen extends Screen {

    private static final int SIDEBAR_WIDTH = NPCSidebarWidget.WIDTH;
    private NPCSidebarWidget sidebar;
    private QuestListPanel questList;

    public QuestScreen() {
        super(Translation.tr("surezs_quest.screen.title"));
    }

    @Override
    protected void init() {
        super.init();
        sidebar = new NPCSidebarWidget(0, 20, this.height - 20);
        questList = new QuestListPanel(SIDEBAR_WIDTH + 2, 0, this.width - SIDEBAR_WIDTH - 2, this.height);
        PacketDistributor.sendToServer(new RequestOpenQuestScreenPacket());
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Override to prevent MC 1.21.1's automatic background blur
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // sidebar
        gfx.fill(0, 0, SIDEBAR_WIDTH, this.height, 0xFF_2C2C2C);
        gfx.fill(SIDEBAR_WIDTH, 0, SIDEBAR_WIDTH + 1, this.height, 0xFF_555555);
        gfx.drawCenteredString(this.font, Translation.tr("surezs_quest.screen.title").getString(), SIDEBAR_WIDTH / 2, 6, 0xFFFFFF);
        sidebar.render(gfx, mouseX, mouseY);

        // quest list (right panel)
        gfx.fill(SIDEBAR_WIDTH + 2, 0, this.width, this.height, 0xFF_1E1E1E);
        questList.render(gfx, mouseX, mouseY);

        super.render(gfx, mouseX, mouseY, partialTick);

        // dialogue popup on top
        DialoguePopup.render(gfx, this.font, this.width, this.height, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (DialoguePopup.mouseClicked(mouseX, mouseY, this.width, this.height)) return true;
        if (sidebar.mouseClicked(mouseX, mouseY)) return true;
        if (questList.mouseClicked(mouseX, mouseY)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (sidebar.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) return true;
        if (questList.mouseScrolled(mouseX, mouseY, deltaX, deltaY)) return true;
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        questList.mouseMoved(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
