package ddraig.net.customraces.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks client-side Were-form transformation states synced from the server.
 */
public class ClientWereState {
    public static final Map<UUID, Boolean> TRANSFORMED_PLAYERS = new ConcurrentHashMap<>();

    public static boolean isTransformed(UUID uuid) {
        return uuid != null && TRANSFORMED_PLAYERS.getOrDefault(uuid, false);
    }

    public static void setTransformed(UUID uuid, boolean transformed) {
        if (uuid == null) return;
        if (transformed) {
            TRANSFORMED_PLAYERS.put(uuid, true);
        } else {
            TRANSFORMED_PLAYERS.remove(uuid);
        }
    }

    public static void clear() {
        TRANSFORMED_PLAYERS.clear();
    }
}
