package ddraig.net.customraces.event;

import ddraig.net.customraces.data.MobAllianceData;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Intercepts mob AI targeting to enforce race mob alliances (Friendly/Neutral/Allied).
 */
public class MobAllianceHandler {

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer && serverPlayer.tickCount % 10 == 0) {
                RaceData race = RaceRegistry.getPlayerRace(serverPlayer.getUUID());
                if (race == null || race.alliances == null || race.alliances.isEmpty()) return;

                AABB searchBox = serverPlayer.getBoundingBox().inflate(24.0);
                List<Mob> nearbyMobs = serverPlayer.level().getEntitiesOfClass(Mob.class, searchBox);

                for (Mob mob : nearbyMobs) {
                    if (mob == null || !mob.isAlive()) continue;
                    ResourceLocation mobLoc = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
                    if (mobLoc == null) continue;
                    String mobId = mobLoc.toString();

                    for (MobAllianceData alliance : race.alliances) {
                        if (alliance != null && alliance.mobId != null && mobId.equalsIgnoreCase(alliance.mobId.trim())) {
                            String stance = alliance.stance != null ? alliance.stance.toLowerCase() : "neutral";
                            if ("neutral".equals(stance) || "allied".equals(stance) || "friendly".equals(stance)) {
                                if (mob.getTarget() == serverPlayer) {
                                    mob.setTarget(null);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        });
    }
}
