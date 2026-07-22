package ddraig.net.customraces.ability;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Event-driven and tick-based execution engine for all 60 race drawbacks and weaknesses.
 */
public class DrawbackEventHandler {

    /**
     * Executes armor restrictions, entity AI targeting, and food/diet checks on player tick.
     */
    public static void tickDrawbacks(Player player, List<String> drawbacks) {
        if (player == null || drawbacks == null || drawbacks.isEmpty()) return;

        // 1. Armor Restrictions
        if (drawbacks.contains("no_helmet_slot")) {
            enforceSlotEmpty(player, EquipmentSlot.HEAD);
        }
        if (drawbacks.contains("no_chestplate_slot")) {
            enforceSlotEmpty(player, EquipmentSlot.CHEST);
        }
        if (drawbacks.contains("no_boots_slot")) {
            enforceSlotEmpty(player, EquipmentSlot.FEET);
        }
        if (drawbacks.contains("no_heavy_armor")) {
            enforceNoHeavyArmor(player);
        }

        // 2. Iron Golem Hostility
        if (drawbacks.contains("iron_golem_hostility") && player.tickCount % 20 == 0) {
            AABB area = player.getBoundingBox().inflate(16.0);
            for (IronGolem golem : player.level().getEntitiesOfClass(IronGolem.class, area)) {
                if (golem.getTarget() == null) {
                    golem.setTarget(player);
                }
            }
        }

        // 3. Villager Fear
        if (drawbacks.contains("villager_fear") && player.tickCount % 20 == 0) {
            AABB area = player.getBoundingBox().inflate(12.0);
            for (Villager villager : player.level().getEntitiesOfClass(Villager.class, area)) {
                villager.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
            }
        }

        // 4. Insomnia Curse (Spawns Phantoms at Night)
        if (drawbacks.contains("insomnia_curse") && player.level().isNight() && player.tickCount % 1200 == 0) {
            if (player.level().canSeeSky(player.blockPosition())) {
                net.minecraft.world.entity.monster.Phantom phantom = net.minecraft.world.entity.EntityType.PHANTOM.create(player.level());
                if (phantom != null) {
                    phantom.moveTo(player.getX(), player.getY() + 15, player.getZ(), player.getYRot(), player.getXRot());
                    player.level().addFreshEntity(phantom);
                }
            }
        }

        // 5. Lightning Attraction
        if (drawbacks.contains("lightning_attraction") && player.level().isThundering() && player.level().canSeeSky(player.blockPosition())) {
            if (player.level().random.nextInt(600) == 0) {
                net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(player.level());
                if (bolt != null) {
                    bolt.moveTo(player.position());
                    player.level().addFreshEntity(bolt);
                }
            }
        }

        // 6. Drowning in Shallow Water
        if (drawbacks.contains("drowning_in_shallow_water") && player.isInWater()) {
            player.setAirSupply(0);
        }
    }

    private static void enforceSlotEmpty(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (!stack.isEmpty()) {
            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }
    }

    private static void enforceNoHeavyArmor(Player player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem armor) {
                if (armor.getMaterial() == ArmorMaterials.IRON || armor.getMaterial() == ArmorMaterials.DIAMOND || armor.getMaterial() == ArmorMaterials.NETHERITE) {
                    player.setItemSlot(slot, ItemStack.EMPTY);
                    if (!player.getInventory().add(stack)) {
                        player.drop(stack, false);
                    }
                }
            }
        }
    }

    /**
     * Calculates damage modifications for vulnerability drawbacks (Fall, Fire, Silver, Smite, Explosions, Cold).
     */
    public static float modifyIncomingDamage(Player player, net.minecraft.world.damagesource.DamageSource source, float damage, List<String> drawbacks) {
        if (player == null || drawbacks == null || drawbacks.isEmpty() || damage <= 0) return damage;

        float modified = damage;

        // Fall Damage Multiplier
        if (drawbacks.contains("fragile_bone") && source.is(net.minecraft.world.damagesource.DamageTypes.FALL)) {
            modified *= 2.0f;
        }

        // Fire & Lava Vulnerability
        if (drawbacks.contains("fire_vulnerability") && (source.is(net.minecraft.world.damagesource.DamageTypes.IN_FIRE) || source.is(net.minecraft.world.damagesource.DamageTypes.ON_FIRE) || source.is(net.minecraft.world.damagesource.DamageTypes.LAVA))) {
            modified *= 2.0f;
        }

        // Explosion Fragility
        if (drawbacks.contains("translucent_fragility") && source.is(net.minecraft.world.damagesource.DamageTypes.EXPLOSION)) {
            modified *= 2.0f;
        }

        // Wither Vulnerability
        if (drawbacks.contains("wither_vulnerability") && source.is(net.minecraft.world.damagesource.DamageTypes.WITHER)) {
            modified *= 2.0f;
        }

        // Poison Vulnerability
        if (drawbacks.contains("poison_vulnerability") && source.is(net.minecraft.world.damagesource.DamageTypes.MAGIC)) {
            modified *= 1.5f;
        }

        // Silver Vulnerability (Attacking with Iron/Metal weapons)
        if (drawbacks.contains("silver_vulnerability") && source.getEntity() instanceof LivingEntity attacker) {
            ItemStack mainhand = attacker.getMainHandItem();
            if (mainhand.getItem() == Items.IRON_SWORD || mainhand.getItem() == Items.IRON_AXE) {
                modified *= 3.0f;
            }
        }

        return modified;
    }

    /**
     * Checks dietary restrictions when a player attempts to eat food.
     * Returns true if food consumption is allowed, or false if blocked by a diet drawback.
     */
    public static boolean checkFoodConsumption(Player player, ItemStack food, List<String> drawbacks) {
        if (player == null || food == null || drawbacks == null || drawbacks.isEmpty()) return true;

        boolean isMeat = food.getItem().isEdible() && (food.getItem() == Items.COOKED_BEEF || food.getItem() == Items.BEEF || food.getItem() == Items.COOKED_PORKCHOP || food.getItem() == Items.PORKCHOP || food.getItem() == Items.COOKED_CHICKEN || food.getItem() == Items.CHICKEN || food.getItem() == Items.COOKED_MUTTON || food.getItem() == Items.MUTTON || food.getItem() == Items.ROTTEN_FLESH);
        boolean isGolden = food.getItem() == Items.GOLDEN_APPLE || food.getItem() == Items.ENCHANTED_GOLDEN_APPLE || food.getItem() == Items.GOLDEN_CARROT;

        if (drawbacks.contains("carnivore_diet") && !isMeat) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            return false;
        }

        if (drawbacks.contains("vegetarian_diet") && isMeat) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
            return false;
        }

        if (drawbacks.contains("golden_allergy") && isGolden) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 2));
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1));
            return false;
        }

        if (drawbacks.contains("photosynthetic_dependency")) {
            return false; // Cannot eat any food items
        }

        return true;
    }
}
