package ddraig.net.customraces.event;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import ddraig.net.customraces.integration.PehkuiIntegration;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles Were-race transformation checks based on Moon Phase (Full Moon / New Moon / Night) and active abilities.
 */
public class WereRaceTransformHandler {
    private static final Map<UUID, Boolean> TRANSFORMED_PLAYERS = new ConcurrentHashMap<>();
    private static final UUID WERE_HEALTH_MOD_UUID = UUID.fromString("c02821b0-1010-4100-a001-000000000010");
    private static final UUID WERE_DAMAGE_MOD_UUID = UUID.fromString("c02821b0-1010-4100-a001-000000000011");
    private static final UUID WERE_SPEED_MOD_UUID = UUID.fromString("c02821b0-1010-4100-a001-000000000012");

    public static void init() {
        TickEvent.PLAYER_POST.register(player -> {
            if (player instanceof ServerPlayer serverPlayer) {
                if (serverPlayer.tickCount % 40 == 0) { // Check every 2 seconds
                    checkTransformation(serverPlayer);
                }
            }
        });
    }

    public static void onPlayerStartTracking(ServerPlayer trackingPlayer, ServerPlayer targetPlayer) {
        if (trackingPlayer == null || targetPlayer == null) return;
        if (isTransformed(targetPlayer.getUUID())) {
            ddraig.net.customraces.network.ModPackets.sendWereStateToPlayer(trackingPlayer, targetPlayer.getUUID(), true);
        }
    }

    public static void syncAllWereStatesTo(ServerPlayer targetPlayer) {
        if (targetPlayer == null || targetPlayer.getServer() == null) return;
        for (Map.Entry<UUID, Boolean> entry : TRANSFORMED_PLAYERS.entrySet()) {
            if (Boolean.TRUE.equals(entry.getValue())) {
                ddraig.net.customraces.network.ModPackets.sendWereStateToPlayer(targetPlayer, entry.getKey(), true);
            }
        }
    }

    public static boolean isTransformed(UUID uuid) {
        if (uuid == null) return false;
        if (TRANSFORMED_PLAYERS.getOrDefault(uuid, false)) return true;
        return ddraig.net.customraces.client.ClientWereState.isTransformed(uuid);
    }

    public static void checkTransformation(ServerPlayer player) {
        if (player == null) return;

        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
        if (race == null || !race.enableWereRace) {
            if (isTransformed(player.getUUID())) {
                revertWereForm(player, race);
            }
            return;
        }

        ServerLevel level = player.serverLevel();
        boolean isNight = level.isNight();
        int moonPhase = level.getMoonPhase();

        boolean conditionMet = false;
        String condition = race.wereTriggerCondition != null ? race.wereTriggerCondition.toUpperCase() : "FULL_MOON";

        switch (condition) {
            case "FULL_MOON" -> conditionMet = isNight && moonPhase == 0;
            case "NEW_MOON" -> conditionMet = isNight && moonPhase == 4;
            case "NIGHT" -> conditionMet = isNight;
            case "DAY" -> conditionMet = !isNight;
            case "WATER", "SUBMERGED" -> conditionMet = player.isInWaterOrBubble() || player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
            case "RAGE", "LOW_HEALTH" -> conditionMet = player.getHealth() <= (player.getMaxHealth() * 0.30f);
            case "KEY", "MANUAL" -> conditionMet = isTransformed(player.getUUID());
            default -> conditionMet = isNight && moonPhase == 0;
        }

        boolean currentlyTransformed = isTransformed(player.getUUID());
        if (conditionMet && !currentlyTransformed) {
            transformIntoWereForm(player, race);
        } else if (!conditionMet && currentlyTransformed && !"MANUAL".equalsIgnoreCase(condition) && !"KEY".equalsIgnoreCase(condition)) {
            revertWereForm(player, race);
        }
    }

        private static final Map<UUID, Long> TRANSFORM_COOLDOWNS = new java.util.concurrent.ConcurrentHashMap<>();

        public static void toggleManualWereForm(ServerPlayer player) {
        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
        if (race == null || !race.enableWereRace) {
            player.displayClientMessage(Component.literal("§c[!] Your current race does not have a Were-form."), true);
            return;
        }

        long now = System.currentTimeMillis();
        long last = TRANSFORM_COOLDOWNS.getOrDefault(player.getUUID(), 0L);
        if (now - last < 1000L) {
            player.displayClientMessage(Component.literal("§c[!] Transformation is on cooldown."), true);
            return;
        }
        TRANSFORM_COOLDOWNS.put(player.getUUID(), now);

        String condition = race.wereTriggerCondition != null ? race.wereTriggerCondition.toUpperCase() : "FULL_MOON";
        boolean current = isTransformed(player.getUUID());
        if (!current) {
            if (!"MANUAL".equals(condition) && !"KEY".equals(condition)) {
                ServerLevel level = player.serverLevel();
                boolean isNight = level.isNight();
                int moonPhase = level.getMoonPhase();
                boolean met = switch (condition) {
                    case "FULL_MOON" -> isNight && moonPhase == 0;
                    case "NEW_MOON" -> isNight && moonPhase == 4;
                    case "NIGHT" -> isNight;
                    case "DAY" -> !isNight;
                    case "WATER", "SUBMERGED" -> player.isInWaterOrBubble() || player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
                    case "RAGE", "LOW_HEALTH" -> player.getHealth() <= (player.getMaxHealth() * 0.30f);
                    default -> isNight && moonPhase == 0;
                };

                if (!met) {
                    player.displayClientMessage(Component.literal("§c[!] Were-form requires trigger condition: " + condition), true);
                    return;
                }
            }
            transformIntoWereForm(player, race);
        } else {
            revertWereForm(player, race);
        }
    }

    public static void transformIntoWereForm(ServerPlayer player, RaceData race) {
        TRANSFORMED_PLAYERS.put(player.getUUID(), true);
        ddraig.net.customraces.network.ModPackets.syncWereStateToAll(player.getServer(), player.getUUID(), true);

        ServerLevel level = player.serverLevel();
        SoundEvent tfSound = SoundEvents.WOLF_HOWL;
        if (race != null && race.wereTransformSound != null && !race.wereTransformSound.trim().isEmpty()) {
            try {
                net.minecraft.resources.ResourceLocation loc = new net.minecraft.resources.ResourceLocation(race.wereTransformSound.trim());
                tfSound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getOptional(loc).orElse(SoundEvents.WOLF_HOWL);
            } catch (Exception ignored) {}
        }
        level.playSound(null, player.blockPosition(), tfSound, SoundSource.PLAYERS, 1.2f, 0.8f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.6, 1.0, 0.6, 0.1);
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.8, 0.4, 0.05);

        // Apply Were-Form Attribute Modifiers
        clearWereModifiers(player);
        player.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, 12000, 0, false, false, true));
        if (race != null) {
            AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttr != null && race.wereHealthBonus > 0) {
                if (healthAttr.getModifier(WERE_HEALTH_MOD_UUID) == null) {
                    healthAttr.addTransientModifier(new AttributeModifier(WERE_HEALTH_MOD_UUID, "Were Health Bonus", race.wereHealthBonus, AttributeModifier.Operation.ADDITION));
                }
            }

            AttributeInstance damageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damageAttr != null && race.wereDamageBonus > 0) {
                if (damageAttr.getModifier(WERE_DAMAGE_MOD_UUID) == null) {
                    damageAttr.addTransientModifier(new AttributeModifier(WERE_DAMAGE_MOD_UUID, "Were Damage Bonus", race.wereDamageBonus, AttributeModifier.Operation.ADDITION));
                }
            }

            AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null && race.wereSpeedBonus > 0) {
                if (speedAttr.getModifier(WERE_SPEED_MOD_UUID) == null) {
                    speedAttr.addTransientModifier(new AttributeModifier(WERE_SPEED_MOD_UUID, "Were Speed Bonus", race.wereSpeedBonus, AttributeModifier.Operation.ADDITION));
                }
            }

            // Apply Were Scales
            PehkuiIntegration.applyRaceScales(player, race);
        }

        player.sendSystemMessage(Component.literal("§c§l[!] You have transformed into your Were-form!"), true);
    }

    public static void revertWereForm(ServerPlayer player, RaceData race) {
        TRANSFORMED_PLAYERS.remove(player.getUUID());
        ddraig.net.customraces.network.ModPackets.syncWereStateToAll(player.getServer(), player.getUUID(), false);
        clearWereModifiers(player);
        player.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.8, 0.5, 0.1);

        if (race != null) {
            PehkuiIntegration.applyRaceScales(player, race);
        }

        player.sendSystemMessage(Component.literal("§a[!] You have reverted from your Were-form."), true);
    }

    private static void clearWereModifiers(ServerPlayer player) {
        if (player == null) return;
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null && healthAttr.getModifier(WERE_HEALTH_MOD_UUID) != null) healthAttr.removeModifier(WERE_HEALTH_MOD_UUID);

        AttributeInstance damageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null && damageAttr.getModifier(WERE_DAMAGE_MOD_UUID) != null) damageAttr.removeModifier(WERE_DAMAGE_MOD_UUID);

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null && speedAttr.getModifier(WERE_SPEED_MOD_UUID) != null) speedAttr.removeModifier(WERE_SPEED_MOD_UUID);
    }
}
