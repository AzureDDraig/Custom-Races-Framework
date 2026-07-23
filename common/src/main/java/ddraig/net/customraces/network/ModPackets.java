package ddraig.net.customraces.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ddraig.net.customraces.ability.ActiveAbilityHandler;
import ddraig.net.customraces.client.gui.RaceCreatorScreen;
import ddraig.net.customraces.client.gui.RaceSelectionScreen;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import ddraig.net.customraces.integration.PehkuiIntegration;
import dev.architectury.networking.NetworkManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Handles network packet registration and communication between client and server.
 */
public class ModPackets {
    private static final Gson GSON = new Gson();

    public static final ResourceLocation SYNC_RACES_ID = new ResourceLocation("customraces", "sync_races");
    public static final ResourceLocation SAVE_RACE_ID = new ResourceLocation("customraces", "save_race");
    public static final ResourceLocation DELETE_RACE_ID = new ResourceLocation("customraces", "delete_race");
    public static final ResourceLocation SET_PLAYER_RACE_ID = new ResourceLocation("customraces", "set_player_race");
    public static final ResourceLocation TRIGGER_ABILITY_ID = new ResourceLocation("customraces", "trigger_ability");
    public static final ResourceLocation OPEN_SELECTION_ID = new ResourceLocation("customraces", "open_selection");
    public static final ResourceLocation OPEN_CREATOR_ID = new ResourceLocation("customraces", "open_creator");
    public static final ResourceLocation SYNC_WERE_STATE_ID = new ResourceLocation("customraces", "sync_were_state");
    public static final ResourceLocation TOGGLE_WERE_FORM_ID = new ResourceLocation("customraces", "toggle_were_form");

    public static void register() {
        // Register Client-Bound (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_WERE_STATE_ID, (buf, context) -> {
            UUID pUuid = buf.readUUID();
            boolean isTransformed = buf.readBoolean();
            context.queue(() -> {
                ddraig.net.customraces.client.ClientWereState.setTransformed(pUuid, isTransformed);
            });
        });
        // Register Client-Bound (S2C)
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SYNC_RACES_ID, (buf, context) -> {
            String racesJson = buf.readUtf(1048576);
            String playersJson = buf.readUtf(1048576);
            context.queue(() -> {
                try {
                    Type raceMapType = new TypeToken<Map<String, RaceData>>() {}.getType();
                    Map<String, RaceData> map = GSON.fromJson(racesJson, raceMapType);
                    if (map != null) {
                        RaceRegistry.loadedRaces.clear();
                        RaceRegistry.loadedRaces.putAll(map);
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

                    // Re-apply local player scale
                    if (Minecraft.getInstance().player != null) {
                        RaceData pRace = RaceRegistry.getPlayerRace(Minecraft.getInstance().player.getUUID());
                        PehkuiIntegration.applyRaceScales(Minecraft.getInstance().player, pRace);
                    }
                } catch (Exception e) {
                    System.err.println("[CustomRaces] Failed to process S2C_SyncRacesPacket: " + e.getMessage());
                }
            });
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OPEN_SELECTION_ID, (buf, context) -> {
            context.queue(() -> {
                Minecraft.getInstance().setScreen(new RaceSelectionScreen());
            });
        });

        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OPEN_CREATOR_ID, (buf, context) -> {
            context.queue(() -> {
                Minecraft.getInstance().setScreen(new RaceCreatorScreen(null));
            });
        });

        // Register Server-Bound (C2S)
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SAVE_RACE_ID, (buf, context) -> {
            String raceJson = buf.readUtf(1048576);
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player.hasPermissions(2)) {
                context.queue(() -> {
                    try {
                        RaceData race = GSON.fromJson(raceJson, RaceData.class);
                        if (race != null && race.id != null && !race.id.isEmpty()) {
                            RaceRegistry.loadedRaces.put(race.id, race);
                            RaceRegistry.saveRaces();
                            syncRacesToAll(player.getServer());
                        }
                    } catch (Exception e) {
                        System.err.println("[CustomRaces] Failed to save race: " + e.getMessage());
                    }
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, DELETE_RACE_ID, (buf, context) -> {
            String raceId = buf.readUtf(256);
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player.hasPermissions(2)) {
                context.queue(() -> {
                    RaceRegistry.loadedRaces.remove(raceId);
                    RaceRegistry.saveRaces();
                    syncRacesToAll(player.getServer());
                });
            }
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, SET_PLAYER_RACE_ID, (buf, context) -> {
            String raceId = buf.readUtf(256);
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            context.queue(() -> {
                RaceRegistry.setPlayerRace(player.getUUID(), raceId);
                RaceData race = RaceRegistry.getRace(raceId);
                PehkuiIntegration.applyRaceScales(player, race);
                syncRacesToAll(player.getServer());
            });
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TRIGGER_ABILITY_ID, (buf, context) -> {
            int slot = buf.readInt();
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            context.queue(() -> {
                ActiveAbilityHandler.triggerAbility(player, slot);
            });
        });

        NetworkManager.registerReceiver(NetworkManager.Side.C2S, TOGGLE_WERE_FORM_ID, (buf, context) -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            context.queue(() -> {
                ddraig.net.customraces.event.WereRaceTransformHandler.toggleManualWereForm(player);
            });
        });
    }

    public static void syncRacesToAll(net.minecraft.server.MinecraftServer server) {
        if (server == null) return;
        String racesJson = GSON.toJson(RaceRegistry.loadedRaces);

        Map<String, String> stringPlayerMap = new HashMap<>();
        for (Map.Entry<UUID, String> entry : RaceRegistry.playerRaces.entrySet()) {
            stringPlayerMap.put(entry.getKey().toString(), entry.getValue());
        }
        String playersJson = GSON.toJson(stringPlayerMap);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUtf(racesJson, 262144);
            buf.writeUtf(playersJson, 262144);
            NetworkManager.sendToPlayer(player, SYNC_RACES_ID, buf);

            // Update player scales on server
            RaceData pRace = RaceRegistry.getPlayerRace(player.getUUID());
            PehkuiIntegration.applyRaceScales(player, pRace);
        }
    }

    public static void sendSaveRace(RaceData race) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(GSON.toJson(race), 65536);
        NetworkManager.sendToServer(SAVE_RACE_ID, buf);
    }

    public static void sendDeleteRace(String raceId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(raceId, 256);
        NetworkManager.sendToServer(DELETE_RACE_ID, buf);
    }

    public static void sendSetPlayerRace(String raceId) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeUtf(raceId, 256);
        NetworkManager.sendToServer(SET_PLAYER_RACE_ID, buf);
    }

    public static void sendTriggerAbility(int slot) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeInt(slot);
        NetworkManager.sendToServer(TRIGGER_ABILITY_ID, buf);
    }

    public static void sendToggleWereForm() {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToServer(TOGGLE_WERE_FORM_ID, buf);
    }

    public static void openRaceSelection(ServerPlayer player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToPlayer(player, OPEN_SELECTION_ID, buf);
    }

    public static void openCreatorGui(ServerPlayer player) {
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        NetworkManager.sendToPlayer(player, OPEN_CREATOR_ID, buf);
    }

    public static void syncWereStateToAll(net.minecraft.server.MinecraftServer server, UUID playerUuid, boolean isTransformed) {
        if (server == null || playerUuid == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
            buf.writeUUID(playerUuid);
            buf.writeBoolean(isTransformed);
            NetworkManager.sendToPlayer(p, SYNC_WERE_STATE_ID, buf);
        }
    }
}
