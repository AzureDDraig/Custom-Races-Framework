package ddraig.net.customraces.integration;

import ddraig.net.customraces.data.RaceData;
import dev.architectury.platform.Platform;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Dynamic integration bridge for Better Combat mod.
 * Handles attack range scaling, dual-wielding combo bonuses, and weapon restriction checks.
 */
public class BetterCombatIntegration {

    public static boolean isBetterCombatLoaded() {
        return Platform.isModLoaded("bettercombat");
    }

    /**
     * Calculates the scale-adjusted melee reach for Better Combat weapon collision.
     */
    public static double getScaleAdjustedReach(Player player, double baseReach, float playerScale) {
        float safeScale = Math.max(0.4f, Math.min(3.0f, playerScale));
        return baseReach * safeScale;
    }

    /**
     * Applies Better Combat racial combo effects (dual wield bonus, counter attack, critical bursts).
     */
    public static float applyComboModifiers(Player player, LivingEntity target, float baseDamage, List<String> passives) {
        if (player == null || passives == null || passives.isEmpty()) return baseDamage;

        float modifiedDamage = baseDamage;

        // Dual Wield Mastery: +20% damage if holding weapons in both hands
        if (passives.contains("dual_wield_mastery")) {
            ItemStack main = player.getMainHandItem();
            ItemStack off = player.getOffhandItem();
            if (!main.isEmpty() && !off.isEmpty()) {
                modifiedDamage *= 1.20f;
            }
        }

        // Counter Attack: +50% bonus damage if player was struck in the last 60 ticks (3 seconds)
        if (passives.contains("counter_attack") && player.hurtTime > 0) {
            modifiedDamage *= 1.50f;
            if (player.level() instanceof ServerLevel serverLevel && target != null) {
                serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1.0, target.getZ(), 12, 0.3, 0.3, 0.3, 0.15);
            }
        }

        // Critical Strike Boost: +30% critical bonus damage
        if (passives.contains("critical_strike_boost")) {
            modifiedDamage *= 1.30f;
            if (player.level() instanceof ServerLevel serverLevel && target != null) {
                serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT, target.getX(), target.getY() + 1.0, target.getZ(), 10, 0.2, 0.4, 0.2, 0.1);
            }
        }

        // Giant Slayer: +25% bonus damage against larger targets
        if (passives.contains("giant_slayer") && target != null) {
            if (target.getBbHeight() > player.getBbHeight() || target.getMaxHealth() > player.getMaxHealth()) {
                modifiedDamage *= 1.25f;
            }
        }

        return modifiedDamage;
    }

    /**
     * Validates whether a player is permitted to perform a Better Combat attack with their equipped weapon.
     */
    public static boolean canPerformAttack(Player player, RaceData race) {
        if (player == null || race == null) return true;

        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString();
            if (race.isItemRestricted(itemId)) {
                player.displayClientMessage(Component.literal("§cYour race cannot wield this weapon!"), true);
                return false;
            }
        }
        return true;
    }
}
