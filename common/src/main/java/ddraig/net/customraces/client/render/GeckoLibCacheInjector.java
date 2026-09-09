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
     * Bakes a GeckoLib model from a JSON string using verified AzureFrameLib GeckoLibModelLoader.
     */
    public static Object bakeModelFromJson(ResourceLocation modelLocation, String jsonString) {
        return ddraig.net.azureframelib.client.GeckoLibModelLoader.bakeModelFromJson(modelLocation, jsonString);
    }

    /**
     * Bakes GeckoLib animations from a JSON string using verified AzureFrameLib GeckoLibModelLoader.
     */
    public static Object bakeAnimationsFromJson(ResourceLocation animLocation, String jsonString) {
        return ddraig.net.azureframelib.client.GeckoLibModelLoader.bakeAnimationsFromJson(animLocation, jsonString);
    }

    public static Map<ResourceLocation, Object> ensureModifiableModelMap() {
        return ddraig.net.azureframelib.client.GeckoLibModelLoader.ensureModifiableModelMap();
    }

    public static Map<ResourceLocation, Object> ensureModifiableAnimationMap() {
        return ddraig.net.azureframelib.client.GeckoLibModelLoader.ensureModifiableAnimationMap();
    }

    public static void injectModel(ResourceLocation location, Object bakedModel) {
        ddraig.net.azureframelib.client.GeckoLibModelLoader.injectModel(location, bakedModel);
    }

    public static void injectAnimations(ResourceLocation location, Object bakedAnimations) {
        ddraig.net.azureframelib.client.GeckoLibModelLoader.injectAnimations(location, bakedAnimations);
    }

    public static boolean isModelBaked(ResourceLocation modelLocation) {
        return ddraig.net.azureframelib.client.GeckoLibModelLoader.isModelBaked(modelLocation);
    }

    public static boolean isAnimationBaked(ResourceLocation animationLocation) {
        return ddraig.net.azureframelib.client.GeckoLibModelLoader.isAnimationBaked(animationLocation);
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
        ddraig.net.azureframelib.client.GeckoLibModelLoader.clearCaches();
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
