package org.surez.surezs_quest.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.surez.surezs_quest.api.npc.NPC;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class NPCManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private final Map<ResourceLocation, NPC> npcs = new ConcurrentHashMap<>();

    public NPC get(ResourceLocation id) { return npcs.get(id); }
    public Collection<NPC> getAll() { return npcs.values(); }

    public void load(Path folder) {
        Map<ResourceLocation, NPC> loaded = new ConcurrentHashMap<>();
        if (!Files.exists(folder)) {
            LOGGER.warn("NPC folder not found: {}", folder);
            npcs.clear();
            return;
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(file -> {
                try (var reader = new InputStreamReader(Files.newInputStream(file))) {
                    JsonElement json = JsonParser.parseReader(reader);
                    NPC.CODEC.decode(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> LOGGER.error("Failed to load NPC {}: {}", file.getFileName(), err))
                        .ifPresent(pair -> loaded.put(pair.getFirst().id(), pair.getFirst()));
                } catch (IOException e) {
                    LOGGER.error("Failed to read NPC file {}: {}", file.getFileName(), e.getMessage());
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to list NPC folder: {}", e.getMessage());
        }
        npcs.clear();
        npcs.putAll(loaded);
        LOGGER.info("Loaded {} NPCs from {}", loaded.size(), folder);
    }
}
