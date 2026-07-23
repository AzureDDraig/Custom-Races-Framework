package ddraig.net.customraces;

import ddraig.net.customraces.command.CustomRacesCommands;
import ddraig.net.customraces.data.RaceRegistry;
import ddraig.net.customraces.event.FirstJoinHandler;
import ddraig.net.customraces.item.RaceOrbItem;
import ddraig.net.customraces.network.ModPackets;
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
        RaceRegistry.init();
        ModPackets.register();
        CustomRacesCommands.init();
        FirstJoinHandler.init();
        ddraig.net.customraces.event.WereRaceTransformHandler.init();
        ddraig.net.customraces.event.RaceSoundHandler.init();
        ddraig.net.customraces.event.MobAllianceHandler.init();

        // Passive ability player tick loop
        TickEvent.PLAYER_POST.register(player -> {
            ddraig.net.customraces.ability.PassiveAbilityHandler.tickPlayer(player);
        });
    }
}
