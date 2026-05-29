package org.surez.surezs_quest.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

public final class JsonDefinitionLoader {

    private JsonDefinitionLoader() {}

    public static <T> Map<ResourceLocation, T> load(
        Path folder,
        Codec<T> codec,
        Function<T, ResourceLocation> idGetter,
        String label,
        Logger logger
    ) {
        Map<ResourceLocation, T> loaded = new ConcurrentHashMap<>();
        if (!Files.exists(folder)) {
            logger.warn("{} folder not found: {}", label, folder);
            return loaded;
        }

        try (Stream<Path> files = Files.list(folder)) {
            files.filter(file -> file.toString().endsWith(".json")).forEach(file -> {
                try (var reader = new InputStreamReader(Files.newInputStream(file))) {
                    JsonElement json = JsonParser.parseReader(reader);
                    codec.decode(JsonOps.INSTANCE, json)
                        .resultOrPartial(err -> logger.error("Failed to load {} {}: {}", label, file.getFileName(), err))
                        .ifPresent(pair -> loaded.put(idGetter.apply(pair.getFirst()), pair.getFirst()));
                } catch (IOException e) {
                    logger.error("Failed to read {} file {}: {}", label, file.getFileName(), e.getMessage());
                }
            });
        } catch (IOException e) {
            logger.error("Failed to list {} folder {}: {}", label, folder, e.getMessage());
        }

        return loaded;
    }
}
