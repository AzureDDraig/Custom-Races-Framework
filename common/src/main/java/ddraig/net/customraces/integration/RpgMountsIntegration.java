package ddraig.net.customraces.integration;

import dev.architectury.platform.Platform;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Strider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.Boat;

import java.util.List;
import java.util.UUID;

/**
 * Dynamic integration bridge for RPG Mounts Framework and vanilla/modded mounts.
 * Enforces mount drawback restrictions and applies racial mount stat boosts.
 */
public class RpgMountsIntegration {

    private static final UUID MOUNT_SPEED_UUID = UUID.fromString("f02821b0-1010-4100-c001-000000000001");
    private static final UUID MOUNT_HEALTH_UUID = UUID.fromString("f02821b0-1010-4100-c001-000000000002");

    public static boolean isRpgMountsLoaded() {
        return Platform.isModLoaded("rpg_mounts") || Platform.isModLoaded("mythicmounts");
    }

    /**
     * Enforces mount drawback restrictions (horse_mount_inability, strider_mount_inability, boat_inability, minecart_inability).
     */
    public static void checkMountRestrictions(Player player, List<String> drawbacks) {
        if (player == null || drawbacks == null || drawbacks.isEmpty() || !player.isPassenger()) return;

        Entity vehicle = player.getVehicle();
        if (vehicle == null) return;

        boolean shouldDismount = false;
        String reason = "";

        if (drawbacks.contains("horse_mount_inability") && vehicle instanceof AbstractHorse) {
            shouldDismount = true;
            reason = "Your race is incapable of riding horses!";
        } else if (drawbacks.contains("strider_mount_inability") && vehicle instanceof Strider) {
            shouldDismount = true;
            reason = "Your race is incapable of riding striders!";
        } else if (drawbacks.contains("boat_inability") && vehicle instanceof Boat) {
            shouldDismount = true;
            reason = "Your race is incapable of operating boats!";
        } else if (drawbacks.contains("minecart_inability") && vehicle instanceof AbstractMinecart) {
            shouldDismount = true;
            reason = "Your race is incapable of riding minecarts!";
        }

        if (shouldDismount) {
            player.stopRiding();
            player.displayClientMessage(Component.literal("§c" + reason), true);
        }
    }

    /**
     * Applies racial buffs to mounts (e.g. pack_leader_buff, beast_instincts).
     */
    public static void applyMountBuffs(Player player, List<String> passives) {
        if (player == null || passives == null || passives.isEmpty() || !player.isPassenger()) return;

        Entity vehicle = player.getVehicle();
        if (vehicle instanceof LivingEntity mount) {
            if (passives.contains("pack_leader_buff")) {
                // Speed +30%
                AttributeInstance speedAttr = mount.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null && speedAttr.getModifier(MOUNT_SPEED_UUID) == null) {
                    speedAttr.addTransientModifier(new AttributeModifier(MOUNT_SPEED_UUID, "Race Mount Speed", 0.30, AttributeModifier.Operation.MULTIPLY_TOTAL));
                }
                // Health +20
                AttributeInstance healthAttr = mount.getAttribute(Attributes.MAX_HEALTH);
                if (healthAttr != null && healthAttr.getModifier(MOUNT_HEALTH_UUID) == null) {
                    healthAttr.addTransientModifier(new AttributeModifier(MOUNT_HEALTH_UUID, "Race Mount Health", 20.0, AttributeModifier.Operation.ADDITION));
                    mount.heal(20.0f);
                }
            }
        }
    }
}
