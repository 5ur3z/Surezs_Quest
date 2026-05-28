package org.surez.surezs_quest.storage;

import com.google.gson.*;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.lang.reflect.Type;
import java.util.*;

public final class QuestDataCodec {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder()
        .registerTypeAdapter(ResourceLocation.class, new ResourceLocationAdapter())
        .registerTypeAdapter(UUID.class, new UUIDAdapter())
        .setPrettyPrinting()
        .create();

    private QuestDataCodec() {}

    // -- PlayerQuestData -----------------------------------------------------

    public static String encodePlayer(PlayerQuestData data) {
        return GSON.toJson(toPlayerJson(data));
    }

    public static PlayerQuestData decodePlayer(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return fromPlayerJson(root);
    }

    private static JsonObject toPlayerJson(PlayerQuestData data) {
        JsonObject obj = new JsonObject();
        obj.add("accepted_quests", toJsonArray(data.acceptedQuests()));
        obj.add("declined_quests", toJsonArray(data.declinedQuests()));
        obj.add("completed_quests", toJsonArray(data.completedQuests()));

        JsonObject progress = new JsonObject();
        for (var entry : data.objectiveProgress().entrySet()) {
            JsonObject inner = new JsonObject();
            for (var innerEntry : entry.getValue().entrySet()) {
                inner.addProperty(String.valueOf(innerEntry.getKey()), innerEntry.getValue());
            }
            progress.add(entry.getKey().toString(), inner);
        }
        obj.add("objective_progress", progress);

        return obj;
    }

    private static PlayerQuestData fromPlayerJson(JsonObject obj) {
        PlayerQuestData data = new PlayerQuestData();

        for (JsonElement e : arr(obj, "accepted_quests"))
            data.accept(ResourceLocation.parse(e.getAsString()));
        for (JsonElement e : arr(obj, "declined_quests"))
            data.decline(ResourceLocation.parse(e.getAsString()));
        for (JsonElement e : arr(obj, "completed_quests"))
            data.markCompleted(ResourceLocation.parse(e.getAsString()));

        if (obj.has("objective_progress")) {
            JsonObject progress = obj.getAsJsonObject("objective_progress");
            for (var entry : progress.entrySet()) {
                ResourceLocation qid = ResourceLocation.parse(entry.getKey());
                JsonObject inner = entry.getValue().getAsJsonObject();
                for (var ie : inner.entrySet()) {
                    try {
                        int idx = Integer.parseInt(ie.getKey());
                        data.setProgress(qid, idx, ie.getValue().getAsInt());
                    } catch (NumberFormatException ex) {
                        LOGGER.warn("Skipping non-numeric objective index '{}' in {}", ie.getKey(), qid);
                    }
                }
            }
        }

        return data;
    }

    // -- ServerQuestData -----------------------------------------------------

    public static String encodeServer(ServerQuestData data) {
        return GSON.toJson(toServerJson(data));
    }

    public static ServerQuestData decodeServer(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return fromServerJson(root);
    }

    private static JsonObject toServerJson(ServerQuestData data) {
        JsonObject quests = new JsonObject();
        data.quests().forEach((qid, entry) -> {
            JsonObject q = new JsonObject();

            JsonObject progress = new JsonObject();
            entry.progress().forEach((idx, val) ->
                progress.addProperty(String.valueOf(idx), val));
            q.add("progress", progress);
            q.add("accepted_players", toJsonArray(entry.acceptedPlayers()));
            q.add("declined_players", toJsonArray(entry.declinedPlayers()));
            q.add("claimed_players", toJsonArray(entry.completedPlayers()));
            q.addProperty("is_completed", entry.completedDebug());

            JsonObject contribs = new JsonObject();
            entry.contributors().forEach((uuid, objMap) -> {
                JsonObject inner = new JsonObject();
                objMap.forEach((idx, val) -> inner.addProperty(String.valueOf(idx), val));
                contribs.add(uuid.toString(), inner);
            });
            q.add("contributors", contribs);

            quests.add(qid.toString(), q);
        });

        JsonObject obj = new JsonObject();
        obj.add("quests", quests);
        return obj;
    }

    private static ServerQuestData fromServerJson(JsonObject obj) {
        ServerQuestData data = new ServerQuestData();

        // new per-quest nested format
        if (obj.has("quests")) {
            JsonObject quests = obj.getAsJsonObject("quests");
            for (var qEntry : quests.entrySet()) {
                ResourceLocation qid = ResourceLocation.parse(qEntry.getKey());
                JsonObject q = qEntry.getValue().getAsJsonObject();

                if (q.has("progress")) {
                    JsonObject progress = q.getAsJsonObject("progress");
                    for (var pe : progress.entrySet()) {
                        try {
                            int idx = Integer.parseInt(pe.getKey());
                            data.addProgress(qid, idx, pe.getValue().getAsInt());
                        } catch (NumberFormatException ex) {
                            LOGGER.warn("Skipping non-numeric objective index '{}' in {}", pe.getKey(), qid);
                        }
                    }
                }

                for (JsonElement e : arr(q, "accepted_players"))
                    data.accept(qid, UUID.fromString(e.getAsString()));
                for (JsonElement e : arr(q, "declined_players"))
                    data.decline(qid, UUID.fromString(e.getAsString()));
                for (JsonElement e : arr(q, "claimed_players"))
                    data.markCompleted(qid, UUID.fromString(e.getAsString()));

                if (q.has("is_completed") && q.get("is_completed").getAsBoolean())
                    data.markDebugComplete(qid);

                if (q.has("contributors")) {
                    JsonObject contribs = q.getAsJsonObject("contributors");
                    for (var ce : contribs.entrySet()) {
                        UUID uuid = UUID.fromString(ce.getKey());
                        JsonObject inner = ce.getValue().getAsJsonObject();
                        for (var ie : inner.entrySet()) {
                            try {
                                int idx = Integer.parseInt(ie.getKey());
                                data.addContribution(qid, uuid, idx, ie.getValue().getAsInt());
                            } catch (NumberFormatException ex) {
                                LOGGER.warn("Skipping non-numeric obj index in contributors {}", qid);
                            }
                        }
                    }
                }
            }
            return data;
        }

        return data;
    }

    // -- helpers -------------------------------------------------------------

    private static JsonArray toJsonArray(Collection<?> set) {
        JsonArray arr = new JsonArray();
        for (Object o : set) arr.add(o.toString());
        return arr;
    }

    private static JsonArray arr(JsonObject obj, String key) {
        return obj.has(key) ? obj.getAsJsonArray(key) : new JsonArray();
    }

    // -- type adapters -------------------------------------------------------

    private static class ResourceLocationAdapter implements
        JsonSerializer<ResourceLocation>, JsonDeserializer<ResourceLocation> {
        @Override
        public JsonElement serialize(ResourceLocation src, Type t, JsonSerializationContext ctx) {
            return new JsonPrimitive(src.toString());
        }
        @Override
        public ResourceLocation deserialize(JsonElement json, Type t, JsonDeserializationContext ctx) {
            return ResourceLocation.parse(json.getAsString());
        }
    }

    private static class UUIDAdapter implements
        JsonSerializer<UUID>, JsonDeserializer<UUID> {
        @Override
        public JsonElement serialize(UUID src, Type t, JsonSerializationContext ctx) {
            return new JsonPrimitive(src.toString());
        }
        @Override
        public UUID deserialize(JsonElement json, Type t, JsonDeserializationContext ctx) {
            return UUID.fromString(json.getAsString());
        }
    }
}
