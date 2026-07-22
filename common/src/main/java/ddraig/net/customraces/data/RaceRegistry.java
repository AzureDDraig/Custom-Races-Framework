package ddraig.net.customraces.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for loading, saving, and querying race configurations and player race assignments.
 */
public class RaceRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final Map<String, RaceData> loadedRaces = new ConcurrentHashMap<>();
    public static final Map<UUID, String> playerRaces = new ConcurrentHashMap<>();
    public static boolean autoOpenSelectionOnJoin = true;

    // Suggestion Cache Arrays to prevent lag on visual creator screen
    public static final List<String> CACHED_SOUNDS = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final List<String> CACHED_ITEMS = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final List<String> CACHED_PARTICLES = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final List<String> CACHED_BIOMES = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final List<String> CACHED_DIMENSIONS = new java.util.concurrent.CopyOnWriteArrayList<>();
    public static final List<String> CACHED_PROJECTILES = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static File getRacesFile() {
        File dir = new File("config/custom_races");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "races.json");
    }

    private static File getPlayerRacesFile() {
        File dir = new File("config/custom_races");
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "player_races.json");
    }

    public static void init() {
        initDirectories();
        loadRaces();
        loadPlayerRaces();
        rebuildSuggestionsCache();
    }

    public static void initDirectories() {
        File dir = new File("config/custom_races");
        if (!dir.exists()) dir.mkdirs();

        File iconsDir = new File("config/custom_races/icons");
        if (!iconsDir.exists()) iconsDir.mkdirs();

        File modelsDir = new File("config/custom_races/models");
        if (!modelsDir.exists()) modelsDir.mkdirs();

        File wereModelsDir = new File("config/custom_races/models/were");
        if (!wereModelsDir.exists()) wereModelsDir.mkdirs();

        File texturesDir = new File("config/custom_races/textures");
        if (!texturesDir.exists()) texturesDir.mkdirs();

        File wereTexturesDir = new File("config/custom_races/textures/were");
        if (!wereTexturesDir.exists()) wereTexturesDir.mkdirs();

        File animsDir = new File("config/custom_races/animations");
        if (!animsDir.exists()) animsDir.mkdirs();

        File wereAnimsDir = new File("config/custom_races/animations/were");
        if (!wereAnimsDir.exists()) wereAnimsDir.mkdirs();
    }

    public static void rebuildSuggestionsCache() {
        try {
            CACHED_SOUNDS.clear();
            for (net.minecraft.resources.ResourceLocation sound : net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.keySet()) {
                CACHED_SOUNDS.add(sound.toString());
            }
            java.util.Collections.sort(CACHED_SOUNDS);

            CACHED_ITEMS.clear();
            for (net.minecraft.resources.ResourceLocation item : net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet()) {
                CACHED_ITEMS.add(item.toString());
            }
            java.util.Collections.sort(CACHED_ITEMS);

            CACHED_PARTICLES.clear();
            for (net.minecraft.resources.ResourceLocation particle : net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.keySet()) {
                CACHED_PARTICLES.add(particle.toString());
            }
            java.util.Collections.sort(CACHED_PARTICLES);

            CACHED_DIMENSIONS.clear();
            CACHED_DIMENSIONS.addAll(java.util.List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"));

            CACHED_BIOMES.clear();
            CACHED_BIOMES.addAll(java.util.List.of(
                "minecraft:plains", "minecraft:desert", "minecraft:swamp", "minecraft:forest",
                "minecraft:taiga", "minecraft:jungle", "minecraft:ocean", "minecraft:nether_wastes",
                "minecraft:crimson_forest", "minecraft:warped_forest", "minecraft:the_end", "minecraft:lush_caves"
            ));

            CACHED_PROJECTILES.clear();
            for (net.minecraft.resources.ResourceLocation entity : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.keySet()) {
                CACHED_PROJECTILES.add(entity.toString());
            }
            java.util.Collections.sort(CACHED_PROJECTILES);
        } catch (Exception ignored) {}
    }

    public static void loadRaces() {
        loadedRaces.clear();
        File file = getRacesFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, RaceData>>() {}.getType();
                Map<String, RaceData> map = GSON.fromJson(reader, type);
                if (map != null) {
                    loadedRaces.putAll(map);
                }
            } catch (Exception e) {
                System.err.println("[CustomRaces] Error loading races.json: " + e.getMessage());
            }
        }

        if (loadedRaces.isEmpty()) {
            registerBuiltinPresets();
            saveRaces();
        }
    }

    public static void saveRaces() {
        File file = getRacesFile();
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(loadedRaces, writer);
        } catch (Exception e) {
            System.err.println("[CustomRaces] Error saving races.json: " + e.getMessage());
        }
    }

    public static void loadPlayerRaces() {
        playerRaces.clear();
        File file = getPlayerRacesFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                Type type = new TypeToken<Map<String, String>>() {}.getType();
                Map<String, String> map = GSON.fromJson(reader, type);
                if (map != null) {
                    for (Map.Entry<String, String> entry : map.entrySet()) {
                        try {
                            playerRaces.put(UUID.fromString(entry.getKey()), entry.getValue());
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                System.err.println("[CustomRaces] Error loading player_races.json: " + e.getMessage());
            }
        }
    }

    public static void savePlayerRaces() {
        File file = getPlayerRacesFile();
        try (FileWriter writer = new FileWriter(file)) {
            Map<String, String> stringMap = new HashMap<>();
            for (Map.Entry<UUID, String> entry : playerRaces.entrySet()) {
                stringMap.put(entry.getKey().toString(), entry.getValue());
            }
            GSON.toJson(stringMap, writer);
        } catch (Exception e) {
            System.err.println("[CustomRaces] Error saving player_races.json: " + e.getMessage());
        }
    }

    public static RaceData getRace(String raceId) {
        if (raceId == null) return null;
        return loadedRaces.get(raceId);
    }

    public static RaceData getPlayerRace(UUID playerUuid) {
        String raceId = playerRaces.get(playerUuid);
        if (raceId != null) {
            return loadedRaces.get(raceId);
        }
        return null;
    }

    public static void setPlayerRace(UUID playerUuid, String raceId) {
        if (raceId == null || raceId.isEmpty() || raceId.equalsIgnoreCase("none")) {
            playerRaces.remove(playerUuid);
        } else {
            playerRaces.put(playerUuid, raceId);
        }
        savePlayerRaces();
    }

    public static void registerBuiltinPresets() {
        // 1. Elf
        RaceData elf = new RaceData("elf", "Elf");
        elf.lore = "Graceful guardians of nature with unmatched agility, photo-regeneration, and night vision.";
        elf.iconItem = "minecraft:oak_sapling";
        elf.playstyleDifficulty = 2;
        elf.movementSpeed = 0.12f;
        elf.passiveAbilities.add("photosynthesis");
        elf.passiveAbilities.add("swiftfoot");
        elf.passiveAbilities.add("night_eyes");
        elf.activeAbilities.put(1, "wind_glide");
        loadedRaces.put(elf.id, elf);

        // 2. Dwarf
        RaceData dwarf = new RaceData("dwarf", "Dwarf");
        dwarf.lore = "Stout, subterranean miners with reinforced skin, heavy bones, and underground haste.";
        dwarf.iconItem = "minecraft:raw_iron";
        dwarf.playstyleDifficulty = 3;
        dwarf.heightScale = 0.85f;
        dwarf.maxHealth = 24.0f;
        dwarf.armor = 4.0f;
        dwarf.passiveAbilities.add("master_miner");
        dwarf.passiveAbilities.add("iron_skin");
        dwarf.passiveAbilities.add("heavy_weight");
        dwarf.activeAbilities.put(1, "earthquake_slam");
        loadedRaces.put(dwarf.id, dwarf);

        // 3. Enderian
        RaceData enderian = new RaceData("enderian", "Enderian");
        enderian.lore = "Mysterious humanoids connected to the void, capable of instant short-range teleportation.";
        enderian.iconItem = "minecraft:ender_pearl";
        enderian.playstyleDifficulty = 4;
        enderian.heightScale = 1.15f;
        enderian.reachScale = 1.2f;
        enderian.passiveAbilities.add("phasing_stealth");
        enderian.passiveAbilities.add("water_vulnerability");
        enderian.passiveAbilities.add("night_eyes");
        enderian.activeAbilities.put(1, "teleport_dash");
        loadedRaces.put(enderian.id, enderian);

        // 4. Merling
        RaceData merling = new RaceData("merling", "Merling");
        merling.lore = "Aquatic inhabitants of the deep ocean, possessing gills, underwater sight, and swift swimming.";
        merling.iconItem = "minecraft:heart_of_the_sea";
        merling.playstyleDifficulty = 3;
        merling.tailType = "fish";
        merling.passiveAbilities.add("gills_of_the_deep");
        merling.passiveAbilities.add("aqua_agility");
        merling.passiveAbilities.add("water_conduit_power");
        merling.passiveAbilities.add("ocean_sight");
        merling.activeAbilities.put(1, "aqua_jet");
        loadedRaces.put(merling.id, merling);

        // 5. Blazeborn
        RaceData blazeborn = new RaceData("blazeborn", "Blazeborn");
        blazeborn.lore = "Fiery entities of the Nether, immune to heat and lava, who unleash scorching fiery cones.";
        blazeborn.iconItem = "minecraft:blaze_powder";
        blazeborn.playstyleDifficulty = 5;
        blazeborn.particleAuras.add(new ParticleAuraData("minecraft:flame", 1.5f, 0.05f, 0.4f));
        blazeborn.passiveAbilities.add("fireproof_scales");
        blazeborn.passiveAbilities.add("lava_walker");
        blazeborn.passiveAbilities.add("blazing_body");
        blazeborn.passiveAbilities.add("water_vulnerability");
        blazeborn.activeAbilities.put(1, "flame_breath");
        loadedRaces.put(blazeborn.id, blazeborn);

        // 6. Dragonkin
        RaceData dragonkin = new RaceData("dragonkin", "Dragonkin");
        dragonkin.lore = "Descendants of ancient dragons, possessing scaled hide, wings, tail, and terrifying roars.";
        dragonkin.iconItem = "minecraft:dragon_head";
        dragonkin.playstyleDifficulty = 6;
        dragonkin.wingType = "dragon";
        dragonkin.tailType = "dragon";
        dragonkin.hornType = "dragon";
        dragonkin.maxHealth = 26.0f;
        dragonkin.passiveAbilities.add("dragon_resilience");
        dragonkin.passiveAbilities.add("fireproof_scales");
        dragonkin.passiveAbilities.add("elytra_wings");
        dragonkin.activeAbilities.put(1, "flame_breath");
        dragonkin.activeAbilities.put(2, "dragon_roar");
        loadedRaces.put(dragonkin.id, dragonkin);

        // 7. Feline
        RaceData feline = new RaceData("feline", "Feline");
        feline.lore = "Agile catfolk with animal ears and tail, silent footsteps, night vision, and leaping pounces.";
        feline.iconItem = "minecraft:feather";
        feline.playstyleDifficulty = 2;
        feline.earType = "cat";
        feline.tailType = "cat";
        feline.passiveAbilities.add("soft_footsteps");
        feline.passiveAbilities.add("night_eyes");
        feline.passiveAbilities.add("acrobatics");
        feline.passiveAbilities.add("shadow_camouflage");
        feline.activeAbilities.put(1, "leap_attack");
        loadedRaces.put(feline.id, feline);

        // 8. Spider Arachnid
        RaceData arachnid = new RaceData("arachnid", "Spider Arachnid");
        arachnid.lore = "Eight-legged web weavers capable of wall climbing, toxic secretions, and launching web traps.";
        arachnid.iconItem = "minecraft:cobweb";
        arachnid.playstyleDifficulty = 5;
        arachnid.legType = "spider";
        arachnid.legCount = 8;
        arachnid.passiveAbilities.add("spider_climb");
        arachnid.passiveAbilities.add("web_weaver");
        arachnid.passiveAbilities.add("toxic_secretions");
        arachnid.activeAbilities.put(1, "web_trap_throw");
        loadedRaces.put(arachnid.id, arachnid);

        // 9. Centaur
        RaceData centaur = new RaceData("centaur", "Centaur");
        centaur.lore = "Noble four-legged equine warriors with high sprinting speed and devastating ground stomps.";
        centaur.iconItem = "minecraft:saddle";
        centaur.playstyleDifficulty = 3;
        centaur.legType = "centaur";
        centaur.legCount = 4;
        centaur.movementSpeed = 0.14f;
        centaur.passiveAbilities.add("step_assist");
        centaur.passiveAbilities.add("swiftfoot");
        centaur.activeAbilities.put(1, "thunder_stomp");
        loadedRaces.put(centaur.id, centaur);

        // 10. Golem
        RaceData golem = new RaceData("golem", "Golem");
        golem.lore = "Massive stone constructs with vast health pools, heavy knockback resistance, and shield walls.";
        golem.iconItem = "minecraft:iron_block";
        golem.playstyleDifficulty = 4;
        golem.heightScale = 1.35f;
        golem.widthScale = 1.25f;
        golem.maxHealth = 30.0f;
        golem.armor = 8.0f;
        golem.passiveAbilities.add("colossal_stature");
        golem.passiveAbilities.add("iron_skin");
        golem.passiveAbilities.add("explosive_resistance");
        golem.activeAbilities.put(1, "shield_wall");
        loadedRaces.put(golem.id, golem);
    }
}
