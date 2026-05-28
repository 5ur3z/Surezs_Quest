package org.surez.surezs_quest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.surez.surezs_quest.avatar.NPCAvatarManager;

@EventBusSubscriber(modid = Surezs_quest.MODID, value = Dist.CLIENT)
public class ClientSetup {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final KeyMapping OPEN_QUEST = new KeyMapping(
        "key.surezs_quest.open",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_K,
        "key.categories.surezs_quest"
    );

    @SubscribeEvent
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_QUEST);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        while (OPEN_QUEST.consumeClick()) {
            Minecraft.getInstance().setScreen(new org.surez.surezs_quest.screen.QuestScreen());
        }
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        NPCAvatarManager.init(FMLPaths.CONFIGDIR.get().resolve(Surezs_quest.MODID));
        LOGGER.info("Surez's Quest client setup — {}", Minecraft.getInstance().getUser().getName());
    }
}
