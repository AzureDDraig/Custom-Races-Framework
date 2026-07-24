package ddraig.net.customraces.integration;

import ddraig.net.customraces.data.RaceData;
import dev.architectury.platform.Platform;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Soft dependency integration with Pehkui scale mod.
 * Applies player model scaling if Pehkui is loaded, falling back to vanilla attribute modifiers otherwise.
 */
public class PehkuiIntegration {
    private static final UUID HEALTH_MOD_UUID = UUID.fromString("c02821b0-1010-4100-a001-000000000001");
    private static final UUID SPEED_MOD_UUID = UUID.fromString("c02821b0-1010-4100-a001-000000000002");
    private static final UUID ARMOR_MOD_UUID = UUID.fromString("c02821b0-1010-4100-a001-000000000003");
    private static final UUID DAMAGE_MOD_UUID = UUID.fromString("c02821b0-1010-4100-a001-000000000004");

    public static boolean isPehkuiLoaded() {
        return Platform.isModLoaded("pehkui");
    }

    public static void setScale(net.minecraft.world.entity.Entity entity, float scale) {
        if (entity == null || !isPehkuiLoaded()) return;
        try {
            Class<?> scaleTypesClass = Class.forName("virtuoel.pehkui.api.ScaleTypes");
            Class<?> scaleDataClass = Class.forName("virtuoel.pehkui.api.ScaleData");
            Method setTargetScaleMethod = scaleDataClass.getMethod("setTargetScale", float.class);
            Method setScaleMethod = scaleDataClass.getMethod("setScale", float.class);

            Object baseType = scaleTypesClass.getField("BASE").get(null);
            Method getScaleDataMethod = baseType.getClass().getMethod("getScaleData", net.minecraft.world.entity.Entity.class);

            Object data = getScaleDataMethod.invoke(baseType, entity);
            if (data != null) {
                setTargetScaleMethod.invoke(data, scale);
                setScaleMethod.invoke(data, scale);
            }
        } catch (Exception ignored) {}
    }

    public static void applyRaceScales(Player player, RaceData race) {
        if (player == null) return;

        // Apply Vanilla Base Stat Modifiers first
        applyVanillaAttributes(player, race);

        if (race != null) {
            boolean isTransformed = ddraig.net.customraces.event.WereRaceTransformHandler.isTransformed(player.getUUID());
            float rawWereHeight = race.wereHeightScale > 0 ? race.wereHeightScale : 1.3f;
            float rawWereWidth = race.wereWidthScale > 0 ? race.wereWidthScale : 1.3f;
            float rawHeight = race.heightScale > 0 ? race.heightScale : 1.0f;
            float rawWidth = race.widthScale > 0 ? race.widthScale : 1.0f;

            float heightMult = isTransformed && race.enableWereRace ? rawWereHeight : rawHeight;
            float widthMult = isTransformed && race.enableWereRace ? rawWereWidth : rawWidth;

            float baseScale = race.baseScale > 0 ? race.baseScale : 1.0f;
            float hScale = heightMult * baseScale;
            float wScale = widthMult * baseScale;
            float rScale = race.reachScale > 0 ? race.reachScale : 1.0f;
            float sScale = race.stepHeightScale > 0 ? race.stepHeightScale : 1.0f;

            if (isPehkuiLoaded()) {
                try {
                    Class<?> scaleTypesClass = Class.forName("virtuoel.pehkui.api.ScaleTypes");
                Class<?> scaleDataClass = Class.forName("virtuoel.pehkui.api.ScaleData");
                Method setTargetScaleMethod = scaleDataClass.getMethod("setTargetScale", float.class);
                Method setScaleMethod = scaleDataClass.getMethod("setScale", float.class);
                Method getScaleDataMethod = null;

                // 1. BASE scale (scales overall entity model & hitbox)
                try {
                    Object baseType = scaleTypesClass.getField("BASE").get(null);
                    getScaleDataMethod = baseType.getClass().getMethod("getScaleData", net.minecraft.world.entity.Entity.class);
                    Object bData = getScaleDataMethod.invoke(baseType, player);
                    if (bData != null) {
                        float avgScale = (hScale + wScale) / 2.0f;
                        setTargetScaleMethod.invoke(bData, avgScale);
                        setScaleMethod.invoke(bData, avgScale);
                    }
                } catch (Exception ignored) {}

                // 2. HEIGHT scale
                try {
                    Object heightType = scaleTypesClass.getField("HEIGHT").get(null);
                    if (getScaleDataMethod == null) getScaleDataMethod = heightType.getClass().getMethod("getScaleData", net.minecraft.world.entity.Entity.class);
                    Object hData = getScaleDataMethod.invoke(heightType, player);
                    if (hData != null) {
                        setTargetScaleMethod.invoke(hData, hScale);
                        setScaleMethod.invoke(hData, hScale);
                    }
                } catch (Exception ignored) {}

                // 3. WIDTH scale
                try {
                    Object widthType = scaleTypesClass.getField("WIDTH").get(null);
                    if (getScaleDataMethod == null) getScaleDataMethod = widthType.getClass().getMethod("getScaleData", net.minecraft.world.entity.Entity.class);
                    Object wData = getScaleDataMethod.invoke(widthType, player);
                    if (wData != null) {
                        setTargetScaleMethod.invoke(wData, wScale);
                        setScaleMethod.invoke(wData, wScale);
                    }
                } catch (Exception ignored) {}

                // 4. REACH scale
                try {
                    Object reachType = scaleTypesClass.getField("REACH").get(null);
                    Object rData = getScaleDataMethod.invoke(reachType, player);
                    if (rData != null) {
                        setTargetScaleMethod.invoke(rData, rScale);
                        setScaleMethod.invoke(rData, rScale);
                    }
                } catch (Exception ignored) {}

                // 5. STEP_HEIGHT scale
                try {
                    Object stepHeightType = scaleTypesClass.getField("STEP_HEIGHT").get(null);
                    Object sData = getScaleDataMethod.invoke(stepHeightType, player);
                    if (sData != null) {
                        setTargetScaleMethod.invoke(sData, sScale);
                        setScaleMethod.invoke(sData, sScale);
                    }
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        }
        }
        try {
            player.refreshDimensions();
        } catch (Exception ignored) {}
    }

    public static void resetPlayerScales(Player player) {
        if (player == null) return;
        clearVanillaAttributes(player);

        // Clear Vanilla Modifiers
        clearVanillaAttributes(player);

        if (isPehkuiLoaded()) {
            try {
                Class<?> scaleTypesClass = Class.forName("virtuoel.pehkui.api.ScaleTypes");
                Class<?> scaleDataClass = Class.forName("virtuoel.pehkui.api.ScaleData");
                Method setTargetScaleMethod = scaleDataClass.getMethod("setTargetScale", float.class);

                Object heightType = scaleTypesClass.getField("HEIGHT").get(null);
                Object widthType = scaleTypesClass.getField("WIDTH").get(null);
                Object reachType = scaleTypesClass.getField("REACH").get(null);
                Object stepHeightType = scaleTypesClass.getField("STEP_HEIGHT").get(null);
                Object motionType = scaleTypesClass.getField("MOTION_AT_REST").get(null);

                Method getScaleDataMethod = heightType.getClass().getMethod("getScaleData", net.minecraft.world.entity.Entity.class);

                Object hData = getScaleDataMethod.invoke(heightType, player);
                Object wData = getScaleDataMethod.invoke(widthType, player);
                Object rData = getScaleDataMethod.invoke(reachType, player);
                Object sData = getScaleDataMethod.invoke(stepHeightType, player);
                Object mData = getScaleDataMethod.invoke(motionType, player);

                Method setScaleMethod = scaleDataClass.getMethod("setScale", float.class);

                if (hData != null) { setTargetScaleMethod.invoke(hData, 1.0f); setScaleMethod.invoke(hData, 1.0f); }
                if (wData != null) { setTargetScaleMethod.invoke(wData, 1.0f); setScaleMethod.invoke(wData, 1.0f); }
                if (rData != null) { setTargetScaleMethod.invoke(rData, 1.0f); setScaleMethod.invoke(rData, 1.0f); }
                if (sData != null) { setTargetScaleMethod.invoke(sData, 1.0f); setScaleMethod.invoke(sData, 1.0f); }
                if (mData != null) { setTargetScaleMethod.invoke(mData, 1.0f); setScaleMethod.invoke(mData, 1.0f); }
            } catch (Exception ignored) {}
        }
        try {
            player.refreshDimensions();
        } catch (Exception ignored) {}
    }

    private static void applyVanillaAttributes(Player player, RaceData race) {
        clearVanillaAttributes(player);
        if (race == null) return;

        // Health modifier
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null && race.maxHealth != 20.0f) {
            float healthDiff = race.maxHealth - 20.0f;
            healthAttr.addTransientModifier(new AttributeModifier(HEALTH_MOD_UUID, "Race Health", healthDiff, AttributeModifier.Operation.ADDITION));
        }

        // Speed modifier
        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null && race.movementSpeed != 0.1f) {
            float speedDiff = (race.movementSpeed - 0.1f) / 0.1f;
            speedAttr.addTransientModifier(new AttributeModifier(SPEED_MOD_UUID, "Race Speed", speedDiff, AttributeModifier.Operation.MULTIPLY_BASE));
        }

        // Armor modifier
        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null && race.armor > 0.0f) {
            armorAttr.addTransientModifier(new AttributeModifier(ARMOR_MOD_UUID, "Race Armor", race.armor, AttributeModifier.Operation.ADDITION));
        }

        // Attack damage modifier
        AttributeInstance damageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null && race.attackDamage != 1.0f) {
            float damageDiff = race.attackDamage - 1.0f;
            damageAttr.addTransientModifier(new AttributeModifier(DAMAGE_MOD_UUID, "Race Damage", damageDiff, AttributeModifier.Operation.ADDITION));
        }
    }

    private static void clearVanillaAttributes(Player player) {
        AttributeInstance healthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) healthAttr.removeModifier(HEALTH_MOD_UUID);

        AttributeInstance speedAttr = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null) speedAttr.removeModifier(SPEED_MOD_UUID);

        AttributeInstance armorAttr = player.getAttribute(Attributes.ARMOR);
        if (armorAttr != null) armorAttr.removeModifier(ARMOR_MOD_UUID);

        AttributeInstance damageAttr = player.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damageAttr != null) damageAttr.removeModifier(DAMAGE_MOD_UUID);
    }
}
