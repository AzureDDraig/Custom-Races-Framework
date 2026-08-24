package ddraig.net.customraces.integration;

import ddraig.net.customraces.data.RaceData;
import dev.architectury.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Dynamic integration bridge for Epic Fight mod.
 * Synchronizes stamina, posture, weight, and battle mode weapon enforcement via reflection.
 */
public class EpicFightIntegration {

    private static final UUID EPIC_STAMINA_UUID = UUID.fromString("e02821b0-1010-4100-b001-000000000001");
    private static final UUID EPIC_STAMINA_REGEN_UUID = UUID.fromString("e02821b0-1010-4100-b001-000000000002");
    private static final UUID EPIC_WEIGHT_UUID = UUID.fromString("e02821b0-1010-4100-b001-000000000003");
    private static final UUID EPIC_IMPACT_UUID = UUID.fromString("e02821b0-1010-4100-b001-000000000004");
    private static final UUID EPIC_ARMOR_NEGATION_UUID = UUID.fromString("e02821b0-1010-4100-b001-000000000005");

    public static boolean isEpicFightLoaded() {
        return Platform.isModLoaded("epicfight");
    }

    /**
     * Applies Epic Fight attribute modifiers according to the player's active racial passives and drawbacks.
     */
    public static void applyEpicFightAttributes(Player player, RaceData race, List<String> passives, List<String> drawbacks) {
        if (player == null || !isEpicFightLoaded()) return;

        try {
            Class<?> attributesClass = Class.forName("yesman.epicfight.world.entity.ai.attribute.EpicFightAttributes");

            // 1. Max Stamina (epicfight:max_stamina)
            double staminaBonus = 0.0;
            if (passives != null) {
                if (passives.contains("beast_instincts")) staminaBonus += 15.0;
                if (passives.contains("unbreakable_will")) staminaBonus += 25.0;
                if (passives.contains("overclock_speed")) staminaBonus += 10.0;
            }
            if (drawbacks != null && drawbacks.contains("fragile_frame")) staminaBonus -= 15.0;
            applyModifier(player, attributesClass, "MAX_STAMINA", EPIC_STAMINA_UUID, "Race Epic Max Stamina", staminaBonus);

            // 2. Stamina Regen (epicfight:stamina_regen)
            double staminaRegenBonus = 0.0;
            if (passives != null) {
                if (passives.contains("speed_boost")) staminaRegenBonus += 1.0;
                if (passives.contains("overclock_speed")) staminaRegenBonus += 2.0;
                if (passives.contains("wild_regeneration")) staminaRegenBonus += 1.5;
            }
            applyModifier(player, attributesClass, "STAMINA_REGEN", EPIC_STAMINA_REGEN_UUID, "Race Epic Stamina Regen", staminaRegenBonus);

            // 3. Weight & Impact (epicfight:weight, epicfight:impact)
            double weightBonus = 0.0;
            double impactBonus = 0.0;
            if (passives != null) {
                if (passives.contains("golem_density")) {
                    weightBonus += 35.0;
                    impactBonus += 15.0;
                }
                if (passives.contains("dragon_scales") || passives.contains("thick_hide")) {
                    weightBonus += 15.0;
                    impactBonus += 8.0;
                }
            }
            applyModifier(player, attributesClass, "WEIGHT", EPIC_WEIGHT_UUID, "Race Epic Weight", weightBonus);
            applyModifier(player, attributesClass, "IMPACT", EPIC_IMPACT_UUID, "Race Epic Impact", impactBonus);

            // 4. Armor Negation (epicfight:armor_negation)
            double armorNegationBonus = 0.0;
            if (passives != null && (passives.contains("armor_piercing") || passives.contains("giant_slayer"))) {
                armorNegationBonus += 0.25;
            }
            applyModifier(player, attributesClass, "ARMOR_NEGATION", EPIC_ARMOR_NEGATION_UUID, "Race Epic Armor Negation", armorNegationBonus);

        } catch (Throwable ignored) {
            // Epic Fight not present or different attribute naming
        }
    }

    private static void applyModifier(Player player, Class<?> attributesClass, String fieldName, UUID uuid, String name, double value) {
        try {
            Object attrObj = attributesClass.getField(fieldName).get(null);
            if (attrObj instanceof Attribute attribute) {
                AttributeInstance instance = player.getAttribute(attribute);
                if (instance != null) {
                    instance.removeModifier(uuid);
                    if (Math.abs(value) > 0.001) {
                        instance.addTransientModifier(new AttributeModifier(uuid, name, value, AttributeModifier.Operation.ADDITION));
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Checks if player is in Battle Mode wielding a restricted weapon.
     */
    public static boolean validateBattleModeWeapon(Player player, RaceData race) {
        if (player == null || race == null || !isEpicFightLoaded()) return true;

        ItemStack mainHand = player.getMainHandItem();
        if (!mainHand.isEmpty()) {
            String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(mainHand.getItem()).toString();
            if (race.isItemRestricted(itemId)) {
                player.displayClientMessage(Component.literal("§c[Epic Fight] Your race cannot wield this weapon!"), true);
                return false;
            }
        }
        return true;
    }
}
