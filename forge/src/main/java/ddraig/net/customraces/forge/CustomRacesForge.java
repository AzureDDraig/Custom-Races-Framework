package ddraig.net.customraces.forge;

import ddraig.net.customraces.CustomRaces;
import ddraig.net.customraces.client.CustomRacesClient;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CustomRaces.MOD_ID)
public class CustomRacesForge {
    public CustomRacesForge() {
        EventBuses.registerModEventBus(CustomRaces.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        CustomRaces.init();
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> CustomRacesClient::init);
    }
}
