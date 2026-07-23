package ddraig.net.customraces.ability;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Handles living tick and event logic for all 100 race passive abilities.
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

        // 1-10: Elemental & Environmental
        if (passives.contains("night_vision")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, true));
        }
        if (passives.contains("water_breathing") || passives.contains("gills_of_the_deep")) {
            player.setAirSupply(player.getMaxAirSupply());
            if (player.isInWater()) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, true));
            }
        }
        if (passives.contains("fire_resistance") || passives.contains("fireproof_scales")) {
            player.clearFire();
            player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
        }
        if (passives.contains("flight") || passives.contains("sky_soarer") || passives.contains("angel_wings_passive")) {
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.onUpdateAbilities();
            }
        }
        if (passives.contains("slow_falling") || passives.contains("feather_light") || passives.contains("feather_weight")) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false, true));
            player.fallDistance = 0.0f;
        }
        if (passives.contains("lava_swimming") || passives.contains("lava_walker")) {
            if (player.isInLava()) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, true));
            }
        }
        if (passives.contains("climbing") || passives.contains("spider_climb")) {
            if (player.horizontalCollision) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.2, player.getDeltaMovement().z);
            }
        }
        if (passives.contains("frost_immunity") || passives.contains("thermal_immunity") || passives.contains("thermal_regulation")) {
            player.setTicksFrozen(0);
        }
        if (passives.contains("lightning_immunity") || passives.contains("hellfire_immunity") || passives.contains("radiation_immunity")) {
            player.clearFire();
        }
        if (passives.contains("poison_immunity")) {
            if (player.hasEffect(MobEffects.POISON)) player.removeEffect(MobEffects.POISON);
        }

        // 11-20: Defense & Resilience
        if (passives.contains("regeneration") || passives.contains("wild_regeneration") || passives.contains("nanite_repair")) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false, true));
        }
        if (passives.contains("wither_immunity")) {
            if (player.hasEffect(MobEffects.WITHER)) player.removeEffect(MobEffects.WITHER);
        }
        if (passives.contains("fall_damage_immunity") || passives.contains("slime_cushion")) {
            player.fallDistance = 0.0f;
        }
        if (passives.contains("arrow_deflection") || passives.contains("forcefield_barrier")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
        }
        if (passives.contains("explosion_resistance") || passives.contains("thick_hide") || passives.contains("golem_density")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
        }
        if (passives.contains("magic_resistance") || passives.contains("abyssal_resilience")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
        }
        if (passives.contains("knockback_immunity")) {
            AttributeInstance kbAttr = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
            if (kbAttr != null && kbAttr.getValue() < 1.0) kbAttr.setBaseValue(1.0);
        }
        if (passives.contains("thorns_skin")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
        }
        if (passives.contains("shield_mastery")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
        }
        if (passives.contains("unbreakable_will")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        }

        // 21-30: Mobility & Movement
        if (passives.contains("speed_boost") || passives.contains("swiftfoot") || passives.contains("overclock_speed")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
        }
        if (passives.contains("high_jump") || passives.contains("acrobatics")) {
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false, true));
        }
        if (passives.contains("web_walking")) {
            player.makeStuckInBlock(net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), new net.minecraft.world.phys.Vec3(1.0, 1.0, 1.0));
        }
        if (passives.contains("soul_speed")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
        }
        if (passives.contains("step_assist")) {
            player.setMaxUpStep(1.5f);
        }
        if (passives.contains("wall_run")) {
            if (player.horizontalCollision) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.25, player.getDeltaMovement().z);
            }
        }
        if (passives.contains("dolphin_grace") || passives.contains("aqua_agility")) {
            if (player.isInWater()) player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 40, 0, false, false, true));
        }
        if (passives.contains("shadow_dash_passive")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
        }
        if (passives.contains("void_floating")) {
            if (player.getY() < 0) {
                player.setDeltaMovement(player.getDeltaMovement().x, 0.5, player.getDeltaMovement().z);
            }
        }

        // 31-40: Combat & Damage
        if (passives.contains("lifesteal") || passives.contains("vampiric_bite_regen")) {
            if (player.tickCount % 40 == 0 && player.swinging) player.heal(1.0f);
        }
        if (passives.contains("critical_strike_boost") || passives.contains("berserk_rage")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        }
        if (passives.contains("backstab_bonus") || passives.contains("predator_stealth")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        }
        if (passives.contains("giant_slayer") || passives.contains("armor_piercing")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        }
        if (passives.contains("execute_passive") || passives.contains("bleed_on_hit")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        }
        if (passives.contains("counter_attack") || passives.contains("dual_wield_mastery")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        }

        // 41-50: Utility & Gathering
        if (passives.contains("auto_smelt") || passives.contains("double_mining_drops")) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, false, false, true));
        }
        if (passives.contains("magnet_aura") || passives.contains("magnetosphere") || passives.contains("magnetic_repulsion")) {
            AABB area = player.getBoundingBox().inflate(7.0);
            for (net.minecraft.world.entity.item.ItemEntity item : player.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area)) {
                item.teleportTo(player.getX(), player.getY() + 0.5, player.getZ());
            }
            for (net.minecraft.world.entity.ExperienceOrb orb : player.level().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class, area)) {
                orb.teleportTo(player.getX(), player.getY() + 0.5, player.getZ());
            }
        }
        if (passives.contains("luck_of_the_sea") || passives.contains("xp_boost")) {
            player.addEffect(new MobEffectInstance(MobEffects.LUCK, 40, 1, false, false, true));
        }
        if (passives.contains("haste_passive") || passives.contains("master_miner") || passives.contains("night_miner")) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 1, false, false, true));
        }
        if (passives.contains("silk_touch_hands")) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, false, false, true));
        }
        if (passives.contains("hunger_less_drain") || passives.contains("photosynthesis")) {
            if (player.level().isDay() && player.level().canSeeSky(player.blockPosition())) {
                if (player.tickCount % 60 == 0) player.getFoodData().eat(1, 0.5f);
            }
        }
        if (passives.contains("saturation_regen")) {
            if (player.tickCount % 80 == 0) player.getFoodData().eat(1, 0.5f);
        }

        // 51-60: Magic & Spectral
        if (passives.contains("mana_regen_boost") || passives.contains("energy_core_boost")) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false, true));
        }
        if (passives.contains("spell_power_boost") || passives.contains("elemental_affinity")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        }
        if (passives.contains("cooldown_reduction")) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, false, false, true));
        }
        if (passives.contains("arcane_shield")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
        }
        if (passives.contains("astral_projection")) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
        }
        if (passives.contains("spectral_glowing")) {
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, true));
        }
        if (passives.contains("invisibility_in_shadows") || passives.contains("shadow_camouflage")) {
            if (player.level().getMaxLocalRawBrightness(player.blockPosition()) < 5) {
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false, true));
            }
        }
        if (passives.contains("telepathic_aura") || passives.contains("scent_tracking") || passives.contains("cybernetic_sight")) {
            AABB area = player.getBoundingBox().inflate(12.0);
            for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (mob != player) mob.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, true));
            }
        }
        if (passives.contains("native_spell")) {
            // Checked by active spell casting engine
        }

        // 61-70: Vampiric & Nether
        if (passives.contains("sunlight_evasion")) {
            if (player.level().isDay() && player.level().canSeeSky(player.blockPosition())) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
            }
        }
        if (passives.contains("nether_affinity") || passives.contains("nether_fire_speed")) {
            if (player.level().dimension().location().getPath().contains("nether")) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
            }
        }
        if (passives.contains("wither_touch")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        }
        if (passives.contains("shadow_healing")) {
            if (player.level().getMaxLocalRawBrightness(player.blockPosition()) < 5 && player.tickCount % 40 == 0) {
                player.heal(1.0f);
            }
        }
        if (passives.contains("soul_collector") || passives.contains("blood_essence_pool")) {
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false, true));
        }
        if (passives.contains("demon_flame_aura")) {
            AABB area = player.getBoundingBox().inflate(4.0);
            for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (mob != player) mob.setSecondsOnFire(2);
            }
        }

        // 71-80: Celestial & Divine
        if (passives.contains("divine_aura") || passives.contains("sanctuary_field") || passives.contains("rejuvenation_aura")) {
            AABB auraBox = player.getBoundingBox().inflate(10.0);
            for (Player teamPlayer : player.level().getEntitiesOfClass(Player.class, auraBox)) {
                teamPlayer.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0, false, false, true));
            }
        }
        if (passives.contains("holy_damage_boost") || passives.contains("undead_bane_aura")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        }
        if (passives.contains("solar_charging")) {
            if (player.level().isDay() && player.level().canSeeSky(player.blockPosition())) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
            }
        }
        if (passives.contains("lunar_power_boost")) {
            if (!player.level().isDay() && player.level().canSeeSky(player.blockPosition())) {
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
            }
        }
        if (passives.contains("radiant_light")) {
            player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, true));
        }
        if (passives.contains("blessing_of_protection") || passives.contains("grace_of_the_gods")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
        }

        // 81-90: Draconic & Beast
        if (passives.contains("dragon_scales") || passives.contains("natural_armor")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false, true));
        }
        if (passives.contains("beast_instincts")) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, true));
        }
        if (passives.contains("pack_leader_buff")) {
            AABB area = player.getBoundingBox().inflate(12.0);
            for (Player ally : player.level().getEntitiesOfClass(Player.class, area)) {
                ally.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
            }
        }
        if (passives.contains("intimidating_presence")) {
            AABB area = player.getBoundingBox().inflate(6.0);
            for (LivingEntity mob : player.level().getEntitiesOfClass(LivingEntity.class, area)) {
                if (mob != player) mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
            }
        }
        if (passives.contains("tail_sweep_passive")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, true));
        }

        // 91-100: Tech & Golem
        if (passives.contains("kinetic_absorption")) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, true));
        }
    }
}
