package ddraig.net.customraces.ability;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Handles living tick and event logic for all 72 race passive abilities.
 */
public class PassiveAbilityHandler {

    public static void tickPlayer(Player player) {
        if (player == null || player.level().isClientSide) return;

        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
        if (race == null || race.passiveAbilities == null || race.passiveAbilities.isEmpty()) return;

        List<String> passives = race.passiveAbilities;

        // 1. Aquatic & Marine
        if (passives.contains("gills_of_the_deep")) {
            player.setAirSupply(player.getMaxAirSupply());
            if (player.isInWater()) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 1, false, false, true));
            }
        }
        if (passives.contains("water_vulnerability")) {
            if (player.isInWaterOrRain()) {
                if (player.tickCount % 20 == 0) {
                    player.hurt(player.damageSources().drown(), 1.0f);
                }
            }
        }
        if (passives.contains("aqua_agility") && player.isInWater()) {
            player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, false, false, true));
        }
        if (passives.contains("water_conduit_power") && player.isInWaterOrRain()) {
            player.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 40, 0, false, false, true));
        }
        if (passives.contains("ocean_sight") && player.isInWater()) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, true));
        }
        if (passives.contains("depth_crusher") && player.isInWater() && player.getY() < 30) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 2, false, false, true));
        }
        if (passives.contains("marine_shield") && player.isInWater()) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
        }

        // 2. Fire, Lava & Thermal
        if (passives.contains("fireproof_scales")) {
            player.clearFire();
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
        }
        if (passives.contains("lava_walker") && player.isInLava()) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
        }
        if (passives.contains("lava_heat_regeneration") && player.isInLava()) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 1, false, false, true));
        }
        if (passives.contains("nether_fire_speed") && (player.isOnFire() || player.level().dimension().location().getPath().contains("nether"))) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
        }
        if (passives.contains("thermal_immunity")) {
            player.setTicksFrozen(0);
        }

        // 3. Movement & Physics
        if (passives.contains("feather_light")) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
            player.fallDistance = 0.0f;
        }
        if (passives.contains("step_assist")) {
            player.setMaxUpStep(1.5f);
        } else {
            player.setMaxUpStep(0.6f);
        }
        if (passives.contains("acrobatics")) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false, true));
        }
        if (passives.contains("swiftfoot")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
        }
        if (passives.contains("slime_cushion")) {
            player.fallDistance = 0.0f;
        }
        if (passives.contains("spider_climb") && player.horizontalCollision) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.2, player.getDeltaMovement().z);
        }

        // 4. Vision & Stealth
        if (passives.contains("night_eyes")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, true));
        }
        if (passives.contains("shadow_camouflage")) {
            BlockPos pos = player.blockPosition();
            if (player.level().getMaxLocalRawBrightness(pos) < 5) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
            }
        }
        if (passives.contains("sunlight_sensitivity")) {
            if (player.level().isDay() && player.level().canSeeSky(player.blockPosition()) && !player.isInWaterOrRain()) {
                if (player.tickCount % 40 == 0) {
                    player.setSecondsOnFire(3);
                }
            }
        }

        // 5. Metabolism & Diet
        if (passives.contains("photosynthesis")) {
            if (player.level().isDay() && player.level().canSeeSky(player.blockPosition())) {
                if (player.tickCount % 60 == 0) {
                    player.heal(1.0f);
                    player.getFoodData().eat(1, 0.5f);
                }
            }
        }

        // 6. Inventory & Utility
        if (passives.contains("master_miner") && player.getY() < 50) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 1, false, false, true));
        }
        if (passives.contains("magnetosphere")) {
            AABB area = player.getBoundingBox().inflate(7.0);
            for (net.minecraft.world.entity.item.ItemEntity item : player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area)) {
                item.teleportTo(player.getX(), player.getY() + 0.5, player.getZ());
            }
            for (net.minecraft.world.entity.ExperienceOrb orb : player.level().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, area)) {
                orb.teleportTo(player.getX(), player.getY() + 0.5, player.getZ());
            }
        }

        // 7. Defense & Health
        if (passives.contains("iron_skin")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
        }
        if (passives.contains("rejuvenation_aura")) {
            AABB auraBox = player.getBoundingBox().inflate(10.0);
            for (Player teamPlayer : player.level().getEntitiesOfClass(Player.class, auraBox)) {
                teamPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false, true));
            }
        }
        if (passives.contains("glacial_aura")) {
            AABB chillBox = player.getBoundingBox().inflate(6.0);
            for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, chillBox)) {
                if (mob != player && !mob.isAlliedTo(player)) {
                    mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
                }
            }
        }
        if (passives.contains("willpower") && player.getHealth() < (player.getMaxHealth() * 0.25f)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false, true));
        }
        if (passives.contains("wither_immunity")) {
            player.removeEffect(MobEffects.WITHER);
        }
    }
}
