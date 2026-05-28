package org.surez.surezs_quest.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.surez.surezs_quest.api.quest.Quest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

public class QuestManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private final Map<ResourceLocation, Quest> quests = new ConcurrentHashMap<>();

    public Quest get(ResourceLocation id) { return quests.get(id); }
    public Collection<Quest> getAll() { return quests.values(); }
    public boolean exists(ResourceLocation id) { return quests.containsKey(id); }

    public void load(Path folder) {
        Map<ResourceLocation, Quest> loaded = new ConcurrentHashMap<>();
        if (!Files.exists(folder)) {
            LOGGER.warn("Quest folder not found: {}", folder);
            quests.clear();
            return;
        }
        try (Stream<Path> files = Files.list(folder)) {
            files.filter(f -> f.toString().endsWith(".json")).forEach(file -> {
                try (var reader = new InputStreamReader(Files.newInputStream(file))) {
                    JsonElement json = JsonParser.parseReader(reader);
                    Quest.CODEC.decode(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> LOGGER.error("Failed to load quest {}: {}", file.getFileName(), err))
                        .ifPresent(pair -> loaded.put(pair.getFirst().id(), pair.getFirst()));
                } catch (IOException e) {
                    LOGGER.error("Failed to read quest file {}: {}", file.getFileName(), e.getMessage());
                }
            });
        } catch (IOException e) {
            LOGGER.error("Failed to list quest folder: {}", e.getMessage());
        }
        quests.clear();
        quests.putAll(loaded);
        LOGGER.info("Loaded {} quests from {}", loaded.size(), folder);
    }
}
