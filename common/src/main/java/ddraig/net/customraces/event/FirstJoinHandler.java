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
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                RaceData race = RaceRegistry.getPlayerRace(serverPlayer.getUUID());
                if (race != null) {
                    PehkuiIntegration.applyRaceScales(serverPlayer, race);
                } else if (RaceRegistry.autoOpenSelectionOnJoin) {
                    ModPackets.openRaceSelection(serverPlayer);
                }
            }
        });
    }
}
