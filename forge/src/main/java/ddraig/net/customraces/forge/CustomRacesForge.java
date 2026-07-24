package ddraig.net.customraces.forge;

import ddraig.net.customraces.CustomRaces;
import ddraig.net.customraces.client.CustomRacesClient;
import ddraig.net.customraces.client.render.PlayerRaceLayer;
import ddraig.net.customraces.event.WereRaceTransformHandler;
import dev.architectury.platform.forge.EventBuses;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CustomRaces.MOD_ID)
public class CustomRacesForge {
    public CustomRacesForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(CustomRaces.MOD_ID, modEventBus);
        CustomRaces.init();

        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.StartTracking event) -> {
            if (event.getTarget() instanceof ServerPlayer targetPlayer && event.getEntity() instanceof ServerPlayer trackingPlayer) {
                WereRaceTransformHandler.onPlayerStartTracking(trackingPlayer, targetPlayer);
            }
        });

        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            CustomRacesClient.init();
            modEventBus.addListener((EntityRenderersEvent.AddLayers event) -> {
                for (String skinName : event.getSkins()) {
                    PlayerRenderer renderer = event.getSkin(skinName);
                    if (renderer != null) {
                        renderer.addLayer(new PlayerRaceLayer(renderer));
                    }
                }
            });
        });
    }
}
