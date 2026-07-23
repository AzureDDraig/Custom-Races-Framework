package ddraig.net.customraces.integration;

import ddraig.net.customraces.data.RaceData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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

    public static boolean castNativeSpell(Player player, RaceData race, boolean isWereForm) {
        return castNativeSpell(player, race, isWereForm, 1);
    }

    /**
     * Casts a Native Spell (Slots 1-5) or triggers Wild Magic for the player.
     */
    public static boolean castNativeSpell(Player player, RaceData race, boolean isWereForm, int slot) {
        if (player == null || race == null) return false;
        if (slot < 1 || slot > 5) slot = 1;

        // Form Toggle Enforcement
        boolean enabled = isWereForm ? race.enableWereNativeSpells : race.enableNativeSpells;
        if (!enabled) {
            player.displayClientMessage(Component.literal("§cNative Spells are disabled for this race form!"), true);
            return false;
        }

        boolean isWildMagic = race.getWildMagic(slot, isWereForm);
        String spellId = race.getNativeSpellId(slot, isWereForm);
        int spellLevel = Math.max(1, Math.min(10, race.getNativeSpellLevel(slot, isWereForm)));

        if (isWildMagic) {
            spellId = ALL_SPELLS.get(RANDOM.nextInt(ALL_SPELLS.size()));
            player.displayClientMessage(Component.literal("§d✨ [Wild Magic] §fCasting random spell: §e" + spellId.replace("irons_spellbooks:", "").replace("totweaks:", "")), true);
        }

        if (spellId == null || spellId.trim().isEmpty() || spellId.equalsIgnoreCase("none")) return false;
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
                        player.displayClientMessage(Component.literal("§d✨ [Native Spell " + slot + "] §fCast " + spellId.replace("irons_spellbooks:", "").replace("totweaks:", "") + " (Lvl " + spellLevel + ")"), true);
                        if (player.level() instanceof net.minecraft.server.level.ServerLevel sLevel) {
                            sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.0, player.getZ(), 25, 0.4, 0.4, 0.4, 0.1);
                        }
                        return true;
                    }
                }
            } catch (Exception e) {
                System.err.println("[CustomRaces] Failed to cast Iron's Spell: " + spellId + " - " + e.getMessage());
            }
        }

        // Fallback feedback: Explicit diagnostic feedback
        if (!modLoaded) {
            player.displayClientMessage(Component.literal("§c[Native Spell " + slot + "] §f" + spellId + " §7(Requires Iron's Spells mod)"), true);
        } else {
            player.displayClientMessage(Component.literal("§c[Native Spell " + slot + "] §fCould not invoke spell: §e" + spellId + " §7(Verify spell ID format)"), true);
        }

        if (player.level() instanceof net.minecraft.server.level.ServerLevel sLevel) {
            sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 1.0, player.getZ(), 20, 0.4, 0.4, 0.4, 0.05);
            sLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.WITCH, player.getX(), player.getY() + 1.0, player.getZ(), 15, 0.3, 0.3, 0.3, 0.05);
        }

        return false;
    }

    private static Object resolveSpellObject(String spellId) {
        if (spellId == null || spellId.trim().isEmpty()) return null;
        spellId = spellId.trim();
        if (!spellId.contains(":")) {
            spellId = "irons_spellbooks:" + spellId;
        }

        net.minecraft.resources.ResourceLocation loc;
        try {
            loc = new net.minecraft.resources.ResourceLocation(spellId);
        } catch (net.minecraft.ResourceLocationException | IllegalArgumentException e) {
            System.err.println("[CustomRaces] Invalid spell ResourceLocation: '" + spellId + "' - " + e.getMessage());
            return null;
        }
        String pathOnly = loc.getPath();

        String[] registryClassNames = {
            "net.ironsspellbooks.api.registry.SpellRegistry",
            "net.ironsspellbooks.spells.SpellRegistry",
            "io.github.elytra.irons_spellbooks.api.registry.SpellRegistry",
            "com.io.github.elytra.irons_spellbooks.api.registry.SpellRegistry",
            "net.ironsspellbooks.api.spells.AbstractSpell",
            "io.github.elytra.irons_spellbooks.api.spells.AbstractSpell"
        };

        // 1. Direct getSpell / get static method lookups on registry classes
        for (String cName : registryClassNames) {
            try {
                Class<?> clazz = Class.forName(cName);

                // getSpell(ResourceLocation)
                try {
                    Method m = clazz.getMethod("getSpell", net.minecraft.resources.ResourceLocation.class);
                    m.setAccessible(true);
                    Object res = m.invoke(null, loc);
                    Object unwrapped = unwrapSpellHolder(res);
                    if (unwrapped != null) return unwrapped;
                } catch (NoSuchMethodException ignored) {} catch (Exception e) {
                    System.err.println("[CustomRaces] Error querying getSpell(ResourceLocation) on " + cName + ": " + e.getMessage());
                }

                // getSpell(String) with full spellId
                try {
                    Method m = clazz.getMethod("getSpell", String.class);
                    m.setAccessible(true);
                    Object res = m.invoke(null, spellId);
                    Object unwrapped = unwrapSpellHolder(res);
                    if (unwrapped != null) return unwrapped;
                } catch (NoSuchMethodException ignored) {} catch (Exception e) {
                    System.err.println("[CustomRaces] Error querying getSpell(String) on " + cName + ": " + e.getMessage());
                }

                // getSpell(String) with pathOnly
                try {
                    Method m = clazz.getMethod("getSpell", String.class);
                    m.setAccessible(true);
                    Object res = m.invoke(null, pathOnly);
                    Object unwrapped = unwrapSpellHolder(res);
                    if (unwrapped != null) return unwrapped;
                } catch (NoSuchMethodException ignored) {} catch (Exception e) {
                    System.err.println("[CustomRaces] Error querying getSpell(pathOnly) on " + cName + ": " + e.getMessage());
                }

                // get(ResourceLocation)
                try {
                    Method m = clazz.getMethod("get", net.minecraft.resources.ResourceLocation.class);
                    m.setAccessible(true);
                    Object res = m.invoke(null, loc);
                    Object unwrapped = unwrapSpellHolder(res);
                    if (unwrapped != null) return unwrapped;
                } catch (NoSuchMethodException ignored) {} catch (Exception ignored) {}

                // get(String)
                try {
                    Method m = clazz.getMethod("get", String.class);
                    m.setAccessible(true);
                    Object res = m.invoke(null, spellId);
                    Object unwrapped = unwrapSpellHolder(res);
                    if (unwrapped != null) return unwrapped;
                } catch (NoSuchMethodException ignored) {} catch (Exception ignored) {}

            } catch (ClassNotFoundException ignored) {}
        }

        // 2. Field lookups (REGISTRY, SPELL_REGISTRY, SPELLS) and getter methods on SpellRegistry classes
        for (String cName : registryClassNames) {
            try {
                Class<?> clazz = Class.forName(cName);
                String[] fieldNames = {"REGISTRY", "SPELL_REGISTRY", "SPELLS", "registry", "spellRegistry"};
                for (String fName : fieldNames) {
                    try {
                        Field field = clazz.getDeclaredField(fName);
                        field.setAccessible(true);
                        Object regObj = field.get(null);
                        if (regObj != null) {
                            Object spellFromReg = getSpellFromRegistryObject(regObj, loc, spellId, pathOnly);
                            Object unwrapped = unwrapSpellHolder(spellFromReg);
                            if (unwrapped != null) return unwrapped;
                        }
                    } catch (NoSuchFieldException ignored) {} catch (Exception ignored) {}
                }

                String[] methodNames = {"getRegistry", "getSpellRegistry", "getSpells"};
                for (String mName : methodNames) {
                    try {
                        Method m = clazz.getMethod(mName);
                        m.setAccessible(true);
                        Object regObj = m.invoke(null);
                        if (regObj != null) {
                            Object spellFromReg = getSpellFromRegistryObject(regObj, loc, spellId, pathOnly);
                            Object unwrapped = unwrapSpellHolder(spellFromReg);
                            if (unwrapped != null) return unwrapped;
                        }
                    } catch (NoSuchMethodException ignored) {} catch (Exception ignored) {}
                }
            } catch (ClassNotFoundException ignored) {}
        }

        // 3. Static constant field matching on SpellRegistry (e.g. FIREBOLT_SPELL, FIREBOLT)
        for (String cName : registryClassNames) {
            try {
                Class<?> clazz = Class.forName(cName);
                String normPath = pathOnly.toUpperCase(Locale.ROOT);
                String normPathSpell = normPath + "_SPELL";
                String normPathNoUnderscore = normPath.replace("_", "");
                String normPathNoUnderscoreSpell = normPathNoUnderscore + "_SPELL";

                for (Field field : clazz.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        String fNameUpper = field.getName().toUpperCase(Locale.ROOT);
                        if (fNameUpper.equals(normPath) || fNameUpper.equals(normPathSpell)
                                || fNameUpper.equals(normPathNoUnderscore) || fNameUpper.equals(normPathNoUnderscoreSpell)) {
                            try {
                                field.setAccessible(true);
                                Object val = field.get(null);
                                Object unwrapped = unwrapSpellHolder(val);
                                if (unwrapped != null) return unwrapped;
                            } catch (Exception ignored) {}
                        }
                    }
                }
            } catch (ClassNotFoundException ignored) {}
        }

        // 4. Fallback lookup in net.minecraft.core.Registry / BuiltInRegistries
        Object vanillaRegSpell = resolveFromVanillaRegistry(loc);
        if (vanillaRegSpell != null) {
            Object unwrapped = unwrapSpellHolder(vanillaRegSpell);
            if (unwrapped != null) return unwrapped;
        }

        return null;
    }

    private static Object getSpellFromRegistryObject(Object regObj, net.minecraft.resources.ResourceLocation loc, String spellId, String pathOnly) {
        if (regObj == null) return null;

        if (regObj instanceof java.util.function.Supplier<?> supplier) {
            try {
                regObj = supplier.get();
            } catch (Exception ignored) {}
        }

        if (regObj == null) return null;

        // Try get(ResourceLocation)
        try {
            Method m = regObj.getClass().getMethod("get", net.minecraft.resources.ResourceLocation.class);
            m.setAccessible(true);
            Object res = m.invoke(regObj, loc);
            if (res != null) return res;
        } catch (Exception ignored) {}

        // Try getValue(ResourceLocation) (Forge IForgeRegistry)
        try {
            Method m = regObj.getClass().getMethod("getValue", net.minecraft.resources.ResourceLocation.class);
            m.setAccessible(true);
            Object res = m.invoke(regObj, loc);
            if (res != null) return res;
        } catch (Exception ignored) {}

        // Try get(String)
        try {
            Method m = regObj.getClass().getMethod("get", String.class);
            m.setAccessible(true);
            Object res = m.invoke(regObj, spellId);
            if (res != null) return res;
        } catch (Exception ignored) {}

        // Try Map lookup
        if (regObj instanceof Map<?, ?> map) {
            if (map.containsKey(loc)) return map.get(loc);
            if (map.containsKey(spellId)) return map.get(spellId);
            if (map.containsKey(pathOnly)) return map.get(pathOnly);
        }

        return null;
    }

    private static Object resolveFromVanillaRegistry(net.minecraft.resources.ResourceLocation loc) {
        try {
            net.minecraft.core.Registry<?> rootRegistry = net.minecraft.core.registries.BuiltInRegistries.REGISTRY;
            if (rootRegistry != null) {
                net.minecraft.resources.ResourceLocation[] possibleRegistryIds = {
                    new net.minecraft.resources.ResourceLocation("irons_spellbooks", "spells"),
                    new net.minecraft.resources.ResourceLocation("irons_spellbooks", "spell"),
                    new net.minecraft.resources.ResourceLocation("irons_spellbooks:spells")
                };
                for (net.minecraft.resources.ResourceLocation regId : possibleRegistryIds) {
                    Object subRegObj = rootRegistry.get(regId);
                    if (subRegObj instanceof net.minecraft.core.Registry<?> subReg) {
                        Object res = subReg.get(loc);
                        if (res != null) return res;
                    }
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static Object unwrapSpellHolder(Object obj) {
        return unwrapSpellHolder(obj, 0);
    }

    private static Object unwrapSpellHolder(Object obj, int depth) {
        if (depth > 10 || obj == null) return null;

        String className = obj.getClass().getName();
        String strVal = obj.toString();
        if (className.contains("VoidSpell") || className.contains("NoneSpell")
                || strVal.equalsIgnoreCase("none")
                || strVal.toLowerCase(Locale.ROOT).contains("irons_spellbooks:none")
                || strVal.toLowerCase(Locale.ROOT).contains("spell.irons_spellbooks.none")) {
            return null;
        }

        // Check if obj itself is an AbstractSpell or spell object
        if (isAbstractSpell(obj)) {
            return obj;
        }

        // Check isPresent() / isEmpty()
        try {
            Method isPresentM = obj.getClass().getMethod("isPresent");
            isPresentM.setAccessible(true);
            Boolean isPresent = (Boolean) isPresentM.invoke(obj);
            if (Boolean.FALSE.equals(isPresent)) {
                return null;
            }
        } catch (Exception ignored) {}

        try {
            Method isEmptyM = obj.getClass().getMethod("isEmpty");
            isEmptyM.setAccessible(true);
            Boolean isEmpty = (Boolean) isEmptyM.invoke(obj);
            if (Boolean.TRUE.equals(isEmpty)) {
                return null;
            }
        } catch (Exception ignored) {}

        String[] getterNames = {"value", "get", "getSpell", "resolve"};
        for (String gName : getterNames) {
            try {
                Method gM = obj.getClass().getMethod(gName);
                gM.setAccessible(true);
                Object val = gM.invoke(obj);
                if (val != obj) {
                    if (val == null) {
                        return null;
                    }
                    return unwrapSpellHolder(val, depth + 1);
                }
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                return null;
            }
        }

        if (hasSpellCastMethods(obj)) {
            return obj;
        }

        return obj;
    }

    private static boolean isAbstractSpell(Object obj) {
        if (obj == null) return false;
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            String name = clazz.getName();
            if (name.endsWith("AbstractSpell") || name.contains("spells.AbstractSpell") || name.contains("api.spells.AbstractSpell")) {
                return true;
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    private static boolean hasSpellCastMethods(Object obj) {
        if (obj == null) return false;
        for (Method m : obj.getClass().getMethods()) {
            String name = m.getName();
            if (name.equalsIgnoreCase("onCast") || name.equalsIgnoreCase("castSpell") || name.equalsIgnoreCase("onCastSpell")) {
                return true;
            }
        }
        return false;
    }

    private static boolean invokeSpellCast(Player player, Object spellObj, int spellLevel) {
        if (spellObj == null || player == null) return false;
        Object castSource = getCastSourceEnum();
        Object magicData = getPlayerMagicData(player);

        if (magicData == null) {
            String[] classPaths = {
                "net.ironsspellbooks.api.magic.MagicData",
                "io.github.elytra.irons_spellbooks.api.magic.MagicData",
                "com.io.github.elytra.irons_spellbooks.api.magic.MagicData"
            };
            for (String cp : classPaths) {
                try {
                    Class<?> mDataClass = Class.forName(cp);
                    magicData = mDataClass.getDeclaredConstructor().newInstance();
                    break;
                } catch (Exception ignored) {}
            }
        }

        // Collect candidate methods matching exact names: onCast, castSpell, onCastSpell
        List<Method> candidates = new ArrayList<>();
        List<Method> allMethods = new ArrayList<>(Arrays.asList(spellObj.getClass().getMethods()));
        for (Method m : spellObj.getClass().getDeclaredMethods()) {
            if (!allMethods.contains(m)) {
                allMethods.add(m);
            }
        }

        for (Method m : allMethods) {
            String mName = m.getName();
            if (mName.equalsIgnoreCase("onCast") || mName.equalsIgnoreCase("castSpell") || mName.equalsIgnoreCase("onCastSpell")) {
                candidates.add(m);
            }
        }

        if (candidates.isEmpty()) {
            System.err.println("[CustomRaces] No onCast / castSpell / onCastSpell method found on spell object: " + spellObj.getClass().getName());
            return false;
        }

        final Object finalCastSource = castSource;
        final Object finalMagicData = magicData;

        // Sort candidates:
        // Tier 1: Target 5-parameter (Level, int, LivingEntity/ServerPlayer/Player, CastSource, MagicData)
        // Tier 2: Target 4-parameter (Level, int, LivingEntity/ServerPlayer/Player, MagicData)
        // Tier 3: Other strict parameter matches
        // Tier 4: Non-strict matches (penalized unmapped generic parameter overloads)
        candidates.sort((m1, m2) -> {
            int tier1 = getTier(m1, finalCastSource, finalMagicData);
            int tier2 = getTier(m2, finalCastSource, finalMagicData);
            if (tier1 != tier2) return Integer.compare(tier1, tier2);

            int nameScore1 = getNameScore(m1.getName());
            int nameScore2 = getNameScore(m2.getName());
            if (nameScore1 != nameScore2) return Integer.compare(nameScore1, nameScore2);

            if (m1.getParameterCount() != m2.getParameterCount()) {
                return Integer.compare(m2.getParameterCount(), m1.getParameterCount());
            }

            int unmapped1 = countUnmappedParameters(m1, finalCastSource, finalMagicData);
            int unmapped2 = countUnmappedParameters(m2, finalCastSource, finalMagicData);
            return Integer.compare(unmapped1, unmapped2);
        });

        for (Method m : candidates) {
            Class<?>[] pTypes = m.getParameterTypes();
            Object[] args = new Object[pTypes.length];

            for (int i = 0; i < pTypes.length; i++) {
                Class<?> p = pTypes[i];
                if (net.minecraft.world.level.Level.class.isAssignableFrom(p)) {
                    args[i] = player.level();
                } else if (p == int.class || p == Integer.class) {
                    args[i] = spellLevel;
                } else if (net.minecraft.world.entity.player.Player.class.isAssignableFrom(p)
                        || net.minecraft.server.level.ServerPlayer.class.isAssignableFrom(p)
                        || net.minecraft.world.entity.LivingEntity.class.isAssignableFrom(p)
                        || net.minecraft.world.entity.Entity.class.isAssignableFrom(p)) {
                    args[i] = player;
                } else if (isCastSourceType(p, finalCastSource)) {
                    args[i] = resolveCastSourceForParam(p, finalCastSource);
                } else if (isMagicDataType(p, finalMagicData)) {
                    args[i] = finalMagicData;
                } else {
                    args[i] = null;
                }

                if (args[i] == null && p.isPrimitive()) {
                    args[i] = getPrimitiveDefault(p);
                }
            }

            try {
                m.setAccessible(true);
                m.invoke(spellObj, args);
                return true;
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() != null ? e.getCause() : e;
                System.err.println("[CustomRaces] InvocationTargetException executing " + m.getName() + " on spell " + spellObj.getClass().getName() + ": " + cause.getMessage());
                cause.printStackTrace();
            } catch (IllegalAccessException e) {
                System.err.println("[CustomRaces] IllegalAccessException executing " + m.getName() + " on spell " + spellObj.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("[CustomRaces] Exception executing " + m.getName() + " on spell " + spellObj.getClass().getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }

        return false;
    }

    private static Object getPrimitiveDefault(Class<?> p) {
        if (p == boolean.class) return false;
        if (p == int.class) return 0;
        if (p == float.class) return 0.0f;
        if (p == double.class) return 0.0;
        if (p == long.class) return 0L;
        if (p == short.class) return (short) 0;
        if (p == byte.class) return (byte) 0;
        if (p == char.class) return '\0';
        return null;
    }

    private static int getNameScore(String name) {
        if (name.equalsIgnoreCase("onCast")) return 1;
        if (name.equalsIgnoreCase("castSpell")) return 2;
        if (name.equalsIgnoreCase("onCastSpell")) return 3;
        return 4;
    }

    private static int getTier(Method m, Object castSource, Object magicData) {
        boolean strict = isStrictParameterMatch(m, castSource, magicData);
        if (strict) {
            if (isTarget5Param(m, castSource, magicData)) return 1;
            if (isTarget4Param(m, castSource, magicData)) return 2;
            return 3;
        }
        return 4;
    }

    private static boolean isTarget5Param(Method m, Object castSource, Object magicData) {
        if (m.getParameterCount() != 5) return false;
        Class<?>[] pTypes = m.getParameterTypes();
        boolean hasLevel = false, hasInt = false, hasEntity = false, hasSource = false, hasMagic = false;
        for (Class<?> p : pTypes) {
            if (net.minecraft.world.level.Level.class.isAssignableFrom(p)) hasLevel = true;
            else if (p == int.class || p == Integer.class) hasInt = true;
            else if (net.minecraft.world.entity.LivingEntity.class.isAssignableFrom(p)
                    || net.minecraft.world.entity.player.Player.class.isAssignableFrom(p)
                    || net.minecraft.server.level.ServerPlayer.class.isAssignableFrom(p)
                    || net.minecraft.world.entity.Entity.class.isAssignableFrom(p)) hasEntity = true;
            else if (isCastSourceType(p, castSource)) hasSource = true;
            else if (isMagicDataType(p, magicData)) hasMagic = true;
        }
        return hasLevel && hasInt && hasEntity && hasSource && hasMagic;
    }

    private static boolean isTarget4Param(Method m, Object castSource, Object magicData) {
        if (m.getParameterCount() != 4) return false;
        Class<?>[] pTypes = m.getParameterTypes();
        boolean hasLevel = false, hasInt = false, hasEntity = false, hasMagic = false;
        for (Class<?> p : pTypes) {
            if (net.minecraft.world.level.Level.class.isAssignableFrom(p)) hasLevel = true;
            else if (p == int.class || p == Integer.class) hasInt = true;
            else if (net.minecraft.world.entity.LivingEntity.class.isAssignableFrom(p)
                    || net.minecraft.world.entity.player.Player.class.isAssignableFrom(p)
                    || net.minecraft.server.level.ServerPlayer.class.isAssignableFrom(p)
                    || net.minecraft.world.entity.Entity.class.isAssignableFrom(p)) hasEntity = true;
            else if (isMagicDataType(p, magicData)) hasMagic = true;
        }
        return hasLevel && hasInt && hasEntity && hasMagic;
    }

    private static boolean isStrictParameterMatch(Method m, Object castSource, Object magicData) {
        return countUnmappedParameters(m, castSource, magicData) == 0;
    }

    private static int countUnmappedParameters(Method m, Object castSource, Object magicData) {
        int count = 0;
        for (Class<?> p : m.getParameterTypes()) {
            if (net.minecraft.world.level.Level.class.isAssignableFrom(p)) continue;
            if (p == int.class || p == Integer.class) continue;
            if (net.minecraft.world.entity.player.Player.class.isAssignableFrom(p)
                    || net.minecraft.server.level.ServerPlayer.class.isAssignableFrom(p)
                    || net.minecraft.world.entity.LivingEntity.class.isAssignableFrom(p)
                    || net.minecraft.world.entity.Entity.class.isAssignableFrom(p)) continue;
            if (isCastSourceType(p, castSource)) continue;
            if (isMagicDataType(p, magicData)) continue;
            count++;
        }
        return count;
    }

    private static boolean isCastSourceType(Class<?> p, Object castSource) {
        if (p == Object.class || p == Enum.class || p == Comparable.class || p == java.io.Serializable.class) {
            return false;
        }
        if (castSource != null && p.isAssignableFrom(castSource.getClass())) {
            return true;
        }
        String pName = p.getName();
        String simpleName = p.getSimpleName();
        if (simpleName.equals("CastSource") || pName.endsWith(".CastSource")) {
            return true;
        }
        if (p.isEnum() && pName.contains("CastSource")) {
            return true;
        }
        return false;
    }

    private static Object resolveCastSourceForParam(Class<?> p, Object castSource) {
        if (castSource != null && p.isAssignableFrom(castSource.getClass())) {
            return castSource;
        }
        if (p.isEnum()) {
            try {
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object val = Enum.valueOf((Class<Enum>) p, "SPELLBOOK");
                return val;
            } catch (Exception e1) {
                try {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Object val = Enum.valueOf((Class<Enum>) p, "INNATE");
                    return val;
                } catch (Exception e2) {
                    Object[] constants = p.getEnumConstants();
                    if (constants != null && constants.length > 0) {
                        return constants[0];
                    }
                }
            }
        }
        return castSource;
    }

    private static boolean isMagicDataType(Class<?> p, Object magicData) {
        if (p == Object.class || p == Enum.class || p == Comparable.class || p == java.io.Serializable.class) {
            return false;
        }
        if (magicData != null && p.isAssignableFrom(magicData.getClass())) {
            return true;
        }
        String pName = p.getName();
        String simpleName = p.getSimpleName();
        return simpleName.equals("MagicData") || pName.endsWith(".MagicData") || pName.contains("MagicData");
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
            "io.github.elytra.irons_spellbooks.api.magic.MagicData",
            "com.io.github.elytra.irons_spellbooks.api.magic.MagicData"
        };
        for (String cp : classPaths) {
            try {
                Class<?> clazz = Class.forName(cp);
                Method getMethod = clazz.getMethod("getPlayerMagicData", Player.class);
                getMethod.setAccessible(true);
                return getMethod.invoke(null, player);
            } catch (Exception e1) {
                try {
                    Class<?> clazz = Class.forName(cp);
                    Method getMethod = clazz.getMethod("get", Player.class);
                    getMethod.setAccessible(true);
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
