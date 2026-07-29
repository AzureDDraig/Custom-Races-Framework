package ddraig.net.customraces.fabric.client;

import ddraig.net.customraces.client.CustomRacesClient;
import ddraig.net.customraces.client.render.PlayerRaceLayer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;

@Environment(EnvType.CLIENT)
public class CustomRacesFabricClient implements ClientModInitializer {
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
