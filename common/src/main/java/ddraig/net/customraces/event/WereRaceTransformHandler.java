package ddraig.net.customraces.event;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import ddraig.net.customraces.integration.PehkuiIntegration;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

    public static boolean isTransformed(UUID uuid) {
        return TRANSFORMED_PLAYERS.getOrDefault(uuid, false);
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

    public static void toggleManualWereForm(ServerPlayer player) {
        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
        if (race == null || !race.enableWereRace) return;

        boolean current = isTransformed(player.getUUID());
        if (!current) {
            transformIntoWereForm(player, race);
        } else {
            revertWereForm(player, race);
        }
    }

    public static void transformIntoWereForm(ServerPlayer player, RaceData race) {
        TRANSFORMED_PLAYERS.put(player.getUUID(), true);

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.2f, 0.8f);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.6, 1.0, 0.6, 0.1);
        level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.8, 0.4, 0.05);

        // Apply Were-Form Attribute Modifiers
        clearWereModifiers(player);
        if (race != null) {
            AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
            if (healthAttr != null && race.wereHealthBonus > 0) {
                healthAttr.addTransientModifier(new AttributeModifier(WERE_HEALTH_MOD_UUID, "Were Health Bonus", race.wereHealthBonus, AttributeModifier.Operation.ADDITION));
            }

            AttributeInstance damageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
            if (damageAttr != null && race.wereDamageBonus > 0) {
                damageAttr.addTransientModifier(new AttributeModifier(WERE_DAMAGE_MOD_UUID, "Were Damage Bonus", race.wereDamageBonus, AttributeModifier.Operation.ADDITION));
            }

            AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null && race.wereSpeedBonus > 0) {
                speedAttr.addTransientModifier(new AttributeModifier(WERE_SPEED_MOD_UUID, "Were Speed Bonus", race.wereSpeedBonus, AttributeModifier.Operation.ADDITION));
            }

            // Apply Were Scales
            PehkuiIntegration.applyRaceScales(player, race);
        }

        player.sendSystemMessage(Component.literal("§c§l[!] You have transformed into your Were-form!"), true);
    }

    public static void revertWereForm(ServerPlayer player, RaceData race) {
        TRANSFORMED_PLAYERS.remove(player.getUUID());
        clearWereModifiers(player);

        ServerLevel level = player.serverLevel();
        level.playSound(null, player.blockPosition(), SoundEvents.POWDER_SNOW_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
        level.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.8, 0.5, 0.1);

        if (race != null) {
            PehkuiIntegration.applyRaceScales(player, race);
        }

        player.sendSystemMessage(Component.literal("§a[!] You have reverted from your Were-form."), true);
    }

    private static void clearWereModifiers(ServerPlayer player) {
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) healthAttr.removeModifier(WERE_HEALTH_MOD_UUID);

        AttributeInstance damageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) damageAttr.removeModifier(WERE_DAMAGE_MOD_UUID);

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.removeModifier(WERE_SPEED_MOD_UUID);
    }
}
