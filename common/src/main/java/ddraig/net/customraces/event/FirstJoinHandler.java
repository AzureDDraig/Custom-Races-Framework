package ddraig.net.customraces.event;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import ddraig.net.customraces.integration.PehkuiIntegration;
import ddraig.net.customraces.network.ModPackets;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles initial player join events, prompting race selection if unassigned.
 */
public class FirstJoinHandler {

    public static void init() {
        PlayerEvent.PLAYER_JOIN.register(serverPlayer -> {
            if (serverPlayer != null && serverPlayer.getServer() != null) {
                ModPackets.syncRacesToAll(serverPlayer.getServer());
                WereRaceTransformHandler.syncAllWereStatesTo(serverPlayer);

                // Dispatch dynamic server pack info packet if pack is available
                if (ddraig.net.customraces.pack.ServerPackManager.hasPack() &&
                        (ddraig.net.customraces.pack.ServerPackHttpServer.isRunning() || serverPlayer.getServer().isSingleplayer())) {
                    ModPackets.sendServerPackInfo(serverPlayer);
                }
            }

            RaceData race = RaceRegistry.getPlayerRace(serverPlayer.getUUID());
            if (race != null) {
                PehkuiIntegration.applyRaceScales(serverPlayer, race);
            } else if (RaceRegistry.autoOpenSelectionOnJoin) {
                ModPackets.openRaceSelection(serverPlayer);
            }
        });

        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
            if (newPlayer != null) {
                RaceData race = RaceRegistry.getPlayerRace(newPlayer.getUUID());
                if (race != null) {
                    PehkuiIntegration.applyRaceScales(newPlayer, race);
                }
                WereRaceTransformHandler.syncAllWereStatesTo(newPlayer);
                if (WereRaceTransformHandler.isTransformed(newPlayer.getUUID())) {
                    ModPackets.syncWereStateToAll(newPlayer.getServer(), newPlayer.getUUID(), true);
                }
            }
        });
    }
}
