package org.surez.surezs_quest.trigger.handlers;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TickGateTest {

    @Test
    void shouldRunOnlyOnIntervalTicksPerPlayer() {
        TickGate gate = new TickGate(3);
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();

        assertFalse(gate.shouldRun(playerA));
        assertFalse(gate.shouldRun(playerA));
        assertTrue(gate.shouldRun(playerA));

        assertFalse(gate.shouldRun(playerB));
        assertFalse(gate.shouldRun(playerB));
        assertTrue(gate.shouldRun(playerB));
    }

    @Test
    void clearResetsOnePlayerCounter() {
        TickGate gate = new TickGate(2);
        UUID player = UUID.randomUUID();

        assertFalse(gate.shouldRun(player));
        gate.clear(player);
        assertFalse(gate.shouldRun(player));
        assertTrue(gate.shouldRun(player));
    }
}
