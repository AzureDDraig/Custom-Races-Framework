package ddraig.net.customraces.fabric;

import ddraig.net.customraces.CustomRaces;
import ddraig.net.customraces.client.CustomRacesClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;

public class CustomRacesFabric implements ModInitializer, ClientModInitializer {
    @Override
    public void onInitialize() {
        CustomRaces.init();
    }

    @Override
    public void onInitializeClient() {
        CustomRacesClient.init();
    }
}
