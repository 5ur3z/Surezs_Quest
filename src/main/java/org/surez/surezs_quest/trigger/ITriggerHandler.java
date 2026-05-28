package org.surez.surezs_quest.trigger;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import org.surez.surezs_quest.api.trigger.QuestTrigger;
import org.surez.surezs_quest.storage.PlayerQuestData;

import java.util.List;
import java.util.UUID;

public interface ITriggerHandler<T extends QuestTrigger> {

    /** 该 Handler 监听哪种 NeoForge 事件 */
    Class<? extends Event> listenedEvent();

    /** 从事件中匹配出触发的 quest trigger 列表 */
    List<T> match(Event event);

    /** 处理单个匹配的触发（Phase 4 仅日志，Phase 5 改为调用 QuestProgressManager） */
    void handle(T trigger, Player player, PlayerQuestData data);

    /** 处理单次事件：match + 对每个匹配结果调 handle */
    @SuppressWarnings("unchecked")
    default void process(Event event, Player player, PlayerQuestData data) {
        for (var trigger : match(event)) {
            handle((T) trigger, player, data);
        }
    }

    /** 玩家下线时清理 per-player 缓存（有需要的 Handler 重写） */
    default void onPlayerLogout(UUID uuid) {}
}
