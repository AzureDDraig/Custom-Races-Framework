package ddraig.net.customraces.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main data structure for race configurations in Custom Races Framework.
 */
public class RaceData {
    public String id = "human";
    public String name = "Human";
    public String nameColor = "#FFAA00"; // Custom RGB Hex color for race name tag/title
    public String customTexture = "";   // Path to PNG picture (e.g. customraces:textures/gui/races/elf.png)
    public String lore = "Versatile and adaptable species with balanced attributes.";
    public String iconItem = "minecraft:player_head";
    public int playstyleDifficulty = 1; // 1 (Easy) to 10 (Insane)
    public String modelType = "Default"; // "Default" or "Custom"

    // Pehkui Model Scales
    public float baseScale = 1.0f;
    public float heightScale = 1.0f;
    public float widthScale = 1.0f;
    public float reachScale = 1.0f;
    public float speedScale = 1.0f;
    public float stepHeightScale = 1.0f;
    public float headScale = 1.0f;

    // Base Stat Modifiers
    public float maxHealth = 20.0f;
    public float armor = 0.0f;
    public float attackDamage = 1.0f;
    public float movementSpeed = 0.1f;
    public float attackReach = 3.0f;

    // Selective Armor Piece Hiding Checkboxes (false = showing/enabled, true = hidden)
    public boolean hideHelmet = false;
    public boolean hideChestplate = false;
    public boolean hideLeggings = false;
    public boolean hideBoots = false;

    // Preset Body Parts
    public String earType = "none";     // none, dog, cat, dragon, bunny
    public String wingType = "none";    // none, dragon, feathered
    public String tailType = "none";    // none, dragon, dog, cat, camel, fish
    public String hornType = "none";    // none, demon, ram, dragon, unicorn
    public String haloType = "none";    // none, angel, demon, flower
    public String legType = "human";    // human, spider, centaur
    public int legCount = 2;            // 0 to 8 legs
    public String customPartId = "none";

    // RGB Hex Colors for Body Parts ("#RRGGBB")
    public Map<String, String> bodyPartColors = new HashMap<>();

    // Part Transforms (Position, Rotation, Scale per part key e.g. "ears", "wings", "tail", "horns", "halo", "legs", "custom")
    public Map<String, PartTransformData> partTransforms = new HashMap<>();

    // Particle Auras
    public List<ParticleAuraData> particleAuras = new ArrayList<>();

    // Sound FX
    public String ambientSound = "";
    public String hurtSound = "";
    public String deathSound = "";
    public String jumpSound = "";
    public String abilitySound = "";

    // Advanced Settings
    public String spawnDimension = ""; // e.g. minecraft:overworld, minecraft:the_nether, minecraft:the_end
    public String spawnBiome = "";     // e.g. minecraft:ocean, minecraft:plains
    public List<String> itemWhitelist = new ArrayList<>();
    public List<String> itemBlacklist = new ArrayList<>();
    public String permissionLock = "";
    public boolean enableAlliances = false;

    // Mob Alliances
    public List<MobAllianceData> alliances = new ArrayList<>();

    // Abilities
    public List<String> passiveAbilities = new ArrayList<>();
    public Map<Integer, String> activeAbilities = new HashMap<>(); // Keybind Slot (1 to 5) -> Ability ID
    public List<String> drawbacks = new ArrayList<>();

    // Were-Race Transformation System
    public boolean enableWereRace = false;
    public String wereTriggerCondition = "FULL_MOON"; // FULL_MOON, NEW_MOON, NIGHT, MANUAL
    public String wereModelPath = "";      // e.g. customraces:models/were/werewolf.geo.json
    public String wereTexturePath = "";    // e.g. customraces:textures/were/werewolf.png
    public String wereAnimationPath = "";  // e.g. customraces:animations/were/werewolf.animation.json
    public String wereIdleAnim = "animation.were.idle";
    public String wereWalkAnim = "animation.were.walk";
    public String wereAttackAnim = "animation.were.attack";
    public String wereTransformAnim = "animation.were.transform";
    public String wereHowlAnim = "animation.were.howl";
    public float wereHeightScale = 1.3f;
    public float wereWidthScale = 1.3f;
    public float wereHealthBonus = 10.0f;
    public float wereSpeedBonus = 0.05f;
    public float wereDamageBonus = 4.0f;
    public String wereTransformSound = "minecraft:entity.wolf.howl";
    public String wereHowlSound = "minecraft:entity.wolf.howl";
    public String wereAmbientSound = "minecraft:entity.wolf.growl";
    public String wereHurtSound = "minecraft:entity.wolf.hurt";
    public String CustomDeathSound = "";
    public String wereDeathSound = "minecraft:entity.wolf.death";

    // Were-Form Specific Granted Abilities & Drawbacks
    public List<String> werePassiveAbilities = new ArrayList<>();
    public Map<Integer, String> wereActiveAbilities = new HashMap<>();
    public List<String> wereDrawbacks = new ArrayList<>();

    // Minion Summon Ability Settings
    public String minionMobType = "minecraft:zombie";
    public int minionCount = 2;
    public float minionScale = 1.0f;
    public boolean minionIsRanged = false;
    public String minionProjectile = "minecraft:arrow";

    // Native Spell Integration (Iron's Spells 'n Spellbooks & T.O Tweaks)
    // Native Spell Integration (Iron's Spells 'n Spellbooks & T.O Tweaks) - Slots 1 to 5
    public boolean enableNativeSpells = false;
    public String nativeSpellId = "irons_spellbooks:fireball";
    public boolean wildMagic = false;
    public int nativeSpellLevel = 1;
    public int nativeSpellCooldown = 100;
    public int nativeSpellManaCost = 25;

    public String nativeSpellId1 = "irons_spellbooks:fireball";
    public boolean wildMagic1 = false;
    public int nativeSpellLevel1 = 1;

    public String nativeSpellId2 = "irons_spellbooks:lightning_lance";
    public boolean wildMagic2 = false;
    public int nativeSpellLevel2 = 1;

    public String nativeSpellId3 = "irons_spellbooks:frostbite";
    public boolean wildMagic3 = false;
    public int nativeSpellLevel3 = 1;

    public String nativeSpellId4 = "irons_spellbooks:starfall";
    public boolean wildMagic4 = false;
    public int nativeSpellLevel4 = 1;

    public String nativeSpellId5 = "totweaks:time_stop";
    public boolean wildMagic5 = false;
    public int nativeSpellLevel5 = 1;

    public boolean enableWereNativeSpells = false;
    public String wereNativeSpellId = "irons_spellbooks:blood_slash";
    public boolean wereWildMagic = false;
    public int wereNativeSpellLevel = 2;
    public int wereNativeSpellCooldown = 60;
    public int wereNativeSpellManaCost = 20;

    public String wereNativeSpellId1 = "irons_spellbooks:blood_slash";
    public boolean wereWildMagic1 = false;
    public int wereNativeSpellLevel1 = 2;

    public String wereNativeSpellId2 = "irons_spellbooks:fire_breath";
    public boolean wereWildMagic2 = false;
    public int wereNativeSpellLevel2 = 2;

    public String wereNativeSpellId3 = "irons_spellbooks:icicle";
    public boolean wereWildMagic3 = false;
    public int wereNativeSpellLevel3 = 2;

    public String wereNativeSpellId4 = "irons_spellbooks:black_hole";
    public boolean wereWildMagic4 = false;
    public int wereNativeSpellLevel4 = 2;

    public String wereNativeSpellId5 = "totweaks:spatial_rend";
    public boolean wereWildMagic5 = false;
    public int wereNativeSpellLevel5 = 2;

    public String getNativeSpellId(int slot, boolean isWere) {
        if (isWere) {
            switch (slot) {
                case 2: return (wereNativeSpellId2 != null && !wereNativeSpellId2.isEmpty()) ? wereNativeSpellId2 : "irons_spellbooks:fire_breath";
                case 3: return (wereNativeSpellId3 != null && !wereNativeSpellId3.isEmpty()) ? wereNativeSpellId3 : "irons_spellbooks:icicle";
                case 4: return (wereNativeSpellId4 != null && !wereNativeSpellId4.isEmpty()) ? wereNativeSpellId4 : "irons_spellbooks:black_hole";
                case 5: return (wereNativeSpellId5 != null && !wereNativeSpellId5.isEmpty()) ? wereNativeSpellId5 : "totweaks:spatial_rend";
                default: return (wereNativeSpellId1 != null && !wereNativeSpellId1.isEmpty()) ? wereNativeSpellId1 : (wereNativeSpellId != null ? wereNativeSpellId : "irons_spellbooks:blood_slash");
            }
        } else {
            switch (slot) {
                case 2: return (nativeSpellId2 != null && !nativeSpellId2.isEmpty()) ? nativeSpellId2 : "irons_spellbooks:lightning_lance";
                case 3: return (nativeSpellId3 != null && !nativeSpellId3.isEmpty()) ? nativeSpellId3 : "irons_spellbooks:frostbite";
                case 4: return (nativeSpellId4 != null && !nativeSpellId4.isEmpty()) ? nativeSpellId4 : "irons_spellbooks:starfall";
                case 5: return (nativeSpellId5 != null && !nativeSpellId5.isEmpty()) ? nativeSpellId5 : "totweaks:time_stop";
                default: return (nativeSpellId1 != null && !nativeSpellId1.isEmpty()) ? nativeSpellId1 : (nativeSpellId != null ? nativeSpellId : "irons_spellbooks:fireball");
            }
        }
    }

    public boolean getWildMagic(int slot, boolean isWere) {
        if (isWere) {
            switch (slot) {
                case 2: return wereWildMagic2;
                case 3: return wereWildMagic3;
                case 4: return wereWildMagic4;
                case 5: return wereWildMagic5;
                default: return wereWildMagic1 || wereWildMagic;
            }
        } else {
            switch (slot) {
                case 2: return wildMagic2;
                case 3: return wildMagic3;
                case 4: return wildMagic4;
                case 5: return wildMagic5;
                default: return wildMagic1 || wildMagic;
            }
        }
    }

    public int getNativeSpellLevel(int slot, boolean isWere) {
        if (isWere) {
            switch (slot) {
                case 2: return wereNativeSpellLevel2 > 0 ? wereNativeSpellLevel2 : 2;
                case 3: return wereNativeSpellLevel3 > 0 ? wereNativeSpellLevel3 : 2;
                case 4: return wereNativeSpellLevel4 > 0 ? wereNativeSpellLevel4 : 2;
                case 5: return wereNativeSpellLevel5 > 0 ? wereNativeSpellLevel5 : 2;
                default: return wereNativeSpellLevel1 > 0 ? wereNativeSpellLevel1 : (wereNativeSpellLevel > 0 ? wereNativeSpellLevel : 2);
            }
        } else {
            switch (slot) {
                case 2: return nativeSpellLevel2 > 0 ? nativeSpellLevel2 : 1;
                case 3: return nativeSpellLevel3 > 0 ? nativeSpellLevel3 : 1;
                case 4: return nativeSpellLevel4 > 0 ? nativeSpellLevel4 : 1;
                case 5: return nativeSpellLevel5 > 0 ? nativeSpellLevel5 : 1;
                default: return nativeSpellLevel1 > 0 ? nativeSpellLevel1 : (nativeSpellLevel > 0 ? nativeSpellLevel : 1);
            }
        }
    }

    public RaceData() {
        initDefaults();
    }

    public RaceData(String id, String name) {
        this.id = id;
        this.name = name;
        initDefaults();
    }

    public void initDefaults() {
        if (!bodyPartColors.containsKey("ears")) bodyPartColors.put("ears", "#FFFFFF");
        if (!bodyPartColors.containsKey("wings")) bodyPartColors.put("wings", "#FFFFFF");
        if (!bodyPartColors.containsKey("tail")) bodyPartColors.put("tail", "#FFFFFF");
        if (!bodyPartColors.containsKey("horns")) bodyPartColors.put("horns", "#FFFFFF");
        if (!bodyPartColors.containsKey("halo")) bodyPartColors.put("halo", "#FFFFFF");
        if (!bodyPartColors.containsKey("legs")) bodyPartColors.put("legs", "#FFFFFF");
        if (!bodyPartColors.containsKey("custom")) bodyPartColors.put("custom", "#FFFFFF");

        if (!partTransforms.containsKey("ears")) partTransforms.put("ears", new PartTransformData());
        if (!partTransforms.containsKey("wings")) partTransforms.put("wings", new PartTransformData());
        if (!partTransforms.containsKey("tail")) partTransforms.put("tail", new PartTransformData());
        if (!partTransforms.containsKey("horns")) partTransforms.put("horns", new PartTransformData());
        if (!partTransforms.containsKey("halo")) partTransforms.put("halo", new PartTransformData());
        if (!partTransforms.containsKey("legs")) partTransforms.put("legs", new PartTransformData());
        if (!partTransforms.containsKey("custom")) partTransforms.put("custom", new PartTransformData());
    }

    public PartTransformData getTransform(String partKey) {
        return partTransforms.computeIfAbsent(partKey, k -> new PartTransformData());
    }

    public String getColor(String partKey) {
        return bodyPartColors.getOrDefault(partKey, "#FFFFFF");
    }

    public void setColor(String partKey, String hexColor) {
        bodyPartColors.put(partKey, hexColor);
    }
}
