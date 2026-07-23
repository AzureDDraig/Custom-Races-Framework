package ddraig.net.customraces.integration;

import ddraig.net.customraces.data.RaceData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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

    /**
     * Casts a Native Spell or triggers Wild Magic for the player.
     */
    public static void castNativeSpell(Player player, RaceData race, boolean isWereForm) {
        if (player == null || race == null) return;

        boolean isWildMagic = isWereForm ? race.wereWildMagic : race.wildMagic;
        String spellId = isWereForm ? race.wereNativeSpellId : race.nativeSpellId;
        int spellLevel = isWereForm ? race.wereNativeSpellLevel : race.nativeSpellLevel;

        if (isWildMagic) {
            spellId = ALL_SPELLS.get(RANDOM.nextInt(ALL_SPELLS.size()));
            player.sendSystemMessage(Component.literal("§d✨ [Wild Magic] §fCasting random spell: §e" + spellId.replace("irons_spellbooks:", "").replace("totweaks:", "")));
        }

        try {
            Class<?> spellRegistryClass = null;
            try {
                spellRegistryClass = Class.forName("io.github.elytra.irons_spellbooks.api.registry.SpellRegistry");
            } catch (ClassNotFoundException e) {
                try {
                    spellRegistryClass = Class.forName("com.io.github.elytra.irons_spellbooks.api.registry.SpellRegistry");
                } catch (Exception ignored) {}
            }

            Object spellObj = null;
            if (spellRegistryClass != null) {
                try {
                    Method getSpellStr = spellRegistryClass.getMethod("getSpell", String.class);
                    spellObj = getSpellStr.invoke(null, spellId);
                } catch (Exception e1) {
                    try {
                        Method getSpellRes = spellRegistryClass.getMethod("getSpell", net.minecraft.resources.ResourceLocation.class);
                        spellObj = getSpellRes.invoke(null, new net.minecraft.resources.ResourceLocation(spellId));
                    } catch (Exception ignored) {}
                }
            }

            if (spellObj != null) {
                Object castSource = getCastSourceEnum();
                for (Method m : spellObj.getClass().getMethods()) {
                    if (m.getName().toLowerCase().contains("cast")) {
                        try {
                            m.setAccessible(true);
                            Class<?>[] params = m.getParameterTypes();
                            if (params.length == 4) {
                                m.invoke(spellObj, player.level(), spellLevel, player, castSource);
                                return;
                            } else if (params.length == 3) {
                                m.invoke(spellObj, player.level(), spellLevel, player);
                                return;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
        } catch (Exception ignored) {}

        // Vanilla Fallback FX when Iron's Spells is not installed
        if (!isWildMagic) {
            player.sendSystemMessage(Component.literal("§c[Native Spell] §f" + spellId + " (Requires Iron's Spells mod to render true spell FX)"));
        }
        player.level().addParticle(net.minecraft.core.particles.ParticleTypes.DRAGON_BREATH, player.getX(), player.getY() + 1.0, player.getZ(), 0.1, 0.1, 0.1);
    }

    private static Class<?> CastSourceClass() throws ClassNotFoundException {
        try {
            return Class.forName("io.github.elytra.irons_spellbooks.api.spells.CastSource");
        } catch (ClassNotFoundException e) {
            return Class.forName("com.io.github.elytra.irons_spellbooks.api.spells.CastSource");
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object getCastSourceEnum() {
        try {
            Class clazz = CastSourceClass();
            return Enum.valueOf(clazz, "SPELLBOOK");
        } catch (Exception e) {
            return null;
        }
    }
}
