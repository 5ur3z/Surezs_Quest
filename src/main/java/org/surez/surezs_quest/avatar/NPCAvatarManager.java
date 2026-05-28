package org.surez.surezs_quest.avatar;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.surez.surezs_quest.Surezs_quest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class NPCAvatarManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ResourceLocation, ResourceLocation> textureCache = new HashMap<>();
    private static Path avatarsDir;
    private static boolean initialized;

    public static void init(Path configDir) {
        avatarsDir = configDir.resolve("avatars");
        try {
            Files.createDirectories(avatarsDir);
        } catch (IOException ignored) {}
        initialized = true;
    }

    public static ResourceLocation getTexture(ResourceLocation npcId) {
        if (!initialized) return defaultTexture();

        return textureCache.computeIfAbsent(npcId, id -> {
            String filename = id.getPath() + ".png";
            Path file = avatarsDir.resolve(filename);

            if (!Files.exists(file)) {
                LOGGER.debug("No avatar for {}, using default", npcId);
                return defaultTexture();
            }

            try (var in = Files.newInputStream(file)) {
                NativeImage image = NativeImage.read(in);
                ResourceLocation texId = ResourceLocation.fromNamespaceAndPath(
                    Surezs_quest.MODID, "avatar_" + id.getNamespace() + "_" + id.getPath());
                Minecraft.getInstance().getTextureManager()
                    .register(texId, new DynamicTexture(image));
                LOGGER.debug("Loaded avatar for {}: {}", npcId, texId);
                return texId;
            } catch (IOException e) {
                LOGGER.warn("Failed to load avatar {}: {}", npcId, e.getMessage());
                return defaultTexture();
            }
        });
    }

    private static ResourceLocation defaultTexture() {
        // return a grey placeholder — we use null and the renderer handles it
        return ResourceLocation.fromNamespaceAndPath(Surezs_quest.MODID, "textures/gui/avatar_default.png");
    }
}
