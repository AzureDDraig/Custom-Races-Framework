package ddraig.net.customraces.ability;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

/**
 * Executes active race abilities triggered via hotbar skill keybinds for all 100 active skills.
 */
public class ActiveAbilityHandler {

    public static void triggerAbility(ServerPlayer player, int slot) {
        executeActiveAbility(player, slot);
    }

    public static void executeActiveAbility(Player player, int slot) {
        if (player == null || player.level().isClientSide) return;
        if (slot < 1 || slot > 5) return;

        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
        if (race == null) return;

        boolean isWere = ddraig.net.customraces.event.WereRaceTransformHandler.isTransformed(player.getUUID());
        Map<Integer, String> actives = isWere ? race.wereActiveAbilities : race.activeAbilities;
        if (actives == null || !actives.containsKey(slot)) return;

        String abilityId = actives.get(slot);
        if (abilityId == null || abilityId.trim().isEmpty() || abilityId.equalsIgnoreCase("none")) return;

        Level level = player.level();
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position();

        player.sendSystemMessage(Component.literal("§aUsed Ability: §e" + abilityId.replace("_", " ")));

        switch (abilityId.toLowerCase().replace(" ", "_")) {
            case "native_spell":
            case "native_spell_1":
                ddraig.net.customraces.integration.IronSpellsHandler.castNativeSpell(player, race, isWere, 1);
                break;
            case "native_spell_2":
                ddraig.net.customraces.integration.IronSpellsHandler.castNativeSpell(player, race, isWere, 2);
                break;
            case "native_spell_3":
                ddraig.net.customraces.integration.IronSpellsHandler.castNativeSpell(player, race, isWere, 3);
                break;
            case "native_spell_4":
                ddraig.net.customraces.integration.IronSpellsHandler.castNativeSpell(player, race, isWere, 4);
                break;
            case "native_spell_5":
                ddraig.net.customraces.integration.IronSpellsHandler.castNativeSpell(player, race, isWere, 5);
                break;

            // 1-10: Fire & Magma
            case "flame_breath":
            case "heat_wave":
            case "combustion_aura":
                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0))) {
                    if (mob != player) { mob.setSecondsOnFire(6); mob.hurt(player.damageSources().playerAttack(player), 4.0f); }
                }
                break;
            case "fireball_burst":
            case "fireball_volley":
            case "pyroblast":
                LargeFireball fireball = new LargeFireball(level, player, look.x, look.y, look.z, 1);
                fireball.setPos(player.getX() + look.x * 1.5, player.getEyeY(), player.getZ() + look.z * 1.5);
                level.addFreshEntity(fireball);
                level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;
            case "inferno_ring":
            case "volcanic_eruption":
            case "magma_slam":
            case "meteor_strike":
            case "flame_charge":
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.0f, 1.0f);
                if (level instanceof ServerLevel sLevel) sLevel.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY() + 0.5, player.getZ(), 60, 2.0, 0.5, 2.0, 0.1);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(7.0))) {
                    if (mob != player) { mob.setSecondsOnFire(8); mob.hurt(player.damageSources().playerAttack(player), 7.0f); }
                }
                break;

            // 11-20: Ice & Frost
            case "frost_nova":
            case "blizzard_storm":
            case "deep_freeze":
            case "absolute_zero":
            case "snowstorm_burst":
                level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                if (level instanceof ServerLevel sLevel) sLevel.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.5, player.getZ(), 50, 2.0, 0.5, 2.0, 0.1);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0))) {
                    if (mob != player) { mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3)); mob.setTicksFrozen(200); }
                }
                break;
            case "ice_lance":
            case "frost_dash":
            case "glacier_wall":
            case "icicle_barrage":
            case "frozen_shield":
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 2));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_HURT_FREEZE, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;

            // 21-30: Lightning & Storm
            case "lightning_strike":
            case "chain_lightning":
            case "lightning_spear":
            case "sky_bolt":
                BlockPos targetPos = player.blockPosition().relative(player.getDirection(), 10);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) { bolt.moveTo(Vec3.atBottomCenterOf(targetPos)); level.addFreshEntity(bolt); }
                break;
            case "thunder_clap":
            case "storm_dash":
            case "overcharge_buff":
            case "plasma_beam":
            case "ball_lightning":
            case "static_field":
                player.setDeltaMovement(look.scale(2.2));
                player.hurtMarked = true;
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 120, 2));
                level.playSound(null, player.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.8f, 1.2f);
                break;

            // 31-40: Shadow & Ender
            case "teleport_dash":
            case "shadow_step":
            case "blink_teleport":
            case "dimensional_rift":
                Vec3 tpTarget = pos.add(look.scale(12.0));
                player.teleportTo(tpTarget.x, tpTarget.y, tpTarget.z);
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                if (level instanceof ServerLevel sLevel) sLevel.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
                break;
            case "black_hole_pull":
            case "shadow_clone":
            case "void_slash":
            case "veil_of_shadows":
            case "abyssal_grip":
            case "nightmare_burst":
                player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 120, 0));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1));
                level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.PLAYERS, 0.5f, 1.5f);
                break;

            // 41-50: Holy & Light
            case "healing_wave":
            case "healing_touch":
            case "sanctuary_heal":
            case "heavenly_resurrection":
                player.heal(8.0f);
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                if (level instanceof ServerLevel sLevel) sLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.5, 0.5, 0.5, 0.1);
                break;
            case "divine_smite":
            case "radiant_beam":
            case "holy_shield":
            case "blessing_buff":
            case "purifying_blast":
            case "solar_beam":
            case "angelic_flight_burst":
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 120, 1));
                level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0f, 1.2f);
                break;

            // 51-60: Blood & Dark Magic
            case "blood_slash":
            case "vampiric_drain":
            case "soul_reap":
                player.heal(4.0f);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.0))) {
                    if (mob != player) mob.hurt(player.damageSources().magic(), 5.0f);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.WITCH_DRINK, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;
            case "dark_pulse":
            case "wither_blast":
            case "curse_aura":
            case "corruption_wave":
            case "blood_shield":
            case "plague_cloud":
            case "necromancy_summon":
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0))) {
                    if (mob != player) { mob.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1)); mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1)); }
                }
                level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 0.8f);
                break;

            // 61-70: Earth & Nature
            case "earthquake_slam":
            case "seismic_wave":
            case "mud_slide":
                level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.8f);
                if (level instanceof ServerLevel sLevel) sLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 40, 2.0, 0.2, 2.0, 0.1);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(5.0))) {
                    if (mob != player) { mob.setDeltaMovement(mob.getDeltaMovement().x, 0.9, mob.getDeltaMovement().z); mob.hurt(player.damageSources().playerAttack(player), 6.0f); }
                }
                break;
            case "boulder_toss":
            case "root_entrapment":
            case "poison_spit":
            case "vine_whip":
            case "thorn_barrage":
            case "nature_heal":
            case "rock_armor_buff":
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 2));
                level.playSound(null, player.blockPosition(), SoundEvents.GRASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;

            // 71-80: Wind & Kinetic
            case "gale_blast":
            case "cyclone_vortex":
            case "wind_dash":
            case "sonic_boom":
            case "shockwave_slam":
            case "air_slash":
            case "repulsion_field":
            case "tornado_burst":
            case "vacuum_pull":
            case "kinetic_blast":
            case "sonic_dash":
                player.setDeltaMovement(look.scale(2.5));
                player.hurtMarked = true;
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.5f);
                if (level instanceof ServerLevel sLevel) sLevel.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                break;

            // 81-90: Beast & Transformation
            case "dragon_roar":
            case "howl_buff":
            case "were_howl":
                level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0f, 1.0f);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(12.0))) {
                    if (mob != player) { mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1)); mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1)); }
                }
                break;
            case "transform_were":
            case "were_transform":
                if (player instanceof ServerPlayer sp) ddraig.net.customraces.event.WereRaceTransformHandler.toggleManualWereForm(sp);
                break;
            case "summon_minions":
            case "pack_call":
                try {
                    int count = Math.max(1, Math.min(10, race.minionCount));
                    net.minecraft.resources.ResourceLocation mobLoc = new net.minecraft.resources.ResourceLocation(race.minionMobType);
                    EntityType<?> mobType = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(mobLoc);
                    if (mobType != null) {
                        level.playSound(null, player.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.PLAYERS, 1.2f, 1.0f);
                        for (int i = 0; i < count; i++) {
                            double angle = (2 * Math.PI / count) * i;
                            double spawnX = player.getX() + Math.cos(angle) * 2.5;
                            double spawnZ = player.getZ() + Math.sin(angle) * 2.5;
                            net.minecraft.world.entity.Entity minion = mobType.create(level);
                            if (minion != null) {
                                minion.setPos(spawnX, player.getY(), spawnZ);
                                level.addFreshEntity(minion);
                            }
                        }
                    }
                } catch (Exception ignored) {}
                break;
            case "beast_leap":
            case "feral_frenzy":
            case "claw_slash":
            case "predator_pounce":
            case "primal_rage":
            case "super_launch":
            case "rocket_jump":
                player.setDeltaMovement(player.getDeltaMovement().x, 1.6, player.getDeltaMovement().z);
                player.hurtMarked = true;
                level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;

            // 91-100: Tech & Special
            case "laser_beam":
            case "emp_blast":
            case "gravity_flip":
            case "time_stop_pulse":
            case "shield_overload":
            case "orbital_strike":
            case "nano_heal":
            case "overdrive_buff":
            case "singularity_bomb":
            case "shield_wall":
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 4));
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
                level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;

            default:
                if (abilityId.startsWith("geckolib_projectile:") || abilityId.startsWith("projectile:") || abilityId.startsWith("custom_mobs:") || abilityId.startsWith("cmobs_projectile:")) {
                    try {
                        String entityId = abilityId.substring(abilityId.indexOf(":") + 1).trim();
                        if (abilityId.startsWith("cmobs_projectile:")) {
                            entityId = "custom_mobs:" + entityId;
                        }
                        net.minecraft.resources.ResourceLocation res = new net.minecraft.resources.ResourceLocation(entityId);
                        EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(res);
                        if (type != null) {
                            net.minecraft.world.entity.Entity proj = type.create(level);
                            if (proj != null) {
                                proj.setPos(player.getX() + look.x * 1.2, player.getEyeY() - 0.1, player.getZ() + look.z * 1.2);
                                proj.setDeltaMovement(look.scale(1.8));
                                level.addFreshEntity(proj);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                break;
        }
    }
}
