package ddraig.net.customraces.client.gui;

import ddraig.net.customraces.data.MobAllianceData;
import ddraig.net.customraces.data.PartTransformData;
import ddraig.net.customraces.data.ParticleAuraData;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import ddraig.net.customraces.network.ModPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin Creator GUI matching CMobs Framework editor layout with 8 tabs and full tooltips.
 */
public class RaceCreatorScreen extends Screen {

    private int activeTab = 0; // 0: Basics, 1: Model, 2: Positions, 3: Passives, 4: Actives, 5: Sounds, 6: Advanced, 7: Alliances
    private RaceData workingRace;
    private int sidebarScrollOffset = 0;

    // GUI Edit Controls
    private EditBox nameBox;
    private EditBox nameColorBox;
    private EditBox customTextureBox;
    private EditBox loreBox;
    private EditBox difficultyBox;
    private EditBox iconBox;

    private Checkbox hideHelmetBox;
    private Checkbox hideChestplateBox;
    private Checkbox hideLeggingsBox;
    private Checkbox hideBootsBox;

    private EditBox heightScaleBox;
    private EditBox widthScaleBox;
    private EditBox healthBox;
    private EditBox speedBox;

    private EditBox ambientSoundBox;
    private EditBox hurtSoundBox;
    private EditBox deathSoundBox;

    private EditBox spawnDimensionBox;
    private EditBox spawnBiomeBox;
    private Checkbox enableAlliancesBox;

    // Were-Race Controls
    private Checkbox enableWereBox;
    private EditBox wereConditionBox;
    private EditBox wereModelBox;
    private EditBox wereTextureBox;
    private EditBox wereAnimFileBox;
    private EditBox wereIdleAnimBox;
    private EditBox wereWalkAnimBox;
    private EditBox wereAttackAnimBox;
    private EditBox wereFlyAnimBox;
    private EditBox wereSwimAnimBox;
    private Checkbox isWereFlyingRaceBox;

    private EditBox wereTransformSoundBox;
    private EditBox wereHowlSoundBox;
    private EditBox wereAmbientSoundBox;
    private EditBox wereHurtSoundBox;
    private EditBox wereDeathSoundBox;
    private EditBox wereDamageBox;

    // Flight & Swim Controls for Base Model
    private Checkbox isFlyingRaceBox;
    private EditBox flyAnimBox;
    private EditBox swimAnimBox;

    // Form Mode Toggle (Base Form vs Were-Form)
    private boolean editingWereForm = false;

    // Search Query State for Passives, Actives, and Drawbacks
    private String searchPassivesQuery = "";
    private String searchActivesQuery = "";
    private String searchDrawbacksQuery = "";

    // Native Spell Controls
    private EditBox nativeSpellBox;

    // Passive & Drawback Widget Cache for Lag-Free Scroll
    private final List<Checkbox> passiveWidgets = new java.util.ArrayList<>();
    private final List<Checkbox> drawbackWidgets = new java.util.ArrayList<>();

    // Native Spells Sub-Slot Selector State
    private int selectedNativeSpellSlot = 1;

    private void setRaceNativeSpell(int slot, boolean isWere, String spellId) {
        if (isWere) {
            switch (slot) {
                case 2: workingRace.wereNativeSpellId2 = spellId; break;
                case 3: workingRace.wereNativeSpellId3 = spellId; break;
                case 4: workingRace.wereNativeSpellId4 = spellId; break;
                case 5: workingRace.wereNativeSpellId5 = spellId; break;
                default: workingRace.wereNativeSpellId1 = spellId; workingRace.wereNativeSpellId = spellId; break;
            }
        } else {
            switch (slot) {
                case 2: workingRace.nativeSpellId2 = spellId; break;
                case 3: workingRace.nativeSpellId3 = spellId; break;
                case 4: workingRace.nativeSpellId4 = spellId; break;
                case 5: workingRace.nativeSpellId5 = spellId; break;
                default: workingRace.nativeSpellId1 = spellId; workingRace.nativeSpellId = spellId; break;
            }
        }
    }

    private void setRaceWildMagic(int slot, boolean isWere, boolean val) {
        if (isWere) {
            switch (slot) {
                case 2: workingRace.wereWildMagic2 = val; break;
                case 3: workingRace.wereWildMagic3 = val; break;
                case 4: workingRace.wereWildMagic4 = val; break;
                case 5: workingRace.wereWildMagic5 = val; break;
                default: workingRace.wereWildMagic1 = val; workingRace.wereWildMagic = val; break;
            }
        } else {
            switch (slot) {
                case 2: workingRace.wildMagic2 = val; break;
                case 3: workingRace.wildMagic3 = val; break;
                case 4: workingRace.wildMagic4 = val; break;
                case 5: workingRace.wildMagic5 = val; break;
                default: workingRace.wildMagic1 = val; workingRace.wildMagic = val; break;
            }
        }
    }

    private void setRaceNativeSpellLevel(int slot, boolean isWere, int level) {
        if (isWere) {
            switch (slot) {
                case 2: workingRace.wereNativeSpellLevel2 = level; break;
                case 3: workingRace.wereNativeSpellLevel3 = level; break;
                case 4: workingRace.wereNativeSpellLevel4 = level; break;
                case 5: workingRace.wereNativeSpellLevel5 = level; break;
                default: workingRace.wereNativeSpellLevel1 = level; workingRace.wereNativeSpellLevel = level; break;
            }
        } else {
            switch (slot) {
                case 2: workingRace.nativeSpellLevel2 = level; break;
                case 3: workingRace.nativeSpellLevel3 = level; break;
                case 4: workingRace.nativeSpellLevel4 = level; break;
                case 5: workingRace.nativeSpellLevel5 = level; break;
                default: workingRace.nativeSpellLevel1 = level; workingRace.nativeSpellLevel = level; break;
            }
        }
    }

    // Passives Scrollbar & Single Column State
    private double passivesScrollAmount = 0;
    private boolean isDraggingPassivesScrollbar = false;
    private int matchingPassivesCount = 0;

    // Drawbacks Scrollbar & Single Column State
    private double drawbacksScrollAmount = 0;
    private boolean isDraggingDrawbacksScrollbar = false;
    private int matchingDrawbacksCount = 0;

    private int getContentLeft() {
        return 156;
    }

    private int getContentTop() {
        return 54;
    }

    private String getPassiveDescription(String passive) {
        switch (passive.toLowerCase()) {
            case "night_vision": return "Grants clear vision in pitch black darkness.";
            case "water_breathing": return "Allows underwater breathing indefinitely.";
            case "fire_resistance": return "Provides complete immunity to fire and lava.";
            case "flight": return "Unlocks creative-style flying capability.";
            case "slow_falling": return "Prevents fall damage and grants gentle gliding.";
            case "regeneration": return "Constantly restores player health over time.";
            case "wither_immunity": return "Immunity to Wither decay and damage.";
            case "fall_damage_immunity": return "Immunity to all impact and fall damage.";
            case "lava_swimming": return "Allows rapid swimming through lava.";
            case "climbing": return "Allows climbing vertical wall surfaces.";
            case "native_spell": return "Casts native spell or Wild Magic. Requires Iron's Spells mod.";
            default: return passive.replace("_", " ");
        }
    }

    private String getDrawbackDescription(String drawbackId) {
        String name = drawbackId.replace("_", " ").toLowerCase();
        return name + " weakness drawback penalty restriction curse vulnerability damage slowness debuff intolerance inability";
    }

    public static final List<String> ALL_PASSIVES = java.util.List.of(
        // 1-10: Elemental & Environmental
        "night_vision", "water_breathing", "fire_resistance", "flight", "slow_falling",
        "lava_swimming", "climbing", "frost_immunity", "lightning_immunity", "poison_immunity",
        // 11-20: Defense & Resilience
        "regeneration", "wither_immunity", "fall_damage_immunity", "arrow_deflection", "explosion_resistance",
        "magic_resistance", "knockback_immunity", "thorns_skin", "shield_mastery", "unbreakable_will",
        // 21-30: Mobility & Movement
        "speed_boost", "high_jump", "web_walking", "soul_speed", "step_assist",
        "wall_run", "dolphin_grace", "feather_weight", "shadow_dash_passive", "void_floating",
        // 31-40: Combat & Damage
        "lifesteal", "critical_strike_boost", "berserk_rage", "backstab_bonus", "giant_slayer",
        "armor_piercing", "execute_passive", "bleed_on_hit", "counter_attack", "dual_wield_mastery",
        // 41-50: Utility & Gathering
        "auto_smelt", "double_mining_drops", "magnet_aura", "luck_of_the_sea", "haste_passive",
        "night_miner", "silk_touch_hands", "xp_boost", "hunger_less_drain", "saturation_regen",
        // 51-60: Magic & Spectral
        "mana_regen_boost", "spell_power_boost", "cooldown_reduction", "arcane_shield", "astral_projection",
        "spectral_glowing", "invisibility_in_shadows", "telepathic_aura", "elemental_affinity", "native_spell",
        // 61-70: Vampiric & Nether
        "vampiric_bite_regen", "sunlight_evasion", "nether_affinity", "wither_touch", "shadow_healing",
        "soul_collector", "blood_essence_pool", "demon_flame_aura", "hellfire_immunity", "abyssal_resilience",
        // 71-80: Celestial & Divine
        "divine_aura", "angel_wings_passive", "holy_damage_boost", "undead_bane_aura", "solar_charging",
        "lunar_power_boost", "radiant_light", "blessing_of_protection", "grace_of_the_gods", "sanctuary_field",
        // 81-90: Draconic & Beast
        "dragon_scales", "beast_instincts", "pack_leader_buff", "natural_armor", "scent_tracking",
        "intimidating_presence", "tail_sweep_passive", "predator_stealth", "thick_hide", "wild_regeneration",
        // 91-100: Tech & Golem
        "nanite_repair", "kinetic_absorption", "thermal_regulation", "cybernetic_sight", "forcefield_barrier",
        "overclock_speed", "golem_density", "magnetic_repulsion", "radiation_immunity", "energy_core_boost"
    );

    public static final List<String> ALL_ACTIVES = java.util.List.of(
        // Native Spells (Iron's Spells Integration)
        "native_spell_1", "native_spell_2", "native_spell_3", "native_spell_4", "native_spell_5",
        // 1-10: Fire & Magma
        "flame_breath", "fireball_burst", "inferno_ring", "magma_slam", "meteor_strike",
        "heat_wave", "combustion_aura", "flame_charge", "pyroblast", "volcanic_eruption",
        // 11-20: Ice & Frost
        "frost_nova", "ice_lance", "blizzard_storm", "frost_dash", "deep_freeze",
        "glacier_wall", "icicle_barrage", "frozen_shield", "absolute_zero", "snowstorm_burst",
        // 21-30: Lightning & Storm
        "lightning_strike", "chain_lightning", "thunder_clap", "storm_dash", "overcharge_buff",
        "plasma_beam", "ball_lightning", "static_field", "lightning_spear", "sky_bolt",
        // 31-40: Shadow & Ender
        "shadow_step", "teleport_dash", "black_hole_pull", "shadow_clone", "void_slash",
        "veil_of_shadows", "abyssal_grip", "dimensional_rift", "blink_teleport", "nightmare_burst",
        // 41-50: Holy & Light
        "healing_wave", "divine_smite", "radiant_beam", "holy_shield", "sanctuary_heal",
        "blessing_buff", "purifying_blast", "solar_beam", "angelic_flight_burst", "heavenly_resurrection",
        // 51-60: Blood & Dark Magic
        "blood_slash", "vampiric_drain", "dark_pulse", "wither_blast", "curse_aura",
        "soul_reap", "corruption_wave", "blood_shield", "plague_cloud", "necromancy_summon",
        // 61-70: Earth & Nature
        "earthquake_slam", "boulder_toss", "root_entrapment", "poison_spit", "vine_whip",
        "thorn_barrage", "nature_heal", "rock_armor_buff", "mud_slide", "seismic_wave",
        // 71-80: Wind & Kinetic
        "gale_blast", "cyclone_vortex", "wind_dash", "sonic_boom", "shockwave_slam",
        "air_slash", "repulsion_field", "tornado_burst", "vacuum_pull", "kinetic_blast",
        // 81-90: Beast & Transformation
        "dragon_roar", "transform_were", "summon_minions", "beast_leap", "feral_frenzy",
        "howl_buff", "claw_slash", "pack_call", "predator_pounce", "primal_rage",
        // 91-100: Special & Tech
        "native_spell", "laser_beam", "emp_blast", "gravity_flip", "time_stop_pulse",
        "shield_overload", "orbital_strike", "nano_heal", "overdrive_buff", "singularity_bomb"
    );

    public static final List<String> ALL_DRAWBACKS = java.util.List.of(
        // 1-10: Environmental & Elemental
        "water_vulnerability", "sunlight_burn", "sunlight_slowness", "cold_vulnerability", "fire_vulnerability",
        "hydrophobia", "claustrophobia", "agoraphobia", "nether_vulnerability", "end_vulnerability",
        // 11-20: Diet & Metabolism
        "carnivore_diet", "vegetarian_diet", "hyper_metabolism", "sluggish_metabolism", "hematophagy",
        "photosynthetic_dependency", "golden_allergy", "potion_intolerance", "soul_hunger", "heavy_eater",
        // 21-30: Combat & Armor
        "fragile_bone", "no_heavy_armor", "no_shield_use", "melee_weakness", "ranged_inaccuracy",
        "silver_vulnerability", "smite_vulnerability", "knockback_vulnerability", "slow_attack_cooldown", "shield_shatter_vulnerability",
        // 31-40: Movement & Physics
        "slowness_curse", "no_sprinting", "reduced_step_height", "slippery_feet", "gravity_bound",
        "jump_penalty", "stamina_exhaustion", "cobweb_entanglement", "clumsy_swimmer", "steep_fall_paralysis",
        // 41-50: Hitbox & Dimensions
        "oversized_hitbox", "undersized_reach", "low_max_health", "glass_cannon", "no_helmet_slot",
        "no_chestplate_slot", "no_boots_slot", "translucent_fragility", "heavy_weight", "drowning_in_shallow_water",
        // 51-60: Arcane & Factions
        "villager_fear", "iron_golem_hostility", "curse_of_shadows", "mana_drain", "totem_nullification",
        "wither_vulnerability", "poison_vulnerability", "insomnia_curse", "lightning_attraction", "blindness_in_nether",
        // 61-70: Extreme Biome & Climate
        "desert_dehydration", "snow_hypothermia", "swamp_miasma", "cave_suffocation", "high_altitude_sickness",
        "ocean_pressure", "jungle_fever", "mushrooms_allergy", "dimension_shock", "weather_depression",
        // 71-80: Equipment & Material
        "no_iron_equipment", "no_diamond_equipment", "no_bow_use", "no_trident_use", "no_elytra_equip",
        "heavy_armor_slowness", "gold_tool_brittleness", "wooden_tool_fragility", "enchantment_rejection", "single_weapon_mastery",
        // 81-90: Advanced Combat
        "vampiric_burn_on_hit", "blunt_damage_vulnerability", "piercing_vulnerability", "slashing_vulnerability", "recoil_damage",
        "bleed_vulnerability", "critical_vulnerability", "shield_break_paralysis", "no_critical_hits", "sweep_attack_weakness",
        // 91-100: Chemical & Status
        "milk_allergy", "extended_debuffs", "reduced_buff_duration", "splash_potion_fragility", "honey_stickiness",
        "sweet_berries_thorns", "fireworks_vulnerability", "anoxia", "sugar_crash", "rotten_flesh_sickness",
        // 101-110: Mob Hostility
        "creeper_attraction", "skeleton_precision_vulnerability", "enderman_aggro", "wolf_hostility", "piglin_permanent_aggro",
        "warden_sonic_vulnerability", "phantom_attraction", "spider_venom_vulnerability", "blaze_fireball_vulnerability", "stray_slowness_vulnerability",
        // 111-120: Mobility & Mounts
        "climb_inability", "boat_inability", "minecart_inability", "horse_mount_inability", "strider_mount_inability",
        "fall_stun", "sprint_jump_exhaustion", "crouch_slowness", "water_current_vulnerability", "lava_sink",
        // 121-130: Sensory & Perception
        "blind_in_darkness", "tunnel_vision", "sound_sensitivity", "echolocation_dependency", "hallucinations",
        "glowing_curse", "paranoia", "vertigo", "color_blindness", "bloodlust_frenzy",
        // 131-140: Magic & Soul
        "wither_rose_curse", "beacon_rejection", "conduit_rejection", "xp_inefficiency", "enchantment_cost_doubled",
        "curse_of_vanishing_gear", "respawn_weakness", "portal_sickness", "totem_shatter", "ender_pearl_damage",
        // 141-150: Morph & Body Shape
        "no_offhand_slot", "tail_tangled", "heavy_horns", "wing_flapping_exhaustion", "web_producer_hunger",
        "shell_weight", "slime_splitting_vulnerability", "centaur_stair_clumsiness", "golem_rust", "dragon_greed"
    );

    // Minion Ability Controls
    private EditBox minionMobTypeBox;
    private EditBox minionCountBox;
    private EditBox minionScaleBox;
    private Checkbox minionIsRangedBox;
    private EditBox minionProjectileBox;

    // Left Sidebar Race Selection List & Filter
    private EditBox raceSearchBox;

    // Auto-Complete Suggestions Overlay Fields
    private EditBox activeField = null;
    private List<String> activeSuggestions = new ArrayList<>();
    private int suggestionsScrollOffset = 0;
    private boolean showSuggestions = false;

    public RaceCreatorScreen(RaceData race) {
        super(Component.literal("Race Creator Admin"));
        if (race != null) {
            this.workingRace = race;
        } else if (!RaceRegistry.loadedRaces.isEmpty()) {
            RaceData selected = RaceRegistry.loadedRaces.values().iterator().next();
            if (this.minecraft != null && this.minecraft.player != null) {
                RaceData playerRace = RaceRegistry.getPlayerRace(this.minecraft.player.getUUID());
                if (playerRace != null) selected = playerRace;
            }
            this.workingRace = selected;
        } else {
            this.workingRace = new RaceData("race_template", "Template Race");
        }
    }

    private void autoSaveWorkingRace() {
        if (workingRace == null) return;
        readFormInputs();
        RaceRegistry.loadedRaces.put(workingRace.id, workingRace);
        ModPackets.sendSaveRace(workingRace);
    }

    private void resetFormFields() {
        nameBox = null; nameColorBox = null; difficultyBox = null; loreBox = null;
        iconBox = null; customTextureBox = null; heightScaleBox = null; widthScaleBox = null;
        healthBox = null; speedBox = null; wereDamageBox = null; ambientSoundBox = null;
        hurtSoundBox = null; deathSoundBox = null; spawnDimensionBox = null; spawnBiomeBox = null;
        minionMobTypeBox = null; minionCountBox = null; minionScaleBox = null; minionProjectileBox = null;
        wereConditionBox = null; wereModelBox = null; wereTextureBox = null; wereAnimFileBox = null;
        wereIdleAnimBox = null; wereWalkAnimBox = null; wereAttackAnimBox = null;
        wereTransformSoundBox = null; wereHowlSoundBox = null; wereAmbientSoundBox = null;
        wereHurtSoundBox = null; wereDeathSoundBox = null; nativeSpellBox = null;
        passiveWidgets.clear(); drawbackWidgets.clear();
    }

    @Override
    public void onClose() {
        autoSaveWorkingRace();
        super.onClose();
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();
        RaceRegistry.rebuildSuggestionsCache();

        int panelX = 8;
        int panelY = 8;
        int panelWidth = 140;
        int panelHeight = this.height - 16;

        // 1. Left Sidebar: Race Filter & Search EditBox
        String prevFilter = this.raceSearchBox != null ? this.raceSearchBox.getValue() : "";
        this.raceSearchBox = new EditBox(this.font, panelX + 5, panelY + 16, 130, 16, Component.literal("Search..."));
        this.raceSearchBox.setMaxLength(2048);
        this.raceSearchBox.setValue(prevFilter);
        this.raceSearchBox.setTooltip(Tooltip.create(Component.literal("Filter loaded races by name or ID")));
        this.raceSearchBox.setResponder(filter -> this.init());
        this.addRenderableWidget(this.raceSearchBox);

        // 2. Left Sidebar: Scrollable Race Buttons List (Custom RPG Flat Buttons)
        int listTop = panelY + 36;
        int listBottom = panelY + panelHeight - 48;

        String currentFilter = this.raceSearchBox.getValue().toLowerCase().trim();
        List<RaceData> matchingRaces = RaceRegistry.loadedRaces.values().stream()
            .filter(r -> currentFilter.isEmpty() || r.name.toLowerCase().contains(currentFilter) || r.id.toLowerCase().contains(currentFilter))
            .collect(Collectors.toList());

        int totalHeight = matchingRaces.size() * 20;
        int maxScroll = Math.max(0, totalHeight - (listBottom - listTop));
        if (sidebarScrollOffset > maxScroll) sidebarScrollOffset = maxScroll;
        if (sidebarScrollOffset < 0) sidebarScrollOffset = 0;

        int btnY = listTop - sidebarScrollOffset;

        for (RaceData r : matchingRaces) {
            if (btnY + 18 >= listTop && btnY <= listBottom - 18) {
                boolean selected = r.id.equalsIgnoreCase(workingRace.id);
                String label = (selected ? "▶ " : "") + r.name;

                FlatButton raceBtn = new FlatButton(panelX + 5, btnY, 130, 18, Component.literal(label), b -> {
                    autoSaveWorkingRace();
                    resetFormFields();
                    this.workingRace = r;
                    this.init();
                }, selected ? 0xFFFF3838 : 0xFF00CEC9, 0xFF7B61FF);

                raceBtn.setTooltip(Tooltip.create(Component.literal("ID: " + r.id + "\nClick to edit race properties.")));
                if (selected) raceBtn.active = false;
                this.addRenderableWidget(raceBtn);
            }
            btnY += 20;
        }

        // 3. Left Sidebar Bottom Management Buttons: Add, Del, Duplicate
        int mgmtY = panelY + panelHeight - 44;

        FlatButton addBtn = new FlatButton(panelX + 5, mgmtY, 62, 18, Component.literal("§a+ Add"), b -> {
            readFormInputs();
            resetFormFields();
            String newId = "race_" + (System.currentTimeMillis() % 10000);
            RaceData newRace = new RaceData(newId, "New Race");
            RaceRegistry.loadedRaces.put(newId, newRace);
            ModPackets.sendSaveRace(newRace);
            this.workingRace = newRace;
            this.init();
        }, 0xFF55FF55, 0xFF55FFFF);
        addBtn.setTooltip(Tooltip.create(Component.literal("Create a new custom race template.")));
        this.addRenderableWidget(addBtn);

        FlatButton deleteBtn = new FlatButton(panelX + 73, mgmtY, 62, 18, Component.literal("§c🗑 Del"), b -> {
            ModPackets.sendDeleteRace(workingRace.id);
            RaceRegistry.loadedRaces.remove(workingRace.id);
            RaceRegistry.playerRaces.entrySet().removeIf(e -> e.getValue().equalsIgnoreCase(workingRace.id));
            resetFormFields();
            if (!RaceRegistry.loadedRaces.isEmpty()) {
                this.workingRace = RaceRegistry.loadedRaces.values().iterator().next();
            } else {
                this.workingRace = new RaceData("new_race", "New Race");
                RaceRegistry.loadedRaces.put(this.workingRace.id, this.workingRace);
            }
            this.init();
        }, 0xFFFF5555, 0xFFFFAA00);
        deleteBtn.setTooltip(Tooltip.create(Component.literal("Delete the currently selected race.")));
        this.addRenderableWidget(deleteBtn);

        // Large Duplicate Button spanning across under Add & Del
        FlatButton duplicateBtn = new FlatButton(panelX + 5, mgmtY + 22, 130, 20, Component.literal("§e📋 Duplicate"), b -> {
            readFormInputs();
            resetFormFields();
            RaceData copy = duplicateRace(workingRace);
            RaceRegistry.loadedRaces.put(copy.id, copy);
            ModPackets.sendSaveRace(copy);
            this.workingRace = copy;
            this.init();
        }, 0xFFFFFF55, 0xFFFF9900);
        duplicateBtn.setTooltip(Tooltip.create(Component.literal("Create an exact copy of the selected race.")));
        this.addRenderableWidget(duplicateBtn);

        // 4. Horizontal Category Tabs (Custom RPG Flat Buttons)
        int contentLeft = 155;
        int tabX = contentLeft;
        int tabY = 28;
        int tabHeight = 18;
        int maxTabX = this.width - 150;
        int lastTabY = tabY;

        String[] tabKeys = {
            "gui.customraces.tab.basics", "gui.customraces.tab.model", "gui.customraces.tab.positions",
            "gui.customraces.tab.passives", "gui.customraces.tab.actives", "gui.customraces.tab.sounds",
            "gui.customraces.tab.advanced", "gui.customraces.tab.alliances", "gui.customraces.tab.were_model",
            "gui.customraces.tab.were_sounds", "gui.customraces.tab.drawbacks", "gui.customraces.tab.native_spells"
        };

        for (int i = 0; i < tabKeys.length; i++) {
            final int index = i;
            if (i == 2 && !"Custom".equalsIgnoreCase(workingRace.modelType)) continue;
            if (i == 7 && !workingRace.enableAlliances) continue;
            if ((i == 8 || i == 9) && !workingRace.enableWereRace) continue;

            String prefix = (editingWereForm || i == 8 || i == 9) ? "🐺 " : "";
            Component tabText = Component.literal(prefix).append(Component.translatable(tabKeys[i]));

            int calcTabWidth = Math.max(52, this.font.width(tabText) + 10);

            if (tabX + calcTabWidth > maxTabX) {
                tabX = contentLeft;
                tabY += tabHeight + 3;
            }
            lastTabY = tabY;

            int tabBorder = (editingWereForm && workingRace.enableWereRace) ? 0xFFFF3838 : 0xFF00CEC9;
            FlatButton tabBtn = new FlatButton(tabX, tabY, calcTabWidth, tabHeight, tabText, b -> {
                autoSaveWorkingRace();
                this.activeTab = index;
                this.init();
            }, activeTab == i ? 0xFFFF9900 : tabBorder, 0xFF7B61FF);

            tabBtn.setTooltip(Tooltip.create(Component.translatable(tabKeys[i])));
            if (activeTab == i) tabBtn.active = false;
            this.addRenderableWidget(tabBtn);
            tabX += calcTabWidth + 3;
        }

        // 5. Header Action Buttons: Save All & Were Mode Toggle
        if (workingRace.enableWereRace) {
            FlatButton modeToggleBtn = new FlatButton(this.width - 275, 4, 115, 18, Component.literal(editingWereForm ? "🐺 WERE FORM" : "👤 BASE FORM"), b -> {
                readFormInputs();
                editingWereForm = !editingWereForm;
                this.init();
            }, editingWereForm ? 0xFFFF3838 : 0xFF00CEC9, 0xFFFF5555);
            modeToggleBtn.setTooltip(Tooltip.create(Component.literal("Switch editor mode between Base Form and Were-Form.")));
            this.addRenderableWidget(modeToggleBtn);
        }

        FlatButton saveBtn = new FlatButton(this.width - 150, 4, 80, 18, Component.literal("§a💾 Save All"), b -> {
            readFormInputs();
            ModPackets.sendSaveRace(workingRace);
            this.onClose();
        }, 0xFF55FF55, 0xFF55FFFF);
        saveBtn.setTooltip(Tooltip.create(Component.translatable("gui.customraces.tooltip.save_race")));
        this.addRenderableWidget(saveBtn);

        // Dynamic Content Area Top Offset (Sits safely below bottom row of tabs)
        int contentTop = lastTabY + tabHeight + 14;

        if (activeTab == 0) { // Basics
            this.nameBox = new EditBox(this.font, contentLeft + 130, contentTop, 160, 18, Component.literal("Name"));
            this.nameBox.setMaxLength(2048);
            this.nameBox.setValue(workingRace.name);
            this.nameBox.setTooltip(Tooltip.create(Component.literal("Display name of the race.")));
            this.addRenderableWidget(this.nameBox);

            this.nameColorBox = new EditBox(this.font, contentLeft + 130, contentTop + 23, 70, 18, Component.literal("Name Color"));
            this.nameColorBox.setMaxLength(2048);
            this.nameColorBox.setValue(workingRace.nameColor);
            this.nameColorBox.setTooltip(Tooltip.create(Component.literal("RGB Hex Color code for race title / nametag (e.g. #FFAA00).")));
            this.addRenderableWidget(this.nameColorBox);

            this.difficultyBox = new EditBox(this.font, contentLeft + 130, contentTop + 46, 60, 18, Component.literal("Difficulty"));
            this.difficultyBox.setMaxLength(2048);
            this.difficultyBox.setValue(String.valueOf(workingRace.playstyleDifficulty));
            this.difficultyBox.setTooltip(Tooltip.create(Component.literal("Playstyle Difficulty rating meter from 1 (Easy) to 10 (Insane).")));
            this.addRenderableWidget(this.difficultyBox);

            this.loreBox = new EditBox(this.font, contentLeft + 130, contentTop + 69, 230, 18, Component.literal("Lore"));
            this.loreBox.setMaxLength(2048);
            this.loreBox.setValue(workingRace.lore);
            this.loreBox.setTooltip(Tooltip.create(Component.literal("Lore and background story description.")));
            this.addRenderableWidget(this.loreBox);

            this.iconBox = new EditBox(this.font, contentLeft + 130, contentTop + 92, 160, 18, Component.literal("Icon Item"));
            this.iconBox.setMaxLength(2048);
            this.iconBox.setValue(workingRace.iconItem);
            this.iconBox.setTooltip(Tooltip.create(Component.literal("ResourceLocation of icon item (e.g. minecraft:player_head).")));
            this.addRenderableWidget(this.iconBox);

            this.customTextureBox = new EditBox(this.font, contentLeft + 130, contentTop + 115, 230, 18, Component.literal("PNG Picture Path"));
            this.customTextureBox.setMaxLength(2048);
            this.customTextureBox.setValue(workingRace.customTexture);
            this.customTextureBox.setTooltip(Tooltip.create(Component.literal("ResourceLocation path to PNG picture (e.g. customraces:textures/gui/races/elf.png).")));
            this.addRenderableWidget(this.customTextureBox);

            // Clean Non-Overlapping Checkboxes Grid (Row 1 at contentTop + 140, Row 2 at contentTop + 162)
            this.hideHelmetBox = new Checkbox(contentLeft, contentTop + 140, 105, 18, Component.literal("Hide Helmet"), workingRace.hideHelmet);
            this.hideHelmetBox.setTooltip(Tooltip.create(Component.literal("Check to hide player helmet armor piece visually.")));
            this.addRenderableWidget(this.hideHelmetBox);

            this.hideChestplateBox = new Checkbox(contentLeft + 110, contentTop + 140, 110, 18, Component.literal("Hide Chestplate"), workingRace.hideChestplate);
            this.hideChestplateBox.setTooltip(Tooltip.create(Component.literal("Check to hide player chestplate armor piece visually.")));
            this.addRenderableWidget(this.hideChestplateBox);

            this.hideLeggingsBox = new Checkbox(contentLeft + 225, contentTop + 140, 105, 18, Component.literal("Hide Leggings"), workingRace.hideLeggings);
            this.hideLeggingsBox.setTooltip(Tooltip.create(Component.literal("Check to hide player leggings armor piece visually.")));
            this.addRenderableWidget(this.hideLeggingsBox);

            this.hideBootsBox = new Checkbox(contentLeft, contentTop + 162, 105, 18, Component.literal("Hide Boots"), workingRace.hideBoots);
            this.hideBootsBox.setTooltip(Tooltip.create(Component.literal("Check to hide player boots armor piece visually.")));
            this.addRenderableWidget(this.hideBootsBox);

            this.enableWereBox = new Checkbox(contentLeft + 110, contentTop + 162, 140, 18, Component.literal("Enable Were-Form"), workingRace.enableWereRace) {
                @Override
                public void onPress() {
                    super.onPress();
                    workingRace.enableWereRace = this.selected();
                    RaceCreatorScreen.this.init();
                }
            };
            this.enableWereBox.setTooltip(Tooltip.create(Component.literal("Check to enable Were-form capabilities and unlock Were Model & Were Sounds tabs.")));
            this.addRenderableWidget(this.enableWereBox);

            this.isFlyingRaceBox = new Checkbox(contentLeft + 255, contentTop + 162, 135, 18, Component.literal("Flying-Only Race"), workingRace.isFlyingRace);
            this.isFlyingRaceBox.setTooltip(Tooltip.create(Component.literal("Check if this race is a flying-only race (always flight capable).")));
            this.addRenderableWidget(this.isFlyingRaceBox);

        } else if (activeTab == 1) { // Model & Animations
            if (editingWereForm && workingRace.enableWereRace) {
                this.heightScaleBox = new EditBox(this.font, contentLeft + 140, contentTop + 30, 60, 18, Component.literal("Were Height Scale"));
                this.heightScaleBox.setMaxLength(2048);
                this.heightScaleBox.setValue(String.valueOf(workingRace.wereHeightScale));
                this.heightScaleBox.setTooltip(Tooltip.create(Component.literal("Height scale multiplier while in Were-form.")));
                this.addRenderableWidget(this.heightScaleBox);

                this.widthScaleBox = new EditBox(this.font, contentLeft + 140, contentTop + 55, 60, 18, Component.literal("Were Width Scale"));
                this.widthScaleBox.setMaxLength(2048);
                this.widthScaleBox.setValue(String.valueOf(workingRace.wereWidthScale));
                this.widthScaleBox.setTooltip(Tooltip.create(Component.literal("Width scale multiplier while in Were-form.")));
                this.addRenderableWidget(this.widthScaleBox);

                this.healthBox = new EditBox(this.font, contentLeft + 140, contentTop + 80, 60, 18, Component.literal("Were Health Bonus"));
                this.healthBox.setMaxLength(2048);
                this.healthBox.setValue(String.valueOf(workingRace.wereHealthBonus));
                this.healthBox.setTooltip(Tooltip.create(Component.literal("Bonus HP granted while in Were-form.")));
                this.addRenderableWidget(this.healthBox);

                this.speedBox = new EditBox(this.font, contentLeft + 140, contentTop + 105, 60, 18, Component.literal("Were Speed Bonus"));
                this.speedBox.setMaxLength(2048);
                this.speedBox.setValue(String.valueOf(workingRace.wereSpeedBonus));
                this.speedBox.setTooltip(Tooltip.create(Component.literal("Bonus movement speed granted while in Were-form.")));
                this.addRenderableWidget(this.speedBox);

                this.wereDamageBox = new EditBox(this.font, contentLeft + 140, contentTop + 130, 60, 18, Component.literal("Were Damage Bonus"));
                this.wereDamageBox.setMaxLength(2048);
                this.wereDamageBox.setValue(String.valueOf(workingRace.wereDamageBonus));
                this.wereDamageBox.setTooltip(Tooltip.create(Component.literal("Bonus attack damage granted while in Were-form.")));
                this.addRenderableWidget(this.wereDamageBox);
            } else {
                Button modelTypeBtn = Button.builder(Component.literal("Type: " + workingRace.modelType), b -> {
                    workingRace.modelType = "Default".equals(workingRace.modelType) ? "Custom" : "Default";
                    this.init();
                }).bounds(contentLeft, contentTop, 140, 20).build();
                modelTypeBtn.setTooltip(Tooltip.create(Component.literal("Toggle Model Type between Default (Steve/Alex) and Custom parts model.")));
                this.addRenderableWidget(modelTypeBtn);

                this.heightScaleBox = new EditBox(this.font, contentLeft + 140, contentTop + 30, 60, 18, Component.literal("Height Scale"));
                this.heightScaleBox.setMaxLength(2048);
                this.heightScaleBox.setValue(String.valueOf(workingRace.heightScale));
                this.heightScaleBox.setTooltip(Tooltip.create(Component.literal("Height scale multiplier. Player model scaling Requires Pehkui.")));
                this.addRenderableWidget(this.heightScaleBox);

                this.widthScaleBox = new EditBox(this.font, contentLeft + 140, contentTop + 55, 60, 18, Component.literal("Width Scale"));
                this.widthScaleBox.setMaxLength(2048);
                this.widthScaleBox.setValue(String.valueOf(workingRace.widthScale));
                this.widthScaleBox.setTooltip(Tooltip.create(Component.literal("Width scale multiplier. Player model scaling Requires Pehkui.")));
                this.addRenderableWidget(this.widthScaleBox);

                this.healthBox = new EditBox(this.font, contentLeft + 140, contentTop + 80, 60, 18, Component.literal("Max Health"));
                this.healthBox.setMaxLength(2048);
                this.healthBox.setValue(String.valueOf(workingRace.maxHealth));
                this.healthBox.setTooltip(Tooltip.create(Component.literal("Base Max Health value (Vanilla default is 20.0).")));
                this.addRenderableWidget(this.healthBox);

                this.speedBox = new EditBox(this.font, contentLeft + 140, contentTop + 105, 60, 18, Component.literal("Movement Speed"));
                this.speedBox.setMaxLength(2048);
                this.speedBox.setValue(String.valueOf(workingRace.movementSpeed));
                this.speedBox.setTooltip(Tooltip.create(Component.literal("Base Movement Speed multiplier (Vanilla default is 0.1).")));
                this.addRenderableWidget(this.speedBox);

                this.flyAnimBox = new EditBox(this.font, contentLeft + 140, contentTop + 130, 150, 18, Component.literal("Fly Animation"));
                this.flyAnimBox.setMaxLength(2048);
                this.flyAnimBox.setValue(workingRace.flyAnim);
                this.flyAnimBox.setTooltip(Tooltip.create(Component.literal("GeckoLib fly animation key (e.g. animation.model.fly).")));
                this.addRenderableWidget(this.flyAnimBox);

                this.swimAnimBox = new EditBox(this.font, contentLeft + 140, contentTop + 155, 150, 18, Component.literal("Swim Animation"));
                this.swimAnimBox.setMaxLength(2048);
                this.swimAnimBox.setValue(workingRace.swimAnim);
                this.swimAnimBox.setTooltip(Tooltip.create(Component.literal("GeckoLib swim animation key (e.g. animation.model.swim).")));
                this.addRenderableWidget(this.swimAnimBox);

                // Open Body Parts & Color Picker Overlay
                Button partsBtn = Button.builder(Component.literal("§ePreset Body Parts & Colors"), b -> {
                    Minecraft.getInstance().setScreen(new BodyPartOverlay(this, workingRace));
                }).bounds(contentLeft, contentTop + 180, 200, 22).build();
                partsBtn.setTooltip(Tooltip.create(Component.literal("Open Body Part Selector & RGB Color Picker Wheel overlay.")));
                this.addRenderableWidget(partsBtn);
                partsBtn.setTooltip(Tooltip.create(Component.literal("Open Body Part Selector & RGB Color Picker Wheel overlay.")));
                this.addRenderableWidget(partsBtn);
            }

        } else if (activeTab == 2) { // Positions / Part Transforms
            int py = contentTop + 18;
            String[] partKeys = {"ears", "wings", "tail", "horns", "halo", "custom"};
            for (String pKey : partKeys) {
                PartTransformData pt = workingRace.partTransforms.computeIfAbsent(pKey, k -> new PartTransformData());

                EditBox xBox = new EditBox(this.font, contentLeft + 110, py, 45, 18, Component.literal(pKey + " X"));
                xBox.setMaxLength(2048);
                xBox.setValue(String.valueOf(pt.posX));
                xBox.setResponder(val -> { try { pt.posX = Float.parseFloat(val); } catch (Exception ignored) {} });
                this.addRenderableWidget(xBox);

                EditBox yBox = new EditBox(this.font, contentLeft + 160, py, 45, 18, Component.literal(pKey + " Y"));
                yBox.setMaxLength(2048);
                yBox.setValue(String.valueOf(pt.posY));
                yBox.setResponder(val -> { try { pt.posY = Float.parseFloat(val); } catch (Exception ignored) {} });
                this.addRenderableWidget(yBox);

                EditBox zBox = new EditBox(this.font, contentLeft + 210, py, 45, 18, Component.literal(pKey + " Z"));
                zBox.setMaxLength(2048);
                zBox.setValue(String.valueOf(pt.posZ));
                zBox.setResponder(val -> { try { pt.posZ = Float.parseFloat(val); } catch (Exception ignored) {} });
                this.addRenderableWidget(zBox);

                py += 24;
            }

        } else if (activeTab == 3) { // Passives (1 Column Scrollable List)
            passiveWidgets.clear();
            EditBox pSearch = new EditBox(this.font, contentLeft + 200, contentTop, 160, 16, Component.literal("Search"));
            pSearch.setMaxLength(2048);
            pSearch.setValue(searchPassivesQuery);
            pSearch.setHint(Component.literal("🔍 Search passives..."));
            pSearch.setResponder(val -> {
                searchPassivesQuery = val;
                this.init();
            });
            this.addRenderableWidget(pSearch);

            List<String> targetList = (editingWereForm && workingRace.enableWereRace) ? workingRace.werePassiveAbilities : workingRace.passiveAbilities;
            String q = searchPassivesQuery.toLowerCase().trim();

            int visibleTop = contentTop + 24;
            int visibleBottom = this.height - 20;
            int visibleHeight = visibleBottom - visibleTop;

            List<String> matchingPassives = new ArrayList<>();
            for (String passive : ALL_PASSIVES) {
                String raw = passive.replace("_", " ");
                String desc = getPassiveDescription(passive);
                if (q.isEmpty() || raw.toLowerCase().contains(q) || desc.toLowerCase().contains(q)) {
                    matchingPassives.add(passive);
                }
            }

            this.matchingPassivesCount = matchingPassives.size();
            int totalContentH = matchingPassives.size() * 20;
            float maxScrollPassives = Math.max(0, totalContentH - visibleHeight);
            passivesScrollAmount = Math.max(0, Math.min(passivesScrollAmount, maxScrollPassives));

            int cbX = contentLeft;
            int cbWidth = 320;
            int rowHeight = 20;

            for (int i = 0; i < matchingPassives.size(); i++) {
                String passive = matchingPassives.get(i);
                int cbY = visibleTop + (i * rowHeight) - (int) passivesScrollAmount;

                boolean active = targetList.contains(passive);
                String raw = passive.replace("_", " ").toUpperCase();
                String desc = getPassiveDescription(passive);

                Checkbox pBox = new Checkbox(cbX, cbY, cbWidth, 18, Component.literal("✨ " + raw), active) {
                    @Override
                    public void onPress() {
                        super.onPress();
                        if (this.selected()) {
                            if (!targetList.contains(passive)) targetList.add(passive);
                        } else {
                            targetList.remove(passive);
                        }
                        autoSaveWorkingRace();
                    }
                };
                pBox.setTooltip(Tooltip.create(Component.literal("✨ " + raw + "\n" + desc)));
                pBox.visible = (cbY >= visibleTop - rowHeight && cbY <= visibleBottom);
                this.addRenderableWidget(pBox);
                this.passiveWidgets.add(pBox);
            }

        } else if (activeTab == 4) { // Actives
            EditBox aSearch = new EditBox(this.font, contentLeft + 230, contentTop, 130, 16, Component.literal("Search Actives"));
            aSearch.setMaxLength(2048);
            aSearch.setValue(searchActivesQuery);
            aSearch.setHint(Component.literal("🔍 Search skills..."));
            aSearch.setResponder(val -> {
                searchActivesQuery = val;
                this.init();
            });
            this.addRenderableWidget(aSearch);

            int py = contentTop + 24;
            Map<Integer, String> targetMap = (editingWereForm && workingRace.enableWereRace) ? workingRace.wereActiveAbilities : workingRace.activeAbilities;

            for (int slot = 1; slot <= 5; slot++) {
                final int currentSlot = slot;
                String currentSkill = targetMap.getOrDefault(slot, "none");

                EditBox slotBox = new EditBox(this.font, contentLeft + 60, py, 200, 18, Component.literal("Slot " + slot));
                slotBox.setMaxLength(2048);
                slotBox.setValue(currentSkill);
                slotBox.setTooltip(Tooltip.create(Component.literal("Skill ID for Slot " + slot + " (e.g. flame_breath, teleport_dash, native_spell, transform_were, summon_minions).")));
                slotBox.setResponder(val -> targetMap.put(currentSlot, val));
                this.addRenderableWidget(slotBox);

                py += 24;
            }

        } else if (activeTab == 7) { // Alliances
            int py = contentTop + 18;
            String[] factions = {"minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:creeper", "minecraft:enderman", "minecraft:piglin"};
            for (String mobId : factions) {
                boolean isNeutral = workingRace.alliances.stream().anyMatch(a -> mobId.equalsIgnoreCase(a.mobId));
                Checkbox aBox = new Checkbox(contentLeft, py, 180, 20, Component.literal(mobId.substring(mobId.indexOf(':') + 1).toUpperCase() + " Neutrality"), isNeutral) {
                    @Override
                    public void onPress() {
                        super.onPress();
                        if (this.selected()) {
                            if (workingRace.alliances.stream().noneMatch(a -> mobId.equalsIgnoreCase(a.mobId))) {
                                workingRace.alliances.add(new MobAllianceData(mobId, "neutral"));
                            }
                        } else {
                            workingRace.alliances.removeIf(a -> mobId.equalsIgnoreCase(a.mobId));
                        }
                    }
                };
                aBox.setTooltip(Tooltip.create(Component.literal("Toggle neutrality stance for " + mobId)));
                this.addRenderableWidget(aBox);
                py += 22;
            }

        } else if (activeTab == 5) { // Sounds & FX
            this.ambientSoundBox = new EditBox(this.font, contentLeft + 110, contentTop, 200, 18, Component.literal("Ambient Sound"));
            this.ambientSoundBox.setMaxLength(2048);
            this.ambientSoundBox.setValue(workingRace.ambientSound);
            this.ambientSoundBox.setTooltip(Tooltip.create(Component.literal("Sound event ID for ambient idle sounds.")));
            this.addRenderableWidget(this.ambientSoundBox);

            Button playAmbBtn = Button.builder(Component.literal("▶ Play"), b -> {
                playPreviewSound(this.ambientSoundBox.getValue());
            }).bounds(contentLeft + 315, contentTop, 50, 18).build();
            playAmbBtn.setTooltip(Tooltip.create(Component.literal("Preview sound playback.")));
            this.addRenderableWidget(playAmbBtn);

            this.hurtSoundBox = new EditBox(this.font, contentLeft + 110, contentTop + 30, 200, 18, Component.literal("Hurt Sound"));
            this.hurtSoundBox.setMaxLength(2048);
            this.hurtSoundBox.setValue(workingRace.hurtSound);
            this.hurtSoundBox.setTooltip(Tooltip.create(Component.literal("Sound event ID when receiving damage.")));
            this.addRenderableWidget(this.hurtSoundBox);

            Button playHurtBtn = Button.builder(Component.literal("▶ Play"), b -> {
                playPreviewSound(this.hurtSoundBox.getValue());
            }).bounds(contentLeft + 315, contentTop + 30, 50, 18).build();
            playHurtBtn.setTooltip(Tooltip.create(Component.literal("Preview sound playback.")));
            this.addRenderableWidget(playHurtBtn);

            this.deathSoundBox = new EditBox(this.font, contentLeft + 110, contentTop + 60, 200, 18, Component.literal("Death Sound"));
            this.deathSoundBox.setMaxLength(2048);
            this.deathSoundBox.setValue(workingRace.deathSound);
            this.deathSoundBox.setTooltip(Tooltip.create(Component.literal("Sound event ID when dying.")));
            this.addRenderableWidget(this.deathSoundBox);

            Button playDeathBtn = Button.builder(Component.literal("▶ Play"), b -> {
                playPreviewSound(this.deathSoundBox.getValue());
            }).bounds(contentLeft + 315, contentTop + 60, 50, 18).build();
            playDeathBtn.setTooltip(Tooltip.create(Component.literal("Preview sound playback.")));
            this.addRenderableWidget(playDeathBtn);

        } else if (activeTab == 6) { // Advanced Features
            this.spawnDimensionBox = new EditBox(this.font, contentLeft + 120, contentTop, 180, 18, Component.literal("Spawn Dimension"));
            this.spawnDimensionBox.setMaxLength(2048);
            this.spawnDimensionBox.setValue(workingRace.spawnDimension);
            this.spawnDimensionBox.setTooltip(Tooltip.create(Component.literal("Dimension ID for custom race spawn point (e.g. minecraft:the_nether).")));
            this.addRenderableWidget(this.spawnDimensionBox);

            this.spawnBiomeBox = new EditBox(this.font, contentLeft + 120, contentTop + 30, 180, 18, Component.literal("Spawn Biome"));
            this.spawnBiomeBox.setMaxLength(2048);
            this.spawnBiomeBox.setValue(workingRace.spawnBiome);
            this.spawnBiomeBox.setTooltip(Tooltip.create(Component.literal("Biome ID for custom race spawn point (e.g. minecraft:ocean).")));
            this.addRenderableWidget(this.spawnBiomeBox);

            this.enableAlliancesBox = new Checkbox(contentLeft, contentTop + 65, 180, 20, Component.literal("Enable Custom Alliances"), workingRace.enableAlliances);
            this.enableAlliancesBox.setTooltip(Tooltip.create(Component.literal("Enables the Alliances tab for setting mob neutrality stances.")));
            this.addRenderableWidget(this.enableAlliancesBox);

            // Minion Ability Settings
            this.minionMobTypeBox = new EditBox(this.font, contentLeft + 120, contentTop + 95, 200, 18, Component.literal("Minion Mob Type"));
            this.minionMobTypeBox.setMaxLength(2048);
            this.minionMobTypeBox.setValue(workingRace.minionMobType);
            this.minionMobTypeBox.setTooltip(Tooltip.create(Component.literal("Mob Entity ID to summon (e.g. minecraft:zombie or custom_mobs:<id>).")));
            this.addRenderableWidget(this.minionMobTypeBox);

            this.minionCountBox = new EditBox(this.font, contentLeft + 120, contentTop + 118, 60, 18, Component.literal("Minion Count"));
            this.minionCountBox.setMaxLength(2048);
            this.minionCountBox.setValue(String.valueOf(workingRace.minionCount));
            this.minionCountBox.setTooltip(Tooltip.create(Component.literal("Number of minions to summon (1 to 10).")));
            this.addRenderableWidget(this.minionCountBox);

            this.minionScaleBox = new EditBox(this.font, contentLeft + 260, contentTop + 118, 60, 18, Component.literal("Minion Scale"));
            this.minionScaleBox.setMaxLength(2048);
            this.minionScaleBox.setValue(String.valueOf(workingRace.minionScale));
            this.minionScaleBox.setTooltip(Tooltip.create(Component.literal("Minion size scaling multiplier (0.5 to 5.0).")));
            this.addRenderableWidget(this.minionScaleBox);

            this.minionIsRangedBox = new Checkbox(contentLeft, contentTop + 141, 120, 20, Component.literal("Minion Ranged"), workingRace.minionIsRanged);
            this.minionIsRangedBox.setTooltip(Tooltip.create(Component.literal("Check if minion shoots ranged projectiles instead of melee.")));
            this.addRenderableWidget(this.minionIsRangedBox);

            this.minionProjectileBox = new EditBox(this.font, contentLeft + 250, contentTop + 142, 160, 18, Component.literal("Minion Projectile"));
            this.minionProjectileBox.setMaxLength(2048);
            this.minionProjectileBox.setValue(workingRace.minionProjectile);
            this.minionProjectileBox.setTooltip(Tooltip.create(Component.literal("Projectile Entity ID if minion is ranged (e.g. minecraft:arrow).")));
            this.addRenderableWidget(this.minionProjectileBox);
        } else if (activeTab == 8) { // Were Model & Anims
            this.enableWereBox = new Checkbox(contentLeft, contentTop, 180, 20, Component.literal("Enable Were-Form"), workingRace.enableWereRace);
            this.enableWereBox.setTooltip(Tooltip.create(Component.literal("Toggle Were-form transformation capabilities for this race.")));
            this.addRenderableWidget(this.enableWereBox);

            Button editWerePassivesBtn = Button.builder(Component.literal("🐺 Were Passives & Skills"), b -> {
                readFormInputs();
                editingWereForm = true;
                activeTab = 3;
                this.init();
            }).bounds(contentLeft + 200, contentTop, 160, 20).build();
            editWerePassivesBtn.setTooltip(Tooltip.create(Component.literal("Switch to Passives/Actives tab in Were-Form editing mode.")));
            this.addRenderableWidget(editWerePassivesBtn);

            this.wereConditionBox = new EditBox(this.font, contentLeft + 120, contentTop + 24, 120, 18, Component.literal("Trigger Condition"));
            this.wereConditionBox.setMaxLength(2048);
            this.wereConditionBox.setValue(workingRace.wereTriggerCondition);
            this.wereConditionBox.setTooltip(Tooltip.create(Component.literal("FULL_MOON, NEW_MOON, NIGHT, DAY, WATER, RAGE, KEY.")));
            this.addRenderableWidget(this.wereConditionBox);

            String[] triggers = {"FULL_MOON", "NEW_MOON", "NIGHT", "DAY", "WATER", "RAGE", "KEY"};
            Button trigBtn = Button.builder(Component.literal("▶ " + workingRace.wereTriggerCondition), b -> {
                String cur = workingRace.wereTriggerCondition != null ? workingRace.wereTriggerCondition.toUpperCase() : "FULL_MOON";
                int idx = 0;
                for (int i = 0; i < triggers.length; i++) {
                    if (triggers[i].equals(cur)) { idx = (i + 1) % triggers.length; break; }
                }
                workingRace.wereTriggerCondition = triggers[idx];
                if (this.wereConditionBox != null) this.wereConditionBox.setValue(triggers[idx]);
                b.setMessage(Component.literal("▶ " + triggers[idx]));
            }).bounds(contentLeft + 245, contentTop + 24, 115, 18).build();
            trigBtn.setTooltip(Tooltip.create(Component.literal("Cycle trigger condition (FULL_MOON, NEW_MOON, NIGHT, DAY, WATER, RAGE, KEY).")));
            this.addRenderableWidget(trigBtn);

            this.wereModelBox = new EditBox(this.font, contentLeft + 135, contentTop + 46, 225, 18, Component.literal("Were Model Geo JSON"));
            this.wereModelBox.setMaxLength(2048);
            this.wereModelBox.setValue(workingRace.wereModelPath);
            this.wereModelBox.setTooltip(Tooltip.create(Component.literal("GeckoLib Were model file (e.g. customraces:models/were/werewolf.geo.json).")));
            this.addRenderableWidget(this.wereModelBox);

            this.wereTextureBox = new EditBox(this.font, contentLeft + 135, contentTop + 68, 225, 18, Component.literal("Were Texture PNG"));
            this.wereTextureBox.setMaxLength(2048);
            this.wereTextureBox.setValue(workingRace.wereTexturePath);
            this.wereTextureBox.setTooltip(Tooltip.create(Component.literal("Were-form PNG texture (e.g. customraces:textures/were/werewolf.png).")));
            this.addRenderableWidget(this.wereTextureBox);

            this.wereAnimFileBox = new EditBox(this.font, contentLeft + 135, contentTop + 90, 225, 18, Component.literal("Were Animation JSON"));
            this.wereAnimFileBox.setMaxLength(2048);
            this.wereAnimFileBox.setValue(workingRace.wereAnimationPath);
            this.wereAnimFileBox.setTooltip(Tooltip.create(Component.literal("GeckoLib Were animation file (e.g. customraces:animations/were/werewolf.animation.json).")));
            this.addRenderableWidget(this.wereAnimFileBox);

            this.wereIdleAnimBox = new EditBox(this.font, contentLeft + 135, contentTop + 112, 140, 18, Component.literal("Idle Animation"));
            this.wereIdleAnimBox.setMaxLength(2048);
            this.wereIdleAnimBox.setValue(workingRace.wereIdleAnim);
            this.addRenderableWidget(this.wereIdleAnimBox);

            this.isWereFlyingRaceBox = new Checkbox(contentLeft + 280, contentTop + 112, 140, 18, Component.literal("Were Flying Race"), workingRace.isWereFlyingRace);
            this.isWereFlyingRaceBox.setTooltip(Tooltip.create(Component.literal("Check if Were-Form is a flying-only race.")));
            this.addRenderableWidget(this.isWereFlyingRaceBox);

            this.wereWalkAnimBox = new EditBox(this.font, contentLeft + 135, contentTop + 134, 140, 18, Component.literal("Walk Animation"));
            this.wereWalkAnimBox.setMaxLength(2048);
            this.wereWalkAnimBox.setValue(workingRace.wereWalkAnim);
            this.addRenderableWidget(this.wereWalkAnimBox);

            this.wereAttackAnimBox = new EditBox(this.font, contentLeft + 135, contentTop + 156, 140, 18, Component.literal("Attack Animation"));
            this.wereAttackAnimBox.setMaxLength(2048);
            this.wereAttackAnimBox.setValue(workingRace.wereAttackAnim);
            this.addRenderableWidget(this.wereAttackAnimBox);

            this.wereFlyAnimBox = new EditBox(this.font, contentLeft + 135, contentTop + 178, 140, 18, Component.literal("Fly Animation"));
            this.wereFlyAnimBox.setMaxLength(2048);
            this.wereFlyAnimBox.setValue(workingRace.wereFlyAnim);
            this.addRenderableWidget(this.wereFlyAnimBox);

            this.wereSwimAnimBox = new EditBox(this.font, contentLeft + 135, contentTop + 200, 140, 18, Component.literal("Swim Animation"));
            this.wereSwimAnimBox.setMaxLength(2048);
            this.wereSwimAnimBox.setValue(workingRace.wereSwimAnim);
            this.addRenderableWidget(this.wereSwimAnimBox);

        } else if (activeTab == 9) { // Were Sounds
            this.wereTransformSoundBox = new EditBox(this.font, contentLeft + 135, contentTop, 185, 18, Component.literal("Transform Sound"));
            this.wereTransformSoundBox.setMaxLength(2048);
            this.wereTransformSoundBox.setValue(workingRace.wereTransformSound);
            this.addRenderableWidget(this.wereTransformSoundBox);

            Button pTr = Button.builder(Component.literal("▶ Play"), b -> playPreviewSound(this.wereTransformSoundBox.getValue())).bounds(contentLeft + 325, contentTop, 50, 18).build();
            this.addRenderableWidget(pTr);

            this.wereHowlSoundBox = new EditBox(this.font, contentLeft + 135, contentTop + 25, 185, 18, Component.literal("Howl Sound"));
            this.wereHowlSoundBox.setMaxLength(2048);
            this.wereHowlSoundBox.setValue(workingRace.wereHowlSound);
            this.addRenderableWidget(this.wereHowlSoundBox);

            Button pHw = Button.builder(Component.literal("▶ Play"), b -> playPreviewSound(this.wereHowlSoundBox.getValue())).bounds(contentLeft + 325, contentTop + 25, 50, 18).build();
            this.addRenderableWidget(pHw);

            this.wereAmbientSoundBox = new EditBox(this.font, contentLeft + 135, contentTop + 50, 185, 18, Component.literal("Ambient Sound"));
            this.wereAmbientSoundBox.setMaxLength(2048);
            this.wereAmbientSoundBox.setValue(workingRace.wereAmbientSound);
            this.addRenderableWidget(this.wereAmbientSoundBox);

            Button pAm = Button.builder(Component.literal("▶ Play"), b -> playPreviewSound(this.wereAmbientSoundBox.getValue())).bounds(contentLeft + 325, contentTop + 50, 50, 18).build();
            this.addRenderableWidget(pAm);

            this.wereHurtSoundBox = new EditBox(this.font, contentLeft + 135, contentTop + 75, 185, 18, Component.literal("Hurt Sound"));
            this.wereHurtSoundBox.setMaxLength(2048);
            this.wereHurtSoundBox.setValue(workingRace.wereHurtSound);
            this.addRenderableWidget(this.wereHurtSoundBox);

            Button pHr = Button.builder(Component.literal("▶ Play"), b -> playPreviewSound(this.wereHurtSoundBox.getValue())).bounds(contentLeft + 325, contentTop + 75, 50, 18).build();
            this.addRenderableWidget(pHr);

            this.wereDeathSoundBox = new EditBox(this.font, contentLeft + 135, contentTop + 100, 185, 18, Component.literal("Death Sound"));
            this.wereDeathSoundBox.setMaxLength(2048);
            this.wereDeathSoundBox.setValue(workingRace.wereDeathSound);
            this.addRenderableWidget(this.wereDeathSoundBox);

            Button pDt = Button.builder(Component.literal("▶ Play"), b -> playPreviewSound(this.wereDeathSoundBox.getValue())).bounds(contentLeft + 325, contentTop + 100, 50, 18).build();
            this.addRenderableWidget(pDt);
        } else if (activeTab == 10) { // Drawbacks (Single Column Scrollable List)
            drawbackWidgets.clear();
            EditBox dSearch = new EditBox(this.font, contentLeft + 200, contentTop, 160, 16, Component.literal("Search Drawbacks"));
            dSearch.setMaxLength(2048);
            dSearch.setValue(searchDrawbacksQuery);
            dSearch.setHint(Component.literal("🔍 Search drawbacks..."));
            dSearch.setResponder(val -> {
                searchDrawbacksQuery = val;
                this.init();
            });
            this.addRenderableWidget(dSearch);

            List<String> activeList = editingWereForm ? workingRace.wereDrawbacks : workingRace.drawbacks;
            if (activeList == null) {
                activeList = new java.util.ArrayList<>();
                if (editingWereForm) workingRace.wereDrawbacks = activeList;
                else workingRace.drawbacks = activeList;
            }

            int visibleTop = contentTop + 24;
            int visibleBottom = this.height - 20;
            int visibleHeight = visibleBottom - visibleTop;

            String q = searchDrawbacksQuery.toLowerCase().trim();
            List<String> matchingDrawbacks = new ArrayList<>();
            for (String drawbackId : ALL_DRAWBACKS) {
                String rawName = drawbackId.replace("_", " ");
                String desc = getDrawbackDescription(drawbackId);
                if (q.isEmpty() || rawName.toLowerCase().contains(q) || desc.toLowerCase().contains(q)) {
                    matchingDrawbacks.add(drawbackId);
                }
            }

            this.matchingDrawbacksCount = matchingDrawbacks.size();
            int totalContentH = matchingDrawbacks.size() * 20;
            float maxScrollDrawbacks = Math.max(0, totalContentH - visibleHeight);
            drawbacksScrollAmount = Math.max(0, Math.min(drawbacksScrollAmount, maxScrollDrawbacks));

            int cbX = contentLeft;
            int cbWidth = 320;
            int rowHeight = 20;

            for (int i = 0; i < matchingDrawbacks.size(); i++) {
                String drawbackId = matchingDrawbacks.get(i);
                int cbY = visibleTop + (i * rowHeight) - (int) drawbacksScrollAmount;

                boolean selected = activeList.contains(drawbackId);
                final List<String> targetList = activeList;
                String rawName = drawbackId.replace("_", " ").toUpperCase();
                String desc = getDrawbackDescription(drawbackId);

                Checkbox cb = new Checkbox(cbX, cbY, cbWidth, 18, Component.literal("⚠️ " + rawName), selected) {
                    @Override
                    public void onPress() {
                        super.onPress();
                        if (this.selected()) {
                            if (!targetList.contains(drawbackId)) targetList.add(drawbackId);
                        } else {
                            targetList.remove(drawbackId);
                        }
                        autoSaveWorkingRace();
                    }
                };
                cb.setTooltip(Tooltip.create(Component.literal("⚠️ " + rawName + "\n" + desc)));
                cb.visible = (cbY >= visibleTop - rowHeight && cbY <= visibleBottom);
                this.addRenderableWidget(cb);
                this.drawbackWidgets.add(cb);
            }
        } else if (activeTab == 11) { // Native Spells (Slots 1 to 5)
            boolean isWere = editingWereForm && workingRace.enableWereRace;
            boolean enabled = isWere ? workingRace.enableWereNativeSpells : workingRace.enableNativeSpells;

            Checkbox enableNativeCb = new Checkbox(contentLeft, contentTop + 18, 180, 18, Component.literal("Enable Native Spells"), enabled) {
                @Override
                public void onPress() {
                    super.onPress();
                    if (isWere) workingRace.enableWereNativeSpells = this.selected();
                    else workingRace.enableNativeSpells = this.selected();
                    autoSaveWorkingRace();
                }
            };
            enableNativeCb.setTooltip(Tooltip.create(Component.translatable("gui.customraces.tooltip.native_spells")));
            this.addRenderableWidget(enableNativeCb);

            // Slot Selection Sub-Buttons (Slot 1 to 5)
            int slotBtnX = contentLeft;
            int slotBtnY = contentTop + 40;
            for (int s = 1; s <= 5; s++) {
                final int currentSlot = s;
                boolean isSelected = (selectedNativeSpellSlot == currentSlot);
                int borderColor = isSelected ? 0xFF00FFCC : 0xFF7B61FF;
                FlatButton slotBtn = new FlatButton(slotBtnX, slotBtnY, 60, 18, Component.literal("Slot " + currentSlot), b -> {
                    selectedNativeSpellSlot = currentSlot;
                    this.init();
                }, borderColor, 0xFF9932CC);
                slotBtn.setTooltip(Tooltip.create(Component.literal("Configure Native Spell Slot " + currentSlot)));
                this.addRenderableWidget(slotBtn);
                slotBtnX += 65;
            }

            int activeSlot = selectedNativeSpellSlot;
            boolean isWild = workingRace.getWildMagic(activeSlot, isWere);
            String currentSpell = workingRace.getNativeSpellId(activeSlot, isWere);
            int level = workingRace.getNativeSpellLevel(activeSlot, isWere);

            Checkbox wildMagicCb = new Checkbox(contentLeft + 190, contentTop + 65, 140, 18, Component.literal("✨ Wild Magic"), isWild) {
                @Override
                public void onPress() {
                    super.onPress();
                    setRaceWildMagic(activeSlot, isWere, this.selected());
                    autoSaveWorkingRace();
                }
            };
            wildMagicCb.setTooltip(Tooltip.create(Component.literal("Wild Magic for Slot " + activeSlot + ": Spawns a random spell from any school as if the player cast it.")));
            this.addRenderableWidget(wildMagicCb);

            this.nativeSpellBox = new EditBox(this.font, contentLeft + 135, contentTop + 90, 190, 18, Component.literal("Native Spell ID Slot " + activeSlot));
            this.nativeSpellBox.setMaxLength(2048);
            this.nativeSpellBox.setValue(currentSpell);
            this.nativeSpellBox.setTooltip(Tooltip.create(Component.translatable("gui.customraces.tooltip.native_spells")));
            this.nativeSpellBox.setResponder(val -> {
                setRaceNativeSpell(activeSlot, isWere, val);
                autoSaveWorkingRace();
            });
            this.addRenderableWidget(this.nativeSpellBox);

            // Cycle Spell Button
            Button spellCycleBtn = Button.builder(Component.literal("▶ Cycle Spell"), b -> {
                List<String> spells = ddraig.net.customraces.integration.IronSpellsHandler.ALL_SPELLS;
                int idx = 0;
                String cur = this.nativeSpellBox != null ? this.nativeSpellBox.getValue() : "";
                for (int i = 0; i < spells.size(); i++) {
                    if (spells.get(i).equalsIgnoreCase(cur)) { idx = (i + 1) % spells.size(); break; }
                }
                String nextSpell = spells.get(idx);
                if (this.nativeSpellBox != null) this.nativeSpellBox.setValue(nextSpell);
                setRaceNativeSpell(activeSlot, isWere, nextSpell);
                autoSaveWorkingRace();
            }).bounds(contentLeft + 330, contentTop + 90, 95, 18).build();
            spellCycleBtn.setTooltip(Tooltip.create(Component.literal("Cycle through all Iron's Spells & T.O Tweaks registered spells.")));
            this.addRenderableWidget(spellCycleBtn);

            EditBox lvlBox = new EditBox(this.font, contentLeft + 135, contentTop + 115, 60, 18, Component.literal("Spell Level Slot " + activeSlot));
            lvlBox.setMaxLength(2048);
            lvlBox.setValue(String.valueOf(level));
            lvlBox.setTooltip(Tooltip.create(Component.literal("Level multiplier for Native Spell Slot " + activeSlot + " (1 to 10).")));
            lvlBox.setResponder(val -> {
                try {
                    int l = Integer.parseInt(val);
                    setRaceNativeSpellLevel(activeSlot, isWere, l);
                    autoSaveWorkingRace();
                } catch (Exception ignored) {}
            });
            this.addRenderableWidget(lvlBox);
        }
    }

    private void readFormInputs() {
        if (workingRace == null) return;
        if (nameBox != null) workingRace.name = nameBox.getValue();
        if (nameColorBox != null) workingRace.nameColor = nameColorBox.getValue();
        if (difficultyBox != null) {
            try { workingRace.playstyleDifficulty = Integer.parseInt(difficultyBox.getValue()); } catch (Exception ignored) {}
        }
        if (loreBox != null) workingRace.lore = loreBox.getValue();
        if (iconBox != null) workingRace.iconItem = iconBox.getValue();
        if (customTextureBox != null) workingRace.customTexture = customTextureBox.getValue();
        if (hideHelmetBox != null) workingRace.hideHelmet = hideHelmetBox.selected();
        if (hideChestplateBox != null) workingRace.hideChestplate = hideChestplateBox.selected();
        if (hideLeggingsBox != null) workingRace.hideLeggings = hideLeggingsBox.selected();
        if (hideBootsBox != null) workingRace.hideBoots = hideBootsBox.selected();
        if (isFlyingRaceBox != null) workingRace.isFlyingRace = isFlyingRaceBox.selected();

        if (editingWereForm) {
            if (heightScaleBox != null) {
                try { workingRace.wereHeightScale = Float.parseFloat(heightScaleBox.getValue()); } catch (Exception ignored) {}
            }
            if (widthScaleBox != null) {
                try { workingRace.wereWidthScale = Float.parseFloat(widthScaleBox.getValue()); } catch (Exception ignored) {}
            }
            if (healthBox != null) {
                try { workingRace.wereHealthBonus = Float.parseFloat(healthBox.getValue()); } catch (Exception ignored) {}
            }
            if (speedBox != null) {
                try { workingRace.wereSpeedBonus = Float.parseFloat(speedBox.getValue()); } catch (Exception ignored) {}
            }
            if (wereDamageBox != null) {
                try { workingRace.wereDamageBonus = Float.parseFloat(wereDamageBox.getValue()); } catch (Exception ignored) {}
            }
        } else {
            if (heightScaleBox != null) {
                try { workingRace.heightScale = Float.parseFloat(heightScaleBox.getValue()); } catch (Exception ignored) {}
            }
            if (widthScaleBox != null) {
                try { workingRace.widthScale = Float.parseFloat(widthScaleBox.getValue()); } catch (Exception ignored) {}
            }
            if (healthBox != null) {
                try { workingRace.maxHealth = Float.parseFloat(healthBox.getValue()); } catch (Exception ignored) {}
            }
            if (speedBox != null) {
                try { workingRace.movementSpeed = Float.parseFloat(speedBox.getValue()); } catch (Exception ignored) {}
            }
        }

        if (flyAnimBox != null) workingRace.flyAnim = flyAnimBox.getValue();
        if (swimAnimBox != null) workingRace.swimAnim = swimAnimBox.getValue();

        if (ambientSoundBox != null) workingRace.ambientSound = ambientSoundBox.getValue();
        if (hurtSoundBox != null) workingRace.hurtSound = hurtSoundBox.getValue();
        if (deathSoundBox != null) workingRace.deathSound = deathSoundBox.getValue();
        if (spawnDimensionBox != null) workingRace.spawnDimension = spawnDimensionBox.getValue();
        if (spawnBiomeBox != null) workingRace.spawnBiome = spawnBiomeBox.getValue();
        if (enableAlliancesBox != null) workingRace.enableAlliances = enableAlliancesBox.selected();
        if (minionMobTypeBox != null) workingRace.minionMobType = minionMobTypeBox.getValue();
        if (minionCountBox != null) {
            try { workingRace.minionCount = Integer.parseInt(minionCountBox.getValue()); } catch (Exception ignored) {}
        }
        if (minionScaleBox != null) {
            try { workingRace.minionScale = Float.parseFloat(minionScaleBox.getValue()); } catch (Exception ignored) {}
        }
        if (minionIsRangedBox != null) workingRace.minionIsRanged = minionIsRangedBox.selected();
        if (minionProjectileBox != null) workingRace.minionProjectile = minionProjectileBox.getValue();

        if (enableWereBox != null) workingRace.enableWereRace = enableWereBox.selected();
        if (isWereFlyingRaceBox != null) workingRace.isWereFlyingRace = isWereFlyingRaceBox.selected();
        if (wereConditionBox != null) workingRace.wereTriggerCondition = wereConditionBox.getValue();
        if (wereModelBox != null) workingRace.wereModelPath = wereModelBox.getValue();
        if (wereTextureBox != null) workingRace.wereTexturePath = wereTextureBox.getValue();
        if (wereAnimFileBox != null) workingRace.wereAnimationPath = wereAnimFileBox.getValue();
        if (wereIdleAnimBox != null) workingRace.wereIdleAnim = wereIdleAnimBox.getValue();
        if (wereWalkAnimBox != null) workingRace.wereWalkAnim = wereWalkAnimBox.getValue();
        if (wereAttackAnimBox != null) workingRace.wereAttackAnim = wereAttackAnimBox.getValue();
        if (wereFlyAnimBox != null) workingRace.wereFlyAnim = wereFlyAnimBox.getValue();
        if (wereSwimAnimBox != null) workingRace.wereSwimAnim = wereSwimAnimBox.getValue();

        if (wereTransformSoundBox != null) workingRace.wereTransformSound = wereTransformSoundBox.getValue();
        if (wereHowlSoundBox != null) workingRace.wereHowlSound = wereHowlSoundBox.getValue();
        if (wereAmbientSoundBox != null) workingRace.wereAmbientSound = wereAmbientSoundBox.getValue();
        if (wereHurtSoundBox != null) workingRace.wereHurtSound = wereHurtSoundBox.getValue();
        if (wereDeathSoundBox != null) workingRace.wereDeathSound = wereDeathSoundBox.getValue();
    }

    private void playPreviewSound(String soundId) {
        if (soundId == null || soundId.trim().isEmpty() || Minecraft.getInstance() == null) return;
        try {
            net.minecraft.resources.ResourceLocation loc = new net.minecraft.resources.ResourceLocation(soundId.trim());
            net.minecraft.client.resources.sounds.SimpleSoundInstance soundInstance =
                new net.minecraft.client.resources.sounds.SimpleSoundInstance(
                    loc,
                    net.minecraft.sounds.SoundSource.MASTER,
                    1.0f, 1.0f,
                    net.minecraft.util.RandomSource.create(),
                    false, 0,
                    net.minecraft.client.resources.sounds.SoundInstance.Attenuation.NONE,
                    0.0D, 0.0D, 0.0D,
                    true
                );
            Minecraft.getInstance().getSoundManager().play(soundInstance);
        } catch (Exception e) {
            try {
                Minecraft.getInstance().getSoundManager().play(
                    net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f)
                );
            } catch (Exception ignored) {}
        }
    }

    private RaceData duplicateRace(RaceData source) {
        if (source == null) return new RaceData("new_race_" + System.currentTimeMillis() % 1000, "New Race");
        String copyId = source.id + "_copy_" + (System.currentTimeMillis() % 1000);
        RaceData copy = new RaceData(copyId, source.name + " Copy");
        copy.nameColor = source.nameColor;
        copy.playstyleDifficulty = source.playstyleDifficulty;
        copy.lore = source.lore;
        copy.iconItem = source.iconItem;
        copy.customTexture = source.customTexture;
        copy.hideHelmet = source.hideHelmet;
        copy.hideChestplate = source.hideChestplate;
        copy.hideLeggings = source.hideLeggings;
        copy.hideBoots = source.hideBoots;
        copy.modelType = source.modelType;
        copy.heightScale = source.heightScale;
        copy.widthScale = source.widthScale;
        copy.baseScale = source.baseScale;
        copy.maxHealth = source.maxHealth;
        copy.movementSpeed = source.movementSpeed;
        copy.armor = source.armor;
        copy.attackDamage = source.attackDamage;
        copy.passiveAbilities = new ArrayList<>(source.passiveAbilities);
        copy.activeAbilities = new java.util.HashMap<>(source.activeAbilities);
        copy.ambientSound = source.ambientSound;
        copy.hurtSound = source.hurtSound;
        copy.deathSound = source.deathSound;
        copy.spawnDimension = source.spawnDimension;
        copy.spawnBiome = source.spawnBiome;
        copy.enableAlliances = source.enableAlliances;
        if (source.alliances != null) {
            copy.alliances = source.alliances.stream()
                .map(a -> new MobAllianceData(a.mobId, a.stance))
                .collect(Collectors.toList());
        }
        copy.minionMobType = source.minionMobType;
        copy.minionCount = source.minionCount;
        copy.minionScale = source.minionScale;
        copy.minionIsRanged = source.minionIsRanged;
        copy.minionProjectile = source.minionProjectile;
        copy.enableWereRace = source.enableWereRace;
        copy.wereTriggerCondition = source.wereTriggerCondition;
        copy.wereModelPath = source.wereModelPath;
        copy.wereTexturePath = source.wereTexturePath;
        copy.wereAnimationPath = source.wereAnimationPath;
        copy.wereIdleAnim = source.wereIdleAnim;
        copy.wereWalkAnim = source.wereWalkAnim;
        copy.wereAttackAnim = source.wereAttackAnim;
        copy.wereHeightScale = source.wereHeightScale;
        copy.wereWidthScale = source.wereWidthScale;
        copy.wereHealthBonus = source.wereHealthBonus;
        copy.wereDamageBonus = source.wereDamageBonus;
        copy.wereSpeedBonus = source.wereSpeedBonus;
        copy.werePassiveAbilities = new ArrayList<>(source.werePassiveAbilities);
        copy.wereActiveAbilities = new java.util.HashMap<>(source.wereActiveAbilities);
        copy.wereTransformSound = source.wereTransformSound;
        copy.wereHowlSound = source.wereHowlSound;
        copy.wereAmbientSound = source.wereAmbientSound;
        copy.wereHurtSound = source.wereHurtSound;
        copy.wereDeathSound = source.wereDeathSound;
        if (source.partTransforms != null) {
            source.partTransforms.forEach((k, v) -> {
                PartTransformData pt = new PartTransformData();
                pt.posX = v.posX; pt.posY = v.posY; pt.posZ = v.posZ;
                pt.rotPitch = v.rotPitch; pt.rotYaw = v.rotYaw; pt.rotRoll = v.rotRoll;
                pt.scaleX = v.scaleX; pt.scaleY = v.scaleY; pt.scaleZ = v.scaleZ;
                copy.partTransforms.put(k, pt);
            });
        }
        return copy;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        // 1. Translucent Obsidian Background Canvas
        guiGraphics.fill(0, 0, this.width, this.height, 0xF50B0D12);

        // 2. Left Sidebar Panel (Races List Container)
        int panelX = 8;
        int panelY = 8;
        int panelWidth = 140;
        int panelHeight = this.height - 16;

        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xEE121622);
        guiGraphics.fill(panelX, panelY, panelX + panelWidth, panelY + 16, 0xFF191F30);
        guiGraphics.fill(panelX, panelY + 15, panelX + panelWidth, panelY + 16, 0xFF00CEC9); // Cyan Accent Line
        guiGraphics.drawCenteredString(this.font, "§b❖ RACES ❖", panelX + 70, panelY + 4, 0xFFFFFF);
        guiGraphics.fill(panelX, panelY + panelHeight - 48, panelX + panelWidth, panelY + panelHeight - 47, 0xFF7B61FF); // Top Violet Border Line

        // 3. High-Tech Header Banner (Crimson Theme Shift if editingWereForm is true)
        int contentLeft = 155;
        boolean isWereMode = editingWereForm && workingRace.enableWereRace;
        int headerBg = isWereMode ? 0xFF2A0A0A : 0xFF121520;
        int headerAccent = isWereMode ? 0xFFFF3838 : 0xFF00CEC9;
        String headerTitle = isWereMode ? "§c§l❖ 🐺 WERE-FORM EDITING MODE ❖" : "§9§l❖ §c§lRACE CREATOR ADMIN GUI §9§l❖";

        guiGraphics.fill(contentLeft - 5, 0, this.width, 24, headerBg);
        guiGraphics.fill(contentLeft - 5, 23, this.width, 24, headerAccent);
        guiGraphics.drawString(this.font, headerTitle, contentLeft, 8, 0xFFFFFF);

        // 4. Form Content Main Container Card
        int lastTabY = 28;
        int tX = contentLeft;
        int maxTabX = this.width - 150;
        String[] tabKeys = {
            "gui.customraces.tab.basics", "gui.customraces.tab.model", "gui.customraces.tab.positions",
            "gui.customraces.tab.passives", "gui.customraces.tab.actives", "gui.customraces.tab.sounds",
            "gui.customraces.tab.advanced", "gui.customraces.tab.alliances", "gui.customraces.tab.were_model",
            "gui.customraces.tab.were_sounds", "gui.customraces.tab.drawbacks", "gui.customraces.tab.native_spells"
        };
        for (int i = 0; i < tabKeys.length; i++) {
            if (i == 2 && !"Custom".equalsIgnoreCase(workingRace.modelType)) continue;
            if (i == 7 && !workingRace.enableAlliances) continue;
            if ((i == 8 || i == 9) && !workingRace.enableWereRace) continue;
            String prefix = (editingWereForm || i == 8 || i == 9) ? "🐺 " : "";
            Component tabText = Component.literal(prefix).append(Component.translatable(tabKeys[i]));
            int calcTabWidth = Math.max(52, this.font.width(tabText) + 10);
            if (tX + calcTabWidth > maxTabX) {
                tX = contentLeft;
                lastTabY += 18 + 3;
            }
            tX += calcTabWidth + 3;
        }

        int contentTop = lastTabY + 18 + 14;
        int contentRight = this.width - 145;
        int contentBottom = this.height - 10;
        int cardBorderColor = isWereMode ? 0xFFFF3838 : 0xFF7B61FF;

        guiGraphics.fill(contentLeft - 5, contentTop - 5, contentRight, contentBottom, 0xEE121622);
        guiGraphics.fill(contentLeft - 5, contentTop - 5, contentRight, contentTop - 4, cardBorderColor); // Top Border Line
        guiGraphics.fill(contentLeft - 5, contentBottom - 1, contentRight, contentBottom, cardBorderColor); // Bottom Border Line

        // Form Field Labels with Styled Bullet Points
        if (activeTab == 0) {
            guiGraphics.drawString(this.font, "§b❖ Race Name:", contentLeft, contentTop + 4, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§b❖ Name Color:", contentLeft, contentTop + 27, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§b❖ Difficulty (1-10):", contentLeft, contentTop + 50, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§b❖ Lore Description:", contentLeft, contentTop + 73, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§b❖ Icon Item ID:", contentLeft, contentTop + 96, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§b❖ PNG Picture Path:", contentLeft, contentTop + 119, 0xFFFFFF);
        } else if (activeTab == 1) {
            guiGraphics.drawString(this.font, isWereMode ? "§c❖ Were Height Scale:" : "§e❖ Height Scale:", contentLeft, contentTop + 34, 0xFFFFFF);
            guiGraphics.drawString(this.font, isWereMode ? "§c❖ Were Width Scale:" : "§e❖ Width Scale:", contentLeft, contentTop + 59, 0xFFFFFF);
            guiGraphics.drawString(this.font, isWereMode ? "§c❖ Were HP Bonus:" : "§e❖ Max Health:", contentLeft, contentTop + 84, 0xFFFFFF);
            guiGraphics.drawString(this.font, isWereMode ? "§c❖ Were Speed Bonus:" : "§e❖ Move Speed:", contentLeft, contentTop + 109, 0xFFFFFF);
            if (isWereMode) guiGraphics.drawString(this.font, "§c❖ Were Damage Bonus:", contentLeft, contentTop + 134, 0xFFFFFF);
        } else if (activeTab == 2) {
            guiGraphics.drawString(this.font, "§e❖ Part Scale & Offset (X, Y, Z):", contentLeft, contentTop + 4, 0xFFFFFF);
            String[] partKeys = {"Ears", "Wings", "Tail", "Horns", "Halo", "Custom"};
            int py = contentTop + 20;
            for (String pKey : partKeys) {
                guiGraphics.drawString(this.font, "§e❖ " + pKey + ":", contentLeft, py + 4, 0xCCCCCC);
                py += 24;
            }
        } else if (activeTab == 3) {
            guiGraphics.drawString(this.font, isWereMode ? "§c❖ Toggle Were-Form Granted Passives:" : "§a❖ Toggle Passive Race Abilities:", contentLeft, contentTop + 4, 0xFFFFFF);

            // Render Passives Vertical Scrollbar Track & Thumb
            int visibleTop = contentTop + 24;
            int visibleBottom = this.height - 20;
            int visibleHeight = visibleBottom - visibleTop;
            int totalContentH = matchingPassivesCount * 20;

            if (totalContentH > visibleHeight && visibleHeight > 0) {
                int trackX = contentLeft + 330;
                int trackY = visibleTop;
                int trackW = 6;
                int trackH = visibleHeight;

                // Track Background
                guiGraphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xFF191F30);
                guiGraphics.fill(trackX + 1, trackY + 1, trackX + trackW - 1, trackY + trackH - 1, 0xEE101422);

                // Scrollbar Thumb
                int thumbH = Math.max(16, (visibleHeight * visibleHeight) / totalContentH);
                float maxScroll = totalContentH - visibleHeight;
                int thumbY = trackY + (int) ((trackH - thumbH) * (passivesScrollAmount / maxScroll));
                thumbY = Math.max(trackY, Math.min(trackY + trackH - thumbH, thumbY));

                int thumbColor = isDraggingPassivesScrollbar ? 0xFFFF9900 : 0xFF55FF55;
                guiGraphics.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, thumbColor);
            }
        } else if (activeTab == 4) {
            guiGraphics.drawString(this.font, isWereMode ? "§c❖ Assign Were-Form Active Skills (Slots 1-5):" : "§c❖ Assign Active Skills (Slots 1-5):", contentLeft, contentTop + 4, 0xFFFFFF);
            int py = contentTop + 20;
            for (int slot = 1; slot <= 5; slot++) {
                guiGraphics.drawString(this.font, "§c❖ Slot " + slot + ":", contentLeft, py + 4, 0xCCCCCC);
                py += 24;
            }
        } else if (activeTab == 7) {
            guiGraphics.drawString(this.font, "§b❖ Mob Faction Neutrality Stances:", contentLeft, contentTop + 4, 0xFFFFFF);
        } else if (activeTab == 5) {
            guiGraphics.drawString(this.font, "§d❖ Ambient Sound:", contentLeft, contentTop + 4, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§d❖ Hurt Sound:", contentLeft, contentTop + 34, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§d❖ Death Sound:", contentLeft, contentTop + 64, 0xFFFFFF);
        } else if (activeTab == 6) {
            guiGraphics.drawString(this.font, "§9❖ Spawn Dimension:", contentLeft, contentTop + 4, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§9❖ Spawn Biome:", contentLeft, contentTop + 34, 0xFFFFFF);
        } else if (activeTab == 8) {
            guiGraphics.drawString(this.font, "§c❖ Trigger Condition:", contentLeft, contentTop + 28, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§c❖ Were Geo Model:", contentLeft, contentTop + 50, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§c❖ Were Texture PNG:", contentLeft, contentTop + 72, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§c❖ Were Anim JSON:", contentLeft, contentTop + 94, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§c❖ Idle Animation:", contentLeft, contentTop + 116, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§c❖ Walk Animation:", contentLeft, contentTop + 138, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§c❖ Attack Animation:", contentLeft, contentTop + 160, 0xFFFFFF);
        } else if (activeTab == 9) {
            guiGraphics.drawString(this.font, "§c❖ Transform Sound:", contentLeft, contentTop + 4, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§c❖ Howl Sound:", contentLeft, contentTop + 29, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§c❖ Ambient Sound:", contentLeft, contentTop + 54, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§c❖ Hurt Sound:", contentLeft, contentTop + 79, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§c❖ Death Sound:", contentLeft, contentTop + 104, 0xFFFFFF);
        } else if (activeTab == 10) {
            guiGraphics.drawString(this.font, isWereMode ? "§c❖ Were-Form Race Drawbacks & Weaknesses:" : "§c❖ Race Drawbacks & Weaknesses:", contentLeft, contentTop + 4, 0xFFFFFF);

            // Render Drawbacks Vertical Scrollbar Track & Thumb
            int visibleTop = contentTop + 24;
            int visibleBottom = this.height - 20;
            int visibleHeight = visibleBottom - visibleTop;
            int totalContentH = matchingDrawbacksCount * 20;

            if (totalContentH > visibleHeight && visibleHeight > 0) {
                int trackX = contentLeft + 330;
                int trackY = visibleTop;
                int trackW = 6;
                int trackH = visibleHeight;

                // Track Background
                guiGraphics.fill(trackX, trackY, trackX + trackW, trackY + trackH, 0xFF191F30);
                guiGraphics.fill(trackX + 1, trackY + 1, trackX + trackW - 1, trackY + trackH - 1, 0xEE101422);

                // Scrollbar Thumb
                int thumbH = Math.max(16, (visibleHeight * visibleHeight) / totalContentH);
                float maxScroll = totalContentH - visibleHeight;
                int thumbY = trackY + (int) ((trackH - thumbH) * (drawbacksScrollAmount / maxScroll));
                thumbY = Math.max(trackY, Math.min(trackY + trackH - thumbH, thumbY));

                int thumbColor = isDraggingDrawbacksScrollbar ? 0xFFFF5555 : 0xFFCC3333;
                guiGraphics.fill(trackX, thumbY, trackX + trackW, thumbY + thumbH, thumbColor);
            }
        } else if (activeTab == 11) {
            guiGraphics.drawString(this.font, "§d❖ Native Spells Configuration (Slots 1-5):", contentLeft, contentTop + 4, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§d❖ Spell ID (Slot " + selectedNativeSpellSlot + "):", contentLeft, contentTop + 94, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§d❖ Spell Level (Slot " + selectedNativeSpellSlot + "):", contentLeft, contentTop + 119, 0xFFFFFF);
        }

        // 5. Right Panel: 3D Holographic Showcase Viewport
        int rightLeft = this.width - 140;
        int rightRight = this.width - 8;
        int rightTop = 28;
        int rightBottom = this.height - 10;
        int viewAccent = isWereMode ? 0xFFFF3838 : 0xFF00CEC9;
        String viewTitle = isWereMode ? "§c❖ 🐺 WERE SHOWCASE ❖" : "§b❖ 3D SHOWCASE ❖";

        guiGraphics.fill(rightLeft, rightTop, rightRight, rightBottom, 0xEE101422);
        guiGraphics.fill(rightLeft, rightTop, rightRight, rightTop + 18, 0xFF191F30);
        guiGraphics.fill(rightLeft, rightTop + 17, rightRight, rightTop + 18, viewAccent); // Glowing Line
        guiGraphics.drawCenteredString(this.font, viewTitle, rightLeft + 66, rightTop + 5, 0xFFFFFF);

        if (this.minecraft != null && this.minecraft.player != null) {
            readFormInputs(); // Sync latest form inputs to workingRace

            // Temporarily register workingRace so live preset body parts render on the GUI preview entity
            String prevPlayerRace = RaceRegistry.playerRaces.get(this.minecraft.player.getUUID());
            RaceData prevLoadedRace = RaceRegistry.loadedRaces.get(workingRace.id);

            RaceRegistry.loadedRaces.put(workingRace.id, workingRace);
            RaceRegistry.playerRaces.put(this.minecraft.player.getUUID(), workingRace.id);

            try {
                int previewX = rightLeft + 70;
                int previewY = rightBottom - 18;

                int viewH = rightBottom - (rightTop + 22);
                float totalRaceScale = Math.max(0.2f, workingRace.heightScale * workingRace.baseScale);
                int scale = (int) Math.min(viewH * 0.38f, 32 * totalRaceScale);

                // Enable Scissor to prevent 3D entity from clipping through top title bar or panel edges
                guiGraphics.enableScissor(rightLeft + 2, rightTop + 21, rightRight - 2, rightBottom - 2);

                // Render Holographic Pedestal Ring
                guiGraphics.fill(previewX - 40, previewY - 5, previewX + 40, previewY + 5, 0x3000CEC9);
                guiGraphics.fill(previewX - 30, previewY - 3, previewX + 30, previewY + 3, 0x606C5CE7);

                InventoryScreen.renderEntityInInventoryFollowsMouse(
                        guiGraphics, previewX, previewY, scale,
                        (float)(previewX - mouseX), (float)(previewY - (int)(scale * 0.9f) - mouseY),
                        this.minecraft.player
                );

                guiGraphics.disableScissor();
            } finally {
                // Restore previous race state
                if (prevPlayerRace != null) {
                    RaceRegistry.playerRaces.put(this.minecraft.player.getUUID(), prevPlayerRace);
                } else {
                    RaceRegistry.playerRaces.remove(this.minecraft.player.getUUID());
                }
                if (prevLoadedRace != null) {
                    RaceRegistry.loadedRaces.put(workingRace.id, prevLoadedRace);
                } else {
                    RaceRegistry.loadedRaces.remove(workingRace.id);
                }
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 5. Render Floating Auto-Complete Suggestion Dropdown Box
        updateCurrentSuggestions();
        if (showSuggestions && !activeSuggestions.isEmpty() && activeField != null) {
            int dropX = activeField.getX();
            int dropY = activeField.getY() + activeField.getHeight() + 2;
            int dropW = Math.max(activeField.getWidth(), 200);
            int rowH = 14;
            int maxVisible = 6;
            int visible = Math.min(maxVisible, activeSuggestions.size());
            int dropH = visible * rowH + 4;

            guiGraphics.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xFE12151B);
            guiGraphics.fill(dropX, dropY, dropX + dropW, dropY + 1, 0xFF4A80C0);
            guiGraphics.fill(dropX, dropY + dropH - 1, dropX + dropW, dropY + dropH, 0xFF4A80C0);

            for (int i = 0; i < visible; i++) {
                int idx = i + suggestionsScrollOffset;
                if (idx >= activeSuggestions.size()) break;
                String sugg = activeSuggestions.get(idx);
                int itemY = dropY + 2 + i * rowH;

                boolean isHover = mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= itemY && mouseY <= itemY + rowH;
                if (isHover) {
                    guiGraphics.fill(dropX + 2, itemY, dropX + dropW - 2, itemY + rowH, 0xFF2A364F);
                }

                guiGraphics.drawString(this.font, "§e" + sugg, dropX + 6, itemY + 3, 0xFFFFFF);
            }

            // Render Vertical Scrollbar Track & Thumb
            if (activeSuggestions.size() > maxVisible) {
                int barX = dropX + dropW - 5;
                int barY = dropY + 2;
                int barH = dropH - 4;
                guiGraphics.fill(barX, barY, barX + 3, barY + barH, 0xFF222233);

                float ratio = (float) maxVisible / activeSuggestions.size();
                int thumbH = Math.max(6, (int) (barH * ratio));
                float maxScroll = activeSuggestions.size() - maxVisible;
                int thumbY = barY + (int) ((barH - thumbH) * ((float) suggestionsScrollOffset / maxScroll));

                guiGraphics.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xFF00CEC9);
            }
        }
    }

    private void updateCurrentSuggestions() {
        showSuggestions = false;
        activeSuggestions.clear();

        for (net.minecraft.client.gui.components.events.GuiEventListener child : this.children()) {
            if (child instanceof EditBox box && box.isFocused()) {
                String val = box.getValue().toLowerCase().trim();
                List<String> source = null;

                if (box == ambientSoundBox || box == hurtSoundBox || box == deathSoundBox || box == wereTransformSoundBox || box == wereHowlSoundBox || box == wereAmbientSoundBox || box == wereHurtSoundBox || box == wereDeathSoundBox) {
                    source = RaceRegistry.CACHED_SOUNDS;
                } else if (box == iconBox) {
                    source = RaceRegistry.CACHED_ITEMS;
                } else if (box == customTextureBox) {
                    source = RaceRegistry.CACHED_TEXTURES;
                } else if (box == nameBox) {
                    source = RaceRegistry.CACHED_NAMES;
                } else if (box == nameColorBox) {
                    source = RaceRegistry.CACHED_COLORS;
                } else if (box == spawnDimensionBox) {
                    source = RaceRegistry.CACHED_DIMENSIONS;
                } else if (box == spawnBiomeBox) {
                    source = RaceRegistry.CACHED_BIOMES;
                } else if (box == minionMobTypeBox || box == minionProjectileBox) {
                    source = RaceRegistry.CACHED_PROJECTILES;
                } else if (box == wereModelBox) {
                    source = RaceRegistry.CACHED_WERE_MODELS;
                } else if (box == wereTextureBox) {
                    source = RaceRegistry.CACHED_WERE_TEXTURES;
                } else if (box == wereAnimFileBox) {
                    source = RaceRegistry.CACHED_WERE_ANIMS;
                } else if (box == wereConditionBox) {
                    source = RaceRegistry.CACHED_TRIGGERS;
                } else if (box == wereIdleAnimBox || box == wereWalkAnimBox || box == wereAttackAnimBox || box == wereFlyAnimBox || box == wereSwimAnimBox) {
                    String animPath = (wereAnimFileBox != null && !wereAnimFileBox.getValue().trim().isEmpty()) ? wereAnimFileBox.getValue().trim() : workingRace.wereAnimationPath;
                    source = RaceRegistry.parseAnimationKeysFromFile(animPath);
                } else if (box == flyAnimBox || box == swimAnimBox) {
                    source = RaceRegistry.parseAnimationKeysFromFile(workingRace.wereAnimationPath);
                } else if (box == nativeSpellBox) {
                    source = ddraig.net.customraces.integration.IronSpellsHandler.ALL_SPELLS;
                } else if (box == heightScaleBox || box == widthScaleBox || box == healthBox || box == speedBox || box == difficultyBox || box == minionCountBox || box == minionScaleBox) {
                    source = RaceRegistry.CACHED_NUMBERS;
                } else {
                    source = RaceRegistry.CACHED_ACTIVE_SKILLS;
                }

                if (source != null && !source.isEmpty()) {
                    if (activeField != box) {
                        activeField = box;
                        suggestionsScrollOffset = 0;
                    }
                    final String query = val;
                    activeSuggestions = source.stream()
                            .filter(s -> query.isEmpty()
                                    || s.toLowerCase().contains(query)
                                    || s.replaceAll(".*/", "").replaceAll(".*:", "").toLowerCase().contains(query))
                            .limit(100)
                            .collect(Collectors.toList());
                    showSuggestions = !activeSuggestions.isEmpty();
                }
                break;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (showSuggestions && !activeSuggestions.isEmpty() && activeField != null) {
            int dropX = activeField.getX();
            int dropY = activeField.getY() + activeField.getHeight() + 2;
            int dropW = Math.max(activeField.getWidth(), 200);
            int maxVisible = 6;
            int dropH = Math.min(maxVisible, activeSuggestions.size()) * 14 + 4;

            if (mouseX >= dropX && mouseX <= dropX + dropW + 10 && mouseY >= dropY && mouseY <= dropY + dropH) {
                if (delta < 0) {
                    if (suggestionsScrollOffset + maxVisible < activeSuggestions.size()) {
                        suggestionsScrollOffset++;
                    }
                } else if (delta > 0) {
                    if (suggestionsScrollOffset > 0) {
                        suggestionsScrollOffset--;
                    }
                }
                return true;
            }
        }

        // Left Sidebar Mouse Wheel Scroll
        int panelX = 8;
        int panelY = 8;
        int panelWidth = 140;
        int panelHeight = this.height - 16;
        if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY + 36 && mouseY <= panelY + panelHeight - 48) {
            if (delta < 0) {
                sidebarScrollOffset += 15;
            } else if (delta > 0) {
                sidebarScrollOffset = Math.max(0, sidebarScrollOffset - 15);
            }
            this.init();
            return true;
        }

        // Tab 3 Passives Single Column Mouse Wheel Scroll
        if (activeTab == 3) {
            int visibleTop = getContentTop() + 24;
            int visibleBottom = this.height - 20;
            int visibleHeight = visibleBottom - visibleTop;
            int totalContentH = matchingPassivesCount * 20;
            float maxScroll = Math.max(0, totalContentH - visibleHeight);

            if (mouseX >= getContentLeft() && mouseX <= getContentLeft() + 340 && mouseY >= visibleTop && mouseY <= visibleBottom) {
                if (delta < 0) {
                    passivesScrollAmount = Math.min(maxScroll, passivesScrollAmount + 20);
                } else if (delta > 0) {
                    passivesScrollAmount = Math.max(0, passivesScrollAmount - 20);
                }
                updatePassivesWidgetPositions();
                return true;
            }
        }

        // Tab 10 Drawbacks Single Column Mouse Wheel Scroll
        if (activeTab == 10) {
            int visibleTop = getContentTop() + 24;
            int visibleBottom = this.height - 20;
            int visibleHeight = visibleBottom - visibleTop;
            int totalContentH = matchingDrawbacksCount * 20;
            float maxScroll = Math.max(0, totalContentH - visibleHeight);

            if (mouseX >= getContentLeft() && mouseX <= getContentLeft() + 340 && mouseY >= visibleTop && mouseY <= visibleBottom) {
                if (delta < 0) {
                    drawbacksScrollAmount = Math.min(maxScroll, drawbacksScrollAmount + 20);
                } else if (delta > 0) {
                    drawbacksScrollAmount = Math.max(0, drawbacksScrollAmount - 20);
                }
                updateDrawbacksWidgetPositions();
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (activeTab == 3) {
            int visibleTop = getContentTop() + 24;
            int visibleBottom = this.height - 20;
            int trackX = getContentLeft() + 330;
            if (mouseX >= trackX - 2 && mouseX <= trackX + 10 && mouseY >= visibleTop && mouseY <= visibleBottom) {
                isDraggingPassivesScrollbar = true;
                updatePassivesScrollFromMouse(mouseY);
                return true;
            }
        }

        if (activeTab == 10) {
            int visibleTop = getContentTop() + 24;
            int visibleBottom = this.height - 20;
            int trackX = getContentLeft() + 330;
            if (mouseX >= trackX - 2 && mouseX <= trackX + 10 && mouseY >= visibleTop && mouseY <= visibleBottom) {
                isDraggingDrawbacksScrollbar = true;
                updateDrawbacksScrollFromMouse(mouseY);
                return true;
            }
        }

        if (showSuggestions && !activeSuggestions.isEmpty() && activeField != null) {
            int dropX = activeField.getX();
            int dropY = activeField.getY() + activeField.getHeight() + 2;
            int dropW = Math.max(activeField.getWidth(), 200);
            int rowH = 14;
            int maxVisible = 6;
            int visible = Math.min(maxVisible, activeSuggestions.size());
            int dropH = visible * rowH + 4;

            if (mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= dropY && mouseY <= dropY + dropH) {
                int row = (int) ((mouseY - dropY - 2) / rowH);
                int idx = row + suggestionsScrollOffset;
                if (idx >= 0 && idx < activeSuggestions.size()) {
                    activeField.setValue(activeSuggestions.get(idx));
                    showSuggestions = false;
                    return true;
                }
            }
        }
        showSuggestions = false;
        activeSuggestions.clear();
        activeField = null;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDraggingPassivesScrollbar) {
            updatePassivesScrollFromMouse(mouseY);
            return true;
        }
        if (isDraggingDrawbacksScrollbar) {
            updateDrawbacksScrollFromMouse(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isDraggingPassivesScrollbar = false;
        isDraggingDrawbacksScrollbar = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updatePassivesScrollFromMouse(double mouseY) {
        int visibleTop = getContentTop() + 24;
        int visibleBottom = this.height - 20;
        int visibleHeight = visibleBottom - visibleTop;
        int totalContentH = matchingPassivesCount * 20;
        float maxScroll = Math.max(1, totalContentH - visibleHeight);

        float ratio = (float) (mouseY - visibleTop) / (float) visibleHeight;
        passivesScrollAmount = Math.max(0, Math.min(ratio * maxScroll, maxScroll));
        updatePassivesWidgetPositions();
    }

    private void updateDrawbacksScrollFromMouse(double mouseY) {
        int visibleTop = getContentTop() + 24;
        int visibleBottom = this.height - 20;
        int visibleHeight = visibleBottom - visibleTop;
        int totalContentH = matchingDrawbacksCount * 20;
        float maxScroll = Math.max(1, totalContentH - visibleHeight);

        float ratio = (float) (mouseY - visibleTop) / (float) visibleHeight;
        drawbacksScrollAmount = Math.max(0, Math.min(ratio * maxScroll, maxScroll));
        updateDrawbacksWidgetPositions();
    }

    private void updatePassivesWidgetPositions() {
        int visibleTop = getContentTop() + 24;
        int visibleBottom = this.height - 20;
        int rowHeight = 20;

        for (int i = 0; i < passiveWidgets.size(); i++) {
            Checkbox cb = passiveWidgets.get(i);
            int cbY = visibleTop + (i * rowHeight) - (int) passivesScrollAmount;
            cb.setY(cbY);
            cb.visible = (cbY >= visibleTop - rowHeight && cbY <= visibleBottom);
        }
    }

    private void updateDrawbacksWidgetPositions() {
        int visibleTop = getContentTop() + 24;
        int visibleBottom = this.height - 20;
        int rowHeight = 20;

        for (int i = 0; i < drawbackWidgets.size(); i++) {
            Checkbox cb = drawbackWidgets.get(i);
            int cbY = visibleTop + (i * rowHeight) - (int) drawbacksScrollAmount;
            cb.setY(cbY);
            cb.visible = (cbY >= visibleTop - rowHeight && cbY <= visibleBottom);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
