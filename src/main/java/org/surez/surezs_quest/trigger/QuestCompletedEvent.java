package org.surez.surezs_quest.trigger;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public record QuestCompletedEvent(
    ResourceLocation questId,
    Player player
) {}
