package ddraig.net.customraces.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Master dictionary and registry providing clean, layman descriptions, category tags,
 * and attribute stats for all 100+ passive abilities and 60+ drawbacks in Custom Races Framework.
 */
public class PassiveAbilityDescriptions {

    public record AbilityInfo(String displayName, String category, String description, String stats) {}

    private static final Map<String, AbilityInfo> DESCRIPTIONS = new HashMap<>();

    static {
        // 1-10: Elemental & Environmental
        add("night_vision", "Night Vision", "Elemental", "Allows you to see clearly in pitch black darkness and underwater.", "+Full Dark Visibility");
        add("water_breathing", "Water Breathing", "Aquatic", "Breathe underwater indefinitely without consuming oxygen.", "+Infinite Oxygen");
        add("fire_resistance", "Fire Resistance", "Thermal", "Immunity to all fire and lava damage, extinguishing flames instantly.", "+Fire/Lava Immunity");
        add("flight", "True Flight", "Movement", "Grants permanent creative-style flight in survival mode.", "+Creative Flight");
        add("slow_falling", "Slow Falling", "Movement", "Glide gently through the air and take zero fall damage upon landing.", "+Gliding & Safe Landing");
        add("lava_swimming", "Lava Swimming", "Thermal", "Swim through lava at full movement speed with complete fire resistance.", "+Lava Agility");
        add("climbing", "Wall Climbing", "Movement", "Scale vertical walls and surfaces freely by walking against them.", "+Spider Climbing");
        add("frost_immunity", "Frost Immunity", "Elemental", "Immune to powdered snow freezing, hypothermia, and ice slowdowns.", "+Freezing Immunity");
        add("lightning_immunity", "Lightning Immunity", "Elemental", "Immune to lightning strikes and electrical shock damage.", "+Lightning Immunity");
        add("poison_immunity", "Poison Immunity", "Resilience", "Complete immunity to poison effects and venomous stings.", "+Poison Immunity");

        // 11-20: Defense & Resilience
        add("regeneration", "Constant Regeneration", "Defense", "Passively restores player health over time automatically.", "+Continuous Healing");
        add("wither_immunity", "Wither Immunity", "Resilience", "Immunity to the deadly Wither effect and Nether decay.", "+Wither Immunity");
        add("fall_damage_immunity", "Fall Damage Immunity", "Resilience", "Take zero damage from falling from any height.", "+Zero Fall Damage");
        add("arrow_deflection", "Arrow Deflection", "Defense", "Deflects incoming projectiles and arrows when attacked.", "+Projectile Shield");
        add("explosion_resistance", "Blast Resistance", "Defense", "Reduces all incoming explosion and TNT damage significantly.", "+Explosion Defense");
        add("magic_resistance", "Magic Resistance", "Defense", "Reduces all magic, potion, and spell damage taken by 50%.", "+50% Spell Resistance");
        add("knockback_immunity", "Knockback Resistance", "Defense", "Stand your ground firmly without being pushed back when attacked.", "+100% Knockback Resist");
        add("thorns_skin", "Spiked Hide", "Defense", "Reflects a portion of melee attack damage back at your attackers.", "+Melee Damage Return");
        add("shield_mastery", "Shield Mastery", "Combat", "Blocking with shields absorbs 100% of damage and prevents shield disabling.", "+Unbreakable Block");
        add("unbreakable_will", "Unbreakable Will", "Resilience", "Immunity to blindness, darkness, confusion, and nausea effects.", "+Mental Fortitude");

        // 21-30: Mobility & Movement
        add("speed_boost", "Swift Movement", "Mobility", "Grants increased baseline movement and sprinting speed.", "+20% Sprint Speed");
        add("high_jump", "High Jump", "Mobility", "Jump significantly higher to easily clear tall blocks and obstacles.", "+2.5 Block Jump Height");
        add("web_walking", "Web Strider", "Mobility", "Walk through cobwebs and sticky blocks with zero slowdown.", "+Web Immunity");
        add("soul_speed", "Soul Strider", "Mobility", "Move at extreme speeds across soul sand and soul soil blocks.", "+Soul Speed Buff");
        add("step_assist", "Step Assist", "Mobility", "Step up full 1.0 block heights smoothly without having to jump.", "+1.0 Step Height");
        add("wall_run", "Wall Running", "Mobility", "Run horizontally along wall surfaces with fluid agility.", "+Wall Agility");
        add("dolphin_grace", "Dolphin Grace", "Aquatic", "Grants extreme swimming speed in oceans, rivers, and deep waters.", "+Dolphin Speed");
        add("feather_weight", "Featherweight", "Mobility", "Greatly reduces player weight, landing softly from jumps and drops.", "+Reduced Gravity");
        add("shadow_dash_passive", "Shadow Dash", "Mobility", "Dash forward in a burst of shadows while sprinting.", "+Sprint Burst");
        add("void_floating", "Void Float", "Mobility", "Levitate gently when falling near the void to recover to safety.", "+Void Protection");

        // 31-40: Combat & Damage
        add("lifesteal", "Vampiric Lifesteal", "Combat", "Restores a portion of your health whenever you deal melee damage.", "+15% Damage Healed");
        add("critical_strike_boost", "Lethal Strikes", "Combat", "Guaranteed bonus critical strike damage on every landed melee blow.", "+25% Crit Damage");
        add("berserk_rage", "Berserker Rage", "Combat", "Deal increased attack damage the lower your health drops in battle.", "+Damage on Low HP");
        add("backstab_bonus", "Shadow Backstab", "Combat", "Deals double damage when striking enemies from behind.", "+100% Rear Damage");
        add("giant_slayer", "Giant Slayer", "Combat", "Deals bonus damage against bosses, golems, and high-health targets.", "+Bonus Boss Damage");
        add("armor_piercing", "Armor Piercing", "Combat", "Attacks bypass a percentage of enemy armor protection directly.", "+Armor Penetration");
        add("execute_passive", "Executioner", "Combat", "Instantly kills non-boss enemies when their health falls below 15%.", "+Low HP Execute");
        add("bleed_on_hit", "Rend & Bleed", "Combat", "Causes struck targets to bleed, suffering damage over time.", "+Bleed Damage Over Time");
        add("counter_attack", "Counter Stance", "Combat", "Chance to retaliate instantly with a free strike when blocking or hit.", "+Retaliation Strike");
        add("dual_wield_mastery", "Dual Wielding", "Combat", "Increases attack speed and damage when holding weapons in both hands.", "+Dual Attack Speed");

        // 41-50: Utility & Gathering
        add("auto_smelt", "Molten Touch", "Gathering", "Automatically smelts mined ores into ingots and blocks instantly.", "+Instant Smelting");
        add("double_mining_drops", "Fortune Miner", "Gathering", "Chance to yield double drops and gems when mining valuable ores.", "+Bonus Ore Drops");
        add("magnet_aura", "Item Magnetism", "Utility", "Pulls nearby dropped items gently toward you within a 4-block radius.", "+4 Block Item Vacuum");
        add("luck_of_the_sea", "Master Angler", "Utility", "Increased fishing luck and treasure chances when fishing in water.", "+High Fishing Luck");
        add("haste_passive", "Permanent Haste", "Utility", "Increases mining and tool swing speed continuously.", "+Haste Mining Buff");
        add("night_miner", "Night Miner", "Gathering", "Mining speed is doubled during the night and deep underground.", "+2x Night Mine Speed");
        add("silk_touch_hands", "Delicate Hands", "Gathering", "Mine blocks with natural silk touch without requiring tool enchantments.", "+Innate Silk Touch");
        add("xp_boost", "Enlightened Mind", "Utility", "Gain 50% more experience orbs from all kills, ores, and activities.", "+50% Bonus EXP");
        add("hunger_less_drain", "Slow Metabolism", "Utility", "Reduces hunger and food saturation depletion rate by half.", "-50% Hunger Drain");
        add("saturation_regen", "Nutrient Synergy", "Utility", "Naturally regenerates food saturation slowly over time.", "+Passive Food Regen");

        // 51-60: Magic & Spectral
        add("mana_regen_boost", "Arcane Focus", "Magic", "Increases mana regeneration speed when using magic mods.", "+50% Mana Recovery");
        add("spell_power_boost", "Spell Mastery", "Magic", "Empowers all spell and magic damage dealt to enemies.", "+25% Spell Damage");
        add("cooldown_reduction", "Rapid Casting", "Magic", "Reduces all ability, spell, and skill cooldown durations by 20%.", "-20% Cooldowns");
        add("arcane_shield", "Arcane Ward", "Magic", "Absorbs incoming magic attacks and converts them into shield energy.", "+Magic Absorption");
        add("astral_projection", "Astral Form", "Magic", "Ghostly form that passes through entities with enhanced dodge chance.", "+Evasion Chance");
        add("spectral_glowing", "Spectral Sight", "Magic", "Highlights nearby living entities through walls with a glowing outline.", "+Wall Glow Radar");
        add("invisibility_in_shadows", "Shadow Cloak", "Magic", "Become completely invisible while standing in dark areas or shadows.", "+Shadow Stealth");
        add("telepathic_aura", "Empathic Link", "Magic", "Detects nearby monsters and warns of ambushes through sensory aura.", "+Mob Detection");
        add("elemental_affinity", "Primal Affinity", "Magic", "Grants resistance to elemental damage types and boosts spell powers.", "+Elemental Boost");
        add("native_spell", "Innate Magic", "Magic", "Enables casting of native Iron's Spells bound to your race ability keys.", "+Spellcasting");

        // 61-70: Vampiric & Nether
        add("vampiric_bite_regen", "Blood Feast", "Vampiric", "Restores substantial health and hunger when eliminating living foes.", "+Health on Kill");
        add("sunlight_evasion", "Nocturnal Speed", "Vampiric", "Move and attack faster during nighttime and darkness.", "+Night Combat Buff");
        add("nether_affinity", "Hellforged", "Nether", "Gain bonus strength, speed, and defense while in the Nether dimension.", "+Nether Dimension Buff");
        add("wither_touch", "Decay Touch", "Nether", "Melee attacks inflict the lethal Wither decay effect on targets.", "+Wither Infliction");
        add("shadow_healing", "Gloom Siphon", "Vampiric", "Heal rapidly when standing in dark caves or shadowed locations.", "+Darkness Healing");
        add("soul_collector", "Soul Harvester", "Nether", "Collects souls from defeated foes to power dark abilities.", "+Soul Gathering");
        add("blood_essence_pool", "Blood Reservoir", "Vampiric", "Stores blood essence to prevent lethal damage once every 5 minutes.", "+Lethal Prevention");
        add("demon_flame_aura", "Hellfire Aura", "Nether", "Ignites nearby hostile monsters that come within melee range.", "+Proximity Ignite");
        add("hellfire_immunity", "Abyssal Flame", "Nether", "Total immunity to soul fire, hellfire, and blazing projectiles.", "+Hellfire Immunity");
        add("abyssal_resilience", "Abyssal Armor", "Nether", "High natural armor toughness forged from Netherite density.", "+Armor Toughness");

        // 71-80: Celestial & Divine
        add("divine_aura", "Sacred Presence", "Divine", "Heals nearby friendly players and pacifies neutral creatures.", "+Group Healing Aura");
        add("angel_wings_passive", "Seraphic Wings", "Divine", "Gently flaps ethereal wings to glide, hover, and avoid fall damage.", "+Glide & Hover");
        add("holy_damage_boost", "Smite of Light", "Divine", "Deals massively increased damage against Undead and Nether monsters.", "+50% Undead Damage");
        add("undead_bane_aura", "Radiant Repulsion", "Divine", "Undead monsters caught near you suffer continuous radiant burn.", "+Undead Aura Burn");
        add("solar_charging", "Solar Infusion", "Divine", "Bathe in sunlight to regenerate health, hunger, and ability energy.", "+Sunlight Regen");
        add("lunar_power_boost", "Moonlight Blessing", "Divine", "Empowers combat damage and movement speed under the moonlight.", "+Moonlight Buff");
        add("radiant_light", "Halo of Radiance", "Divine", "Emits bright ambient light around the player, illuminating caves.", "+Mobile Light Source");
        add("blessing_of_protection", "Guardian Ward", "Divine", "Grants temporary resistance shields when health drops below 30%.", "+Emergency Shield");
        add("grace_of_the_gods", "Divine Grace", "Divine", "Immunity to negative curse and hex potion effects.", "+Curse Immunity");
        add("sanctuary_field", "Holy Sanctuary", "Divine", "Creates a protective holy barrier that repels enemy projectiles.", "+Barrier Ward");

        // 81-90: Draconic & Beast
        add("dragon_scales", "Draconic Scales", "Draconic", "Provides heavy natural armor points and high knockback resistance.", "+4 Natural Armor");
        add("beast_instincts", "Beast Instincts", "Beast", "Increases sprint acceleration, jump agility, and melee attack speed.", "+Beast Agility");
        add("pack_leader_buff", "Alpha Call", "Beast", "Allied wolves and tamed animals gain bonus damage and health near you.", "+Pet Damage Buff");
        add("natural_armor", "Tough Fur / Hide", "Beast", "Thick hide cushions physical melee attacks and fall impacts.", "+2 Natural Armor");
        add("scent_tracking", "Scent Tracking", "Beast", "Tracks low-health entities nearby through scent radar trail.", "+Scent Tracking");
        add("intimidating_presence", "Apex Roar", "Beast", "Causes weaker hostile monsters to hesitate or flee before attacking.", "+Monster Flee");
        add("tail_sweep_passive", "Tail Sweep", "Beast", "Sweeping melee attacks strike all surrounding enemies in a 360 circle.", "+360 Sweep Attack");
        add("predator_stealth", "Crouch Stalker", "Beast", "Sneaking makes your footsteps 100% silent and avoids mob detection.", "+Silent Sneaking");
        add("thick_hide", "Reinforced Hide", "Beast", "Reduces piercing and physical melee damage taken by 25%.", "+25% Physical Defense");
        add("wild_regeneration", "Feral Vigor", "Beast", "Rapidly regenerates health when full on hunger outside of combat.", "+Fast Out-of-Combat Heal");

        // 91-100: Tech & Golem
        add("nanite_repair", "Automated Repair", "Tech", "Passively repairs held tools and equipped armor over time.", "+Item Mending");
        add("kinetic_absorption", "Kinetic Armor", "Tech", "Converts absorbed physical attack kinetic energy into bonus speed.", "+Kinetic Speed");
        add("thermal_regulation", "Climate Regulator", "Tech", "Immune to both extreme heat/fire and extreme freezing temperatures.", "+Climate Immunity");
        add("cybernetic_sight", "HUD Scanner", "Tech", "Displays target health, distance, and armor stats in your HUD view.", "+Entity Scanner");
        add("forcefield_barrier", "Plasma Shield", "Tech", "Deploys a personal energy forcefield that absorbs initial hits.", "+Energy Shield");
        add("overclock_speed", "Overclocked Servos", "Tech", "Sprint speed and attack speed are boosted by 30%.", "+30% Attack/Move Speed");
        add("golem_density", "Heavy Golem Frame", "Tech", "Immune to all knockback and drowning, but sinks quickly in liquids.", "+Heavy Golem Mass");
        add("magnetic_repulsion", "Magnetic Repulsion", "Tech", "Repels metallic arrows and incoming hostile projectiles away.", "+Deflect Metal Arrows");
        add("radiation_immunity", "Hazmat Shielding", "Tech", "Complete immunity to radiation, poisonous clouds, and wither gas.", "+Radiation Immunity");
        add("energy_core_boost", "Fusion Reactor", "Tech", "Empowers all technological energy and electric skills with double power.", "+Tech Damage Buff");

        // Key Drawbacks & Weaknesses
        add("hydrophobic", "Water Vulnerability", "Weakness", "Suffers continuous damage when touching water or exposed to rain.", "Damaged by Water");
        add("water_vulnerability", "Water Vulnerability", "Weakness", "Takes drowning damage when standing in water or rain.", "Damaged by Water");
        add("sunlight_burn", "Sunlight Burn", "Weakness", "Catches fire and burns when exposed to direct sunlight without a helmet.", "Burns in Sunlight");
        add("fragile_bones", "Fragile Bones", "Weakness", "Takes 50% increased fall damage and suffers slowness when falling.", "+50% Fall Damage");
        add("carnivore_diet", "Strict Carnivore", "Diet", "Cannot digest plant foods, vegetables, or breads; only meat restores hunger.", "Meat Only");
        add("herbivore_diet", "Strict Herbivore", "Diet", "Cannot digest meat or poultry; only fruits, vegetables, and crops restore hunger.", "Plants Only");
        add("iron_allergy", "Iron Allergy", "Weakness", "Cannot wield iron weapons or wear iron armor; suffers poison if holding iron.", "No Iron Equipment");
        add("gold_greed", "Gold Addiction", "Weakness", "Takes damage over time unless carrying gold, emeralds, or diamonds in inventory.", "Greed Curse");
        add("desert_dehydration", "Desert Dehydration", "Weakness", "Loses food and water rapidly while sprinting in hot desert and badland biomes.", "Desert Hunger Drain");
        add("snow_hypothermia", "Snow Hypothermia", "Weakness", "Suffers intense slowness and freezes rapidly in snowy and icy biomes.", "Cold Vulnerability");
        add("no_heavy_armor", "No Heavy Armor", "Restriction", "Cannot equip Iron, Diamond, or Netherite armor pieces.", "Light Armor Only");
        add("no_shield", "No Shield", "Restriction", "Cannot wield shields or block incoming attacks.", "No Shields");
        add("no_bow", "No Bow", "Restriction", "Cannot shoot bows or crossbows.", "No Ranged Bows");
        add("no_trident", "No Trident", "Restriction", "Cannot wield or throw tridents.", "No Tridents");
    }

    private static void add(String id, String displayName, String category, String description, String stats) {
        DESCRIPTIONS.put(id.toLowerCase(), new AbilityInfo(displayName, category, description, stats));
    }

    public static AbilityInfo get(String id) {
        if (id == null) return null;
        String cleanId = id.trim().toLowerCase();
        AbilityInfo info = DESCRIPTIONS.get(cleanId);
        if (info != null) return info;

        // Fallback: format name cleanly
        String prettyName = formatName(cleanId);
        return new AbilityInfo(prettyName, "General", "A passive racial ability or trait.", "+Racial Trait");
    }

    public static String getDisplayName(String id) {
        AbilityInfo info = get(id);
        return info != null ? info.displayName() : formatName(id);
    }

    public static String getDescription(String id) {
        AbilityInfo info = get(id);
        return info != null ? info.description() : "A passive racial ability.";
    }

    public static String getStats(String id) {
        AbilityInfo info = get(id);
        return info != null ? info.stats() : "";
    }

    public static String getCategory(String id) {
        AbilityInfo info = get(id);
        return info != null ? info.category() : "General";
    }

    private static String formatName(String id) {
        if (id == null || id.isEmpty()) return "Unknown";
        String[] parts = id.replace("customraces:", "").replace("_", " ").split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
