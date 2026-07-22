package ddraig.net.customraces.fabric;

import ddraig.net.customraces.CustomRaces;
import ddraig.net.customraces.client.CustomRacesClient;
import ddraig.net.customraces.client.render.PlayerRaceLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

public class CustomRacesFabric implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        CustomRaces.init();
    }

    @Override
    public void onInitializeClient() {
        CustomRacesClient.init();

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof PlayerRenderer playerRenderer) {
                registrationHelper.register(new PlayerRaceLayer(playerRenderer));
            }
        });
    }
}
