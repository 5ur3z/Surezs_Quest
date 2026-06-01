package org.surez.surezs_quest.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.surez.surezs_quest.Translation;
import org.surez.surezs_quest.avatar.NPCAvatarManager;

public class NPCAvatarRenderer {

    public static final int AVATAR_SIZE = 16;

    public static void render(GuiGraphics gfx, ResourceLocation npcId, int x, int y) {
        ResourceLocation tex = NPCAvatarManager.getTexture(npcId);
        // if texture isn't loaded yet or is default placeholder, draw a colored square
        if (tex == null || tex.getPath().contains("avatar_default")) {
            gfx.fill(x, y, x + AVATAR_SIZE, y + AVATAR_SIZE, 0xFF_666666);
            gfx.drawString(net.minecraft.client.Minecraft.getInstance().font, Translation.tr("surezs_quest.npc.unknown_avatar").getString(), x + 5, y + 2, 0xFFFFFF);
            return;
        }

        gfx.blit(tex, x, y, AVATAR_SIZE, AVATAR_SIZE, 0, 0, AVATAR_SIZE, AVATAR_SIZE, AVATAR_SIZE, AVATAR_SIZE);
    }
}
