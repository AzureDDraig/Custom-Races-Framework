package ddraig.net.customraces.ability;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Event-driven and tick-based execution engine for all 60 race drawbacks and weaknesses,
 * plus custom item/weapon/food restrictions defined in RaceData.
 */
public class DrawbackEventHandler {

    /**
     * Executes armor restrictions, entity AI targeting, and food/diet checks on player tick.
     */
    public static void tickDrawbacks(Player player, List<String> drawbacks) {
        if (player == null) return;

        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());

        // 0. Custom Item & Equipment Restrictions defined in RaceData
        if (race != null) {
            checkCustomItemRestrictions(player, race);
        }

        if (drawbacks == null || drawbacks.isEmpty()) return;

        // 1. Armor Slot Restrictions
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

        // 2. Specific Weapon Drawbacks
        if (drawbacks.contains("no_shield")) {
            if (player.getMainHandItem().getItem() instanceof ShieldItem) {
                enforceHandEmpty(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            if (player.getOffhandItem().getItem() instanceof ShieldItem) {
                enforceHandEmpty(player, net.minecraft.world.InteractionHand.OFF_HAND);
            }
        }
        if (drawbacks.contains("no_bow")) {
            if (player.getMainHandItem().getItem() instanceof BowItem || player.getMainHandItem().getItem() instanceof CrossbowItem) {
                enforceHandEmpty(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            if (player.getOffhandItem().getItem() instanceof BowItem || player.getOffhandItem().getItem() instanceof CrossbowItem) {
                enforceHandEmpty(player, net.minecraft.world.InteractionHand.OFF_HAND);
            }
        }
        if (drawbacks.contains("no_trident")) {
            if (player.getMainHandItem().getItem() instanceof TridentItem) {
                enforceHandEmpty(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            if (player.getOffhandItem().getItem() instanceof TridentItem) {
                enforceHandEmpty(player, net.minecraft.world.InteractionHand.OFF_HAND);
            }
        }

        // 3. Iron Golem Hostility
        if (drawbacks.contains("iron_golem_hostility") && player.tickCount % 20 == 0) {
            AABB area = player.getBoundingBox().inflate(16.0);
            for (IronGolem golem : player.level().getEntitiesOfClass(IronGolem.class, area)) {
                if (golem.getTarget() == null) {
                    golem.setTarget(player);
                }
            }
        }

        // 4. Villager Fear
        if (drawbacks.contains("villager_fear") && player.tickCount % 20 == 0) {
            AABB area = player.getBoundingBox().inflate(12.0);
            for (Villager villager : player.level().getEntitiesOfClass(Villager.class, area)) {
                villager.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, true));
            }
        }

        // 5. Insomnia Curse (Spawns Phantoms at Night)
        if (drawbacks.contains("insomnia_curse") && player.level().isNight() && player.tickCount % 1200 == 0) {
            if (player.level().canSeeSky(player.blockPosition())) {
                net.minecraft.world.entity.monster.Phantom phantom = net.minecraft.world.entity.EntityType.PHANTOM.create(player.level());
                if (phantom != null) {
                    phantom.moveTo(player.getX(), player.getY() + 15, player.getZ(), player.getYRot(), player.getXRot());
                    player.level().addFreshEntity(phantom);
                }
            }
        }

        // 6. Lightning Attraction
        if (drawbacks.contains("lightning_attraction") && player.level().isThundering() && player.level().canSeeSky(player.blockPosition())) {
            if (player.level().random.nextInt(600) == 0) {
                net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(player.level());
                if (bolt != null) {
                    bolt.moveTo(player.position());
                    player.level().addFreshEntity(bolt);
                }
            }
        }

        // 7. Drowning in Shallow Water
        if (drawbacks.contains("drowning_in_shallow_water") && player.isInWater()) {
            player.setAirSupply(0);
        }

        // 8. Equipment Restrictions (61-80)
        if (drawbacks.contains("no_offhand_slot") && !player.getOffhandItem().isEmpty()) {
            ItemStack stack = player.getOffhandItem();
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        if (drawbacks.contains("no_iron_equipment")) {
            enforceNoMaterial(player, Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
        }
        if (drawbacks.contains("no_diamond_equipment")) {
            enforceNoMaterial(player, Items.DIAMOND_SWORD, Items.DIAMOND_PICKAXE, Items.DIAMOND_AXE, Items.DIAMOND_SHOVEL, Items.DIAMOND_HOE, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
        }
        if (drawbacks.contains("no_elytra_equip") && player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) {
            enforceSlotEmpty(player, EquipmentSlot.CHEST);
        }

        // 9. Mount Restrictions (111-120)
        if (player.isPassenger()) {
            net.minecraft.world.entity.Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                if (drawbacks.contains("boat_inability") && vehicle instanceof net.minecraft.world.entity.vehicle.Boat) player.stopRiding();
                if (drawbacks.contains("minecart_inability") && vehicle instanceof net.minecraft.world.entity.vehicle.AbstractMinecart) player.stopRiding();
                if (drawbacks.contains("horse_mount_inability") && (vehicle instanceof net.minecraft.world.entity.animal.horse.AbstractHorse)) player.stopRiding();
                if (drawbacks.contains("strider_mount_inability") && vehicle instanceof net.minecraft.world.entity.monster.Strider) player.stopRiding();
            }
        }

        // 10. Biome & Climate Vulnerabilities (61-70)
        String biomePath = player.level().getBiome(player.blockPosition()).unwrapKey().map(k -> k.location().getPath()).orElse("");
        if (drawbacks.contains("desert_dehydration") && (biomePath.contains("desert") || biomePath.contains("badlands"))) {
            if (player.tickCount % 20 == 0) {
                player.causeFoodExhaustion(player.isSprinting() ? 0.6f : 0.2f);
            }
        }
        if (drawbacks.contains("snow_hypothermia") && (biomePath.contains("snow") || biomePath.contains("ice"))) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
            player.setTicksFrozen(Math.min(player.getTicksRequiredToFreeze() + 20, player.getTicksFrozen() + 2));
        }
        if (drawbacks.contains("swamp_miasma") && biomePath.contains("swamp")) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0, false, false, true));
        }
        if (drawbacks.contains("cave_suffocation") && player.getY() < 0) {
            if (player.tickCount % 40 == 0) player.hurt(player.damageSources().inWall(), 1.0f);
        }
        if (drawbacks.contains("high_altitude_sickness") && player.getY() > 120) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, true));
        }
        if (drawbacks.contains("ocean_pressure") && player.isInWater() && player.getY() < 30) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 40, 1, false, false, true));
        }

        // 11. Sensory & Mental (121-130)
        if (drawbacks.contains("blind_in_darkness") && player.level().getMaxLocalRawBrightness(player.blockPosition()) < 3) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false, true));
        }
        if (drawbacks.contains("glowing_curse")) {
            player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false, true));
        }
        if (drawbacks.contains("bloodlust_frenzy") && player.getHealth() < (player.getMaxHealth() * 0.20f)) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, true));
        }
        if (drawbacks.contains("golem_rust") && player.isInWater() && player.tickCount % 600 == 0) {
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 0, false, false, true));
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, 0, false, false, true));
        }
        if (drawbacks.contains("dragon_greed") && player.tickCount % 1200 == 0) {
            boolean hasTreasure = false;
            for (ItemStack s : player.getInventory().items) {
                if (s.is(Items.GOLD_INGOT) || s.is(Items.DIAMOND) || s.is(Items.EMERALD) || s.is(Items.GOLD_BLOCK) || s.is(Items.DIAMOND_BLOCK)) {
                    hasTreasure = true; break;
                }
            }
            if (!hasTreasure) player.hurt(player.damageSources().magic(), 1.0f);
        }
    }

    private static void checkCustomItemRestrictions(Player player, RaceData race) {
        // Mainhand
        ItemStack mainhand = player.getMainHandItem();
        if (!mainhand.isEmpty()) {
            ResourceLocation loc = BuiltInRegistries.ITEM.getKey(mainhand.getItem());
            if (loc != null && race.isItemRestricted(loc.toString())) {
                enforceHandEmpty(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        }

        // Offhand
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty()) {
            ResourceLocation loc = BuiltInRegistries.ITEM.getKey(offhand.getItem());
            if (loc != null && race.isItemRestricted(loc.toString())) {
                enforceHandEmpty(player, net.minecraft.world.InteractionHand.OFF_HAND);
            }
        }

        // Armor Slots
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                ResourceLocation loc = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (loc != null && race.isItemRestricted(loc.toString())) {
                    enforceSlotEmpty(player, slot);
                }
            }
        }
    }

    private static void enforceHandEmpty(Player player, net.minecraft.world.InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.isEmpty()) {
            player.setItemInHand(hand, ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            if (player.tickCount % 40 == 0) {
                player.displayClientMessage(Component.literal("§c[!] Your race cannot equip this item!"), true);
            }
        }
    }

    private static void enforceSlotEmpty(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (!stack.isEmpty()) {
            player.setItemSlot(slot, ItemStack.EMPTY);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            if (player.tickCount % 60 == 0) {
                player.displayClientMessage(Component.literal("§c[!] Your race cannot equip this item!"), true);
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

    private static void enforceNoMaterial(Player player, net.minecraft.world.item.Item... items) {
        for (net.minecraft.world.item.Item forbidden : items) {
            if (player.getMainHandItem().is(forbidden)) {
                ItemStack stack = player.getMainHandItem();
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                if (!player.getInventory().add(stack)) player.drop(stack, false);
            }
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (player.getItemBySlot(slot).is(forbidden)) {
                    enforceSlotEmpty(player, slot);
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
     * Returns true if food consumption is allowed, or false if blocked by a diet drawback or custom restricted food.
     */
    public static boolean checkFoodConsumption(Player player, ItemStack food, List<String> drawbacks) {
        if (player == null || food == null) return true;

        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
        if (race != null) {
            ResourceLocation foodLoc = BuiltInRegistries.ITEM.getKey(food.getItem());
            if (foodLoc != null && race.isFoodRestricted(foodLoc.toString())) {
                player.displayClientMessage(Component.literal("§c[!] Your race cannot digest this food!"), true);
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0));
                return false;
            }
        }

        if (drawbacks == null || drawbacks.isEmpty()) return true;

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
