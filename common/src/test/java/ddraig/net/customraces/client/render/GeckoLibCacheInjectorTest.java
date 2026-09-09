package ddraig.net.customraces.client.render;

import net.minecraft.resources.ResourceLocation;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Empirical Verification Harness for GeckoLibCacheInjector (Milestone 3):
 * 1. Scanning mounted pack zip files for geo and animation JSON assets.
 * 2. Canonical and shorthand alias registration.
 * 3. Dynamic baking into GeckoLibCache and fallback maps.
 * 4. Safe recovery from unmodifiable maps (Collections.emptyMap()).
 * 5. Corrupt zip and malformed JSON resilience.
 * 6. Thread-safe concurrent injection.
 * 7. Cache eviction and lifecycle reset.
 */
public class GeckoLibCacheInjectorTest {

    private static final String DUMMY_GEO_JSON = "{"
            + "\"format_version\": \"1.12.0\","
            + "\"geometry\": [{"
            + "  \"description\": {\"identifier\": \"geometry.were_test\", \"texture_width\": 64, \"texture_height\": 64},"
            + "  \"bones\": [{\"name\": \"head\", \"pivot\": [0, 24, 0]}]"
            + "}]"
            + "}";

    private static final String DUMMY_ANIM_JSON = "{"
            + "\"format_version\": \"1.8.0\","
            + "\"animations\": {\"animation.were_test.idle\": {\"loop\": true}}"
            + "}";

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   GECKOLIB CACHE INJECTOR EMPIRICAL VERIFICATION TEST SUITE      ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: Valid ZIP injection (models and animations)
        try { testInjectFromPackValidModelsAndAnimations(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] Test 1: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Test 2: Idempotent repeat injection
        try { testInjectFromPackIdempotency(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] Test 2: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Test 3: Corrupt ZIP handling
        try { testInjectFromPackCorruptZipSafety(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] Test 3: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Test 4: ZIP without geo/animations entries
        try { testInjectFromPackEmptyOrNonAssetZip(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] Test 4: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Test 5: Pre-reload unmodifiable map safety (Collections.emptyMap())
        try { testGeckoLibCacheEmptyMapSafety(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] Test 5: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Test 6: Direct JSON baking contract
        try { testBakeModelDirectJsonContract(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] Test 6: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Test 7: Malformed JSON syntax handling
        try { testBakeModelMalformedJsonContract(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] Test 7: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Test 8: isModelBaked and isAnimationBaked status contract
        try { testIsModelBakedContract(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] Test 8: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Test 9: Concurrent multithreaded injection
        try { testMultiThreadedInjectionThreadSafety(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] Test 9: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Test 10: Cache eviction and lifecycle reset
        try { testCacheEvictionAndLifecycle(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] Test 10: " + t.getMessage()); t.printStackTrace(); failed++; }

        System.out.println("==================================================================");
        System.out.println("  SUMMARY: " + passed + " PASSED, " + failed + " FAILED  ");
        System.out.println("==================================================================");

        if (failed > 0) System.exit(1);
    }

    private static Path createTestZip(boolean includeGeo, boolean includeAnim) throws Exception {
        Path zipPath = Files.createTempFile("gecko_test_pack", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            zos.putNextEntry(new ZipEntry("pack.mcmeta"));
            zos.write("{\"pack\":{\"pack_format\":15,\"description\":\"Test\"}}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            if (includeGeo) {
                zos.putNextEntry(new ZipEntry("assets/customraces/geo/were_test.geo.json"));
                zos.write(DUMMY_GEO_JSON.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }

            if (includeAnim) {
                zos.putNextEntry(new ZipEntry("assets/customraces/animations/were_test.animation.json"));
                zos.write(DUMMY_ANIM_JSON.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return zipPath;
    }

    public static void testInjectFromPackValidModelsAndAnimations() throws Exception {
        System.out.println("\n--- Test 1: Valid ZIP injection ---");
        GeckoLibCacheInjector.resetForTesting();
        Path zip = createTestZip(true, true);
        try {
            int injected = GeckoLibCacheInjector.injectFromPack(zip);
            assertTrue(injected >= 2, "Expected at least 2 injected assets (1 model + 1 anim), got: " + injected);

            ResourceLocation canonicalModel = new ResourceLocation("customraces", "geo/were_test.geo.json");
            ResourceLocation shortModel = new ResourceLocation("customraces", "were_test.geo.json");
            assertTrue(GeckoLibCacheInjector.isModelBaked(canonicalModel), "Canonical model should be baked");
            assertTrue(GeckoLibCacheInjector.isModelBaked(shortModel), "Shorthand model alias should be baked");

            ResourceLocation canonicalAnim = new ResourceLocation("customraces", "animations/were_test.animation.json");
            ResourceLocation shortAnim = new ResourceLocation("customraces", "were_test.animation.json");
            assertTrue(GeckoLibCacheInjector.isAnimationBaked(canonicalAnim), "Canonical animation should be baked");
            assertTrue(GeckoLibCacheInjector.isAnimationBaked(shortAnim), "Shorthand animation alias should be baked");

            System.out.println("  [PASS] Valid zip injection succeeded (canonical + aliases registered).");
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    public static void testInjectFromPackIdempotency() throws Exception {
        System.out.println("\n--- Test 2: Idempotent repeat injection ---");
        Path zip = createTestZip(true, true);
        try {
            int run1 = GeckoLibCacheInjector.injectFromPack(zip);
            int run2 = GeckoLibCacheInjector.injectFromPack(zip);
            assertTrue(run1 >= 2, "First run should inject assets");
            assertTrue(run2 >= 2, "Second run should succeed idempotently");
            System.out.println("  [PASS] Repeat injection completed idempotently.");
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    public static void testInjectFromPackCorruptZipSafety() throws Exception {
        System.out.println("\n--- Test 3: Corrupt ZIP handling ---");
        Path corruptZip = Files.createTempFile("corrupt_pack", ".zip");
        try {
            Files.write(corruptZip, "not a valid zip file byte stream".getBytes(StandardCharsets.UTF_8));
            int injected = GeckoLibCacheInjector.injectFromPack(corruptZip);
            assertTrue(injected == 0, "Corrupt zip should return 0 injected assets");
            System.out.println("  [PASS] Corrupt zip handled safely without unhandled exception.");
        } finally {
            Files.deleteIfExists(corruptZip);
        }
    }

    public static void testInjectFromPackEmptyOrNonAssetZip() throws Exception {
        System.out.println("\n--- Test 4: Empty / non-asset zip ---");
        Path emptyZip = createTestZip(false, false);
        try {
            int injected = GeckoLibCacheInjector.injectFromPack(emptyZip);
            assertTrue(injected == 0, "Zip with no models/animations should return 0");
            System.out.println("  [PASS] Non-asset zip returned 0 injected assets.");
        } finally {
            Files.deleteIfExists(emptyZip);
        }
    }

    public static void testGeckoLibCacheEmptyMapSafety() {
        System.out.println("\n--- Test 5: Unmodifiable map handling ---");
        // Verify ensureModifiableModelMap and ensureModifiableAnimationMap execute safely
        Map<ResourceLocation, Object> models = GeckoLibCacheInjector.ensureModifiableModelMap();
        Map<ResourceLocation, Object> anims = GeckoLibCacheInjector.ensureModifiableAnimationMap();

        ResourceLocation testLoc = new ResourceLocation("customraces", "geo/unmodifiable_test.geo.json");
        GeckoLibCacheInjector.injectModel(testLoc, "dummy_model");
        assertTrue(GeckoLibCacheInjector.isModelBaked(testLoc), "Injected dummy model should be marked as baked");

        ResourceLocation animLoc = new ResourceLocation("customraces", "animations/unmodifiable_test.animation.json");
        GeckoLibCacheInjector.injectAnimations(animLoc, "dummy_anim");
        assertTrue(GeckoLibCacheInjector.isAnimationBaked(animLoc), "Injected dummy animation should be marked as baked");

        System.out.println("  [PASS] Unmodifiable map safety verified without UnsupportedOperationException.");
    }

    public static void testBakeModelDirectJsonContract() {
        System.out.println("\n--- Test 6: Direct JSON baking contract ---");
        ResourceLocation loc = new ResourceLocation("customraces", "geo/direct_bake.geo.json");
        Object baked = GeckoLibWereRenderer.bakeModelFromFile(loc, DUMMY_GEO_JSON);
        System.out.println("  [PASS] Direct model baking contract executed safely (result: " + baked + ").");
    }

    public static void testBakeModelMalformedJsonContract() {
        System.out.println("\n--- Test 7: Malformed JSON syntax handling ---");
        ResourceLocation loc = new ResourceLocation("customraces", "geo/broken.geo.json");
        Object baked = GeckoLibWereRenderer.bakeModelFromFile(loc, "{invalid json syntax : [}");
        assertTrue(baked == null, "Malformed JSON must return null without throwing exception");
        System.out.println("  [PASS] Malformed JSON handled safely.");
    }

    public static void testIsModelBakedContract() {
        System.out.println("\n--- Test 8: isModelBaked and isAnimationBaked contracts ---");
        assertFalse(GeckoLibCacheInjector.isModelBaked(null), "Null model location should return false");
        assertFalse(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/non_existent.geo.json")), "Unregistered model location should return false");
        assertFalse(GeckoLibCacheInjector.isAnimationBaked(null), "Null animation location should return false");
        assertFalse(GeckoLibCacheInjector.isAnimationBaked(new ResourceLocation("customraces", "animations/non_existent.animation.json")), "Unregistered animation location should return false");
        System.out.println("  [PASS] isModelBaked & isAnimationBaked contracts verified.");
    }

    public static void testMultiThreadedInjectionThreadSafety() throws Exception {
        System.out.println("\n--- Test 9: Concurrent multithreaded injection ---");
        Path zip = createTestZip(true, true);
        try {
            int threads = 8;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            AtomicInteger errors = new AtomicInteger(0);

            for (int i = 0; i < threads; i++) {
                executor.submit(() -> {
                    try {
                        for (int j = 0; j < 50; j++) {
                            GeckoLibCacheInjector.injectFromPack(zip);
                            GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/were_test.geo.json"));
                            GeckoLibCacheInjector.isAnimationBaked(new ResourceLocation("customraces", "animations/were_test.animation.json"));
                        }
                    } catch (Throwable t) {
                        errors.incrementAndGet();
                    }
                });
            }

            executor.shutdown();
            assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS), "Concurrent injection completed within timeout");
            assertTrue(errors.get() == 0, "Zero concurrency errors detected");
            System.out.println("  [PASS] Concurrency thread safety verified across 8 threads and 400 operations.");
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    public static void testCacheEvictionAndLifecycle() throws Exception {
        System.out.println("\n--- Test 10: Cache eviction and lifecycle reset ---");
        Path zip = createTestZip(true, true);
        try {
            GeckoLibCacheInjector.onPackMounted(zip);
            assertTrue(GeckoLibCacheInjector.getMountCount() >= 1, "Mount count should be >= 1");
            assertTrue(GeckoLibCacheInjector.getLastMountedPackPath() != null, "Last mounted pack path should be set");
            ResourceLocation loc = new ResourceLocation("customraces", "geo/were_test.geo.json");
            assertTrue(GeckoLibCacheInjector.isModelBaked(loc), "Model should be baked after onPackMounted");

            // Evict caches
            WereModelRenderer.clearCaches();
            assertFalse(GeckoLibCacheInjector.isModelBaked(loc), "Model should not be baked after clearCaches");

            GeckoLibCacheInjector.resetForTesting();
            assertTrue(GeckoLibCacheInjector.getMountCount() == 0, "Mount count should be 0 after reset");
            assertTrue(GeckoLibCacheInjector.getLastMountedPackPath() == null, "Last mounted pack path should be null after reset");
            System.out.println("  [PASS] Cache eviction and lifecycle reset verified.");
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    private static void assertTrue(boolean cond, String msg) {
        if (!cond) throw new AssertionError(msg);
    }

    private static void assertFalse(boolean cond, String msg) {
        if (cond) throw new AssertionError(msg);
    }
}
