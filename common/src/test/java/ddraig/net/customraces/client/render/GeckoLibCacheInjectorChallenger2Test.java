package ddraig.net.customraces.client.render;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.GeckoLibCache;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Challenger M3-2 Empirical Adversarial Stress Test Suite:
 * 1. High-concurrency race conditions (16 threads simultaneously calling injectFromPack, onPackMounted, isModelBaked, clearInternalCaches).
 * 2. Rapid pack mounting / unmounting cycles (flapping test across 20 cycles with zero memory leak and exact count tracking).
 * 3. Map promotion stress: repeatedly forcing GeckoLibCache.MODELS and ANIMATIONS back to unmodifiable maps while worker threads read/write.
 * 4. Multi-animation injection: pack with 12 distinct models and 12 distinct animation files verifying 100% registration without crosstalk.
 * 5. Corrupt entry containment and path traversal protection.
 */
public class GeckoLibCacheInjectorChallenger2Test {

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   GECKOLIB CACHE INJECTOR CHALLENGER 2 EMPIRICAL ADVERSARIAL STRESS SUITE      ");
        System.out.println("================================================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: High-concurrency race conditions (16 threads)
        try {
            testHighConcurrencyRaceConditions16Threads();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 1 (High Concurrency 16 Threads): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 2: Rapid pack mounting / unmounting cycles (flapping test - 20 cycles)
        try {
            testRapidPackFlapping20CyclesZeroLeak();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 2 (Rapid Flapping 20 Cycles): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 3: Map promotion stress (unmodifiable map replacement during live access)
        try {
            testMapPromotionStressUnderAdversarialDemotion();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 3 (Map Promotion Stress): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 4: Multi-animation and multi-model injection (12+ assets without crosstalk)
        try {
            testMultiAnimationInjectionNoCrosstalk();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 4 (Multi-Animation & Model No Crosstalk): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 5: Path traversal and malformed entry resilience
        try {
            testZipSecurityAndMalformedEntryResilience();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 5 (Security & Malformed Resilience): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("================================================================================");
        System.out.println("  CHALLENGER 2 SUMMARY: " + passed + " PASSED, " + failed + " FAILED  ");
        System.out.println("================================================================================");

        if (failed > 0) {
            System.err.println("VERDICT: REQUEST_CHANGES — " + failed + " stress test(s) failed.");
            System.exit(1);
        } else {
            System.out.println("VERDICT: APPROVE — All stress tests passed with 100% thread safety.");
        }
    }

    /**
     * Test 1: High-concurrency race conditions with 16 threads simultaneously executing
     * injectFromPack, onPackMounted, isModelBaked, and clearInternalCaches across same and different pack zips.
     */
    public static void testHighConcurrencyRaceConditions16Threads() throws Exception {
        System.out.println("\n--- Test 1: High-concurrency race conditions (16 threads) ---");
        GeckoLibCacheInjector.resetForTesting();

        Path packAlpha = createMultiAssetZip("alpha", 3, 2);
        Path packBeta = createMultiAssetZip("beta", 3, 2);

        int threadCount = 16;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CyclicBarrier barrier = new CyclicBarrier(threadCount);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        int iterationsPerThread = 80;

        try {
            List<Future<?>> futures = new ArrayList<>();

            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                futures.add(executor.submit(() -> {
                    try {
                        barrier.await(); // Synchronize all 16 threads to fire at the exact same instant

                        for (int i = 0; i < iterationsPerThread; i++) {
                            int mod = threadId % 4;
                            Path targetPack = (i % 2 == 0) ? packAlpha : packBeta;

                            switch (mod) {
                                case 0 -> {
                                    // Worker group 0: injectFromPack
                                    int injected = GeckoLibCacheInjector.injectFromPack(targetPack);
                                    assertTrue(injected >= 0, "Injected count must be non-negative");
                                }
                                case 1 -> {
                                    // Worker group 1: onPackMounted
                                    GeckoLibCacheInjector.onPackMounted(targetPack);
                                    assertTrue(GeckoLibCacheInjector.getMountCount() > 0, "Mount count must be positive");
                                }
                                case 2 -> {
                                    // Worker group 2: isModelBaked and isAnimationBaked lookups
                                    ResourceLocation alphaModel = new ResourceLocation("customraces", "geo/alpha_model_0.geo.json");
                                    ResourceLocation betaModel = new ResourceLocation("customraces", "beta_model_0.geo.json");
                                    ResourceLocation alphaAnim = new ResourceLocation("customraces", "animations/alpha_anim_0.animation.json");
                                    ResourceLocation missing = new ResourceLocation("customraces", "geo/non_existent.geo.json");

                                    GeckoLibCacheInjector.isModelBaked(alphaModel);
                                    GeckoLibCacheInjector.isModelBaked(betaModel);
                                    GeckoLibCacheInjector.isAnimationBaked(alphaAnim);
                                    assertFalse(GeckoLibCacheInjector.isModelBaked(missing), "Missing model must return false");
                                }
                                case 3 -> {
                                    // Worker group 3: cache clearing
                                    if (i % 2 == 0) {
                                        GeckoLibCacheInjector.clearInternalCaches();
                                    } else {
                                        WereModelRenderer.clearCaches();
                                    }
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        errors.add(ex);
                    }
                }));
            }

            for (Future<?> f : futures) {
                f.get(25, TimeUnit.SECONDS);
            }

            assertTrue(errors.isEmpty(), "Zero concurrency exceptions expected across 16 threads, but caught: " + errors.size() + ". First error: " + (errors.isEmpty() ? "none" : errors.peek()));
            System.out.println("  [PASS] 16 concurrent threads executed " + (threadCount * iterationsPerThread) + " simultaneous operations with 0 race condition errors.");
        } finally {
            executor.shutdownNow();
            Files.deleteIfExists(packAlpha);
            Files.deleteIfExists(packBeta);
            GeckoLibCacheInjector.resetForTesting();
        }
    }

    /**
     * Test 2: Rapid pack mounting / unmounting cycles (flapping test across 20 cycles)
     * verifying exact mount tracking, consistent injection numbers, and zero memory leaks.
     */
    public static void testRapidPackFlapping20CyclesZeroLeak() throws Exception {
        System.out.println("\n--- Test 2: Rapid pack mounting / unmounting cycles (20 cycles) ---");
        GeckoLibCacheInjector.resetForTesting();

        Path packA = createMultiAssetZip("flapping_a", 2, 2);
        Path packB = createMultiAssetZip("flapping_b", 4, 1);

        int initialMounts = GeckoLibCacheInjector.getMountCount();
        assertTrue(initialMounts == 0, "Mount count must be 0 after reset");

        try {
            int cycles = 20;
            for (int i = 0; i < cycles; i++) {
                boolean isEven = (i % 2 == 0);
                Path activePack = isEven ? packA : packB;
                String prefix = isEven ? "flapping_a" : "flapping_b";
                int expectedModels = isEven ? 2 : 4;
                int expectedAnims = isEven ? 2 : 1;

                // 1. Mount pack
                GeckoLibCacheInjector.onPackMounted(activePack);

                // 2. Verify mount count tracking
                assertTrue(GeckoLibCacheInjector.getMountCount() == (i + 1), "Mount count must increment strictly by 1");
                assertTrue(activePack.equals(GeckoLibCacheInjector.getLastMountedPackPath()), "Last mounted pack path must match");
                assertTrue(GeckoLibCacheInjector.getLastInjectedModelCount() == expectedModels, "Model count must match active pack");
                assertTrue(GeckoLibCacheInjector.getLastInjectedAnimationCount() == expectedAnims, "Animation count must match active pack");

                // 3. Verify models and animations are baked
                ResourceLocation modelLoc = new ResourceLocation("customraces", "geo/" + prefix + "_model_0.geo.json");
                ResourceLocation animLoc = new ResourceLocation("customraces", "animations/" + prefix + "_anim_0.animation.json");
                assertTrue(GeckoLibCacheInjector.isModelBaked(modelLoc), "Model must be baked during cycle " + i);
                assertTrue(GeckoLibCacheInjector.isAnimationBaked(animLoc), "Animation must be baked during cycle " + i);

                // 4. Unmount / Evict caches
                WereModelRenderer.clearCaches();

                // 5. Verify models are evicted
                assertFalse(GeckoLibCacheInjector.isModelBaked(modelLoc), "Model must be evicted after clearCaches in cycle " + i);
                assertFalse(GeckoLibCacheInjector.isAnimationBaked(animLoc), "Animation must be evicted after clearCaches in cycle " + i);
            }

            // Memory leak and retention verification
            System.gc();
            Thread.sleep(50);

            // Verify GeckoLibCache.MODELS and ANIMATIONS have zero remaining customraces keys
            Map<ResourceLocation, Object> models = GeckoLibCache.getBakedModels();
            if (models != null) {
                for (ResourceLocation loc : models.keySet()) {
                    assertFalse("customraces".equals(loc.getNamespace()), "Residual customraces model found in GeckoLibCache: " + loc);
                }
            }

            Map<ResourceLocation, Object> anims = GeckoLibCache.getBakedAnimations();
            if (anims != null) {
                for (ResourceLocation loc : anims.keySet()) {
                    assertFalse("customraces".equals(loc.getNamespace()), "Residual customraces animation found in GeckoLibCache: " + loc);
                }
            }

            System.out.println("  [PASS] 20 flapping mount/unmount cycles verified with exact count tracking and zero lingering entries.");
        } finally {
            Files.deleteIfExists(packA);
            Files.deleteIfExists(packB);
            GeckoLibCacheInjector.resetForTesting();
        }
    }

    /**
     * Test 3: Map promotion stress: repeatedly forcing GeckoLibCache.MODELS and ANIMATIONS
     * back to unmodifiable maps while worker threads are actively writing and reading.
     * Verifies that ensureModifiableMap and retry handlers guarantee ZERO UnsupportedOperationException.
     */
    public static void testMapPromotionStressUnderAdversarialDemotion() throws Exception {
        System.out.println("\n--- Test 3: Map promotion stress (adversarial unmodifiable demotion) ---");
        GeckoLibCacheInjector.resetForTesting();

        int workerCount = 8;
        int demoterCount = 2;
        int totalThreads = workerCount + demoterCount;

        ExecutorService executor = Executors.newFixedThreadPool(totalThreads);
        CyclicBarrier barrier = new CyclicBarrier(totalThreads);
        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();
        AtomicBoolean running = new AtomicBoolean(true);
        AtomicInteger writeOperations = new AtomicInteger(0);

        try {
            List<Future<?>> futures = new ArrayList<>();

            // 1. Adversarial demoter threads: repeatedly resets GeckoLibCache static maps to unmodifiable maps
            for (int d = 0; d < demoterCount; d++) {
                futures.add(executor.submit(() -> {
                    try {
                        barrier.await();
                        while (running.get()) {
                            // Adversarially force MODELS and ANIMATIONS to unmodifiable empty maps
                            forceUnmodifiableGeckoLibMaps();
                            Thread.yield();
                        }
                    } catch (Throwable t) {
                        errors.add(t);
                    }
                }));
            }

            // 2. Worker threads: concurrently injecting models and animations and performing queries
            for (int w = 0; w < workerCount; w++) {
                final int workerId = w;
                futures.add(executor.submit(() -> {
                    try {
                        barrier.await();
                        for (int i = 0; i < 150; i++) {
                            ResourceLocation mLoc = new ResourceLocation("customraces", "geo/stress_model_" + workerId + "_" + i + ".geo.json");
                            ResourceLocation aLoc = new ResourceLocation("customraces", "animations/stress_anim_" + workerId + "_" + i + ".animation.json");

                            // Inject model and animation (triggers ensureModifiableMap and try/catch retry)
                            GeckoLibCacheInjector.injectModel(mLoc, "mock_geo_" + i);
                            GeckoLibCacheInjector.injectAnimations(aLoc, "mock_anim_" + i);
                            writeOperations.addAndGet(2);

                            // Query baked status
                            assertTrue(GeckoLibCacheInjector.isModelBaked(mLoc), "Model must be baked even under map demotion");
                            assertTrue(GeckoLibCacheInjector.isAnimationBaked(aLoc), "Animation must be baked even under map demotion");

                            // Call ensureModifiableMap directly
                            GeckoLibCacheInjector.ensureModifiableModelMap();
                            GeckoLibCacheInjector.ensureModifiableAnimationMap();
                        }
                    } catch (Throwable t) {
                        errors.add(t);
                    }
                }));
            }

            // Wait for workers to finish
            Thread.sleep(1500);
            running.set(false);

            for (Future<?> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }

            assertTrue(errors.isEmpty(), "Expected 0 UnsupportedOperationException or other errors, caught: " + errors.size() + ". First error: " + (errors.isEmpty() ? "none" : errors.peek()));
            assertTrue(writeOperations.get() > 0, "Write operations must have executed");
            System.out.println("  [PASS] Map promotion stress passed (" + writeOperations.get() + " writes against adversarial unmodifiable demotions with 0 UnsupportedOperationException).");
        } finally {
            running.set(false);
            executor.shutdownNow();
            GeckoLibCacheInjector.resetForTesting();
        }
    }

    /**
     * Test 4: Multi-animation and multi-model injection with 12 distinct models and 12 distinct animations.
     * Verifies that 100% of models and animations register cleanly and no crosstalk occurs.
     */
    public static void testMultiAnimationInjectionNoCrosstalk() throws Exception {
        System.out.println("\n--- Test 4: Multi-animation and model injection (12+ assets without crosstalk) ---");
        GeckoLibCacheInjector.resetForTesting();

        int assetCount = 12;
        Path bigPack = createDistinctMultiAssetZip("beast", assetCount);

        try {
            int totalInjected = GeckoLibCacheInjector.injectFromPack(bigPack);
            assertTrue(totalInjected == (assetCount * 2), "Expected " + (assetCount * 2) + " injected assets, got: " + totalInjected);
            assertTrue(GeckoLibCacheInjector.getLastInjectedModelCount() == assetCount, "Expected " + assetCount + " models");
            assertTrue(GeckoLibCacheInjector.getLastInjectedAnimationCount() == assetCount, "Expected " + assetCount + " animations");

            // 1. Verify 100% of models and animations are registered under canonical and shorthand aliases
            for (int i = 0; i < assetCount; i++) {
                ResourceLocation canonicalModel = new ResourceLocation("customraces", "geo/beast_" + i + ".geo.json");
                ResourceLocation shortModel = new ResourceLocation("customraces", "beast_" + i + ".geo.json");
                ResourceLocation canonicalAnim = new ResourceLocation("customraces", "animations/beast_" + i + ".animation.json");
                ResourceLocation shortAnim = new ResourceLocation("customraces", "beast_" + i + ".animation.json");

                assertTrue(GeckoLibCacheInjector.isModelBaked(canonicalModel), "Canonical model " + i + " must be baked");
                assertTrue(GeckoLibCacheInjector.isModelBaked(shortModel), "Shorthand model alias " + i + " must be baked");
                assertTrue(GeckoLibCacheInjector.isAnimationBaked(canonicalAnim), "Canonical animation " + i + " must be baked");
                assertTrue(GeckoLibCacheInjector.isAnimationBaked(shortAnim), "Shorthand animation alias " + i + " must be baked");
            }

            // 2. Crosstalk verification: ensure each model/animation maintains its own distinct payload
            for (int i = 0; i < assetCount; i++) {
                ResourceLocation modelLoc = new ResourceLocation("customraces", "geo/beast_" + i + ".geo.json");
                ResourceLocation animLoc = new ResourceLocation("customraces", "animations/beast_" + i + ".animation.json");

                Object bakedModel = GeckoLibCache.getBakedModels().get(modelLoc);
                if (bakedModel == null) {
                    // Check fallback map via reflection
                    Field fallbackField = GeckoLibCacheInjector.class.getDeclaredField("FALLBACK_MODELS");
                    fallbackField.setAccessible(true);
                    Map<?, ?> fallback = (Map<?, ?>) fallbackField.get(null);
                    bakedModel = fallback.get(modelLoc);
                }
                assertTrue(bakedModel != null, "Model payload for " + i + " must not be null");
                String modelJson = bakedModel.toString();
                assertTrue(modelJson.contains("geometry.beast_" + i), "Model " + i + " must contain its own geometry identifier");
                assertTrue(modelJson.contains("bone_beast_" + i), "Model " + i + " must contain its own unique bone");

                // Ensure it does NOT contain adjacent bone signatures (no crosstalk)
                int otherIndex = (i + 1) % assetCount;
                assertFalse(modelJson.contains("bone_beast_" + otherIndex), "Model " + i + " must NOT contain bone of model " + otherIndex);

                Object bakedAnim = GeckoLibCache.getBakedAnimations().get(animLoc);
                if (bakedAnim == null) {
                    Field fallbackField = GeckoLibCacheInjector.class.getDeclaredField("FALLBACK_ANIMATIONS");
                    fallbackField.setAccessible(true);
                    Map<?, ?> fallback = (Map<?, ?>) fallbackField.get(null);
                    bakedAnim = fallback.get(animLoc);
                }
                assertTrue(bakedAnim != null, "Animation payload for " + i + " must not be null");
                String animJson = bakedAnim.toString();
                assertTrue(animJson.contains("animation.beast_" + i + ".action"), "Animation " + i + " must contain its own action signature");
                assertFalse(animJson.contains("animation.beast_" + otherIndex + ".action"), "Animation " + i + " must NOT contain action of animation " + otherIndex);
            }

            // 3. Negative alien query checks
            assertFalse(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/beast_999.geo.json")), "Alien model 999 must return false");
            assertFalse(GeckoLibCacheInjector.isAnimationBaked(new ResourceLocation("customraces", "animations/beast_999.animation.json")), "Alien animation 999 must return false");
            assertFalse(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("minecraft", "geo/beast_0.geo.json")), "Alien namespace must return false");

            System.out.println("  [PASS] Multi-animation injection registered 100% of 24 assets across 12 distinct models and animations with zero crosstalk.");
        } finally {
            Files.deleteIfExists(bigPack);
            GeckoLibCacheInjector.resetForTesting();
        }
    }

    /**
     * Test 5: Path traversal protection, malformed JSON recovery, and null/corrupt input safety.
     */
    public static void testZipSecurityAndMalformedEntryResilience() throws Exception {
        System.out.println("\n--- Test 5: Path traversal, malformed JSON & input edge cases ---");
        GeckoLibCacheInjector.resetForTesting();

        // 1. Null and invalid file paths
        assertTrue(GeckoLibCacheInjector.injectFromPack(null) == 0, "Null path must return 0");
        assertTrue(GeckoLibCacheInjector.injectFromPack(Path.of("non_existent_pack_path.zip")) == 0, "Non-existent path must return 0");

        // 2. Path with path traversal attempts ('..') and leading slash
        Path maliciousZip = Files.createTempFile("malicious_pack", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(maliciousZip.toFile()))) {
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/../../etc/passwd.geo.json"));
            zos.write("{\"dummy\": true}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("/assets/customraces/geo/leading_slash.geo.json"));
            zos.write(createGeoJson("leading_slash", "bone_slash").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Malformed JSON alongside a valid one
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/broken.geo.json"));
            zos.write("{{{{{ invalid json".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            zos.putNextEntry(new ZipEntry("assets/customraces/geo/valid_companion.geo.json"));
            zos.write(createGeoJson("valid_companion", "bone_companion").getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        try {
            int injected = GeckoLibCacheInjector.injectFromPack(maliciousZip);
            // Valid companion (and leading slash if sanitized) should be injected, but path traversal and broken JSON skipped
            assertTrue(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/valid_companion.geo.json")), "Valid companion must be injected");
            assertFalse(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/broken.geo.json")), "Broken JSON must not be baked");
            assertFalse(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "etc/passwd.geo.json")), "Path traversal entry must not be baked");

            System.out.println("  [PASS] Path traversal rejected and valid companion recovered despite broken entries.");
        } finally {
            Files.deleteIfExists(maliciousZip);
            GeckoLibCacheInjector.resetForTesting();
        }
    }

    // Helper utilities

    private static void forceUnmodifiableGeckoLibMaps() {
        try {
            Field modelsField = GeckoLibCache.class.getDeclaredField("MODELS");
            modelsField.setAccessible(true);
            modelsField.set(null, Collections.unmodifiableMap(Collections.emptyMap()));

            Field animsField = GeckoLibCache.class.getDeclaredField("ANIMATIONS");
            animsField.setAccessible(true);
            animsField.set(null, Collections.unmodifiableMap(Collections.emptyMap()));
        } catch (Throwable ignored) {}
    }

    private static Path createMultiAssetZip(String prefix, int modelCount, int animCount) throws IOException {
        Path zipPath = Files.createTempFile("gecko_multi_" + prefix, ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            zos.putNextEntry(new ZipEntry("pack.mcmeta"));
            zos.write("{\"pack\":{\"pack_format\":15,\"description\":\"Test\"}}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            for (int i = 0; i < modelCount; i++) {
                zos.putNextEntry(new ZipEntry("assets/customraces/geo/" + prefix + "_model_" + i + ".geo.json"));
                zos.write(createGeoJson(prefix + "_" + i, "bone_" + prefix + "_" + i).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            for (int i = 0; i < animCount; i++) {
                zos.putNextEntry(new ZipEntry("assets/customraces/animations/" + prefix + "_anim_" + i + ".animation.json"));
                zos.write(createAnimJson(prefix + "_" + i).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zipPath;
    }

    private static Path createDistinctMultiAssetZip(String prefix, int count) throws IOException {
        Path zipPath = Files.createTempFile("gecko_distinct_" + prefix, ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            zos.putNextEntry(new ZipEntry("pack.mcmeta"));
            zos.write("{\"pack\":{\"pack_format\":15,\"description\":\"Distinct Pack\"}}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            for (int i = 0; i < count; i++) {
                zos.putNextEntry(new ZipEntry("assets/customraces/geo/" + prefix + "_" + i + ".geo.json"));
                zos.write(createGeoJson(prefix + "_" + i, "bone_" + prefix + "_" + i).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();

                zos.putNextEntry(new ZipEntry("assets/customraces/animations/" + prefix + "_" + i + ".animation.json"));
                zos.write(createAnimJson(prefix + "_" + i).getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zipPath;
    }

    private static String createGeoJson(String identifier, String boneName) {
        return "{\n"
                + "  \"format_version\": \"1.12.0\",\n"
                + "  \"geometry\": [{\n"
                + "    \"description\": {\"identifier\": \"geometry." + identifier + "\", \"texture_width\": 64, \"texture_height\": 64},\n"
                + "    \"bones\": [{\"name\": \"" + boneName + "\", \"pivot\": [0, 24, 0]}]\n"
                + "  }]\n"
                + "}";
    }

    private static String createAnimJson(String identifier) {
        return "{\n"
                + "  \"format_version\": \"1.8.0\",\n"
                + "  \"animations\": {\n"
                + "    \"animation." + identifier + ".action\": {\"loop\": true, \"animation_length\": 1.5}\n"
                + "  }\n"
                + "}";
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    private static void assertFalse(boolean cond, String msg) {
        if (cond) throw new AssertionError(msg);
    }
}
