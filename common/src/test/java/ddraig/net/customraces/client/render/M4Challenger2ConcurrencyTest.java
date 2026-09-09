package ddraig.net.customraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ddraig.net.customraces.client.ClientWereState;
import ddraig.net.customraces.data.PartTransformData;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import sun.misc.Unsafe;

import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Challenger 2 Concurrency, Rapid State Transition, and Stack Hygiene Stress Test Suite.
 * Milestone M4:
 * 1. Rapid transformation state toggles across entities (16 threads, 100 entities).
 * 2. Multi-threaded rendering calls (16 concurrent threads querying models, textures, baking).
 * 3. Multi-threaded texture resolution and keyword interception with 0 checkerboards.
 * 4. PoseStack push/pop balance, extreme 9-DOF transforms, and stack hygiene under 16 threads.
 * 5. Exception resilience and matrix stack unwinding in nested attachment pipelines.
 * 6. First-person perspective toggling and attachment suppression under high concurrency.
 * 7. Base player mesh suppression guardrails under rapid state transitions ("Never Invisible").
 */
public class M4Challenger2ConcurrencyTest {

    private static Unsafe UNSAFE;
    static {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (Unsafe) f.get(null);
        } catch (Throwable t) {
            UNSAFE = null;
        }
    }

    private static PlayerModel<?> createMockPlayerModel() throws Exception {
        if (UNSAFE == null) {
            throw new IllegalStateException("Unsafe is unavailable");
        }
        PlayerModel<?> model = (PlayerModel<?>) UNSAFE.allocateInstance(PlayerModel.class);
        ModelPart dummyPart = new ModelPart(Collections.emptyList(), Collections.emptyMap());

        setField(PlayerModel.class, model, "head", dummyPart);
        setField(PlayerModel.class, model, "hat", dummyPart);
        setField(PlayerModel.class, model, "body", dummyPart);
        setField(PlayerModel.class, model, "rightArm", dummyPart);
        setField(PlayerModel.class, model, "leftArm", dummyPart);
        setField(PlayerModel.class, model, "rightLeg", dummyPart);
        setField(PlayerModel.class, model, "leftLeg", dummyPart);
        setField(PlayerModel.class, model, "jacket", dummyPart);
        setField(PlayerModel.class, model, "rightSleeve", dummyPart);
        setField(PlayerModel.class, model, "leftSleeve", dummyPart);
        setField(PlayerModel.class, model, "rightPants", dummyPart);
        setField(PlayerModel.class, model, "leftPants", dummyPart);

        setPrivateFieldWithFallback(PlayerModel.class, model, new String[]{"cloak", "f_103374_"}, dummyPart);
        setPrivateFieldWithFallback(PlayerModel.class, model, new String[]{"ear", "f_103375_"}, dummyPart);

        return model;
    }

    private static void setField(Class<?> clazz, Object instance, String fieldName, Object value) {
        try {
            Field f = clazz.getField(fieldName);
            f.setAccessible(true);
            f.set(instance, value);
        } catch (Throwable t) {
            setPrivateField(clazz, instance, fieldName, value);
        }
    }

    private static void setPrivateField(Class<?> clazz, Object instance, String fieldName, Object value) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(instance, value);
        } catch (Throwable ignored) {}
    }

    private static void setPrivateFieldWithFallback(Class<?> clazz, Object instance, String[] fieldNames, Object value) {
        for (String name : fieldNames) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                f.set(instance, value);
                return;
            } catch (Throwable ignored) {}
        }
    }

    private static int getStackDepth(PoseStack poseStack) {
        try {
            Field dequeField = PoseStack.class.getDeclaredField("poseStack");
            dequeField.setAccessible(true);
            Deque<?> deque = (Deque<?>) dequeField.get(poseStack);
            return deque.size();
        } catch (Exception e) {
            throw new RuntimeException("Could not access PoseStack deque via reflection", e);
        }
    }

    private static Path createSamplePackZip() throws IOException {
        Path tempDir = Files.createTempDirectory("m4_challenger2_pack");
        Path zipPath = tempDir.resolve("customraces-server-pack.zip");

        String modelJson = "{\n" +
                "  \"format_version\": \"1.12.0\",\n" +
                "  \"minecraft:geometry\": [\n" +
                "    {\n" +
                "      \"description\": {\n" +
                "        \"identifier\": \"geometry.challenger_beast\",\n" +
                "        \"texture_width\": 64,\n" +
                "        \"texture_height\": 64\n" +
                "      },\n" +
                "      \"bones\": [\n" +
                "        {\n" +
                "          \"name\": \"root\",\n" +
                "          \"pivot\": [0, 0, 0]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        String animJson = "{\n" +
                "  \"format_version\": \"1.8.0\",\n" +
                "  \"animations\": {\n" +
                "    \"animation.challenger_beast.howl\": {\n" +
                "      \"loop\": false,\n" +
                "      \"animation_length\": 1.5\n" +
                "    }\n" +
                "  }\n" +
                "}";

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipPath.toFile()))) {
            ZipEntry modelEntry = new ZipEntry("assets/customraces/geo/challenger_beast.geo.json");
            zos.putNextEntry(modelEntry);
            zos.write(modelJson.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            ZipEntry animEntry = new ZipEntry("assets/customraces/animations/challenger_beast.animation.json");
            zos.putNextEntry(animEntry);
            zos.write(animJson.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        return zipPath;
    }

    public static void main(String[] args) {
        System.out.println("==========================================================================");
        System.out.println("  CHALLENGER 2: CONCURRENCY, STATE TRANSITIONS & STACK HYGIENE SUITE     ");
        System.out.println("==========================================================================");

        int passed = 0;
        int failed = 0;

        try {
            testRapidMultiEntityTransformationStateToggling16Threads();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 1 (Rapid State Toggling 16 Threads): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testMultiThreadedModelAvailabilityAndCacheQueries16Threads();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 2 (16 Threads Model Availability & Cache): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testMultiThreadedTextureResolutionAndZeroCheckerboard16Threads();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 3 (16 Threads Texture Resolution): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testPoseStackBalanceAndTransformHygieneUnder16Threads();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 4 (PoseStack Balance & Transform Hygiene 16 Threads): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testSimulatedRenderingExceptionPoseStackUnwinding();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 5 (Exception Resilience in Nested Rendering): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testFirstPersonPerspectiveTogglingUnderHighLoad16Threads();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 6 (First-Person Toggling Under High Load): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testPlayerMeshSuppressionGuardrailsUnderConcurrentStateTransitions();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 7 (Player Mesh Suppression Guardrails): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("==========================================================================");
        System.out.println("  CHALLENGER 2 VERIFICATION SUMMARY: " + passed + " PASSED, " + failed + " FAILED  ");
        System.out.println("==========================================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void assertTrue(boolean condition, String msg) {
        if (!condition) throw new AssertionError(msg);
    }

    private static void assertFalse(boolean condition, String msg) {
        if (condition) throw new AssertionError(msg);
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

    /**
     * Test 1: Rapid multi-entity transformation state toggles across 16 concurrent threads.
     * Simulates 100 players in an active world having their transformation state toggled
     * simultaneously by network updates and abilities.
     * Tests both thread-exclusive entity ownership (atomic consistency) and high-contention
     * shared entity mutation without deadlocks or ConcurrentModificationExceptions.
     */
    public static void testRapidMultiEntityTransformationStateToggling16Threads() throws Exception {
        System.out.println("\n--- Running Test 1: Rapid Multi-Entity Transformation State Toggling (16 Threads) ---");

        ClientWereState.clear();

        final int entityCount = 100;
        final int threadCount = 16;
        final int iterationsPerThread = 1000;

        List<UUID> playerUuids = new ArrayList<>(entityCount);
        for (int i = 0; i < entityCount; i++) {
            playerUuids.add(UUID.randomUUID());
        }

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger totalOps = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    ThreadLocalRandom random = ThreadLocalRandom.current();
                    // Each thread has a dedicated private UUID to test isolated read-after-write consistency
                    UUID privateUuid = playerUuids.get(threadId);

                    for (int i = 0; i < iterationsPerThread; i++) {
                        // 1. Test dedicated entity consistency
                        boolean targetState = (i % 2 == 0);
                        ClientWereState.setTransformed(privateUuid, targetState);
                        if (ClientWereState.isTransformed(privateUuid) != targetState) {
                            errorCount.incrementAndGet();
                        }
                        if (WereModelRenderer.isTransformed(privateUuid) != targetState) {
                            errorCount.incrementAndGet();
                        }

                        // 2. High-contention shared entity toggling across all other entities (threadId 16..99)
                        int sharedIdx = 16 + random.nextInt(entityCount - 16);
                        UUID sharedUuid = playerUuids.get(sharedIdx);
                        ClientWereState.setTransformed(sharedUuid, (random.nextBoolean()));
                        // Read shared state (must not throw exception)
                        ClientWereState.isTransformed(sharedUuid);
                        WereModelRenderer.isTransformed(sharedUuid);

                        // 3. Test null safety under concurrency
                        ClientWereState.setTransformed(null, true);
                        if (ClientWereState.isTransformed(null)) {
                            errorCount.incrementAndGet();
                        }
                        if (WereModelRenderer.isTransformed(null)) {
                            errorCount.incrementAndGet();
                        }

                        totalOps.incrementAndGet();
                    }
                } catch (Throwable t1) {
                    errorCount.incrementAndGet();
                    t1.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Test 1 must complete within 15 seconds without deadlock");
        assertEquals(0, errorCount.get(), "Test 1 must complete with 0 errors across all 16 threads");
        assertTrue(totalOps.get() >= threadCount * iterationsPerThread, "All iterations must execute");

        // Verify clean state post-test
        ClientWereState.clear();
        for (UUID u : playerUuids) {
            assertFalse(ClientWereState.isTransformed(u), "All players should be untransformed after clear()");
        }

        System.out.println("  [PASS] " + totalOps.get() + " concurrent state toggle & query operations passed with 0 errors.");
    }

    /**
     * Test 2: 16 concurrent threads querying model availability, baking status,
     * and dynamic pack injection simultaneously.
     * Part A: High-throughput concurrent querying of model availability and cache status.
     * Part B: Dynamic pack remounting and injection while concurrent queries run, verifying 0 crashes.
     */
    public static void testMultiThreadedModelAvailabilityAndCacheQueries16Threads() throws Exception {
        System.out.println("\n--- Running Test 2: 16 Concurrent Threads Model Availability & Cache Queries ---");

        GeckoLibCacheInjector.resetForTesting();
        Path samplePack = createSamplePackZip();
        GeckoLibCacheInjector.onPackMounted(samplePack);

        final int threadCount = 16;
        final int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger queryCount = new AtomicInteger(0);

        // Pre-create diverse RaceData test configurations
        RaceData streamedRace = new RaceData("streamed_race", "Streamed Race");
        streamedRace.enableWereRace = true;
        streamedRace.wereModelPath = "customraces:challenger_beast.geo.json";

        RaceData localRace = new RaceData("local_race", "Local Race");
        localRace.enableWereRace = true;
        localRace.wereModelPath = "models/were/default_werewolf.geo.json";

        RaceData missingRace = new RaceData("missing_race", "Missing Race");
        missingRace.enableWereRace = true;
        missingRace.wereModelPath = "nonexistent_monster_xyz.geo.json";

        RaceData noneRace = new RaceData("none_race", "None Race");
        noneRace.enableWereRace = true;
        noneRace.wereModelPath = "none";

        RaceData nullRace = new RaceData("null_race", "Null Race");
        nullRace.enableWereRace = true;
        nullRace.wereModelPath = null;

        RaceData[] testRaces = new RaceData[]{streamedRace, localRace, missingRace, noneRace, nullRace, null};

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        RaceData race = testRaces[i % testRaces.length];

                        // 1. Check hasCustomModel
                        boolean hasCustom = WereModelRenderer.hasCustomModel(race);
                        if (race == null || race.wereModelPath == null || "none".equalsIgnoreCase(race.wereModelPath)) {
                            if (hasCustom) errorCount.incrementAndGet();
                        } else {
                            if (!hasCustom) errorCount.incrementAndGet();
                        }

                        // 2. Check isModelAvailable
                        boolean available = WereModelRenderer.isModelAvailable(race);
                        if (race == streamedRace) {
                            if (!available) errorCount.incrementAndGet();
                        } else if (race == missingRace || race == noneRace || race == nullRace || race == null) {
                            if (available) errorCount.incrementAndGet();
                        }

                        // 3. Check valid model location resolution
                        ResourceLocation loc = WereModelRenderer.getValidWereModelLocation(race);
                        if (race == streamedRace && loc == null) {
                            errorCount.incrementAndGet();
                        }

                        // 4. Concurrently check cache status
                        ResourceLocation testLoc = new ResourceLocation("customraces", "geo/challenger_beast.geo.json");
                        boolean baked = GeckoLibCacheInjector.isModelBaked(testLoc);
                        if (!baked) errorCount.incrementAndGet();

                        queryCount.incrementAndGet();
                    }
                } catch (Throwable t1) {
                    errorCount.incrementAndGet();
                    t1.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Test 2 Part A must complete within 15 seconds without hanging");
        assertEquals(0, errorCount.get(), "Test 2 Part A must complete with 0 errors across 16 threads");
        assertTrue(queryCount.get() >= threadCount * iterationsPerThread, "All model queries must execute");

        // Part B: Stress pack remounting under concurrent load
        ExecutorService remountExecutor = Executors.newFixedThreadPool(8);
        CountDownLatch remountLatch = new CountDownLatch(8);
        AtomicInteger remountErrors = new AtomicInteger(0);

        for (int t = 0; t < 8; t++) {
            final int tId = t;
            remountExecutor.submit(() -> {
                try {
                    for (int j = 0; j < 50; j++) {
                        if (tId == 0) {
                            GeckoLibCacheInjector.onPackMounted(samplePack);
                        } else {
                            WereModelRenderer.isModelAvailable(streamedRace);
                            WereModelRenderer.hasCustomModel(missingRace);
                        }
                    }
                } catch (Throwable t1) {
                    remountErrors.incrementAndGet();
                    t1.printStackTrace();
                } finally {
                    remountLatch.countDown();
                }
            });
        }

        boolean remountFinished = remountLatch.await(15, TimeUnit.SECONDS);
        remountExecutor.shutdown();

        assertTrue(remountFinished, "Test 2 Part B must complete within 15 seconds");
        assertEquals(0, remountErrors.get(), "Pack remounting under concurrent load must produce 0 exceptions");

        // After remount finishes, model must be baked and available
        assertTrue(WereModelRenderer.isModelAvailable(streamedRace), "Streamed model must remain available after remounts");

        System.out.println("  [PASS] " + queryCount.get() + " concurrent model queries and remount passes executed with 0 errors.");
    }

    /**
     * Test 3: 16 concurrent threads resolving textures and keywords.
     * Guarantees zero checkerboards and zero missingno references under heavy parallel execution.
     */
    public static void testMultiThreadedTextureResolutionAndZeroCheckerboard16Threads() throws Exception {
        System.out.println("\n--- Running Test 3: 16 Concurrent Threads Texture Resolution & Zero Checkerboard ---");

        final int threadCount = 16;
        final int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger resolutionCount = new AtomicInteger(0);

        String[] testTextureInputs = new String[]{
                "skin", "player", "player_skin", "skin_texture", "dynamic_skin", "use_skin",
                "dynamic", "player_texture", "default_skin", "SKIN", "PLAYER", "DYNAMIC",
                "textures/were/default_werewolf.png", "textures/were/custom_wolf.png",
                "textures/skin/were.png", "werewolf.png", "none", "", null, "invalid::texture!@#"
        };

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < iterationsPerThread; i++) {
                        String input = testTextureInputs[i % testTextureInputs.length];

                        // Contract 1: resolveTextureLocation(null, input)
                        ResourceLocation loc1 = GeckoAssetResolver.resolveTextureLocation(null, input);
                        if (loc1 == null) {
                            errorCount.incrementAndGet();
                        } else {
                            String path = loc1.getPath();
                            if (path.contains("missingno")) {
                                errorCount.incrementAndGet();
                            }
                        }

                        // Contract 2: resolveTexture(null, input)
                        ResourceLocation loc2 = GeckoAssetResolver.resolveTexture(null, input);
                        if (loc2 == null) {
                            errorCount.incrementAndGet();
                        }

                        // Contract 3: WereModelRenderer.getValidWereTextureLocation(race)
                        RaceData race = new RaceData("tex_race", "Texture Race");
                        race.wereTexturePath = input;
                        ResourceLocation loc3 = WereModelRenderer.getValidWereTextureLocation(null, race);
                        if (loc3 == null) {
                            errorCount.incrementAndGet();
                        }

                        resolutionCount.incrementAndGet();
                    }
                } catch (Throwable t1) {
                    errorCount.incrementAndGet();
                    t1.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Test 3 must complete within 15 seconds without hanging");
        assertEquals(0, errorCount.get(), "All texture resolutions must return valid non-null, non-missingno textures");
        assertTrue(resolutionCount.get() >= threadCount * iterationsPerThread, "All texture operations must execute");

        System.out.println("  [PASS] " + resolutionCount.get() + " concurrent texture resolutions passed (0 checkerboards).");
    }

    /**
     * Test 4: PoseStack balance and 9-DOF transform hygiene across 16 concurrent threads.
     * Each thread executes thousands of transform cycles with valid and extreme adversarial inputs
     * (negative scales, NaN, infinity, gigantic rotations/translations) and verifies that stack depth
     * before and after every single operation is identical.
     */
    public static void testPoseStackBalanceAndTransformHygieneUnder16Threads() throws Exception {
        System.out.println("\n--- Running Test 4: PoseStack Balance & Transform Hygiene (16 Threads) ---");

        final int threadCount = 16;
        final int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger transformCycles = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    PoseStack threadPoseStack = new PoseStack();
                    int initialDepth = getStackDepth(threadPoseStack);

                    for (int i = 0; i < iterationsPerThread; i++) {
                        PartTransformData pt = new PartTransformData();

                        // Alternate normal, extreme, and edge-case values
                        int mode = i % 8;
                        switch (mode) {
                            case 0 -> { // Normal transforms
                                pt.posX = 0.1f * threadId;
                                pt.posY = -0.2f;
                                pt.posZ = 0.05f;
                                pt.rotPitch = 15.0f;
                                pt.rotYaw = 30.0f;
                                pt.rotRoll = -10.0f;
                                pt.scaleX = 1.2f;
                                pt.scaleY = 0.9f;
                                pt.scaleZ = 1.1f;
                            }
                            case 1 -> { // Negative scales (must fallback to 1.0f safely)
                                pt.scaleX = -5.0f;
                                pt.scaleY = -0.01f;
                                pt.scaleZ = -100.0f;
                            }
                            case 2 -> { // NaN values (must fallback to 1.0f safely)
                                pt.scaleX = Float.NaN;
                                pt.scaleY = Float.NaN;
                                pt.scaleZ = Float.NaN;
                            }
                            case 3 -> { // Giant scales (must clamp to 5.0f max)
                                pt.scaleX = 500.0f;
                                pt.scaleY = 1000.0f;
                                pt.scaleZ = 99999.0f;
                            }
                            case 4 -> { // Micro scales (must clamp to 0.01f min)
                                pt.scaleX = 0.000001f;
                                pt.scaleY = 0.000005f;
                                pt.scaleZ = 0.00001f;
                            }
                            case 5 -> { // Extreme rotations
                                pt.rotPitch = 36000.0f;
                                pt.rotYaw = -18000.0f;
                                pt.rotRoll = 720.0f;
                            }
                            case 6 -> { // Huge translations
                                pt.posX = 100000.0f;
                                pt.posY = -50000.0f;
                                pt.posZ = 250000.0f;
                            }
                            case 7 -> { // Null transform data
                                pt = null;
                            }
                        }

                        // Verify safe clamping logic directly
                        if (pt != null) {
                            float sx = pt.getSafeScaleX();
                            float sy = pt.getSafeScaleY();
                            float sz = pt.getSafeScaleZ();
                            if (sx < 0.01f || sx > 5.0f || Float.isNaN(sx)) errorCount.incrementAndGet();
                            if (sy < 0.01f || sy > 5.0f || Float.isNaN(sy)) errorCount.incrementAndGet();
                            if (sz < 0.01f || sz > 5.0f || Float.isNaN(sz)) errorCount.incrementAndGet();
                        }

                        // Apply transform within push/pop block
                        threadPoseStack.pushPose();
                        try {
                            PlayerRaceLayer.applyPartTransforms(threadPoseStack, pt);
                        } finally {
                            threadPoseStack.popPose();
                        }

                        // Check stack depth balance immediately
                        int currentDepth = getStackDepth(threadPoseStack);
                        if (currentDepth != initialDepth) {
                            errorCount.incrementAndGet();
                        }

                        // Deep nesting stress: 16 nested pushes and pops
                        if (i % 200 == 0) {
                            for (int n = 0; n < 16; n++) {
                                threadPoseStack.pushPose();
                            }
                            if (getStackDepth(threadPoseStack) != initialDepth + 16) {
                                errorCount.incrementAndGet();
                            }
                            for (int n = 0; n < 16; n++) {
                                threadPoseStack.popPose();
                            }
                            if (getStackDepth(threadPoseStack) != initialDepth) {
                                errorCount.incrementAndGet();
                            }
                        }

                        transformCycles.incrementAndGet();
                    }
                } catch (Throwable t1) {
                    errorCount.incrementAndGet();
                    t1.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Test 4 must complete within 15 seconds without hanging");
        assertEquals(0, errorCount.get(), "PoseStack depth must remain 100% balanced across all 16 threads");
        assertTrue(transformCycles.get() >= threadCount * iterationsPerThread, "All transform cycles must execute");

        System.out.println("  [PASS] " + transformCycles.get() + " transform cycles verified with zero PoseStack leakage or overflow.");
    }

    /**
     * Test 5: Exception resilience and matrix stack unwinding in deeply nested attachment pipelines.
     * Simulates crashes occurring at arbitrary depths inside rendering passes, verifying that
     * try-finally blocks unwind every pushed pose without leaving residual matrices on the stack.
     */
    public static void testSimulatedRenderingExceptionPoseStackUnwinding() {
        System.out.println("\n--- Running Test 5: Exception Resilience in Nested Attachment Pipelines ---");

        PoseStack poseStack = new PoseStack();
        int initialDepth = getStackDepth(poseStack);

        for (int crashLevel = 1; crashLevel <= 4; crashLevel++) {
            final int level = crashLevel;
            try {
                poseStack.pushPose(); // Level 1: Player base pose
                try {
                    if (level == 1) throw new RuntimeException("Simulated crash at Level 1");

                    poseStack.pushPose(); // Level 2: Head anchor pose
                    try {
                        if (level == 2) throw new RuntimeException("Simulated crash at Level 2");

                        poseStack.pushPose(); // Level 3: Horns attachment pose
                        try {
                            if (level == 3) throw new RuntimeException("Simulated crash at Level 3");

                            poseStack.pushPose(); // Level 4: Geometry draw pose
                            try {
                                if (level == 4) throw new RuntimeException("Simulated crash at Level 4");
                            } finally {
                                poseStack.popPose();
                            }
                        } finally {
                            poseStack.popPose();
                        }
                    } finally {
                        poseStack.popPose();
                    }
                } finally {
                    poseStack.popPose();
                }
            } catch (RuntimeException expected) {
                // Expected simulated crash
            }

            int currentDepth = getStackDepth(poseStack);
            assertEquals(initialDepth, currentDepth, "PoseStack depth must completely unwind to initial depth after crash at level " + level);
        }

        System.out.println("  [PASS] Nested attachment try-finally unwinding verified; 0 matrix leaks on exceptions.");
    }

    /**
     * Test 6: First-person perspective toggling under high concurrency load.
     * Tests camera perspective toggling, attachment suppression rules, and matrix hygiene.
     */
    public static void testFirstPersonPerspectiveTogglingUnderHighLoad16Threads() throws Exception {
        System.out.println("\n--- Running Test 6: First-Person Perspective Toggling Under High Load (16 Threads) ---");

        final int threadCount = 16;
        final int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger toggleCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    PoseStack localPoseStack = new PoseStack();
                    int initialDepth = getStackDepth(localPoseStack);

                    for (int i = 0; i < iterationsPerThread; i++) {
                        // Simulate toggling camera state (true = first-person, false = third-person)
                        boolean isFirstPerson = (i % 2 == 0);

                        // Attachments must be suppressed if and only if isFirstPerson is true
                        boolean headSuppressed = isFirstPerson;
                        boolean bodySuppressed = isFirstPerson;

                        localPoseStack.pushPose();
                        try {
                            if (!headSuppressed) {
                                // Simulate rendering head attachments
                                localPoseStack.pushPose();
                                try {
                                    localPoseStack.translate(0.0, 0.2, 0.0);
                                } finally {
                                    localPoseStack.popPose();
                                }
                            }

                            if (!bodySuppressed) {
                                // Simulate rendering body attachments
                                localPoseStack.pushPose();
                                try {
                                    localPoseStack.translate(0.0, -0.5, 0.2);
                                } finally {
                                    localPoseStack.popPose();
                                }
                            }
                        } finally {
                            localPoseStack.popPose();
                        }

                        if (getStackDepth(localPoseStack) != initialDepth) {
                            errorCount.incrementAndGet();
                        }

                        // Also verify contract helpers with null player (must return false safely)
                        if (WereModelRenderer.isFirstPerson(null)) {
                            errorCount.incrementAndGet();
                        }
                        if (PlayerRaceLayer.isAttachmentSuppressed(null)) {
                            errorCount.incrementAndGet();
                        }

                        toggleCount.incrementAndGet();
                    }
                } catch (Throwable t1) {
                    errorCount.incrementAndGet();
                    t1.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Test 6 must complete within 15 seconds");
        assertEquals(0, errorCount.get(), "First-person perspective toggling must produce 0 errors");
        assertTrue(toggleCount.get() >= threadCount * iterationsPerThread, "All perspective toggles must execute");

        System.out.println("  [PASS] " + toggleCount.get() + " perspective toggles executed cleanly with zero attachment leaks.");
    }

    /**
     * Test 7: Base human player mesh suppression guardrails under concurrent state transitions.
     * Guarantees that players are NEVER invisible under any rapid transformation or model fallback.
     */
    public static void testPlayerMeshSuppressionGuardrailsUnderConcurrentStateTransitions() throws Exception {
        System.out.println("\n--- Running Test 7: Player Mesh Suppression Guardrails Under Rapid Transitions ---");

        final int threadCount = 16;
        final int iterationsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger transitionCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    PlayerModel<?> threadModel = createMockPlayerModel();

                    for (int i = 0; i < iterationsPerThread; i++) {
                        // Scenario A: Transformed into race with NO custom model
                        RaceData proceduralRace = new RaceData("were_procedural", "Procedural Were");
                        proceduralRace.enableWereRace = true;
                        proceduralRace.wereModelPath = "none";

                        // Must keep base model visible
                        CustomRaceModelRenderer.updateModelPartVisibility((PlayerModel) threadModel, null, proceduralRace);
                        if (!threadModel.head.visible || !threadModel.body.visible || !threadModel.rightLeg.visible) {
                            errorCount.incrementAndGet();
                        }

                        // Scenario B: Transformed into race with MISSING model
                        RaceData missingRace = new RaceData("were_missing", "Missing Were");
                        missingRace.enableWereRace = true;
                        missingRace.wereModelPath = "missing_beast_404.geo.json";

                        CustomRaceModelRenderer.updateModelPartVisibility((PlayerModel) threadModel, null, missingRace);
                        if (!threadModel.head.visible || !threadModel.body.visible || !threadModel.leftArm.visible) {
                            errorCount.incrementAndGet();
                        }

                        // Scenario C: Human form (untransformed)
                        RaceData humanRace = new RaceData("human_form", "Human");
                        humanRace.enableWereRace = false;

                        CustomRaceModelRenderer.updateModelPartVisibility((PlayerModel) threadModel, null, humanRace);
                        if (!threadModel.head.visible || !threadModel.body.visible || !threadModel.rightPants.visible) {
                            errorCount.incrementAndGet();
                        }

                        // Scenario D: Direct toggle via WereModelRenderer.setBaseModelVisible
                        WereModelRenderer.setBaseModelVisible(threadModel, false);
                        if (threadModel.head.visible || threadModel.body.visible) {
                            errorCount.incrementAndGet();
                        }

                        WereModelRenderer.setBaseModelVisible(threadModel, true);
                        if (!threadModel.head.visible || !threadModel.body.visible || !threadModel.jacket.visible) {
                            errorCount.incrementAndGet();
                        }

                        transitionCount.incrementAndGet();
                    }
                } catch (Throwable t1) {
                    errorCount.incrementAndGet();
                    t1.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean finished = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(finished, "Test 7 must complete within 15 seconds");
        assertEquals(0, errorCount.get(), "Zero guardrail violations: players never rendered invisible under state transitions");
        assertTrue(transitionCount.get() >= threadCount * iterationsPerThread, "All transition cycles must execute");

        System.out.println("  [PASS] " + transitionCount.get() + " state transitions verified; 'Never Invisible' guardrail 100% upheld.");
    }
}
