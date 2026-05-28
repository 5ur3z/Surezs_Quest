package org.surez.surezs_quest.trigger;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.slf4j.Logger;
import org.surez.surezs_quest.storage.PlayerQuestData;
import org.surez.surezs_quest.storage.QuestDataManager;

public class GameEventListener {

    private static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        process(event, event.getEntity());
    }

    @SubscribeEvent
    public void onLivingDeath(LivingDeathEvent event) {
        if (event.getSource().getEntity() instanceof Player player) {
            process(event, player);
        }
    }

    private void process(Event event, Player player) {
        if (!(player instanceof ServerPlayer)) return;

        PlayerQuestData data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) return;

        for (var handler : TriggerRegistry.getHandlers(event.getClass())) {
            try {
                handler.process(event, player, data);
            } catch (Exception e) {
                LOGGER.error("Handler {} failed for {}: {}",
                    handler.getClass().getSimpleName(),
                    event.getClass().getSimpleName(),
                    e.getMessage());
            }
        }
    }
}
