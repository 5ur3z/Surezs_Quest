package org.surez.surezs_quest.trigger.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class TickGate {

    private final int intervalTicks;
    private final Map<UUID, Integer> counters = new HashMap<>();

    TickGate(int intervalTicks) {
        if (intervalTicks <= 0) throw new IllegalArgumentException("intervalTicks must be positive");
        this.intervalTicks = intervalTicks;
    }

    boolean shouldRun(UUID uuid) {
        int count = counters.merge(uuid, 1, Integer::sum);
        return count % intervalTicks == 0;
    }

    void clear(UUID uuid) {
        counters.remove(uuid);
    }
}
