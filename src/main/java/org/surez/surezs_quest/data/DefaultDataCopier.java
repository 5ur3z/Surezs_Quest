package org.surez.surezs_quest.data;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class DefaultDataCopier {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String[] DEFAULT_FILES = {
        "quests/collect_iron.json",
        "quests/kill_zombies.json",
        "quests/secret_weapon.json",
        "quests/server_collect_iron.json",
        "quests/craft_furnace.json",
        "npcs/aleksei.json",
    };

    /**
     * Copy built-in data files from the JAR classpath to config/surezs_quest/.
     * Skips any file that already exists (won't overwrite user modifications).
     */
    public static void copyDefaults(Path configDir) {
        Path questDir = configDir.resolve("quests");
        if (Files.exists(questDir)) {
            LOGGER.debug("Quest data already exists in config, skipping copy");
            return;
        }

        int copied = 0;
        for (String relPath : DEFAULT_FILES) {
            Path target = configDir.resolve(relPath);
            if (Files.exists(target)) continue;

            String classpathPath = "/data/surezs_quest/" + relPath;
            try (InputStream in = DefaultDataCopier.class.getResourceAsStream(classpathPath)) {
                if (in == null) {
                    LOGGER.warn("Default data file not found in JAR: {}", classpathPath);
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                copied++;
            } catch (IOException e) {
                LOGGER.error("Failed to copy default data {}: {}", relPath, e.getMessage());
            }
        }

        LOGGER.info("Copied {} default data files to {}", copied, configDir);
    }
}
