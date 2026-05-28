package org.surez.surezs_quest;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;
import org.surez.surezs_quest.command.QuestCommand;
import org.surez.surezs_quest.data.DataLoaders;
import org.surez.surezs_quest.network.NetworkHandler;
import org.surez.surezs_quest.storage.QuestDataManager;
import org.surez.surezs_quest.trigger.GameEventListener;
import org.surez.surezs_quest.trigger.TriggerRegistry;
import org.surez.surezs_quest.trigger.handlers.*;

@Mod(Surezs_quest.MODID)
public class Surezs_quest {

    public static final String MODID = "surezs_quest";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Surezs_quest(IEventBus modEventBus, ModContainer modContainer) {
        var configDir = FMLPaths.CONFIGDIR.get().resolve(MODID);
        Config.load(configDir);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(NetworkHandler::register);

        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new GameEventListener());

        DataLoaders.init(configDir);

        TriggerRegistry.register(new InventoryHandler());
        TriggerRegistry.register(new LocationHandler());
        TriggerRegistry.register(new KillEntityHandler());
        TriggerRegistry.register(new CraftHandler());

        QuestChainHandler.register();

        QuestDataManager.INSTANCE.init(FMLPaths.CONFIGDIR.get().resolve(MODID));
        NeoForge.EVENT_BUS.register(QuestDataManager.INSTANCE);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Surez's Quest loading...");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        QuestCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Surez's Quest ready");
    }
}
