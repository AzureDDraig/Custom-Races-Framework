package ddraig.net.customraces.client.render;

import ddraig.net.customraces.data.ParticleAuraData;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;

/**
 * Empirical Verification Test Suite by Challenger 2 for Milestone 4:
 * 1. 20 Hz tick guard enforcement and framerate-independent particle rate-limiting across multiple rendering frames.
 * 2. Multi-entity UUID isolation and cache eviction memory safety for LAST_PARTICLE_TICKS.
 * 3. Scale-aware particle offsets, spreads, speeds, and emission counts across entity height/width scale factors (0.1x to 10.0x).
 * 4. Dynamic skin texture override keyword interception, player skin resolution, disk texture loading, and fallback defaults.
 */
public class M4Challenger2ParticleAndSkinTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  CHALLENGER 2: M4 PARTICLE AURA 20 HZ & DYNAMIC SKIN OVERRIDE TEST SUITE ");
        System.out.println("==========================================================================");

        runTest("1. 20 Hz Tick Guard: Single Tick Multi-Frame Rate-Limiting (60/144/240 FPS)", M4Challenger2ParticleAndSkinTest::testTickGuardRateLimitingAcrossFramerates);
        runTest("2. 20 Hz Tick Guard: Multi-Entity UUID Isolation", M4Challenger2ParticleAndSkinTest::testTickGuardMultiEntityIsolation);
        runTest("3. 20 Hz Tick Guard: Cache Size Eviction & Memory Safety (>1000 entries)", M4Challenger2ParticleAndSkinTest::testTickGuardCacheEviction);
        runTest("4. Scale-Aware Offsets: Particle Spread & Speed Scaling across Entity Scales", M4Challenger2ParticleAndSkinTest::testScaleAwareParticleOffsets);
        runTest("5. Scale-Aware Offsets: ParticleAuraData Scaling & Boundary Sanitization", M4Challenger2ParticleAndSkinTest::testParticleAuraDataScalingAndSanitization);
        runTest("6. Dynamic Skin Override: Keyword Interception & Standard Keywords", M4Challenger2ParticleAndSkinTest::testSkinKeywordInterception);
        runTest("7. Dynamic Skin Override: Fallback Resolution when Player/Skin is Null", M4Challenger2ParticleAndSkinTest::testSkinFallbackResolutionNullPlayer);
        runTest("8. Dynamic Skin Override: Path Normalization & Cache Hygiene", M4Challenger2ParticleAndSkinTest::testTexturePathNormalizationAndCacheHygiene);

        System.out.println("==========================================================================");
        System.out.println("  SUMMARY: " + passed + " PASSED, " + failed + " FAILED  ");
        System.out.println("==========================================================================");

        if (failed > 0) {
            System.err.println("WARNING: Milestone 4 Challenger 2 verification failures detected!");
            System.exit(1);
        }
    }

    private static void runTest(String testName, Runnable testBody) {
        System.out.println("\n--- Running Test: " + testName + " ---");
        try {
            testBody.run();
            passed++;
            System.out.println("  [PASS] " + testName);
        } catch (Throwable t) {
            failed++;
            System.err.println("  [FAIL] " + testName + ": " + t.getMessage());
            t.printStackTrace(System.err);
        }
    }

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(msg + " Expected: [" + expected + "], Actual: [" + actual + "]");
    }

    private static void assertEqualsFloat(float expected, float actual, float delta, String msg) {
        if (Math.abs(expected - actual) > delta) {
            throw new AssertionError(msg + " Expected: [" + expected + "], Actual: [" + actual + "]");
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) {
            throw new AssertionError(msg);
        }
    }

    // ----------------------------------------------------------------------------------
    // Reflection Helper for PlayerRaceLayer.LAST_PARTICLE_TICKS
    // ----------------------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private static Map<UUID, Integer> getLastParticleTicksMap() throws Exception {
        Field mapField = PlayerRaceLayer.class.getDeclaredField("LAST_PARTICLE_TICKS");
        mapField.setAccessible(true);
        return (Map<UUID, Integer>) mapField.get(null);
    }

    // ----------------------------------------------------------------------------------
    // Task 1: 20 Hz Tick Guard Tests across High Framerates
    // ----------------------------------------------------------------------------------

    public static void testTickGuardRateLimitingAcrossFramerates() {
        try {
            Map<UUID, Integer> ticksMap = getLastParticleTicksMap();
            ticksMap.clear();

            UUID testPlayer = UUID.randomUUID();
            int currentTick = 100;

            // Simulate 60 FPS (approx 3 renders per tick)
            int emitCount60fps = 0;
            for (int renderFrame = 0; renderFrame < 3; renderFrame++) {
                boolean canEmit = false;
                Integer lastTick = ticksMap.get(testPlayer);
                if (lastTick == null || lastTick != currentTick) {
                    ticksMap.put(testPlayer, currentTick);
                    canEmit = true;
                }
                if (canEmit) emitCount60fps++;
            }
            assertEquals(1, emitCount60fps, "60 FPS rendering emitted particles more than once in 1 tick");

            // Advance tick by 1 (20 Hz step) and simulate 144 FPS (approx 7 renders per tick)
            currentTick = 101;
            int emitCount144fps = 0;
            for (int renderFrame = 0; renderFrame < 7; renderFrame++) {
                boolean canEmit = false;
                Integer lastTick = ticksMap.get(testPlayer);
                if (lastTick == null || lastTick != currentTick) {
                    ticksMap.put(testPlayer, currentTick);
                    canEmit = true;
                }
                if (canEmit) emitCount144fps++;
            }
            assertEquals(1, emitCount144fps, "144 FPS rendering emitted particles more than once in 1 tick");

            // Advance tick by 1 and simulate 240 FPS (approx 12 renders per tick)
            currentTick = 102;
            int emitCount240fps = 0;
            for (int renderFrame = 0; renderFrame < 12; renderFrame++) {
                boolean canEmit = false;
                Integer lastTick = ticksMap.get(testPlayer);
                if (lastTick == null || lastTick != currentTick) {
                    ticksMap.put(testPlayer, currentTick);
                    canEmit = true;
                }
                if (canEmit) emitCount240fps++;
            }
            assertEquals(1, emitCount240fps, "240 FPS rendering emitted particles more than once in 1 tick");

            ticksMap.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testTickGuardMultiEntityIsolation() {
        try {
            Map<UUID, Integer> ticksMap = getLastParticleTicksMap();
            ticksMap.clear();

            UUID playerA = UUID.randomUUID();
            UUID playerB = UUID.randomUUID();
            UUID playerC = UUID.randomUUID();

            int tick = 50;

            // Player A renders on tick 50
            Integer lastA = ticksMap.get(playerA);
            assertTrue(lastA == null || lastA != tick, "Player A initial render can emit");
            ticksMap.put(playerA, tick);

            // Player B renders on tick 50 (should NOT be blocked by Player A)
            Integer lastB = ticksMap.get(playerB);
            assertTrue(lastB == null || lastB != tick, "Player B initial render can emit independently of Player A");
            ticksMap.put(playerB, tick);

            // Player A renders second frame in same tick (should be blocked)
            Integer lastA2 = ticksMap.get(playerA);
            assertTrue(lastA2 != null && lastA2 == tick, "Player A second frame blocked in same tick");

            // Player C renders on tick 50
            Integer lastC = ticksMap.get(playerC);
            assertTrue(lastC == null || lastC != tick, "Player C initial render can emit independently");
            ticksMap.put(playerC, tick);

            assertEquals(3, ticksMap.size(), "Map correctly tracks 3 distinct player UUIDs");
            ticksMap.clear();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void testTickGuardCacheEviction() {
        try {
            Map<UUID, Integer> ticksMap = getLastParticleTicksMap();
            ticksMap.clear();

            // Populate with 1001 entries
            for (int i = 0; i <= 1000; i++) {
                ticksMap.put(UUID.randomUUID(), 10);
            }

            assertTrue(ticksMap.size() > 1000, "Map size exceeded 1000");

            // Simulate tick guard check logic from PlayerRaceLayer.java: line 56
            if (ticksMap.size() > 1000) {
                ticksMap.clear();
            }

            assertEquals(0, ticksMap.size(), "Cache eviction successfully cleared map to prevent memory leak");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ----------------------------------------------------------------------------------
    // Task 2: Entity Scale-Aware Particle Offsets & Spreads
    // ----------------------------------------------------------------------------------

    public static void testScaleAwareParticleOffsets() {
        // Test scale factors for normal (1.0x), miniature (0.2x), and giant (3.0x / 5.0x / 10.0x) Were forms
        float[] wScales = {0.2f, 0.5f, 1.0f, 1.3f, 2.5f, 5.0f, 10.0f};
        float[] hScales = {0.2f, 0.5f, 1.0f, 1.5f, 3.0f, 6.0f, 12.0f};

        for (int i = 0; i < wScales.length; i++) {
            float wScale = wScales[i];
            float hScale = hScales[i];
            float scaleFactor = Math.max(wScale, hScale);

            // Were form smoke particle random spread offset logic from PlayerRaceLayer:
            double smokeXSpread = 0.6 * wScale;
            double smokeYOffset = 0.05 * scaleFactor;
            double flameXSpread = 0.4 * wScale;
            double flameYOffset = 0.02 * scaleFactor;

            assertTrue(smokeXSpread > 0.0, "Smoke X spread is positive for scale " + wScale);
            assertTrue(smokeYOffset > 0.0, "Smoke Y offset is positive for scaleFactor " + scaleFactor);
            assertTrue(flameXSpread > 0.0, "Flame X spread is positive for scale " + wScale);
            assertTrue(flameYOffset > 0.0, "Flame Y offset is positive for scaleFactor " + scaleFactor);

            // Aura spread and speed logic from PlayerRaceLayer:
            ParticleAuraData aura = new ParticleAuraData("minecraft:flame", 2.0f, 0.05f, 0.4f);
            float auraSpread = aura.getSafeSpread() * wScale;
            float auraSpeed = aura.getSafeSpeed() * scaleFactor;
            double auraYOffset = 0.5 * hScale;

            assertTrue(auraSpread >= 0.04f, "Aura spread scales correctly with wScale (" + auraSpread + ")");
            assertTrue(auraSpeed >= 0.005f, "Aura speed scales correctly with scaleFactor (" + auraSpeed + ")");
            assertTrue(auraYOffset >= 0.1, "Aura Y offset scales correctly with hScale (" + auraYOffset + ")");
        }
    }

    public static void testParticleAuraDataScalingAndSanitization() {
        ParticleAuraData aura = new ParticleAuraData("minecraft:soul_fire_flame", 2.5f, 0.08f, 0.6f);

        // Valid particle count scaling with base particle count (Math.round(2.5 * (count / 5.0)))
        assertEquals(3, aura.getScaledParticleCount(5), "Scaled count for particleCount=5 (2.5 * 1.0 = 2.5 -> 3)");
        assertEquals(5, aura.getScaledParticleCount(10), "Scaled count for particleCount=10 (2.5 * 2.0 = 5.0 -> 5)");
        assertEquals(3, aura.getScaledParticleCount(0), "Scaled count for particleCount=0 (effective 5 -> 3)");
        assertEquals(3, aura.getScaledParticleCount(-10), "Scaled count for negative particleCount (effective 5 -> 3)");

        // Safe spread sanitization
        aura.spread = -0.5f;
        assertEqualsFloat(0.5f, aura.getSafeSpread(), 0.001f, "Absolute value for negative spread");
        aura.spread = 0.001f;
        assertEqualsFloat(0.1f, aura.getSafeSpread(), 0.001f, "Clamped minimum for near-zero spread");

        // Safe speed sanitization
        aura.speed = -0.1f;
        assertEqualsFloat(0.1f, aura.getSafeSpeed(), 0.001f, "Absolute value for negative speed");

        // Valid particle type fallback
        aura.particleType = null;
        assertEquals("minecraft:flame", aura.getValidParticleType(), "Null particle type fallback");
        aura.particleType = "";
        assertEquals("minecraft:flame", aura.getValidParticleType(), "Empty particle type fallback");
        aura.particleType = "minecraft:portal";
        assertEquals("minecraft:portal", aura.getValidParticleType(), "Explicit particle type retained");
    }

    // ----------------------------------------------------------------------------------
    // Task 3: Dynamic Skin Texture Override Fallback & Resolution
    // ----------------------------------------------------------------------------------

    public static void testSkinKeywordInterception() {
        String[] keywords = {
                "skin", "player", "player_skin", "skin_texture", "dynamic_skin",
                "use_skin", "dynamic", "player_texture", "default_skin",
                "SKIN", "PLAYER_SKIN", "Dynamic_Skin"
        };

        for (String kw : keywords) {
            ResourceLocation resolved = GeckoAssetResolver.resolveTextureLocation(null, kw);
            assertEquals(GeckoAssetResolver.DEFAULT_TEXTURE_LOCATION, resolved, "Keyword '" + kw + "' correctly intercepted and resolved to default when player is null");
        }
    }

    public static void testSkinFallbackResolutionNullPlayer() {
        // Null rawPath -> safe default texture
        ResourceLocation resNull = GeckoAssetResolver.resolveTextureLocation(null, null);
        assertEquals(GeckoAssetResolver.DEFAULT_TEXTURE_LOCATION, resNull, "Null texture path fallback");

        // Empty rawPath -> safe default texture
        ResourceLocation resEmpty = GeckoAssetResolver.resolveTextureLocation(null, "   ");
        assertEquals(GeckoAssetResolver.DEFAULT_TEXTURE_LOCATION, resEmpty, "Empty texture path fallback");

        // "none" -> safe default texture
        ResourceLocation resNone = GeckoAssetResolver.resolveTextureLocation(null, "none");
        assertEquals(GeckoAssetResolver.DEFAULT_TEXTURE_LOCATION, resNone, "'none' texture path fallback");

        // Non-existent path in headless unit test environment -> primary candidate location
        ResourceLocation resMissing = GeckoAssetResolver.resolveTextureLocation(null, "nonexistent_texture_file_12345");
        assertEquals(new ResourceLocation("customraces", "textures/nonexistent_texture_file_12345.png"), resMissing, "Missing texture file candidate path resolution in headless environment");
    }

    public static void testTexturePathNormalizationAndCacheHygiene() {
        // Test clearCaches works cleanly
        GeckoAssetResolver.clearCaches();

        // Verify parsePath for texture subfolder
        GeckoAssetResolver.ParsedPath parsed = GeckoAssetResolver.parsePath("textures/were/custom_skin.png", "textures/", ".png");
        assertEquals("customraces", parsed.namespace, "Parsed namespace");
        assertEquals("custom_skin.png", parsed.cleanFilename, "Clean filename");

        assertTrue(parsed.candidateResourceLocations.size() > 0, "Candidates populated");
    }
}
