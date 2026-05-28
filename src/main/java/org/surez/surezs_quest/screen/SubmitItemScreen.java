package org.surez.surezs_quest.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import org.surez.surezs_quest.network.packet.ConfirmSubmitPacket;
import org.surez.surezs_quest.network.packet.OpenSubmitScreenPacket;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SubmitItemScreen extends Screen {

    private final ResourceLocation questId;
    private final List<OpenSubmitScreenPacket.SlotItem> items;
    private final Set<Integer> selectedSlots = new HashSet<>();
    private int scrollOffset;

    public SubmitItemScreen(ResourceLocation questId, List<OpenSubmitScreenPacket.SlotItem> items) {
        super(Component.translatable("surezs_quest.screen.submit_title"));
        this.questId = questId;
        this.items = items;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        // Prevent auto blur
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, this.width, this.height, 0xFF_222222);

        gfx.drawCenteredString(this.font, Component.translatable("surezs_quest.submit.select_items").getString(), this.width / 2, 10, 0xFFFFFF);

        int startX = this.width / 2 - 100;
        int y = 30;
        int index = 0;
        for (var item : items) {
            if (y > this.height - 50) break;

            boolean selected = selectedSlots.contains(item.slotIndex());
            int bgColor = selected ? 0xFF_446644 : 0xFF_333333;
            gfx.fill(startX, y, startX + 200, y + 20, bgColor);

            String label = item.itemId().getPath() + " x" + item.count() + String.format(Component.translatable("surezs_quest.submit.slot_label").getString(), item.slotIndex());
            gfx.drawString(this.font, label, startX + 4, y + 4, 0xFFFFFF);

            y += 22;
            index++;
        }

        // confirm button with hover
        int btnY = this.height - 30;
        if (!selectedSlots.isEmpty()) {
            int btnX1 = this.width / 2 - 30, btnX2 = this.width / 2 + 30;
            boolean hover = mouseX >= btnX1 && mouseX <= btnX2 && mouseY >= btnY && mouseY <= btnY + 20;
            gfx.fill(btnX1, btnY, btnX2, btnY + 20, hover ? 0xFF_337733 : 0xFF_226622);
            gfx.drawCenteredString(this.font, Component.translatable("surezs_quest.submit.confirm").getString(), this.width / 2, btnY + 4, 0xFFFFFF);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int startX = this.width / 2 - 100;
        int y = 30;
        for (var item : items) {
            if (mouseX >= startX && mouseX <= startX + 200 && mouseY >= y && mouseY <= y + 20) {
                if (selectedSlots.contains(item.slotIndex())) {
                    selectedSlots.remove(item.slotIndex());
                } else {
                    selectedSlots.add(item.slotIndex());
                }
                return true;
            }
            y += 22;
        }

        // confirm button
        if (!selectedSlots.isEmpty()) {
            int btnY = this.height - 30;
            if (mouseX >= this.width / 2 - 30 && mouseX <= this.width / 2 + 30
                && mouseY >= btnY && mouseY <= btnY + 20) {
                PacketDistributor.sendToServer(new ConfirmSubmitPacket(questId,
                    selectedSlots.stream().toList()));
                Minecraft.getInstance().setScreen(new org.surez.surezs_quest.screen.QuestScreen());
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
