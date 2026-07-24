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

    // Particle Configurations
    public int particleCount = 5;      // Base form ambient particle emission rate (Default: 5)
    public int wereParticleCount = 10;  // Were-form ambient particle emission rate (Default: 10)

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

    // Flying & Swimming Custom Model Animations
    public boolean isFlyingRace = false;
    public String flyAnim = "animation.model.fly";
    public String swimAnim = "animation.model.swim";

    // Were-Race Transformation System
    public boolean enableWereRace = false;
    public boolean isWereFlyingRace = false;
    public String wereTriggerCondition = "FULL_MOON"; // FULL_MOON, NEW_MOON, NIGHT, MANUAL
    public String wereModelPath = "";      // e.g. customraces:models/were/werewolf.geo.json
    public String wereTexturePath = "";    // e.g. customraces:textures/were/werewolf.png
    public String wereAnimationPath = "";  // e.g. customraces:animations/were/werewolf.animation.json
    public String wereIdleAnim = "animation.were.idle";
    public String wereWalkAnim = "animation.were.walk";
    public String wereAttackAnim = "animation.were.attack";
    public String wereFlyAnim = "animation.were.fly";
    public String wereSwimAnim = "animation.were.swim";
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
    public String nativeSpellId = "";
    public boolean wildMagic = false;
    public int nativeSpellLevel = 1;
    public int nativeSpellCooldown = 100;
    public int nativeSpellManaCost = 25;

    public String nativeSpellId1 = "";
    public boolean wildMagic1 = false;
    public int nativeSpellLevel1 = 1;

    public String nativeSpellId2 = "";
    public boolean wildMagic2 = false;
    public int nativeSpellLevel2 = 1;

    public String nativeSpellId3 = "";
    public boolean wildMagic3 = false;
    public int nativeSpellLevel3 = 1;

    public String nativeSpellId4 = "";
    public boolean wildMagic4 = false;
    public int nativeSpellLevel4 = 1;

    public String nativeSpellId5 = "";
    public boolean wildMagic5 = false;
    public int nativeSpellLevel5 = 1;

    public boolean enableWereNativeSpells = false;
    public String wereNativeSpellId = "";
    public boolean wereWildMagic = false;
    public int wereNativeSpellLevel = 2;
    public int wereNativeSpellCooldown = 60;
    public int wereNativeSpellManaCost = 20;

    public String wereNativeSpellId1 = "";
    public boolean wereWildMagic1 = false;
    public int wereNativeSpellLevel1 = 2;

    public String wereNativeSpellId2 = "";
    public boolean wereWildMagic2 = false;
    public int wereNativeSpellLevel2 = 2;

    public String wereNativeSpellId3 = "";
    public boolean wereWildMagic3 = false;
    public int wereNativeSpellLevel3 = 2;

    public String wereNativeSpellId4 = "";
    public boolean wereWildMagic4 = false;
    public int wereNativeSpellLevel4 = 2;

    public String wereNativeSpellId5 = "";
    public boolean wereWildMagic5 = false;
    public int wereNativeSpellLevel5 = 2;

    public String getNativeSpellId(int slot, boolean isWere) {
        if (isWere) {
            switch (slot) {
                case 2: return wereNativeSpellId2 != null ? wereNativeSpellId2 : "";
                case 3: return wereNativeSpellId3 != null ? wereNativeSpellId3 : "";
                case 4: return wereNativeSpellId4 != null ? wereNativeSpellId4 : "";
                case 5: return wereNativeSpellId5 != null ? wereNativeSpellId5 : "";
                default: return (wereNativeSpellId1 != null && !wereNativeSpellId1.isEmpty()) ? wereNativeSpellId1 : (wereNativeSpellId != null ? wereNativeSpellId : "");
            }
        } else {
            switch (slot) {
                case 2: return nativeSpellId2 != null ? nativeSpellId2 : "";
                case 3: return nativeSpellId3 != null ? nativeSpellId3 : "";
                case 4: return nativeSpellId4 != null ? nativeSpellId4 : "";
                case 5: return nativeSpellId5 != null ? nativeSpellId5 : "";
                default: return (nativeSpellId1 != null && !nativeSpellId1.isEmpty()) ? nativeSpellId1 : (nativeSpellId != null ? nativeSpellId : "");
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

        if (flyAnim == null) flyAnim = "animation.model.fly";
        if (swimAnim == null) swimAnim = "animation.swim";
        if (wereFlyAnim == null) wereFlyAnim = "animation.were.fly";
        if (wereSwimAnim == null) wereSwimAnim = "animation.were.swim";
        if (spawnDimension == null) spawnDimension = "";
        if (spawnBiome == null) spawnBiome = "";
        if (particleCount <= 0) particleCount = 5;
        if (wereParticleCount <= 0) wereParticleCount = 10;
        enableNativeSpells = true;
        enableWereNativeSpells = true;
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

    public int getParticleCount() {
        return particleCount <= 0 ? 5 : particleCount;
    }

    public void setParticleCount(int particleCount) {
        this.particleCount = particleCount <= 0 ? 5 : Math.min(100, particleCount);
    }

    public int getWereParticleCount() {
        return wereParticleCount <= 0 ? 10 : wereParticleCount;
    }

    public void setWereParticleCount(int wereParticleCount) {
        this.wereParticleCount = wereParticleCount <= 0 ? 10 : Math.min(100, wereParticleCount);
    }

    public net.minecraft.nbt.CompoundTag toNBT(net.minecraft.nbt.CompoundTag tag) {
        if (tag == null) tag = new net.minecraft.nbt.CompoundTag();
        tag.putString("id", id != null ? id : "human");
        tag.putString("name", name != null ? name : "Human");
        tag.putString("lore", lore != null ? lore : "");
        tag.putString("modelType", modelType != null ? modelType : "Default");
        tag.putFloat("heightScale", heightScale);
        tag.putFloat("widthScale", widthScale);
        tag.putFloat("baseScale", baseScale);
        tag.putFloat("reachScale", reachScale);
        tag.putFloat("stepHeightScale", stepHeightScale);
        tag.putFloat("maxHealth", maxHealth);
        tag.putFloat("movementSpeed", movementSpeed);
        tag.putFloat("armor", armor);
        tag.putFloat("attackDamage", attackDamage);
        tag.putBoolean("isFlyingRace", isFlyingRace);
        tag.putInt("particleCount", getParticleCount());
        tag.putInt("wereParticleCount", getWereParticleCount());

        tag.putBoolean("enableWereRace", enableWereRace);
        tag.putBoolean("isWereFlyingRace", isWereFlyingRace);
        tag.putString("wereTriggerCondition", wereTriggerCondition != null ? wereTriggerCondition : "FULL_MOON");
        tag.putString("wereModelPath", wereModelPath != null ? wereModelPath : "");
        tag.putString("wereTexturePath", wereTexturePath != null ? wereTexturePath : "");
        tag.putString("wereAnimationPath", wereAnimationPath != null ? wereAnimationPath : "");
        tag.putFloat("wereHeightScale", wereHeightScale);
        tag.putFloat("wereWidthScale", wereWidthScale);
        tag.putFloat("wereHealthBonus", wereHealthBonus);
        tag.putFloat("wereSpeedBonus", wereSpeedBonus);
        tag.putFloat("wereDamageBonus", wereDamageBonus);
        tag.putString("wereTransformSound", wereTransformSound != null ? wereTransformSound : "");
        tag.putString("wereHowlSound", wereHowlSound != null ? wereHowlSound : "");
        tag.putString("wereAmbientSound", wereAmbientSound != null ? wereAmbientSound : "");
        tag.putString("wereHurtSound", wereHurtSound != null ? wereHurtSound : "");
        tag.putString("wereDeathSound", wereDeathSound != null ? wereDeathSound : "");
        tag.putString("CustomDeathSound", CustomDeathSound != null ? CustomDeathSound : "");

        tag.putBoolean("enableNativeSpells", enableNativeSpells);
        tag.putString("nativeSpellId", nativeSpellId != null ? nativeSpellId : "");
        tag.putString("nativeSpellId1", nativeSpellId1 != null ? nativeSpellId1 : "");
        tag.putString("nativeSpellId2", nativeSpellId2 != null ? nativeSpellId2 : "");
        tag.putString("nativeSpellId3", nativeSpellId3 != null ? nativeSpellId3 : "");
        tag.putString("nativeSpellId4", nativeSpellId4 != null ? nativeSpellId4 : "");
        tag.putString("nativeSpellId5", nativeSpellId5 != null ? nativeSpellId5 : "");

        tag.putBoolean("enableWereNativeSpells", enableWereNativeSpells);
        tag.putString("wereNativeSpellId", wereNativeSpellId != null ? wereNativeSpellId : "");
        tag.putString("wereNativeSpellId1", wereNativeSpellId1 != null ? wereNativeSpellId1 : "");
        tag.putString("wereNativeSpellId2", wereNativeSpellId2 != null ? wereNativeSpellId2 : "");
        tag.putString("wereNativeSpellId3", wereNativeSpellId3 != null ? wereNativeSpellId3 : "");
        tag.putString("wereNativeSpellId4", wereNativeSpellId4 != null ? wereNativeSpellId4 : "");
        tag.putString("wereNativeSpellId5", wereNativeSpellId5 != null ? wereNativeSpellId5 : "");

        tag.putString("minionMobType", minionMobType != null ? minionMobType : "minecraft:zombie");
        tag.putInt("minionCount", minionCount);
        return tag;
    }

    public void fromNBT(net.minecraft.nbt.CompoundTag tag) {
        if (tag == null) return;
        if (tag.contains("id")) this.id = tag.getString("id");
        if (tag.contains("name")) this.name = tag.getString("name");
        if (tag.contains("lore")) this.lore = tag.getString("lore");
        if (tag.contains("modelType")) this.modelType = tag.getString("modelType");
        if (tag.contains("heightScale")) this.heightScale = tag.getFloat("heightScale");
        if (tag.contains("widthScale")) this.widthScale = tag.getFloat("widthScale");
        if (tag.contains("baseScale")) this.baseScale = tag.getFloat("baseScale");
        if (tag.contains("reachScale")) this.reachScale = tag.getFloat("reachScale");
        if (tag.contains("stepHeightScale")) this.stepHeightScale = tag.getFloat("stepHeightScale");
        if (tag.contains("maxHealth")) this.maxHealth = tag.getFloat("maxHealth");
        if (tag.contains("movementSpeed")) this.movementSpeed = tag.getFloat("movementSpeed");
        if (tag.contains("armor")) this.armor = tag.getFloat("armor");
        if (tag.contains("attackDamage")) this.attackDamage = tag.getFloat("attackDamage");
        if (tag.contains("isFlyingRace")) this.isFlyingRace = tag.getBoolean("isFlyingRace");
        if (tag.contains("particleCount")) this.particleCount = tag.getInt("particleCount");
        if (tag.contains("wereParticleCount")) this.wereParticleCount = tag.getInt("wereParticleCount");

        if (tag.contains("enableWereRace")) this.enableWereRace = tag.getBoolean("enableWereRace");
        if (tag.contains("isWereFlyingRace")) this.isWereFlyingRace = tag.getBoolean("isWereFlyingRace");
        if (tag.contains("wereTriggerCondition")) this.wereTriggerCondition = tag.getString("wereTriggerCondition");
        if (tag.contains("wereModelPath")) this.wereModelPath = tag.getString("wereModelPath");
        if (tag.contains("wereTexturePath")) this.wereTexturePath = tag.getString("wereTexturePath");
        if (tag.contains("wereAnimationPath")) this.wereAnimationPath = tag.getString("wereAnimationPath");
        if (tag.contains("wereHeightScale")) this.wereHeightScale = tag.getFloat("wereHeightScale");
        if (tag.contains("wereWidthScale")) this.wereWidthScale = tag.getFloat("wereWidthScale");
        if (tag.contains("wereHealthBonus")) this.wereHealthBonus = tag.getFloat("wereHealthBonus");
        if (tag.contains("wereSpeedBonus")) this.wereSpeedBonus = tag.getFloat("wereSpeedBonus");
        if (tag.contains("wereDamageBonus")) this.wereDamageBonus = tag.getFloat("wereDamageBonus");
        if (tag.contains("wereTransformSound")) this.wereTransformSound = tag.getString("wereTransformSound");
        if (tag.contains("wereHowlSound")) this.wereHowlSound = tag.getString("wereHowlSound");
        if (tag.contains("wereAmbientSound")) this.wereAmbientSound = tag.getString("wereAmbientSound");
        if (tag.contains("wereHurtSound")) this.wereHurtSound = tag.getString("wereHurtSound");
        if (tag.contains("wereDeathSound")) this.wereDeathSound = tag.getString("wereDeathSound");
        if (tag.contains("CustomDeathSound")) this.CustomDeathSound = tag.getString("CustomDeathSound");

        if (tag.contains("enableNativeSpells")) this.enableNativeSpells = tag.getBoolean("enableNativeSpells");
        if (tag.contains("nativeSpellId")) this.nativeSpellId = tag.getString("nativeSpellId");
        if (tag.contains("nativeSpellId1")) this.nativeSpellId1 = tag.getString("nativeSpellId1");
        if (tag.contains("nativeSpellId2")) this.nativeSpellId2 = tag.getString("nativeSpellId2");
        if (tag.contains("nativeSpellId3")) this.nativeSpellId3 = tag.getString("nativeSpellId3");
        if (tag.contains("nativeSpellId4")) this.nativeSpellId4 = tag.getString("nativeSpellId4");
        if (tag.contains("nativeSpellId5")) this.nativeSpellId5 = tag.getString("nativeSpellId5");

        if (tag.contains("enableWereNativeSpells")) this.enableWereNativeSpells = tag.getBoolean("enableWereNativeSpells");
        if (tag.contains("wereNativeSpellId")) this.wereNativeSpellId = tag.getString("wereNativeSpellId");
        if (tag.contains("wereNativeSpellId1")) this.wereNativeSpellId1 = tag.getString("wereNativeSpellId1");
        if (tag.contains("wereNativeSpellId2")) this.wereNativeSpellId2 = tag.getString("wereNativeSpellId2");
        if (tag.contains("wereNativeSpellId3")) this.wereNativeSpellId3 = tag.getString("wereNativeSpellId3");
        if (tag.contains("wereNativeSpellId4")) this.wereNativeSpellId4 = tag.getString("wereNativeSpellId4");
        if (tag.contains("wereNativeSpellId5")) this.wereNativeSpellId5 = tag.getString("wereNativeSpellId5");

        if (tag.contains("minionMobType")) this.minionMobType = tag.getString("minionMobType");
        if (tag.contains("minionCount")) this.minionCount = tag.getInt("minionCount");
        initDefaults();
    }
}
