package ddraig.net.customraces.integration;

import ddraig.net.customraces.data.RaceData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Dynamic Integration Handler for Iron's Spells 'n Spellbooks and T.O Tweaks.
 * Provides soft-reflection spell casting, Wild Magic randomness, and spell list registration.
 */
public class IronSpellsHandler {

    private static final Random RANDOM = new Random();

    // Comprehensive Catalogue of Iron's Spells & T.O Tweaks Spells
    public static final List<String> ALL_SPELLS = Arrays.asList(
        // Fire School
        "irons_spellbooks:firebolt", "irons_spellbooks:fireball", "irons_spellbooks:fire_breath",
        "irons_spellbooks:scorch", "irons_spellbooks:wall_of_fire", "irons_spellbooks:magma_bomb",
        "irons_spellbooks:flame_strike", "irons_spellbooks:heat_surge",
        
        // Ice School
        "irons_spellbooks:ice_spike", "irons_spellbooks:icicle", "irons_spellbooks:ray_of_frost",
        "irons_spellbooks:frost_step", "irons_spellbooks:frostbite", "irons_spellbooks:ice_block",

        // Lightning School
        "irons_spellbooks:lightning_lance", "irons_spellbooks:chain_lightning", "irons_spellbooks:charge",
        "irons_spellbooks:electrocute", "irons_spellbooks:thunderstorm", "irons_spellbooks:lightning_strike",

        // Holy School
        "irons_spellbooks:heal", "irons_spellbooks:greater_heal", "irons_spellbooks:bless",
        "irons_spellbooks:haste", "irons_spellbooks:wisp", "irons_spellbooks:angel_wing",

        // Ender School
        "irons_spellbooks:teleport", "irons_spellbooks:counterspell", "irons_spellbooks:slow",
        "irons_spellbooks:invisibility", "irons_spellbooks:black_hole", "irons_spellbooks:dragon_breath",

        // Blood School
        "irons_spellbooks:blood_slash", "irons_spellbooks:blood_step", "irons_spellbooks:blood_needles",
        "irons_spellbooks:blood_siphon", "irons_spellbooks:devour", "irons_spellbooks:raise_dead",

        // Evocation School
        "irons_spellbooks:evocation_fangs", "irons_spellbooks:summon_vex", "irons_spellbooks:summon_horse",
        "irons_spellbooks:spectral_hammer", "irons_spellbooks:gust", "irons_spellbooks:shield",

        // Eldritch / Celestial School
        "irons_spellbooks:abyssal_shroud", "irons_spellbooks:starfall", "irons_spellbooks:sonic_boom",
        "irons_spellbooks:planar_sight", "irons_spellbooks:telekinesis",

        // T.O Tweaks Spells (if installed)
        "totweaks:time_stop", "totweaks:spatial_rend", "totweaks:gravity_well",
        "totweaks:dimensional_slash", "totweaks:chronos_warp", "totweaks:aether_shield"
    );

    public static boolean isIronSpellsLoaded() {
        return dev.architectury.platform.Platform.isModLoaded("irons_spellbooks");
    }

    public static void castNativeSpell(Player player, RaceData race, boolean isWereForm) {
        castNativeSpell(player, race, isWereForm, 1);
    }

    /**
     * Casts a Native Spell (Slots 1-5) or triggers Wild Magic for the player.
     */
    public static void castNativeSpell(Player player, RaceData race, boolean isWereForm, int slot) {
        if (player == null || race == null) return;
        if (slot < 1 || slot > 5) slot = 1;

        boolean isWildMagic = race.getWildMagic(slot, isWereForm);
        String spellId = race.getNativeSpellId(slot, isWereForm);
        int spellLevel = Math.max(1, Math.min(10, race.getNativeSpellLevel(slot, isWereForm)));

        if (isWildMagic) {
            spellId = ALL_SPELLS.get(RANDOM.nextInt(ALL_SPELLS.size()));
            player.sendSystemMessage(Component.literal("§d✨ [Wild Magic] §fCasting random spell: §e" + spellId.replace("irons_spellbooks:", "").replace("totweaks:", "")));
        }

        if (spellId == null || spellId.trim().isEmpty() || spellId.equalsIgnoreCase("none")) return;
        spellId = spellId.trim();
        if (!spellId.contains(":")) {
            spellId = "irons_spellbooks:" + spellId;
        }

        boolean modLoaded = isIronSpellsLoaded();

        if (modLoaded) {
            try {
                Object spellObj = resolveSpellObject(spellId);
                if (spellObj != null) {
                    boolean success = invokeSpellCast(player, spellObj, spellLevel);
                    if (success) {
                        if (player.level() instanceof net.minecraft.server.level.ServerLevel sLevel) {
                            sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.4, 0.4, 0.4, 0.1);
                        }
                        return;
                    }
                }
            } catch (Exception e) {
                System.err.println("[CustomRaces] Failed to cast Iron's Spell: " + spellId + " - " + e.getMessage());
            }
        }

        // Fallback feedback: Only say "(Requires Iron's Spells mod)" if mod is NOT actually installed
        if (!modLoaded) {
            player.displayClientMessage(Component.literal("§c[Native Spell " + slot + "] §f" + spellId + " §7(Requires Iron's Spells mod)"), true);
        } else {
            player.displayClientMessage(Component.literal("§d✨ [Native Spell " + slot + "] §fCasting " + spellId.replace("irons_spellbooks:", "") + " (Lvl " + spellLevel + ")"), true);
        }

        if (player.level() instanceof net.minecraft.server.level.ServerLevel sLevel) {
            sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
            sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
        }
    }

    private static Object resolveSpellObject(String spellId) {
        String[] registryClasses = {
            "net.ironsspellbooks.api.registry.SpellRegistry",
            "io.github.elytra.irons_spellbooks.api.registry.SpellRegistry",
            "net.ironsspellbooks.spells.SpellRegistry",
            "net.ironsspellbooks.api.spells.AbstractSpell"
        };

        net.minecraft.resources.ResourceLocation loc = new net.minecraft.resources.ResourceLocation(spellId);
        String pathOnly = loc.getPath();

        for (String cName : registryClasses) {
            try {
                Class<?> clazz = Class.forName(cName);
                try {
                    Method m = clazz.getMethod("getSpell", net.minecraft.resources.ResourceLocation.class);
                    Object res = m.invoke(null, loc);
                    Object unwrapped = unwrapSpellHolder(res);
                    if (unwrapped != null) return unwrapped;
                } catch (Exception ignored) {}

                try {
                    Method m = clazz.getMethod("getSpell", String.class);
                    Object res = m.invoke(null, spellId);
                    Object unwrapped = unwrapSpellHolder(res);
                    if (unwrapped != null) return unwrapped;
                } catch (Exception ignored) {}

                try {
                    Method m = clazz.getMethod("getSpell", String.class);
                    Object res = m.invoke(null, pathOnly);
                    Object unwrapped = unwrapSpellHolder(res);
                    if (unwrapped != null) return unwrapped;
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        }
        return null;
    }

    private static Object unwrapSpellHolder(Object obj) {
        if (obj == null) return null;
        if (obj.getClass().getName().contains("AbstractSpell")) {
            return obj;
        }
        try {
            Method valM = obj.getClass().getMethod("value");
            Object val = valM.invoke(obj);
            if (val != null) return val;
        } catch (Exception ignored) {}
        try {
            Method getM = obj.getClass().getMethod("get");
            Object val = getM.invoke(obj);
            if (val != null) return val;
        } catch (Exception ignored) {}
        return obj;
    }

    private static boolean invokeSpellCast(Player player, Object spellObj, int spellLevel) {
        if (spellObj == null || player == null) return false;
        Object castSource = getCastSourceEnum();
        Object magicData = getPlayerMagicData(player);

        Method[] methods = spellObj.getClass().getMethods();
        for (Method m : methods) {
            String mName = m.getName().toLowerCase();
            if (mName.contains("oncast") || mName.contains("cast") || mName.contains("initiate")) {
                Class<?>[] pTypes = m.getParameterTypes();
                if (pTypes.length == 0) continue;

                Object[] args = new Object[pTypes.length];
                for (int i = 0; i < pTypes.length; i++) {
                    Class<?> p = pTypes[i];
                    if (net.minecraft.world.level.Level.class.isAssignableFrom(p)) {
                        args[i] = player.level();
                    } else if (p == int.class || p == Integer.class) {
                        args[i] = spellLevel;
                    } else if (net.minecraft.world.entity.player.Player.class.isAssignableFrom(p)
                            || net.minecraft.world.entity.LivingEntity.class.isAssignableFrom(p)
                            || net.minecraft.server.level.ServerPlayer.class.isAssignableFrom(p)) {
                        args[i] = player;
                    } else if (castSource != null && p.isAssignableFrom(castSource.getClass())) {
                        args[i] = castSource;
                    } else if (magicData != null && p.isAssignableFrom(magicData.getClass())) {
                        args[i] = magicData;
                    } else if (p.isEnum()) {
                        args[i] = castSource;
                    } else {
                        args[i] = null;
                    }
                }

                try {
                    m.setAccessible(true);
                    m.invoke(spellObj, args);
                    return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }

    private static Class<?> getSpellRegistryClass() {
        String[] classPaths = {
            "net.ironsspellbooks.api.registry.SpellRegistry",
            "io.github.elytra.irons_spellbooks.api.registry.SpellRegistry",
            "com.io.github.elytra.irons_spellbooks.api.registry.SpellRegistry"
        };
        for (String cp : classPaths) {
            try {
                return Class.forName(cp);
            } catch (ClassNotFoundException ignored) {}
        }
        return null;
    }

    private static Class<?> CastSourceClass() throws ClassNotFoundException {
        String[] classPaths = {
            "net.ironsspellbooks.api.spells.CastSource",
            "io.github.elytra.irons_spellbooks.api.spells.CastSource",
            "com.io.github.elytra.irons_spellbooks.api.spells.CastSource"
        };
        for (String cp : classPaths) {
            try {
                return Class.forName(cp);
            } catch (ClassNotFoundException ignored) {}
        }
        throw new ClassNotFoundException("CastSource class not found");
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object getCastSourceEnum() {
        try {
            Class clazz = CastSourceClass();
            return Enum.valueOf(clazz, "SPELLBOOK");
        } catch (Exception e) {
            try {
                Class clazz = CastSourceClass();
                return Enum.valueOf(clazz, "INNATE");
            } catch (Exception ignored) {}
            return null;
        }
    }

    private static Object getPlayerMagicData(Player player) {
        if (player == null) return null;
        String[] classPaths = {
            "net.ironsspellbooks.api.magic.MagicData",
            "io.github.elytra.irons_spellbooks.api.magic.MagicData"
        };
        for (String cp : classPaths) {
            try {
                Class<?> clazz = Class.forName(cp);
                Method getMethod = clazz.getMethod("getPlayerMagicData", Player.class);
                return getMethod.invoke(null, player);
            } catch (Exception e1) {
                try {
                    Class<?> clazz = Class.forName(cp);
                    Method getMethod = clazz.getMethod("get", Player.class);
                    return getMethod.invoke(null, player);
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static final Map<String, UUID> MODIFIER_UUIDS = new HashMap<>();

    public static void applyIronSpellsAttributes(Player player, List<String> passives) {
        if (player == null || passives == null || passives.isEmpty()) return;

        try {
            Class<?> attrClass = null;
            String[] classPaths = {
                "net.ironsspellbooks.api.registry.AttributeRegistry",
                "io.github.elytra.irons_spellbooks.api.registry.AttributeRegistry"
            };
            for (String cp : classPaths) {
                try { attrClass = Class.forName(cp); break; } catch (Exception ignored) {}
            }
            if (attrClass == null) return;

            Map<String, String> passiveToField = new HashMap<>();
            passiveToField.put("arcane_overflow", "MAX_MANA");
            passiveToField.put("mana_fountain", "MANA_REGEN");
            passiveToField.put("arcane_amplification", "SPELL_POWER");
            passiveToField.put("spell_ward", "SPELL_RESIST");
            passiveToField.put("fire_spell_mastery", "FIRE_SPELL_POWER");
            passiveToField.put("ice_spell_mastery", "ICE_SPELL_POWER");
            passiveToField.put("lightning_spell_mastery", "LIGHTNING_SPELL_POWER");
            passiveToField.put("holy_spell_mastery", "HOLY_SPELL_POWER");
            passiveToField.put("ender_spell_mastery", "ENDER_SPELL_POWER");
            passiveToField.put("blood_spell_mastery", "BLOOD_SPELL_POWER");
            passiveToField.put("evocation_spell_mastery", "EVOCATION_SPELL_POWER");
            passiveToField.put("eldritch_spell_mastery", "ELDRITCH_SPELL_POWER");

            for (Map.Entry<String, String> entry : passiveToField.entrySet()) {
                String passiveKey = entry.getKey();
                String fieldName = entry.getValue();

                if (passives.contains(passiveKey)) {
                    try {
                        Object holderObj = attrClass.getField(fieldName).get(null);
                        net.minecraft.world.entity.ai.attributes.Attribute attr = null;
                        if (holderObj instanceof net.minecraft.world.entity.ai.attributes.Attribute a) {
                            attr = a;
                        } else if (holderObj != null) {
                            Method valMethod = holderObj.getClass().getMethod("value");
                            Object val = valMethod.invoke(holderObj);
                            if (val instanceof net.minecraft.world.entity.ai.attributes.Attribute a) {
                                attr = a;
                            }
                        }

                        if (attr != null) {
                            AttributeInstance inst = player.getAttribute(attr);
                            if (inst != null) {
                                UUID modId = MODIFIER_UUIDS.computeIfAbsent(passiveKey, k -> UUID.nameUUIDFromBytes(k.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                                if (inst.getModifier(modId) == null) {
                                    double val = passiveKey.contains("overflow") ? 150.0 : (passiveKey.contains("fountain") ? 0.40 : 0.25);
                                    AttributeModifier.Operation op = passiveKey.contains("overflow") ? AttributeModifier.Operation.ADDITION : AttributeModifier.Operation.MULTIPLY_BASE;
                                    inst.addTransientModifier(new AttributeModifier(modId, "Custom Races " + passiveKey, val, op));
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }
}
