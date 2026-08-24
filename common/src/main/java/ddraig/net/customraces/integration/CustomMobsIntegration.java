package ddraig.net.customraces.integration;

import ddraig.net.customraces.data.RaceData;
import dev.architectury.platform.Platform;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.phys.Vec3;

import java.io.File;
import java.util.EnumSet;
import java.util.UUID;

/**
 * Dynamic integration bridge for CustomMobs mod.
 * Handles custom entity spawning, minion owner follow/protect AI, and projectile abilities.
 */
public class CustomMobsIntegration {

    public static boolean isCustomMobsLoaded() {
        return Platform.isModLoaded("custom_mobs") || new File("config/custom_mobs").exists();
    }

    /**
     * Spawns a fully managed minion (CustomMobs or Vanilla) with owner-follow and protection AI.
     */
    public static Entity spawnMinion(ServerLevel level, ServerPlayer player, RaceData race, double spawnX, double spawnY, double spawnZ) {
        if (level == null || player == null || race == null) return null;

        String rawMobType = race.minionMobType != null && !race.minionMobType.trim().isEmpty() ? race.minionMobType.trim() : "minecraft:wolf";
        String cleanMobId = rawMobType.toLowerCase();

        EntityType<?> targetType = null;
        boolean isCustomMob = false;

        // 1. Try resolving CustomMobs entity type if custom ID
        if (cleanMobId.startsWith("custom_mobs:") || cleanMobId.startsWith("custommobs:")) {
            ResourceLocation cmLoc = new ResourceLocation("custom_mobs", "custom_mob");
            targetType = BuiltInRegistries.ENTITY_TYPE.get(cmLoc);
            if (targetType != null && targetType != EntityType.PIG) {
                isCustomMob = true;
            }
        }

        // 2. Try vanilla / general registry resolution
        if (targetType == null || targetType == EntityType.PIG && !cleanMobId.contains("pig")) {
            ResourceLocation loc = ResourceLocation.tryParse(cleanMobId);
            if (loc != null && BuiltInRegistries.ENTITY_TYPE.containsKey(loc)) {
                targetType = BuiltInRegistries.ENTITY_TYPE.get(loc);
            }
        }

        // 3. Fallback to Wolf if unresolvable (instead of wandering pig)
        if (targetType == null) {
            targetType = EntityType.WOLF;
        }

        Entity minion = targetType.create(level);
        if (minion == null) return null;

        minion.setPos(spawnX, spawnY, spawnZ);

        // Populate CustomMobs Compound NBT
        if (isCustomMob || cleanMobId.startsWith("custom_mobs:")) {
            String subId = cleanMobId.replace("custom_mobs:", "").replace("custommobs:", "");
            try {
                java.lang.reflect.Method m = minion.getClass().getMethod("getPersistentData");
                Object data = m.invoke(minion);
                if (data instanceof CompoundTag tag) {
                    tag.putString("CustomMobId", subId);
                    tag.putString("custom_mob_id", subId);
                    tag.putUUID("OwnerUUID", player.getUUID());
                    tag.putUUID("SummonerUUID", player.getUUID());
                }
            } catch (Exception ignored) {}
        }

        // Pehkui scaling for minion
        if (race.minionScale > 0.0f && race.minionScale != 1.0f) {
            PehkuiIntegration.setScale(minion, race.minionScale);
        }

        // Standard Tameable support
        if (minion instanceof TamableAnimal tamable) {
            tamable.tame(player);
        }

        if (minion instanceof LivingEntity living) {
            living.setHealth(living.getMaxHealth());
        }

        // Minion AI Injection: Follow and Protect Owner
        if (minion instanceof Mob mob) {
            setupMinionAI(mob, player);
        }

        // Spawn visual & sound effects
        level.addFreshEntity(minion);
        level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, spawnX, spawnY + 0.5, spawnZ, 12, 0.2, 0.4, 0.2, 0.03);
        level.sendParticles(ParticleTypes.WITCH, spawnX, spawnY + 0.5, spawnZ, 8, 0.2, 0.3, 0.2, 0.02);

        return minion;
    }

    /**
     * Injects Follow & Protect Owner goals into the summoned mob.
     */
    public static void setupMinionAI(Mob mob, ServerPlayer owner) {
        if (mob == null || owner == null) return;

        try {
            // Priority targeting: assist owner in combat
            if (owner.getLastHurtMob() != null && owner.getLastHurtMob().isAlive()) {
                mob.setTarget(owner.getLastHurtMob());
            } else if (owner.getLastHurtByMob() != null && owner.getLastHurtByMob().isAlive()) {
                mob.setTarget(owner.getLastHurtByMob());
            }

            // Inject dynamic Goal via reflection for protected field access
            java.lang.reflect.Field f = Mob.class.getDeclaredField("goalSelector");
            f.setAccessible(true);
            net.minecraft.world.entity.ai.goal.GoalSelector selector = (net.minecraft.world.entity.ai.goal.GoalSelector) f.get(mob);
            if (selector != null) {
                selector.addGoal(1, new MinionFollowOwnerGoal(mob, owner, 1.25, 4.0f, 20.0f));
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Custom AI Goal that keeps summoned minions near the player and teleports them if too far.
     */
    public static class MinionFollowOwnerGoal extends Goal {
        private final Mob mob;
        private final ServerPlayer owner;
        private final double speedModifier;
        private final float stopDistance;
        private final float teleportDistance;
        private int timeToRecalcPath;

        public MinionFollowOwnerGoal(Mob mob, ServerPlayer owner, double speedModifier, float stopDistance, float teleportDistance) {
            this.mob = mob;
            this.owner = owner;
            this.speedModifier = speedModifier;
            this.stopDistance = stopDistance;
            this.teleportDistance = teleportDistance;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return this.owner != null && this.owner.isAlive() && this.mob.isAlive() && this.mob.distanceToSqr(this.owner) > (this.stopDistance * this.stopDistance);
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse() && !this.mob.getNavigation().isDone();
        }

        @Override
        public void tick() {
            if (this.owner == null || !this.mob.isAlive()) return;

            this.mob.getLookControl().setLookAt(this.owner, 10.0F, (float) this.mob.getMaxHeadXRot());
            double distSqr = this.mob.distanceToSqr(this.owner);

            if (distSqr > (this.teleportDistance * this.teleportDistance)) {
                // Teleport to owner if stranded
                this.mob.moveTo(this.owner.getX(), this.owner.getY(), this.owner.getZ(), this.mob.getYRot(), this.mob.getXRot());
                this.mob.getNavigation().stop();
                return;
            }

            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                this.mob.getNavigation().moveTo(this.owner, this.speedModifier);
            }
        }
    }

    /**
     * Launches a custom projectile entity (CustomMobs or Vanilla) along the player's look vector.
     */
    public static Entity launchCustomProjectile(ServerLevel level, ServerPlayer player, String projectileId, float speed, float inaccuracy) {
        if (level == null || player == null) return null;

        String cleanId = (projectileId != null && !projectileId.trim().isEmpty()) ? projectileId.trim().toLowerCase() : "minecraft:small_fireball";
        Vec3 look = player.getLookAngle();
        double spawnX = player.getX() + look.x * 1.2;
        double spawnY = player.getEyeY() + look.y * 0.5;
        double spawnZ = player.getZ() + look.z * 1.2;

        try {
            if (cleanId.contains("fireball") || cleanId.contains("flame")) {
                net.minecraft.world.entity.projectile.SmallFireball fireball = new net.minecraft.world.entity.projectile.SmallFireball(level, player, look.x, look.y, look.z);
                fireball.setPos(spawnX, spawnY, spawnZ);
                level.addFreshEntity(fireball);
                level.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);
                return fireball;
            } else if (cleanId.contains("arrow")) {
                net.minecraft.world.entity.projectile.Arrow arrow = new net.minecraft.world.entity.projectile.Arrow(level, player);
                arrow.setPos(spawnX, spawnY, spawnZ);
                arrow.shoot(look.x, look.y, look.z, speed > 0 ? speed : 2.5f, inaccuracy);
                level.addFreshEntity(arrow);
                level.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                return arrow;
            } else if (cleanId.contains("wither") || cleanId.contains("skull")) {
                net.minecraft.world.entity.projectile.WitherSkull skull = new net.minecraft.world.entity.projectile.WitherSkull(level, player, look.x, look.y, look.z);
                skull.setPos(spawnX, spawnY, spawnZ);
                level.addFreshEntity(skull);
                level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 1.0f);
                return skull;
            } else if (cleanId.contains("trident")) {
                net.minecraft.world.entity.projectile.ThrownTrident trident = new net.minecraft.world.entity.projectile.ThrownTrident(level, player, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.TRIDENT));
                trident.setPos(spawnX, spawnY, spawnZ);
                trident.shoot(look.x, look.y, look.z, speed > 0 ? speed : 2.0f, inaccuracy);
                level.addFreshEntity(trident);
                level.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);
                return trident;
            } else {
                net.minecraft.world.entity.projectile.Snowball snowball = new net.minecraft.world.entity.projectile.Snowball(level, player);
                snowball.setPos(spawnX, spawnY, spawnZ);
                snowball.shoot(look.x, look.y, look.z, speed > 0 ? speed : 1.8f, inaccuracy);
                level.addFreshEntity(snowball);
                level.sendParticles(ParticleTypes.CRIT, spawnX, spawnY, spawnZ, 8, 0.1, 0.1, 0.1, 0.05);
                level.playSound(null, player.blockPosition(), SoundEvents.SNOWBALL_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);
                return snowball;
            }
        } catch (Throwable t) {
            System.err.println("[CustomRaces] Failed to launch projectile: " + t.getMessage());
            return null;
        }
    }
}
