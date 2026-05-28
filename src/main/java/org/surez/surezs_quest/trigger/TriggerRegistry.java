package org.surez.surezs_quest.trigger;

import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TriggerRegistry {

    private static final Map<Class<? extends Event>, List<ITriggerHandler<?>>> handlers = new ConcurrentHashMap<>();

    public static void register(ITriggerHandler<?> handler) {
        handlers.computeIfAbsent(handler.listenedEvent(), k -> new ArrayList<>()).add(handler);
    }

    public static List<ITriggerHandler<?>> getHandlers(Class<? extends Event> eventClass) {
        return handlers.getOrDefault(eventClass, List.of());
    }

    public static void clear() {
        handlers.clear();
    }

    public static void notifyLogout(UUID uuid) {
        for (var list : handlers.values()) {
            for (var handler : list) {
                handler.onPlayerLogout(uuid);
            }
        }
    }
}
