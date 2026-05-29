package org.surez.surezs_quest.data;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class DefinitionManager<T> {

    private final Map<ResourceLocation, T> entries = new ConcurrentHashMap<>();
    private final Codec<T> codec;
    private final Function<T, ResourceLocation> idGetter;
    private final String label;
    private final Logger logger;

    public DefinitionManager(Codec<T> codec, Function<T, ResourceLocation> idGetter, String label, Logger logger) {
        this.codec = codec;
        this.idGetter = idGetter;
        this.label = label;
        this.logger = logger;
    }

    public T get(ResourceLocation id) { return entries.get(id); }
    public Collection<T> getAll() { return entries.values(); }
    public boolean exists(ResourceLocation id) { return entries.containsKey(id); }

    public void load(Path folder) {
        Map<ResourceLocation, T> loaded = JsonDefinitionLoader.load(folder, codec, idGetter, label, logger);
        entries.clear();
        entries.putAll(loaded);
        logger.info("Loaded {} {}s from {}", loaded.size(), label, folder);
    }
}
