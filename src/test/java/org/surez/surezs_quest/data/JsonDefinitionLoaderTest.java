package org.surez.surezs_quest.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class JsonDefinitionLoaderTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonDefinitionLoaderTest.class);

    record TestDefinition(ResourceLocation id, int value) {
        static final Codec<TestDefinition> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                ResourceLocation.CODEC.fieldOf("id").forGetter(TestDefinition::id),
                Codec.INT.fieldOf("value").forGetter(TestDefinition::value)
            ).apply(instance, TestDefinition::new)
        );
    }

    @Test
    void loadsJsonFilesAndKeysByDefinitionId(@TempDir Path tempDir) throws Exception {
        Files.writeString(tempDir.resolve("one.json"), """
            {
              "id": "surezs_quest:one",
              "value": 7
            }
            """);
        Files.writeString(tempDir.resolve("ignored.txt"), """
            {
              "id": "surezs_quest:ignored",
              "value": 99
            }
            """);

        var loaded = JsonDefinitionLoader.load(
            tempDir,
            TestDefinition.CODEC,
            TestDefinition::id,
            "test definition",
            LOGGER
        );

        assertEquals(1, loaded.size());
        assertEquals(7, loaded.get(ResourceLocation.parse("surezs_quest:one")).value());
    }

    @Test
    void missingFolderReturnsEmptyMap(@TempDir Path tempDir) {
        var loaded = JsonDefinitionLoader.load(
            tempDir.resolve("missing"),
            TestDefinition.CODEC,
            TestDefinition::id,
            "test definition",
            LOGGER
        );

        assertTrue(loaded.isEmpty());
    }
}
