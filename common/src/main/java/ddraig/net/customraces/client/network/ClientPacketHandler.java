package ddraig.net.customraces.client.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ddraig.net.customraces.client.ClientWereState;
import ddraig.net.customraces.client.gui.RaceCreatorScreen;
import ddraig.net.customraces.client.gui.RaceSelectionScreen;
import ddraig.net.customraces.client.render.WereModelRenderer;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import ddraig.net.customraces.integration.PehkuiIntegration;
import ddraig.net.customraces.network.ModPackets;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.UUID;

public class ClientPacketHandler {
    private static final Gson GSON = new Gson();

    public static void registerClientReceivers() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ModPackets.SYNC_WERE_STATE_ID, (buf, context) -> {
            UUID pUuid = buf.readUUID();
            boolean isTransformed = buf.readBoolean();
            context.queue(() -> {
                ClientWereState.setTransformed(pUuid, isTransformed);
                Minecraft mc = Minecraft.getInstance();
                if (mc.level != null) {
                    net.minecraft.world.entity.player.Player target = mc.level.getPlayerByUUID(pUuid);
                    if (target != null) {
                        RaceData race = RaceRegistry.getPlayerRace(pUuid);
                        PehkuiIntegration.applyRaceScales(target, race);
                        target.refreshDimensions();
                    }
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ModPackets.SYNC_RACES_ID, (buf, context) -> {
            String racesJson = buf.readUtf(1048576);
            String playersJson = buf.readUtf(1048576);
            context.queue(() -> {
                try {
                    Type raceMapType = new TypeToken<Map<String, RaceData>>() {}.getType();
                    Map<String, RaceData> map = GSON.fromJson(racesJson, raceMapType);
                    if (map != null) {
                        RaceRegistry.loadedRaces.clear();
                        RaceRegistry.loadedRaces.putAll(map);
                        WereModelRenderer.clearCaches();
                    }

                    Type playerMapType = new TypeToken<Map<String, String>>() {}.getType();
                    Map<String, String> pMap = GSON.fromJson(playersJson, playerMapType);
                    if (pMap != null) {
                        RaceRegistry.playerRaces.clear();
                        for (Map.Entry<String, String> entry : pMap.entrySet()) {
                            try {
                                RaceRegistry.playerRaces.put(UUID.fromString(entry.getKey()), entry.getValue());
                            } catch (Exception ignored) {}
                        }
                    }

                    if (Minecraft.getInstance().player != null) {
                        RaceData pRace = RaceRegistry.getPlayerRace(Minecraft.getInstance().player.getUUID());
                        PehkuiIntegration.applyRaceScales(Minecraft.getInstance().player, pRace);
                    }
                } catch (Exception e) {
                    System.err.println("[CustomRaces] Failed to process S2C_SyncRacesPacket: " + e.getMessage());
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ModPackets.OPEN_SELECTION_ID, (buf, context) -> {
            context.queue(() -> {
                Minecraft.getInstance().setScreen(new RaceSelectionScreen());
            });
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ModPackets.OPEN_CREATOR_ID, (buf, context) -> {
            context.queue(() -> {
                Minecraft.getInstance().setScreen(new RaceCreatorScreen(null));
            });
        });
    }
}
