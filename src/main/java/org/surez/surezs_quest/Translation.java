package org.surez.surezs_quest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads translations from assets/surezs_quest/lang/{language}.json
 * based on Config.INSTANCE.language().
 */
public class Translation {
    private static final Map<String, String> strings = new HashMap<>();
    private static String loadedLang = "";

    public static void reload() {
        String lang = Config.INSTANCE.language();
        if (lang.equals(loadedLang)) return;

        strings.clear();
        String path = "/assets/surezs_quest/lang/" + lang + ".json";
        String content = readResource(path);
        if (content == null) {
            content = readResource("/assets/surezs_quest/lang/en_us.json");
        }
        if (content != null) {
            JsonObject obj = JsonParser.parseString(content).getAsJsonObject();
            for (var entry : obj.entrySet()) {
                strings.put(entry.getKey(), entry.getValue().getAsString());
            }
        }

        loadedLang = lang;
    }

    private static String readResource(String path) {
        try (InputStream in = Translation.class.getResourceAsStream(path)) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    public static String get(String key) {
        return strings.getOrDefault(key, key);
    }

    public static Component tr(String key, Object... args) {
        String template = strings.getOrDefault(key, key);
        if (args.length == 0) return Component.literal(template);
        return Component.literal(String.format(template, args));
    }
}
