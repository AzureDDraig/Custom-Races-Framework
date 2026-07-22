package ddraig.net.customraces.ability;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles server execution and cooldown tracking for all 60 keybound active abilities.
 */
public class ActiveAbilityHandler {
    private static final Map<UUID, Map<Integer, Long>> COOLDOWNS = new ConcurrentHashMap<>();
    private static final long DEFAULT_COOLDOWN_MS = 10000; // 10 seconds default

    public static void triggerAbility(ServerPlayer player, int slot) {
        if (player == null || slot < 1 || slot > 5) return;

        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
        if (race == null) return;

        String abilityId = race.activeAbilities != null ? race.activeAbilities.get(slot) : null;
        if (ddraig.net.customraces.event.WereRaceTransformHandler.isTransformed(player.getUUID()) && race.wereActiveAbilities != null) {
            String wAbility = race.wereActiveAbilities.get(slot);
            if (wAbility != null && !wAbility.isEmpty() && !"none".equalsIgnoreCase(wAbility)) {
                abilityId = wAbility;
            }
        }
        if (abilityId == null || abilityId.isEmpty() || abilityId.equals("none")) return;

        // Check Cooldown
        long now = System.currentTimeMillis();
        Map<Integer, Long> pMap = COOLDOWNS.computeIfAbsent(player.getUUID(), k -> new ConcurrentHashMap<>());
        long lastUse = pMap.getOrDefault(slot, 0L);

        if (now - lastUse < DEFAULT_COOLDOWN_MS) {
            long remainingSec = (DEFAULT_COOLDOWN_MS - (now - lastUse)) / 1000 + 1;
            player.sendSystemMessage(Component.literal("§cAbility " + slot + " on cooldown! (" + remainingSec + "s)"), true);
            return;
        }

        // Set Cooldown
        pMap.put(slot, now);

        ServerLevel level = player.serverLevel();
        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position();

        player.sendSystemMessage(Component.literal("§aUsed Ability: §e" + abilityId.replace("_", " ")), true);

        switch (abilityId.toLowerCase()) {
            case "flame_breath":
            case "flame breath":
                for (int i = 0; i < 15; i++) {
                    double spreadX = look.x + (level.random.nextDouble() - 0.5) * 0.4;
                    double spreadY = look.y + (level.random.nextDouble() - 0.5) * 0.4;
                    double spreadZ = look.z + (level.random.nextDouble() - 0.5) * 0.4;
                    level.sendParticles(ParticleTypes.FLAME, player.getX() + look.x, player.getEyeY(), player.getZ() + look.z, 2, spreadX, spreadY, spreadZ, 0.2);
                }
                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                AABB coneBox = player.getBoundingBox().inflate(6.0);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, coneBox)) {
                    if (mob != player) {
                        mob.setSecondsOnFire(6);
                        mob.hurt(player.damageSources().playerAttack(player), 4.0f);
                    }
                }
                break;

            case "teleport_dash":
            case "teleport dash":
                Vec3 tpTarget = pos.add(look.scale(12.0));
                player.teleportTo(tpTarget.x, tpTarget.y, tpTarget.z);
                level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.5, 0.5, 0.5, 0.1);
                break;

            case "lightning_strike":
            case "lightning strike":
                BlockPos targetPos = player.blockPosition().relative(player.getDirection(), 10);
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(Vec3.atBottomCenterOf(targetPos));
                    level.addFreshEntity(bolt);
                }
                break;

            case "frost_nova":
            case "frost nova":
                level.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.SNOWFLAKE, player.getX(), player.getY() + 0.5, player.getZ(), 50, 2.0, 0.5, 2.0, 0.1);
                AABB frostBox = player.getBoundingBox().inflate(6.0);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, frostBox)) {
                    if (mob != player) {
                        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                        mob.setTicksFrozen(200);
                    }
                }
                break;

            case "healing_touch":
            case "healing touch":
            case "light grace":
                player.heal(8.0f); // 4 hearts
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.5, 0.5, 0.5, 0.1);
                break;

            case "super_launch":
            case "super launch":
            case "rocket jump":
                player.setDeltaMovement(player.getDeltaMovement().x, 1.6, player.getDeltaMovement().z);
                player.hurtMarked = true;
                level.playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.FIREWORK, player.getX(), player.getY(), player.getZ(), 20, 0.2, 0.2, 0.2, 0.1);
                break;

            case "sonic_dash":
            case "sonic dash":
                player.setDeltaMovement(look.scale(2.5));
                player.hurtMarked = true;
                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 0.5f, 1.5f);
                level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0, player.getZ(), 10, 0.3, 0.3, 0.3, 0.1);
                break;

            case "dragon_roar":
            case "dragon roar":
                level.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 1.0f, 1.0f);
                AABB roarBox = player.getBoundingBox().inflate(15.0);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, roarBox)) {
                    if (mob != player) {
                        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 1));
                        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 160, 1));
                    }
                }
                break;

            case "thunder_stomp":
            case "thunder stomp":
                level.playSound(null, player.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.0f, 0.8f);
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 40, 2.0, 0.2, 2.0, 0.1);
                AABB stompBox = player.getBoundingBox().inflate(5.0);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, stompBox)) {
                    if (mob != player) {
                        mob.setDeltaMovement(mob.getDeltaMovement().x, 0.9, mob.getDeltaMovement().z);
                        mob.hurt(player.damageSources().playerAttack(player), 6.0f);
                    }
                }
                break;

            case "shield_wall":
            case "shield wall":
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 120, 4)); // Resistance V
                level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;

            case "fireball_volley":
            case "fireball volley":
                LargeFireball fireball = new LargeFireball(level, player, look.x, look.y, look.z, 1);
                fireball.setPos(player.getX() + look.x * 1.5, player.getEyeY(), player.getZ() + look.z * 1.5);
                level.addFreshEntity(fireball);
                level.playSound(null, player.blockPosition(), SoundEvents.GHAST_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;

            case "web_trap_throw":
            case "web trap throw":
                Snowball webProj = new Snowball(level, player);
                webProj.setPos(player.getX() + look.x, player.getEyeY(), player.getZ() + look.z);
                webProj.shoot(look.x, look.y, look.z, 1.5f, 1.0f);
                level.addFreshEntity(webProj);
                level.playSound(null, player.blockPosition(), SoundEvents.SPIDER_AMBIENT, SoundSource.PLAYERS, 1.0f, 1.0f);
                break;

            case "transform_were":
            case "were_transform":
            case "were transform":
                ddraig.net.customraces.event.WereRaceTransformHandler.toggleManualWereForm(player);
                break;

            case "were_howl":
            case "were howl":
                level.playSound(null, player.blockPosition(), SoundEvents.WOLF_HOWL, SoundSource.PLAYERS, 1.5f, 0.7f);
                level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getEyeY(), player.getZ(), 1, look.x, look.y, look.z, 0.0);
                AABB howlBox = player.getBoundingBox().inflate(12.0);
                for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class, howlBox)) {
                    if (mob != player) {
                        mob.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2));
                        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2));
                    }
                }
                break;

            case "summon_minions":
            case "summon minions":
            case "summon_minion":
            case "summon minion":
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
                            double spawnY = player.getY();

                            net.minecraft.world.entity.Entity minion = mobType.create(level);
                            if (minion != null) {
                                minion.setPos(spawnX, spawnY, spawnZ);

                                // Pehkui scaling for minion
                                if (race.minionScale != 1.0f) {
                                    ddraig.net.customraces.integration.PehkuiIntegration.setScale(minion, race.minionScale);
                                }

                                if (minion instanceof net.minecraft.world.entity.TamableAnimal tamable) {
                                    tamable.tame(player);
                                }

                                if (minion instanceof net.minecraft.world.entity.Mob mob) {
                                    // Target nearest hostile mob if available
                                    AABB area = player.getBoundingBox().inflate(15.0);
                                    for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area)) {
                                        if (target != player && !(target instanceof net.minecraft.world.entity.TamableAnimal && ((net.minecraft.world.entity.TamableAnimal)target).isOwnedBy(player))) {
                                            mob.setTarget(target);
                                            break;
                                        }
                                    }
                                }

                                level.addFreshEntity(minion);
                                level.sendParticles(ParticleTypes.POOF, spawnX, spawnY + 0.5, spawnZ, 15, 0.3, 0.3, 0.3, 0.05);
                                level.sendParticles(ParticleTypes.WITCH, spawnX, spawnY + 1.0, spawnZ, 10, 0.3, 0.3, 0.3, 0.05);
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[CustomRaces] Failed to summon minions: " + e.getMessage());
                }
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
                                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                                break;
                            }
                        }
                    } catch (Exception e) {
                        System.err.println("[CustomRaces] Failed to spawn custom projectile: " + e.getMessage());
                    }
                }
                // Universal fallback active skill effect
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
                level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.5, 0.5, 0.5, 0.1);
                break;
        }
    }
}
