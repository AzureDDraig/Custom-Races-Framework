package ddraig.net.customraces;

import ddraig.net.customraces.command.CustomRacesCommands;
import ddraig.net.customraces.data.RaceRegistry;
import ddraig.net.customraces.event.FirstJoinHandler;
import ddraig.net.customraces.item.RaceOrbItem;
import ddraig.net.customraces.network.ModPackets;
import ddraig.net.customraces.pack.ServerPackHttpServer;
import ddraig.net.customraces.pack.ServerPackManager;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class CustomRaces {
    public static final String MOD_ID = "customraces";
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> ORB_OF_REBIRTH = ITEMS.register("orb_of_rebirth",
            () -> new RaceOrbItem(new Item.Properties().stacksTo(16)));

    public static void init() {
        ITEMS.register();
        ddraig.net.azureframelib.resource.AzureResourceManager.init();
        RaceRegistry.init();
        ModPackets.register();
        CustomRacesCommands.init();
        FirstJoinHandler.init();
        ddraig.net.customraces.event.WereRaceTransformHandler.init();
        ddraig.net.customraces.event.RaceSoundHandler.init();
        ddraig.net.customraces.event.MobAllianceHandler.init();
        ddraig.net.customraces.event.CustomSpawnHandler.init();

        // Server lifecycle: generate resource pack and start HTTP server on startup
        LifecycleEvent.SERVER_STARTING.register(server -> {
            try {
                ServerPackManager.buildPack();
                int port = RaceRegistry.getServerPackPort();
                ServerPackHttpServer.start(port);
            } catch (Exception e) {
                System.err.println("[CustomRaces] Failed to initialize ServerPackManager / ServerPackHttpServer on server start: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Server lifecycle: cleanly stop HTTP server when server stops
        LifecycleEvent.SERVER_STOPPING.register(server -> {
            try {
                ServerPackHttpServer.stop();
            } catch (Exception e) {
                System.err.println("[CustomRaces] Failed to stop ServerPackHttpServer on server stop: " + e.getMessage());
            }
        });

        // Passive ability and teleport warmup player tick loop
        TickEvent.PLAYER_POST.register(player -> {
            ddraig.net.customraces.ability.PassiveAbilityHandler.tickPlayer(player);
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
                ddraig.net.azureframelib.util.TeleportHelper.tickPlayer(sp);
            }
        });
    }
}
