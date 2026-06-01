package org.surez.surezs_quest.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.loading.FMLPaths;
import org.surez.surezs_quest.Config;
import org.surez.surezs_quest.Translation;
import org.surez.surezs_quest.Surezs_quest;
import org.surez.surezs_quest.api.quest.Quest;
import org.surez.surezs_quest.data.DataLoaders;
import org.surez.surezs_quest.network.NetworkHandler;
import org.surez.surezs_quest.storage.QuestDataManager;
import org.surez.surezs_quest.trigger.QuestProgressManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QuestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("quest")
                .requires(src -> src.hasPermission(2))
                // /quest give <player> <quest_id>
                .then(Commands.literal("give")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("quest_id", ResourceLocationArgument.id())
                            .suggests((ctx, builder) -> {
                                for (var q : DataLoaders.QUESTS.getAll())
                                    builder.suggest(q.id().toString());
                                return builder.buildFuture();
                            })
                            .executes(ctx -> giveQuest(ctx, EntityArgument.getPlayer(ctx, "player"),
                                ResourceLocationArgument.getId(ctx, "quest_id"))))))
                // /quest complete <player> <quest_id>
                .then(Commands.literal("complete")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("quest_id", ResourceLocationArgument.id())
                            .suggests((ctx, builder) -> {
                                for (var q : DataLoaders.QUESTS.getAll())
                                    builder.suggest(q.id().toString());
                                return builder.buildFuture();
                            })
                            .executes(ctx -> completeQuest(ctx, EntityArgument.getPlayer(ctx, "player"),
                                ResourceLocationArgument.getId(ctx, "quest_id"))))))
                // /quest reset <player> — warn, needs confirm
                // /quest reset <player> confirm — reset all
                // /quest reset <player> <quest_id> — reset single
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> warnResetAll(ctx, EntityArgument.getPlayer(ctx, "player")))
                        .then(Commands.literal("confirm")
                            .executes(ctx -> resetAllQuests(ctx, EntityArgument.getPlayer(ctx, "player"))))
                        .then(Commands.argument("quest_id", ResourceLocationArgument.id())
                            .suggests((ctx, builder) -> {
                                for (var q : DataLoaders.QUESTS.getAll())
                                    builder.suggest(q.id().toString());
                                return builder.buildFuture();
                            })
                            .executes(ctx -> resetQuest(ctx, EntityArgument.getPlayer(ctx, "player"),
                                ResourceLocationArgument.getId(ctx, "quest_id"))))))
                // /quest list <player>
                .then(Commands.literal("list")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> listQuests(ctx, EntityArgument.getPlayer(ctx, "player")))))
                // /quest reload
                .then(Commands.literal("reload")
                    .executes(QuestCommand::reloadData))
                // /quest editor [port] — start web editor
                .then(Commands.literal("editor")
                    .executes(ctx -> startEditor(ctx, -1))
                    .then(Commands.argument("port", IntegerArgumentType.integer(1024, 65535))
                        .executes(ctx -> startEditor(ctx, IntegerArgumentType.getInteger(ctx, "port")))))
                // /quest server reset <quest_id>
                .then(Commands.literal("server")
                    .then(Commands.literal("reset")
                        .then(Commands.argument("quest_id", ResourceLocationArgument.id())
                            .suggests((ctx, builder) -> {
                                for (var q : DataLoaders.QUESTS.getAll())
                                    if (q.scope() == Quest.Scope.SERVER)
                                        builder.suggest(q.id().toString());
                                return builder.buildFuture();
                            })
                            .executes(ctx -> resetServerQuest(ctx,
                                ResourceLocationArgument.getId(ctx, "quest_id"))))))
        );
    }

    private static int giveQuest(CommandContext<CommandSourceStack> ctx, ServerPlayer player, ResourceLocation questId) {
        if (!DataLoaders.QUESTS.exists(questId)) {
            ctx.getSource().sendFailure(Translation.tr("surezs_quest.command.quest_not_found", questId.toString()));
            return 0;
        }
        Quest quest = DataLoaders.QUESTS.get(questId);
        if (quest == null) return 0;

        if (quest.scope() == Quest.Scope.SERVER) {
            var serverData = QuestDataManager.INSTANCE.getServerData();
            serverData.accept(questId, player.getUUID());
            QuestDataManager.INSTANCE.saveServer();
        } else {
            var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
            if (data == null) {
                ctx.getSource().sendFailure(Translation.tr("surezs_quest.command.player_data_unavailable"));
                return 0;
            }
            data.accept(questId);
            QuestDataManager.INSTANCE.savePlayer(player.getUUID());
        }

        ctx.getSource().sendSuccess(() -> Translation.tr("surezs_quest.command.give_quest", questId.toString(), player.getName().getString()), true);
        return 1;
    }

    private static int completeQuest(CommandContext<CommandSourceStack> ctx, ServerPlayer player, ResourceLocation questId) {
        Quest quest = DataLoaders.QUESTS.get(questId);
        if (quest == null) {
            ctx.getSource().sendFailure(Translation.tr("surezs_quest.command.quest_not_found", questId.toString()));
            return 0;
        }

        if (quest.scope() == Quest.Scope.SERVER) {
            var serverData = QuestDataManager.INSTANCE.getServerData();
            for (int i = 0; i < quest.objectives().size(); i++)
                serverData.addProgress(questId, i, QuestProgressManager.getObjectiveMax(quest, i) - serverData.getProgress(questId, i));
            serverData.markDebugComplete(questId);
            QuestDataManager.INSTANCE.saveServer();
        } else {
            var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
            if (data == null) {
                ctx.getSource().sendFailure(Translation.tr("surezs_quest.command.player_data_unavailable"));
                return 0;
            }
            QuestProgressManager.forceComplete(player, data, quest);
        }

        ctx.getSource().sendSuccess(() -> Translation.tr("surezs_quest.command.complete_quest", questId.toString(), player.getName().getString()), true);
        return 1;
    }

    private static int resetQuest(CommandContext<CommandSourceStack> ctx, ServerPlayer player, ResourceLocation questId) {
        Quest quest = DataLoaders.QUESTS.get(questId);

        if (quest != null && quest.scope() == Quest.Scope.SERVER) {
            var serverData = QuestDataManager.INSTANCE.getServerData();
            var entry = serverData.quests().get(questId);
            if (entry != null) {
                entry.acceptedPlayers().remove(player.getUUID());
                entry.declinedPlayers().remove(player.getUUID());
                entry.completedPlayers().remove(player.getUUID());
                entry.contributors().remove(player.getUUID());
            }
            QuestDataManager.INSTANCE.saveServer();
            ctx.getSource().sendSuccess(() -> Translation.tr("surezs_quest.command.reset_quest", player.getName().getString(), questId.toString()), true);
            return 1;
        }

        var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) {
            ctx.getSource().sendFailure(Translation.tr("surezs_quest.command.player_data_unavailable"));
            return 0;
        }
        clearQuestData(data, questId);

        var cascaded = new ArrayList<String>();
        var visited = new HashSet<ResourceLocation>();
        visited.add(questId);
        cascadeReset(data, questId, cascaded, visited);

        QuestDataManager.INSTANCE.savePlayer(player.getUUID());

        // full refresh of client state after reset
        NetworkHandler.refreshQuestScreen(player);

        String msg = Translation.tr("surezs_quest.command.reset_quest", player.getName().getString(), questId.toString()).getString();
        if (!cascaded.isEmpty()) msg += Translation.tr("surezs_quest.command.cascade_reset", String.join(", ", cascaded)).getString();
        final String finalMsg = msg;
        ctx.getSource().sendSuccess(() -> Component.literal(finalMsg), true);
        return 1;
    }

    private static void clearQuestData(org.surez.surezs_quest.storage.PlayerQuestData data, ResourceLocation id) {
        data.clearQuest(id);
    }

    private static void cascadeReset(org.surez.surezs_quest.storage.PlayerQuestData data,
                                      ResourceLocation questId, List<String> cascaded, Set<ResourceLocation> visited) {
        for (Quest q : DataLoaders.QUESTS.getAll()) {
            if (q.prerequisites().contains(questId) && visited.add(q.id())) {
                clearQuestData(data, q.id());
                cascaded.add(q.id().getPath());
                cascadeReset(data, q.id(), cascaded, visited);
            }
        }
    }

    private static int listQuests(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) {
            ctx.getSource().sendFailure(Translation.tr("surezs_quest.command.player_data_unavailable"));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Translation.tr("surezs_quest.command.list_header", player.getName().getString()), false);
        ctx.getSource().sendSuccess(() -> Component.literal(Translation.tr("surezs_quest.command.list_accepted").getString() + data.acceptedQuests()), false);
        ctx.getSource().sendSuccess(() -> Component.literal(Translation.tr("surezs_quest.command.list_declined").getString() + data.declinedQuests()), false);

        // show server quest state
        var serverData = QuestDataManager.INSTANCE.getServerData();
        var serverAccepted = new ArrayList<String>();
        var serverClaimed = new ArrayList<String>();
        serverData.quests().forEach((qid, entry) -> {
            if (entry.acceptedPlayers().contains(player.getUUID())) serverAccepted.add(qid.toString());
            if (entry.completedPlayers().contains(player.getUUID())) serverClaimed.add(qid.toString());
        });
        if (!serverAccepted.isEmpty())
            ctx.getSource().sendSuccess(() -> Component.literal(Translation.tr("surezs_quest.command.list_server_accepted").getString() + serverAccepted), false);
        if (!serverClaimed.isEmpty())
            ctx.getSource().sendSuccess(() -> Component.literal(Translation.tr("surezs_quest.command.list_server_claimed").getString() + serverClaimed), false);
        return 1;
    }

    private static int reloadData(CommandContext<CommandSourceStack> ctx) {
        DataLoaders.reload();
        Translation.reload();
        ctx.getSource().sendSuccess(() -> Translation.tr("surezs_quest.command.reload_ok"), true);
        return 1;
    }

    private static int warnResetAll(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        ctx.getSource().sendSuccess(() -> Translation.tr("surezs_quest.command.reset_warn", player.getName().getString(), player.getName().getString()), false);
        return 0;
    }

    private static int resetAllQuests(CommandContext<CommandSourceStack> ctx, ServerPlayer player) {
        var data = QuestDataManager.INSTANCE.getPlayerData(player.getUUID());
        if (data == null) {
            ctx.getSource().sendFailure(Translation.tr("surezs_quest.command.player_data_unavailable"));
            return 0;
        }
        data.clear();
        QuestDataManager.INSTANCE.savePlayer(player.getUUID());
        ctx.getSource().sendSuccess(() -> Translation.tr("surezs_quest.command.reset_all_ok", player.getName().getString()), true);
        return 1;
    }

    private static int resetServerQuest(CommandContext<CommandSourceStack> ctx, ResourceLocation questId) {
        var serverData = QuestDataManager.INSTANCE.getServerData();
        serverData.resetProgress(questId);
        QuestDataManager.INSTANCE.saveServer();
        ctx.getSource().sendSuccess(() -> Translation.tr("surezs_quest.command.server_reset_ok", questId.toString()), true);
        return 1;
    }

    private static int startEditor(CommandContext<CommandSourceStack> ctx, int port) {
        if (Surezs_quest.WEB_SERVER.isRunning()) {
            ctx.getSource().sendSuccess(() ->
                Component.literal("Web editor is already running"), false);
            return 0;
        }
        int actualPort = port > 0 ? port : Config.INSTANCE.webEditorPort();
        if (actualPort <= 0) actualPort = 17080;

        Path configDir = FMLPaths.CONFIGDIR.get().resolve(Surezs_quest.MODID);
        try {
            Surezs_quest.WEB_SERVER.start(actualPort,
                configDir.resolve("quests"),
                configDir.resolve("npcs"),
                configDir);
            int p = actualPort;
            ctx.getSource().sendSuccess(() ->
                Component.literal("Web editor started at http://localhost:" + p), true);
            return 1;
        } catch (Exception e) {
            ctx.getSource().sendFailure(
                Component.literal("Failed to start web editor: " + e.getMessage()));
            return 0;
        }
    }
}
