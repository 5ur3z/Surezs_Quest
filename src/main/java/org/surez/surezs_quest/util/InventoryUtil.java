package org.surez.surezs_quest.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class InventoryUtil {

    private InventoryUtil() {}

    public record SlotCount(int slotIndex, ResourceLocation itemId, int count) {}

    public static Map<ResourceLocation, Integer> snapshot(Player player) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        for (var stack : player.getInventory().items) {
            if (!stack.isEmpty()) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                counts.merge(id, stack.getCount(), Integer::sum);
            }
        }
        return counts;
    }

    public static int count(Player player, ResourceLocation itemId) {
        return snapshot(player).getOrDefault(itemId, 0);
    }

    public static List<SlotCount> matchingSlots(Player player, ResourceLocation target) {
        List<SlotCount> slots = new ArrayList<>();
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) {
            var stack = player.getInventory().items.get(slot);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(target)) {
                slots.add(new SlotCount(slot, target, stack.getCount()));
            }
        }
        return slots;
    }
}
