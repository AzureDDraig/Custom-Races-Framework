package ddraig.net.customraces.fabric;

import ddraig.net.customraces.CustomRaces;
import ddraig.net.customraces.event.WereRaceTransformHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.minecraft.server.level.ServerPlayer;

public class CustomRacesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        CustomRaces.init();

        EntityTrackingEvents.START_TRACKING.register((trackedEntity, player) -> {
            if (trackedEntity instanceof ServerPlayer targetPlayer && player != null) {
                WereRaceTransformHandler.onPlayerStartTracking(player, targetPlayer);
            }
        });
    }
}
