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
