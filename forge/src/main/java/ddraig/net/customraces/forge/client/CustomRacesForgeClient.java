package ddraig.net.customraces.forge.client;

import ddraig.net.customraces.client.CustomRacesClient;
import ddraig.net.customraces.client.render.PlayerRaceLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;

@OnlyIn(Dist.CLIENT)
public class CustomRacesForgeClient {
    public static void init(IEventBus modEventBus) {
        CustomRacesClient.init();
        modEventBus.addListener((EntityRenderersEvent.AddLayers event) -> {
            for (String skinName : event.getSkins()) {
                PlayerRenderer renderer = event.getSkin(skinName);
                if (renderer != null) {
                    renderer.addLayer(new PlayerRaceLayer(renderer));
                }
            }
        });
    }
}
