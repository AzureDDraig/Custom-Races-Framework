package ddraig.net.customraces.event;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/**
 * Handles custom race dimension spawning and respawn positioning.
 */
public class CustomSpawnHandler {

    public static void init() {
        PlayerEvent.PLAYER_RESPAWN.register((newPlayer, conqueredEnd) -> {
            if (newPlayer != null) {
                // If player has no bed/anchor spawn, use race spawn dimension
                if (newPlayer.getRespawnPosition() == null) {
                    teleportToRaceSpawnDimension(newPlayer);
                }
            }
        });
    }

    public static void teleportToRaceSpawnDimension(ServerPlayer player) {
        if (player == null || player.getServer() == null) return;
        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
        if (race == null || race.spawnDimension == null || race.spawnDimension.trim().isEmpty()) return;

        try {
            ResourceLocation dimLoc = new ResourceLocation(race.spawnDimension.trim());
            ResourceKey<Level> dimKey = ResourceKey.create(Registries.DIMENSION, dimLoc);
            ServerLevel targetLevel = player.getServer().getLevel(dimKey);

            if (targetLevel != null) {
                BlockPos spawnPos = targetLevel.getSharedSpawnPos();

                if (race.spawnBiome != null && !race.spawnBiome.trim().isEmpty()) {
                    try {
                        ResourceLocation biomeLoc = new ResourceLocation(race.spawnBiome.trim());
                        for (java.lang.reflect.Method m : targetLevel.getClass().getMethods()) {
                            if (m.getName().toLowerCase().contains("findnearestbiome")) {
                                try {
                                    m.setAccessible(true);
                                    java.util.function.Predicate<net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome>> pred = b -> b.unwrapKey().map(k -> k.location().equals(biomeLoc)).orElse(false);
                                    Object res = m.invoke(targetLevel, pred, spawnPos, 6400, 32, 64);
                                    if (res instanceof com.mojang.datafixers.util.Pair<?, ?> pair) {
                                        if (pair.getFirst() instanceof BlockPos bp) {
                                            spawnPos = targetLevel.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bp);
                                            break;
                                        }
                                    }
                                } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                }

                player.teleportTo(targetLevel, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            }
        } catch (Exception e) {
            System.err.println("[CustomRaces] Failed to teleport to race spawn dimension: " + e.getMessage());
        }
    }
}
