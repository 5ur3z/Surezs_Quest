package org.surez.surezs_quest.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class DialoguePopup {

    public static ResourceLocation activeNpcId;
    public static String activeText = "";

    private static final int POPUP_W = 240;
    private static final int POPUP_H = 90;

    public static void show(ResourceLocation npcId, String text) {
        activeNpcId = npcId;
        activeText = text;
    }

    public static void hide() {
        activeNpcId = null;
        activeText = "";
    }

    public static boolean isVisible() {
        return activeNpcId != null && !activeText.isEmpty();
    }

    public static void render(GuiGraphics gfx, Font font, int screenW, int screenH, int mouseX, int mouseY) {
        if (!isVisible()) return;

        int px = (screenW - POPUP_W) / 2;
        int py = (screenH - POPUP_H) / 2;

        // Push entire popup to a higher Z layer so text from cards below doesn't bleed through
        gfx.pose().pushPose();
        gfx.pose().translate(0, 0, 400);

        // light dim overlay over entire screen
        gfx.fill(0, 0, screenW, screenH, 0x88_000000);

        // popup shadow
        gfx.fill(px + 2, py + 2, px + POPUP_W + 2, py + POPUP_H + 2, 0x88_000000);

        // popup background — solid, fully opaque
        gfx.fill(px, py, px + POPUP_W, py + POPUP_H, 0xFF_353545);
        // top accent bar
        gfx.fill(px, py, px + POPUP_W, py + 3, 0xFF_5577AA);

        // NPC avatar
        NPCAvatarRenderer.render(gfx, activeNpcId, px + 8, py + 10);

        // NPC name
        String npcName = ClientQuestDataCache.INSTANCE.getNpcName(activeNpcId);
        gfx.drawString(font, npcName, px + 28, py + 10, 0xFF_FFD700);

        // divider
        gfx.fill(px + 8, py + 30, px + POPUP_W - 8, py + 31, 0xFF_555555);

        // dialogue text — wrap long lines
        String text = activeText;
        int maxWidth = POPUP_W - 20;
        if (font.width(text) > maxWidth) {
            // simple word wrap
            var sb = new StringBuilder();
            int lineW = 0;
            for (String word : text.split(" ")) {
                int wordW = font.width(word + " ");
                if (lineW + wordW > maxWidth && lineW > 0) {
                    sb.append('\n');
                    lineW = 0;
                }
                sb.append(word).append(' ');
                lineW += wordW;
            }
            text = sb.toString().trim();
        }
        gfx.drawString(font, text, px + 10, py + 38, 0xFF_FFFFFF);

        // close button with hover
        int btnX = px + POPUP_W - 44;
        int btnY = py + POPUP_H - 22;
        boolean hoverClose = mouseX >= btnX && mouseX <= btnX + 36 && mouseY >= btnY && mouseY <= btnY + 16;
        int closeColor = hoverClose ? 0xFF_6688AA : 0xFF_445566;
        gfx.fill(btnX, btnY, btnX + 36, btnY + 16, closeColor);
        gfx.drawCenteredString(font, Component.translatable("surezs_quest.button.close").getString(), btnX + 18, btnY + 2, 0xFF_FFFFFF);

        gfx.pose().popPose();
    }

    public static boolean mouseClicked(double mx, double my, int screenW, int screenH) {
        if (!isVisible()) return false;

        int px = (screenW - POPUP_W) / 2;
        int py = (screenH - POPUP_H) / 2;

        // close button
        int btnX = px + POPUP_W - 44;
        int btnY = py + POPUP_H - 22;
        if (mx >= btnX && mx <= btnX + 36 && my >= btnY && my <= btnY + 16) {
            hide();
            return true;
        }

        // clicked outside popup → close
        if (mx < px || mx > px + POPUP_W || my < py || my > py + POPUP_H) {
            hide();
            return true;
        }

        return true; // consumed click inside popup
    }
}
