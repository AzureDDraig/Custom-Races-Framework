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
        if (race == null) return;

        List<String> passives = new java.util.ArrayList<>();
        if (race.passiveAbilities != null) passives.addAll(race.passiveAbilities);
        if (race.drawbacks != null) passives.addAll(race.drawbacks);

        if (ddraig.net.customraces.event.WereRaceTransformHandler.isTransformed(player.getUUID())) {
            if (race.werePassiveAbilities != null) {
                for (String wp : race.werePassiveAbilities) {
                    if (!passives.contains(wp)) passives.add(wp);
                }
            }
            if (race.wereDrawbacks != null) {
                for (String wd : race.wereDrawbacks) {
                    if (!passives.contains(wd)) passives.add(wd);
                }
            }
        }
        if (passives.isEmpty()) return;

        // Execute Event-Driven and Tick Drawbacks
        DrawbackEventHandler.tickDrawbacks(player, passives);

        // 1. Aquatic & Marine
        if (passives.contains("gills_of_the_deep") || passives.contains("water_breathing")) {
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
        if (passives.contains("fireproof_scales") || passives.contains("fire_resistance")) {
            player.clearFire();
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
        }
        if ((passives.contains("lava_walker") || passives.contains("lava_swimming")) && player.isInLava()) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
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
        if (passives.contains("flight") || passives.contains("sky_soarer")) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        }
        if (passives.contains("feather_light") || passives.contains("slow_falling") || passives.contains("fall_damage_immunity")) {
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
        if ((passives.contains("spider_climb") || passives.contains("climbing")) && player.horizontalCollision) {
            player.setDeltaMovement(player.getDeltaMovement().x, 0.2, player.getDeltaMovement().z);
        }

        // 4. Vision & Stealth
        if (passives.contains("night_eyes") || passives.contains("night_vision")) {
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

        // 8. Dedicated 60 Drawbacks & Weaknesses Handling
        // Environmental & Elemental
        if (passives.contains("sunlight_burn") && player.level().isDay() && player.level().canSeeSky(player.blockPosition()) && !player.isInWaterOrRain()) {
            if (player.tickCount % 20 == 0) player.setSecondsOnFire(3);
        }
        if (passives.contains("sunlight_slowness") && player.level().isDay() && player.level().canSeeSky(player.blockPosition())) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
        }
        if (passives.contains("cold_vulnerability") && (player.isInPowderSnow || player.getTicksFrozen() > 0)) {
            player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze() + 20, player.getTicksFrozen() + 4));
        }
        if (passives.contains("hydrophobia") && player.isInWater()) {
            player.setDeltaMovement(player.getDeltaMovement().x, -0.3, player.getDeltaMovement().z);
        }
        if (passives.contains("claustrophobia") && player.getY() < 50) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
        }
        if (passives.contains("agoraphobia") && player.level().canSeeSky(player.blockPosition()) && player.level().isDay()) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
        }
        if (passives.contains("nether_vulnerability") && player.level().dimension().location().getPath().contains("nether")) {
            if (player.tickCount % 40 == 0) player.hurt(player.damageSources().magic(), 1.0f);
        }
        if (passives.contains("end_vulnerability") && player.level().dimension().location().getPath().contains("end")) {
            if (player.tickCount % 40 == 0) player.hurt(player.damageSources().magic(), 1.0f);
        }

        // Diet & Metabolism
        if (passives.contains("hyper_metabolism")) {
            if (player.isSprinting() && player.tickCount % 10 == 0) player.causeFoodExhaustion(0.2f);
        }
        if (passives.contains("photosynthetic_dependency")) {
            if (player.level().isDay() && player.level().canSeeSky(player.blockPosition())) {
                if (player.tickCount % 40 == 0) player.getFoodData().eat(1, 0.2f);
            }
        }
        if (passives.contains("soul_hunger") && player.tickCount % 100 == 0) {
            player.causeFoodExhaustion(0.5f);
        }
        if (passives.contains("heavy_eater") && player.isSprinting() && player.tickCount % 10 == 0) {
            player.causeFoodExhaustion(0.4f);
        }

        // Combat & Restrictions
        if (passives.contains("no_shield_use") && player.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem) {
            player.drop(player.getOffhandItem().copy(), false);
            player.getOffhandItem().setCount(0);
        }
        if (passives.contains("melee_weakness")) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
        }

        // Movement & Physics
        if (passives.contains("slowness_curse")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
        }
        if (passives.contains("no_sprinting") && player.isSprinting()) {
            player.setSprinting(false);
        }
        if (passives.contains("reduced_step_height")) {
            player.setMaxUpStep(0.5f);
        }
        if (passives.contains("slippery_feet")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
        }
        if (passives.contains("gravity_bound")) {
            if (player.isFallFlying()) player.stopFallFlying();
            if (player.hasEffect(MobEffects.LEVITATION)) player.removeEffect(MobEffects.LEVITATION);
        }
        if (passives.contains("clumsy_swimmer") && player.isInWater()) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
        }

        // Hitbox & Health Limits
        if (passives.contains("low_max_health")) {
            net.minecraft.world.entity.ai.attributes.AttributeInstance hpAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (hpAttr != null && hpAttr.getBaseValue() > 10.0) hpAttr.setBaseValue(10.0);
        }
        if (passives.contains("glass_cannon")) {
            net.minecraft.world.entity.ai.attributes.AttributeInstance hpAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
            if (hpAttr != null && hpAttr.getBaseValue() > 6.0) hpAttr.setBaseValue(6.0);
        }

        // Faction & Curse
        if (passives.contains("curse_of_shadows") && player.level().getMaxLocalRawBrightness(player.blockPosition()) > 10) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
        }
        if (passives.contains("blindness_in_nether") && player.level().dimension().location().getPath().contains("nether")) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
        }
    }
}
