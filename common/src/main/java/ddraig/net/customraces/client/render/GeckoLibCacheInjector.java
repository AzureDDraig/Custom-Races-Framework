package ddraig.net.customraces.client.render;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Dynamic GeckoLib model and animation cache injector.
 * Scans streamed server resource pack zip archives and hot-injects baked
 * models and animations directly into GeckoLibCache without screen freezes.
 */
public class GeckoLibCacheInjector {

    private static Path lastMountedPackPath = null;
    private static int mountCount = 0;
    private static int lastInjectedModelCount = 0;
    private static int lastInjectedAnimationCount = 0;

    // Fallback thread-safe maps for testing or headless runtime when GeckoLib is not on classpath
    private static final ConcurrentMap<ResourceLocation, Object> FALLBACK_MODELS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<ResourceLocation, Object> FALLBACK_ANIMATIONS = new ConcurrentHashMap<>();

    /**
     * Primary entry point called when a pack zip is mounted.
     * Clears previous caches, tracks mount count and path, and hot-injects models & animations.
     *
     * @param packZipPath the path to the mounted zip file
     */
    public static synchronized void onPackMounted(Path packZipPath) {
        lastMountedPackPath = packZipPath;
        mountCount++;
        // Clear previous caches first so stale or updated assets resolve cleanly
        WereModelRenderer.clearCaches();

        int injected = injectFromPack(packZipPath);
        System.out.println("[CustomRaces] GeckoLibCacheInjector processed mounted pack: " + packZipPath
                + " (Injected: " + injected + " assets, Total mounts: " + mountCount + ")");
    }

    /**
     * Scans the pack zip file using ZipFile and bakes all models and animations into GeckoLibCache.
     * Dual-registers canonical keys and shorthand aliases.
     *
     * @param packZipPath the path to the zip file
     * @return the total number of models and animations successfully injected
     */
    public static int injectFromPack(Path packZipPath) {
        if (packZipPath == null || !Files.isRegularFile(packZipPath)) {
            return 0;
        }

        int injectedCount = 0;
        int modelCount = 0;
        int animCount = 0;

        ensureModifiableModelMap();
        ensureModifiableAnimationMap();

        try (ZipFile zipFile = new ZipFile(packZipPath.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) continue;

                String name = entry.getName().replace('\\', '/');
                if (name.startsWith("/")) name = name.substring(1);
                if (name.contains("..")) continue;

                // Expected format: assets/<namespace>/...
                if (!name.startsWith("assets/")) continue;
                int nextSlash = name.indexOf('/', 7);
                if (nextSlash <= 7) continue;

                String namespace = name.substring(7, nextSlash).toLowerCase(Locale.ROOT);
                String subpath = name.substring(nextSlash + 1);

                // 1. Models: assets/<namespace>/geo/**.json or assets/<namespace>/models/**.geo.json
                if ((subpath.startsWith("geo/") && subpath.endsWith(".json")) ||
                    (subpath.startsWith("models/") && subpath.endsWith(".geo.json"))) {
                    try {
                        String jsonContent = readEntryString(zipFile, entry);
                        if (jsonContent != null && !jsonContent.trim().isEmpty()) {
                            String relPath = subpath.startsWith("geo/") ? subpath.substring(4) : subpath.substring(7);
                            ResourceLocation canonicalLoc = new ResourceLocation(namespace, "geo/" + relPath);
                            ResourceLocation shortLoc = new ResourceLocation(namespace, relPath);
                            ResourceLocation modelsLoc = new ResourceLocation(namespace, "models/" + relPath);

                            Object baked = bakeModelFromJson(canonicalLoc, jsonContent);
                            if (baked != null) {
                                injectModel(canonicalLoc, baked);
                                injectModel(shortLoc, baked);
                                injectModel(modelsLoc, baked);
                                modelCount++;
                                injectedCount++;
                            }
                        }
                    } catch (Throwable t) {
                        System.err.println("[CustomRaces] Skipping invalid model in pack " + name + ": " + t.getMessage());
                    }
                }
                // 2. Animations: assets/<namespace>/animations/**.json
                else if (subpath.startsWith("animations/") && subpath.endsWith(".json")) {
                    try {
                        String jsonContent = readEntryString(zipFile, entry);
                        if (jsonContent != null && !jsonContent.trim().isEmpty()) {
                            String relPath = subpath.substring(11);
                            ResourceLocation canonicalLoc = new ResourceLocation(namespace, "animations/" + relPath);
                            ResourceLocation shortLoc = new ResourceLocation(namespace, relPath);

                            Object baked = bakeAnimationsFromJson(canonicalLoc, jsonContent);
                            if (baked != null) {
                                injectAnimations(canonicalLoc, baked);
                                injectAnimations(shortLoc, baked);
                                animCount++;
                                injectedCount++;
                            }
                        }
                    } catch (Throwable t) {
                        System.err.println("[CustomRaces] Skipping invalid animation in pack " + name + ": " + t.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[CustomRaces] Corrupted or unreadable pack zip " + packZipPath + ": " + e.getMessage());
            return 0;
        } catch (Throwable t) {
            System.err.println("[CustomRaces] Unexpected error reading pack zip " + packZipPath + ": " + t.getMessage());
            return 0;
        }

        lastInjectedModelCount = modelCount;
        lastInjectedAnimationCount = animCount;
        return injectedCount;
    }

    private static String readEntryString(ZipFile zipFile, ZipEntry entry) throws IOException {
        try (InputStream is = zipFile.getInputStream(entry);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                baos.write(buf, 0, len);
            }
            return baos.toString(StandardCharsets.UTF_8);
        }
    }

    /**
     * Bakes a GeckoLib model from a JSON string using verified GeckoLib 4.8.3 APIs.
     * Falls back to JSON syntax verification if GeckoLib is not on classpath.
     */
    public static Object bakeModelFromJson(ResourceLocation modelLocation, String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            // First check if already baked in GeckoLibCache
            try {
                Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
                Method getModelsMethod = cacheClass.getMethod("getBakedModels");
                Map<?, ?> bakedModels = (Map<?, ?>) getModelsMethod.invoke(null);
                if (bakedModels != null && modelLocation != null && bakedModels.containsKey(modelLocation)) {
                    return bakedModels.get(modelLocation);
                }
            } catch (Throwable ignored) {}

            // Validate JSON syntax first
            JsonElement parsed = JsonParser.parseString(jsonString);
            if (!parsed.isJsonObject()) return null;

            // Use GeckoLib 4.8.3 reflection: JsonUtil.GEO_GSON.fromJson(jsonString, Model.class)
            Class<?> jsonUtilClass = Class.forName("software.bernie.geckolib.util.JsonUtil");
            Field geoGsonField = jsonUtilClass.getField("GEO_GSON");
            Object geoGson = geoGsonField.get(null);

            Class<?> modelClass = Class.forName("software.bernie.geckolib.loading.json.raw.Model");
            Method fromJsonMethod = geoGson.getClass().getMethod("fromJson", String.class, Class.class);
            Object rawModel = fromJsonMethod.invoke(geoGson, jsonString, modelClass);
            if (rawModel == null) return null;

            // GeometryTree.fromModel(rawModel)
            Class<?> geomTreeClass = Class.forName("software.bernie.geckolib.loading.object.GeometryTree");
            Method fromModelMethod = geomTreeClass.getMethod("fromModel", modelClass);
            Object tree = fromModelMethod.invoke(null, rawModel);
            if (tree == null) return null;

            // BakedModelFactory.getForNamespace(ns).constructGeoModel(tree)
            Class<?> modelFactoryClass = Class.forName("software.bernie.geckolib.loading.object.BakedModelFactory");
            Method getFactoryMethod = modelFactoryClass.getMethod("getForNamespace", String.class);
            String ns = (modelLocation != null) ? modelLocation.getNamespace() : "customraces";
            Object factoryObj = getFactoryMethod.invoke(null, ns);

            Method constructGeoModelMethod = modelFactoryClass.getMethod("constructGeoModel", geomTreeClass);
            Object bakedGeoModel = constructGeoModelMethod.invoke(factoryObj, tree);

            if (bakedGeoModel != null && modelLocation != null) {
                injectModel(modelLocation, bakedGeoModel);
            }
            return bakedGeoModel;
        } catch (ClassNotFoundException cnfe) {
            // Offline / headless test fallback where GeckoLib runtime jar is absent
            try {
                JsonElement parsed = JsonParser.parseString(jsonString);
                if (parsed != null && parsed.isJsonObject() && modelLocation != null) {
                    FALLBACK_MODELS.put(modelLocation, parsed);
                }
                return parsed;
            } catch (Throwable t) {
                return null;
            }
        } catch (Throwable t) {
            System.err.println("[CustomRaces] Failed to bake GeckoLib model " + modelLocation + ": " + t.getMessage());
            return null;
        }
    }

    /**
     * Bakes GeckoLib animations from a JSON string using verified GeckoLib 4.8.3 APIs.
     * Falls back to JSON syntax verification if GeckoLib is not on classpath.
     */
    public static Object bakeAnimationsFromJson(ResourceLocation animLocation, String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        try {
            // First check if already baked in GeckoLibCache
            try {
                Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
                Method getAnimsMethod = cacheClass.getMethod("getBakedAnimations");
                Map<?, ?> bakedAnims = (Map<?, ?>) getAnimsMethod.invoke(null);
                if (bakedAnims != null && animLocation != null && bakedAnims.containsKey(animLocation)) {
                    return bakedAnims.get(animLocation);
                }
            } catch (Throwable ignored) {}

            JsonElement parsed = JsonParser.parseString(jsonString);
            if (!parsed.isJsonObject()) return null;
            JsonObject root = parsed.getAsJsonObject();
            JsonObject animObj = root.has("animations") ? root.getAsJsonObject("animations") : root;

            Class<?> jsonUtilClass = Class.forName("software.bernie.geckolib.util.JsonUtil");
            Field geoGsonField = jsonUtilClass.getField("GEO_GSON");
            Object geoGson = geoGsonField.get(null);

            Class<?> bakedAnimsClass = Class.forName("software.bernie.geckolib.loading.object.BakedAnimations");
            Method fromJsonElementMethod = geoGson.getClass().getMethod("fromJson", JsonElement.class, Class.class);
            Object bakedAnimObj = fromJsonElementMethod.invoke(geoGson, animObj, bakedAnimsClass);

            if (bakedAnimObj != null && animLocation != null) {
                injectAnimations(animLocation, bakedAnimObj);
            }
            return bakedAnimObj;
        } catch (ClassNotFoundException cnfe) {
            // Headless unit test fallback
            try {
                JsonElement parsed = JsonParser.parseString(jsonString);
                if (!parsed.isJsonObject()) return null;
                JsonObject root = parsed.getAsJsonObject();
                JsonObject animObj = root.has("animations") ? root.getAsJsonObject("animations") : root;
                if (animLocation != null) {
                    FALLBACK_ANIMATIONS.put(animLocation, animObj);
                }
                return animObj;
            } catch (Throwable t) {
                return null;
            }
        } catch (Throwable t) {
            System.err.println("[CustomRaces] Failed to bake GeckoLib animations " + animLocation + ": " + t.getMessage());
            return null;
        }
    }

    /**
     * Ensures GeckoLibCache.MODELS is a thread-safe modifiable Map.
     * Swaps out Collections.emptyMap() or non-concurrent maps via reflection.
     */
    public static Map<ResourceLocation, Object> ensureModifiableModelMap() {
        return ensureModifiableMap("MODELS");
    }

    /**
     * Ensures GeckoLibCache.ANIMATIONS is a thread-safe modifiable Map.
     * Swaps out Collections.emptyMap() or non-concurrent maps via reflection.
     */
    public static Map<ResourceLocation, Object> ensureModifiableAnimationMap() {
        return ensureModifiableMap("ANIMATIONS");
    }

    @SuppressWarnings("unchecked")
    public static Map<ResourceLocation, Object> ensureModifiableMap(String fieldName) {
        try {
            Class<?> geckoLibClass = Class.forName("software.bernie.geckolib.GeckoLib");
            try {
                Field initField = geckoLibClass.getField("hasInitialized");
                if (!initField.getBoolean(null)) {
                    initField.setBoolean(null, true);
                }
            } catch (Throwable ignored) {}

            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            Field field = cacheClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            Map<?, ?> map = (Map<?, ?>) field.get(null);

            boolean needsSwap = (map == null)
                    || map.getClass().getName().contains("EmptyMap")
                    || map.getClass().getName().contains("Unmodifiable")
                    || !(map instanceof ConcurrentMap);

            if (needsSwap) {
                Map<ResourceLocation, Object> mutableMap = new ConcurrentHashMap<>();
                if (map != null) {
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (entry.getKey() instanceof ResourceLocation loc) {
                            mutableMap.put(loc, entry.getValue());
                        }
                    }
                }
                field.set(null, mutableMap);
                return mutableMap;
            }
            return (Map<ResourceLocation, Object>) map;
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * Safely injects a baked model into GeckoLibCache and fallback tracking.
     */
    public static void injectModel(ResourceLocation location, Object bakedModel) {
        if (location == null || bakedModel == null) return;
        Map<ResourceLocation, Object> models = ensureModifiableModelMap();
        if (models != null) {
            try {
                models.put(location, bakedModel);
            } catch (UnsupportedOperationException uoe) {
                // Re-swap and retry
                Map<ResourceLocation, Object> swapped = ensureModifiableModelMap();
                if (swapped != null) swapped.put(location, bakedModel);
            }
        }
        FALLBACK_MODELS.put(location, bakedModel);
    }

    /**
     * Safely injects baked animations into GeckoLibCache and fallback tracking.
     */
    public static void injectAnimations(ResourceLocation location, Object bakedAnimations) {
        if (location == null || bakedAnimations == null) return;
        Map<ResourceLocation, Object> anims = ensureModifiableAnimationMap();
        if (anims != null) {
            try {
                anims.put(location, bakedAnimations);
            } catch (UnsupportedOperationException uoe) {
                // Re-swap and retry
                Map<ResourceLocation, Object> swapped = ensureModifiableAnimationMap();
                if (swapped != null) swapped.put(location, bakedAnimations);
            }
        }
        FALLBACK_ANIMATIONS.put(location, bakedAnimations);
    }

    /**
     * Checks if a model for the given ResourceLocation is baked and ready.
     * Supports checking both canonical ("geo/...") and shorthand paths.
     */
    public static boolean isModelBaked(ResourceLocation modelLocation) {
        if (modelLocation == null) return false;
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            Method getModelsMethod = cacheClass.getMethod("getBakedModels");
            Map<?, ?> bakedModels = (Map<?, ?>) getModelsMethod.invoke(null);
            if (bakedModels != null) {
                if (bakedModels.containsKey(modelLocation)) return true;
                String path = modelLocation.getPath();
                String ns = modelLocation.getNamespace();
                if (!path.startsWith("geo/") && bakedModels.containsKey(new ResourceLocation(ns, "geo/" + path))) {
                    return true;
                }
                if (path.startsWith("geo/") && bakedModels.containsKey(new ResourceLocation(ns, path.substring(4)))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        if (FALLBACK_MODELS.containsKey(modelLocation)) return true;
        String path = modelLocation.getPath();
        String ns = modelLocation.getNamespace();
        if (!path.startsWith("geo/") && FALLBACK_MODELS.containsKey(new ResourceLocation(ns, "geo/" + path))) {
            return true;
        }
        if (path.startsWith("geo/") && FALLBACK_MODELS.containsKey(new ResourceLocation(ns, path.substring(4)))) {
            return true;
        }
        return false;
    }

    /**
     * Checks if an animation for the given ResourceLocation is baked and ready.
     * Supports checking both canonical ("animations/...") and shorthand paths.
     */
    public static boolean isAnimationBaked(ResourceLocation animationLocation) {
        if (animationLocation == null) return false;
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            Method getAnimsMethod = cacheClass.getMethod("getBakedAnimations");
            Map<?, ?> bakedAnims = (Map<?, ?>) getAnimsMethod.invoke(null);
            if (bakedAnims != null) {
                if (bakedAnims.containsKey(animationLocation)) return true;
                String path = animationLocation.getPath();
                String ns = animationLocation.getNamespace();
                if (!path.startsWith("animations/") && bakedAnims.containsKey(new ResourceLocation(ns, "animations/" + path))) {
                    return true;
                }
                if (path.startsWith("animations/") && bakedAnims.containsKey(new ResourceLocation(ns, path.substring(11)))) {
                    return true;
                }
            }
        } catch (Throwable ignored) {}

        if (FALLBACK_ANIMATIONS.containsKey(animationLocation)) return true;
        String path = animationLocation.getPath();
        String ns = animationLocation.getNamespace();
        if (!path.startsWith("animations/") && FALLBACK_ANIMATIONS.containsKey(new ResourceLocation(ns, "animations/" + path))) {
            return true;
        }
        if (path.startsWith("animations/") && FALLBACK_ANIMATIONS.containsKey(new ResourceLocation(ns, path.substring(11)))) {
            return true;
        }
        return false;
    }

    public static Path getLastMountedPackPath() {
        return lastMountedPackPath;
    }

    public static int getMountCount() {
        return mountCount;
    }

    public static int getLastInjectedModelCount() {
        return lastInjectedModelCount;
    }

    public static int getLastInjectedAnimationCount() {
        return lastInjectedAnimationCount;
    }

    /**
     * Clears internal fallback tracking caches.
     */
    public static synchronized void clearInternalCaches() {
        FALLBACK_MODELS.clear();
        FALLBACK_ANIMATIONS.clear();
    }

    /**
     * Resets all internal states for testing isolation.
     */
    public static synchronized void resetForTesting() {
        lastMountedPackPath = null;
        mountCount = 0;
        lastInjectedModelCount = 0;
        lastInjectedAnimationCount = 0;
        clearInternalCaches();
    }
}
