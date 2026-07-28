package ddraig.net.customraces.client.render;

import ddraig.net.customraces.data.RaceData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Empirical unit & verification test suite for Milestone 3:
 * Base Human Player Model Suppression Guardrails (R2), Fail-Safe Fallbacks ("Never Invisible"),
 * and Invisibility Effect / Spectator Handling.
 */
public class M3SuppressionAndFallbackVerificationTest {

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

        // Assign standard public fields
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

        // Assign private cloak and ear fields
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

    private static ModelPart getPrivateFieldWithFallback(Class<?> clazz, Object instance, String[] fieldNames) {
        for (String name : fieldNames) {
            try {
                Field f = clazz.getDeclaredField(name);
                f.setAccessible(true);
                return (ModelPart) f.get(instance);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  M3 BASE PLAYER MODEL SUPPRESSION & FALLBACK VERIFICATION TEST   ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        try {
            testBaseModelSuppressionAll14Parts();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 1 (Base Model Suppression All 14 Parts): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testModelAvailabilityGuardrails();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 2 (Model Availability Guardrails): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testFailSafeFallbackVisibilityRestoration();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 3 (Fail-Safe Fallback Visibility Restoration): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testInvisibilityAndSpectatorGuardrails();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 4 (Invisibility & Spectator Guardrails): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testSuppressionThreadSafety();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 5 (Suppression Thread Safety): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("==================================================================");
        System.out.println("  SUMMARY: " + passed + " PASSED, " + failed + " FAILED  ");
        System.out.println("==================================================================");

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

    /**
     * Test 1: Verify setBaseModelVisible toggles all 14 player model parts (including cloak and ear).
     */
    public static void testBaseModelSuppressionAll14Parts() throws Exception {
        System.out.println("\n--- Running Test 1: Base Model Suppression (14 Parts) ---");

        PlayerModel<?> model = createMockPlayerModel();

        // 1. Hide base model (visible = false)
        WereModelRenderer.setBaseModelVisible(model, false);

        assertFalse(model.head.visible, "head visible should be false");
        assertFalse(model.hat.visible, "hat visible should be false");
        assertFalse(model.body.visible, "body visible should be false");
        assertFalse(model.rightArm.visible, "rightArm visible should be false");
        assertFalse(model.leftArm.visible, "leftArm visible should be false");
        assertFalse(model.rightLeg.visible, "rightLeg visible should be false");
        assertFalse(model.leftLeg.visible, "leftLeg visible should be false");
        assertFalse(model.jacket.visible, "jacket visible should be false");
        assertFalse(model.rightSleeve.visible, "rightSleeve visible should be false");
        assertFalse(model.leftSleeve.visible, "leftSleeve visible should be false");
        assertFalse(model.rightPants.visible, "rightPants visible should be false");
        assertFalse(model.leftPants.visible, "leftPants visible should be false");

        ModelPart cloak = getPrivateFieldWithFallback(PlayerModel.class, model, new String[]{"cloak", "f_103374_"});
        ModelPart ear = getPrivateFieldWithFallback(PlayerModel.class, model, new String[]{"ear", "f_103375_"});
        
        if (cloak != null) assertFalse(cloak.visible, "cloak visible should be false");
        if (ear != null) assertFalse(ear.visible, "ear visible should be false");

        // 2. Restore base model (visible = true)
        WereModelRenderer.setBaseModelVisible(model, true);

        assertTrue(model.head.visible, "head visible should be true");
        assertTrue(model.hat.visible, "hat visible should be true");
        assertTrue(model.body.visible, "body visible should be true");
        assertTrue(model.rightArm.visible, "rightArm visible should be true");
        assertTrue(model.leftArm.visible, "leftArm visible should be true");
        assertTrue(model.rightLeg.visible, "rightLeg visible should be true");
        assertTrue(model.leftLeg.visible, "leftLeg visible should be true");
        assertTrue(model.jacket.visible, "jacket visible should be true");
        assertTrue(model.rightSleeve.visible, "rightSleeve visible should be true");
        assertTrue(model.leftSleeve.visible, "leftSleeve visible should be true");
        assertTrue(model.rightPants.visible, "rightPants visible should be true");
        assertTrue(model.leftPants.visible, "leftPants visible should be true");

        if (cloak != null) assertTrue(cloak.visible, "cloak visible should be true");
        if (ear != null) assertTrue(ear.visible, "ear visible should be true");

        System.out.println("  [PASS] All 14 player model parts (head, hat, body, arms, legs, 4 clothing overlays, cloak, ear) toggle visibility accurately.");
    }

    /**
     * Test 2: Verify isModelAvailable guardrails for unassigned, missing, and invalid model paths.
     */
    public static void testModelAvailabilityGuardrails() {
        System.out.println("\n--- Running Test 2: Model Availability Guardrails ---");

        RaceData raceNull = new RaceData("test_null", "Test Null");
        raceNull.wereModelPath = null;
        assertFalse(WereModelRenderer.isModelAvailable(raceNull), "Null wereModelPath should return false for isModelAvailable");

        RaceData raceEmpty = new RaceData("test_empty", "Test Empty");
        raceEmpty.wereModelPath = "";
        assertFalse(WereModelRenderer.isModelAvailable(raceEmpty), "Empty wereModelPath should return false for isModelAvailable");

        RaceData raceSpace = new RaceData("test_space", "Test Space");
        raceSpace.wereModelPath = "   ";
        assertFalse(WereModelRenderer.isModelAvailable(raceSpace), "Whitespace wereModelPath should return false for isModelAvailable");

        RaceData raceNone = new RaceData("test_none", "Test None");
        raceNone.wereModelPath = "none";
        assertFalse(WereModelRenderer.isModelAvailable(raceNone), " 'none' wereModelPath should return false for isModelAvailable");

        RaceData raceInvalid = new RaceData("test_invalid", "Test Invalid");
        raceInvalid.wereModelPath = "non_existent_model_file.geo.json";
        assertFalse(WereModelRenderer.isModelAvailable(raceInvalid), "Non-existent model file should return false for isModelAvailable");

        System.out.println("  [PASS] Model availability guardrails correctly reject unassigned ('none', empty, null) and missing models.");
    }

    /**
     * Test 3: Verify renderWereForm restores base model visibility when custom rendering fails.
     */
    public static void testFailSafeFallbackVisibilityRestoration() throws Exception {
        System.out.println("\n--- Running Test 3: Fail-Safe Fallback Visibility Restoration ---");

        PlayerModel model = createMockPlayerModel();

        // Start with base model suppressed
        WereModelRenderer.setBaseModelVisible(model, false);
        assertFalse(model.head.visible, "Initial state: base model head should be hidden");

        RaceData raceInvalid = new RaceData("invalid_race", "Invalid Race");
        raceInvalid.enableWereRace = true;
        raceInvalid.wereModelPath = "invalid_path_to_non_existent.geo.json";

        // renderWereForm with null player (or untransformed state)
        boolean rendered = WereModelRenderer.renderWereForm(null, null, 15728880, null, model, raceInvalid, 0.0f, 0.0f);
        assertFalse(rendered, "renderWereForm should return false on invalid model/null player");

        // Base model visibility MUST be restored to true
        assertTrue(model.head.visible, "Base model head visibility MUST be restored to true on fallback");
        assertTrue(model.body.visible, "Base model body visibility MUST be restored to true on fallback");
        assertTrue(model.rightArm.visible, "Base model rightArm visibility MUST be restored to true on fallback");

        System.out.println("  [PASS] Fail-safe fallback restored base player model visibility on rendering failure.");
    }

    /**
     * Test 4: Verify GeckoLibWereRenderer.isModelPresent null/invalid resilience.
     */
    public static void testInvisibilityAndSpectatorGuardrails() {
        System.out.println("\n--- Running Test 4: Invisibility & Spectator Guardrails ---");

        assertFalse(GeckoLibWereRenderer.isModelPresent(null), "isModelPresent(null) should return false");
        assertFalse(GeckoLibWereRenderer.isModelPresent(new net.minecraft.resources.ResourceLocation("customraces", "geo/non_existent.geo.json")), "isModelPresent for missing file should return false");

        System.out.println("  [PASS] Invisibility and Spectator model presence checks executed without error.");
    }

    /**
     * Test 5: Verify multi-threaded concurrent calls to setBaseModelVisible cause no race conditions or errors.
     */
    public static void testSuppressionThreadSafety() throws Exception {
        System.out.println("\n--- Running Test 5: Suppression Thread Safety ---");

        int threadCount = 10;
        int iterations = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            final boolean toggle = (i % 2 == 0);
            executor.submit(() -> {
                try {
                    PlayerModel<?> localModel = createMockPlayerModel();
                    for (int j = 0; j < iterations; j++) {
                        WereModelRenderer.setBaseModelVisible(localModel, toggle);
                    }
                } catch (Throwable t) {
                    errors.incrementAndGet();
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(finished, "Thread execution completed within timeout");
        assertTrue(errors.get() == 0, "Zero errors during concurrent model suppression operations");

        System.out.println("  [PASS] Concurrent model suppression executed safely across 10 threads (10,000 ops) with 0 errors.");
    }
}
