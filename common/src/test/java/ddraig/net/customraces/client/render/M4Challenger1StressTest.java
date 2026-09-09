package ddraig.net.customraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ddraig.net.customraces.data.PartTransformData;
import ddraig.net.customraces.data.RaceData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Deque;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Challenger 1 Adversarial Stress Test Suite for Milestone M4:
 * Were-Form & Body Part Attachment Rendering & Verification.
 *
 * Empirical Corner-Case Matrix:
 * 1. Scale extremes (0.0, negative, NaN, positive infinity, negative infinity, 1000.0, micro-scales)
 *    on 9-DOF transforms and matrix stack hygiene.
 * 2. Invalid, null, empty, whitespace-only, and unbaked model identifiers.
 * 3. Path traversal attacks (../../ traversal, backslashes, absolute paths, special characters).
 * 4. Missing texture fallbacks, keyword interception ("skin", "player"), and zero checkerboards.
 * 5. Player mesh suppression guardrails ("Never Invisible") under invalid and failing models.
 * 6. Procedural were-beast fallback geometry, hex color resilience, and first-person suppression.
 * 7. Multithreaded concurrency stress hammer (12 threads, 12,000 operations).
 */
public class M4Challenger1StressTest {

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

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  M4 CHALLENGER 1 EMPIRICAL ADVERSARIAL STRESS TEST SUITE         ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        // Group 1: Scale Extremes on 9-DOF Transform Behavior & Matrix Hygiene
        try {
            testScaleExtremesAndBoundsClamping();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 1 (Scale Extremes & Bounds Clamping): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testPoseStackHygieneUnderExtremeTransforms();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 2 (PoseStack Hygiene Under Extreme Transforms): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Group 2: Invalid / Null / Empty Model Identifiers & Path Traversal Attacks
        try {
            testInvalidAndNullModelIdentifiers();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 3 (Invalid & Null Model Identifiers): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testPathTraversalAttemptsOnModelsAndTextures();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 4 (Path Traversal Attempts): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Group 3: Player Mesh Suppression Guardrails ("Never Invisible")
        try {
            testPlayerMeshSuppressionGuardrailsOnAdversarialModels();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 5 (Player Mesh Suppression Guardrails): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Group 4: Texture Resolution Extremes & Zero Checkerboard Fallback
        try {
            testTextureResolutionExtremesAndFallbacks();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 6 (Texture Resolution Extremes & Fallbacks): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Group 5: Procedural Were-Beast Fallback, Color Parsing, and View Suppression
        try {
            testProceduralFallbackAndColorResilience();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 7 (Procedural Fallback & Color Resilience): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Group 6: Massive Multithreaded Concurrency Stress Hammer
        try {
            testMassiveMultithreadedConcurrencyStress();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 8 (Massive Multithreaded Concurrency Stress): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("==================================================================");
        System.out.println("  M4 CHALLENGER 1 SUMMARY: " + passed + " PASSED, " + failed + " FAILED  ");
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
     * Test 1: Stress-test scale extremes on PartTransformData:
     * - scale = 0.0f -> safe fallback to 1.0f
     * - negative scales (-0.0001f, -1.0f, -100.0f, -Float.MAX_VALUE, Float.NEGATIVE_INFINITY) -> fallback to 1.0f
     * - Float.NaN -> safe fallback to 1.0f
     * - scale = Float.POSITIVE_INFINITY -> clamped to 5.0f
     * - scale = 1000.0f, 10000.0f, Float.MAX_VALUE -> clamped to 5.0f
     * - micro scales (0.00001f, 0.0099f) -> clamped to 0.01f
     * - valid values (0.01f, 0.5f, 1.0f, 2.5f, 5.0f) -> preserved
     */
    public static void testScaleExtremesAndBoundsClamping() {
        System.out.println("\n--- Running Test 1: Scale Extremes & Bounds Clamping ---");

        PartTransformData pt = new PartTransformData();

        // 1. Zero Scale
        pt.scaleX = 0.0f;
        pt.scaleY = -0.0f;
        pt.scaleZ = 0.0000000f;
        assertEqualsFloat(1.0f, pt.getSafeScaleX(), 0.0001f, "Zero scaleX should fallback to 1.0f");
        assertEqualsFloat(1.0f, pt.getSafeScaleY(), 0.0001f, "Zero scaleY should fallback to 1.0f");
        assertEqualsFloat(1.0f, pt.getSafeScaleZ(), 0.0001f, "Zero scaleZ should fallback to 1.0f");

        // 2. Negative Scales
        float[] negativeScales = {-0.0001f, -0.5f, -1.0f, -10.0f, -100.0f, -Float.MAX_VALUE, Float.NEGATIVE_INFINITY};
        for (float neg : negativeScales) {
            pt.scaleX = neg;
            pt.scaleY = neg;
            pt.scaleZ = neg;
            assertEqualsFloat(1.0f, pt.getSafeScaleX(), 0.0001f, "Negative scale (" + neg + ") should fallback to 1.0f");
            assertEqualsFloat(1.0f, pt.getSafeScaleY(), 0.0001f, "Negative scale (" + neg + ") should fallback to 1.0f");
            assertEqualsFloat(1.0f, pt.getSafeScaleZ(), 0.0001f, "Negative scale (" + neg + ") should fallback to 1.0f");
        }

        // 3. Float.NaN
        pt.scaleX = Float.NaN;
        pt.scaleY = Float.NaN;
        pt.scaleZ = Float.NaN;
        assertEqualsFloat(1.0f, pt.getSafeScaleX(), 0.0001f, "NaN scaleX should fallback to 1.0f");
        assertEqualsFloat(1.0f, pt.getSafeScaleY(), 0.0001f, "NaN scaleY should fallback to 1.0f");
        assertEqualsFloat(1.0f, pt.getSafeScaleZ(), 0.0001f, "NaN scaleZ should fallback to 1.0f");

        // 4. Infinite & Huge Scales (1000.0f, etc.)
        float[] hugeScales = {1000.0f, 50000.0f, Float.MAX_VALUE, Float.POSITIVE_INFINITY};
        for (float huge : hugeScales) {
            pt.scaleX = huge;
            pt.scaleY = huge;
            pt.scaleZ = huge;
            assertEqualsFloat(5.0f, pt.getSafeScaleX(), 0.0001f, "Huge scale (" + huge + ") should clamp to 5.0f");
            assertEqualsFloat(5.0f, pt.getSafeScaleY(), 0.0001f, "Huge scale (" + huge + ") should clamp to 5.0f");
            assertEqualsFloat(5.0f, pt.getSafeScaleZ(), 0.0001f, "Huge scale (" + huge + ") should clamp to 5.0f");
        }

        // 5. Micro Scales
        float[] microScales = {0.000001f, 0.001f, 0.0099f};
        for (float micro : microScales) {
            pt.scaleX = micro;
            pt.scaleY = micro;
            pt.scaleZ = micro;
            assertEqualsFloat(0.01f, pt.getSafeScaleX(), 0.0001f, "Micro scale (" + micro + ") should clamp to 0.01f");
            assertEqualsFloat(0.01f, pt.getSafeScaleY(), 0.0001f, "Micro scale (" + micro + ") should clamp to 0.01f");
            assertEqualsFloat(0.01f, pt.getSafeScaleZ(), 0.0001f, "Micro scale (" + micro + ") should clamp to 0.01f");
        }

        // 6. Threshold Boundaries & Valid Scales
        pt.scaleX = 0.01f;
        pt.scaleY = 5.0f;
        pt.scaleZ = 1.0f;
        assertEqualsFloat(0.01f, pt.getSafeScaleX(), 0.0001f, "Boundary 0.01f preserved");
        assertEqualsFloat(5.0f, pt.getSafeScaleY(), 0.0001f, "Boundary 5.0f preserved");
        assertEqualsFloat(1.0f, pt.getSafeScaleZ(), 0.0001f, "Normal 1.0f preserved");

        System.out.println("  [PASS] All scale extremes (0.0, negative, NaN, infinity, 1000.0) safely clamped.");
    }

    /**
     * Test 2: Stress-test PoseStack hygiene under extreme 9-DOF transforms:
     * - Null PartTransformData / Null PoseStack -> no crashes, no leaks.
     * - Extreme translation, rotation, and scale values.
     * - Exact stack depth balance verification before and after transform cycles.
     */
    public static void testPoseStackHygieneUnderExtremeTransforms() {
        System.out.println("\n--- Running Test 2: PoseStack Hygiene Under Extreme Transforms ---");

        PoseStack poseStack = new PoseStack();
        int initialDepth = getStackDepth(poseStack);

        // 1. Null handling
        PlayerRaceLayer.applyPartTransforms(null, new PartTransformData());
        PlayerRaceLayer.applyPartTransforms(poseStack, null);
        PlayerRaceLayer.applyPartTransforms(null, null);
        assertEquals(initialDepth, getStackDepth(poseStack), "Null arguments must not alter PoseStack");

        // 2. Extreme translation and rotation values
        PartTransformData extremePt = new PartTransformData();
        extremePt.posX = 1e6f;
        extremePt.posY = -1e6f;
        extremePt.posZ = Float.MAX_VALUE;
        extremePt.rotPitch = 72000.0f;
        extremePt.rotYaw = -36000.0f;
        extremePt.rotRoll = 18000.0f;
        extremePt.scaleX = Float.POSITIVE_INFINITY;
        extremePt.scaleY = Float.NaN;
        extremePt.scaleZ = -999.0f;

        poseStack.pushPose();
        try {
            PlayerRaceLayer.applyPartTransforms(poseStack, extremePt);
        } finally {
            poseStack.popPose();
        }
        assertEquals(initialDepth, getStackDepth(poseStack), "PoseStack depth must match initial after extreme transform");

        // 3. Repeated Push/Pop Transform Cycles
        for (int i = 0; i < 500; i++) {
            poseStack.pushPose();
            try {
                extremePt.posX = (float) Math.sin(i);
                extremePt.rotPitch = i * 45.0f;
                extremePt.scaleX = (i % 2 == 0) ? 0.0f : 100.0f;
                PlayerRaceLayer.applyPartTransforms(poseStack, extremePt);
            } finally {
                poseStack.popPose();
            }
        }
        assertEquals(initialDepth, getStackDepth(poseStack), "PoseStack depth must be 100% balanced after 500 transform cycles");

        System.out.println("  [PASS] PoseStack matrix stack perfectly balanced under extreme 9-DOF transforms.");
    }

    /**
     * Test 3: Verify behavior with invalid, null, empty, whitespace-only,
     * unbaked, and non-existent model identifiers.
     */
    public static void testInvalidAndNullModelIdentifiers() {
        System.out.println("\n--- Running Test 3: Invalid & Null Model Identifiers ---");

        // 1. hasCustomModel with null/empty/none
        assertFalse(WereModelRenderer.hasCustomModel(null), "Null race should not have custom model");

        RaceData race = new RaceData("test_race", "Test Race");
        race.wereModelPath = null;
        assertFalse(WereModelRenderer.hasCustomModel(race), "Null model path should not have custom model");

        race.wereModelPath = "";
        assertFalse(WereModelRenderer.hasCustomModel(race), "Empty model path should not have custom model");

        race.wereModelPath = "   \t\n  ";
        assertFalse(WereModelRenderer.hasCustomModel(race), "Whitespace model path should not have custom model");

        race.wereModelPath = "none";
        assertFalse(WereModelRenderer.hasCustomModel(race), " 'none' model path should not have custom model");

        race.wereModelPath = "NONE";
        assertFalse(WereModelRenderer.hasCustomModel(race), " 'NONE' model path should not have custom model");

        race.wereModelPath = "  none  ";
        assertFalse(WereModelRenderer.hasCustomModel(race), " '  none  ' model path should not have custom model");

        // 2. isModelAvailable on missing and unbaked models
        race.wereModelPath = "nonexistent_were_model_12345.geo.json";
        assertFalse(WereModelRenderer.isModelAvailable(race), "Nonexistent model should not be available");

        race.wereModelPath = "customraces:models/were/unbaked_creature.geo.json";
        assertFalse(WereModelRenderer.isModelAvailable(race), "Unbaked model should not be available");

        // 3. resolveModelLocation null/empty contracts
        assertEquals(null, GeckoAssetResolver.resolveModelLocation(null), "Null path resolves to null model");
        assertEquals(null, GeckoAssetResolver.resolveModelLocation(""), "Empty path resolves to null model");
        assertEquals(null, GeckoAssetResolver.resolveModelLocation("   "), "Whitespace path resolves to null model");
        assertEquals(null, GeckoAssetResolver.resolveModelLocation("none"), " 'none' resolves to null model");
        assertEquals(null, GeckoAssetResolver.resolveModelLocation("NONE"), " 'NONE' resolves to null model");

        // 4. resolveAnimationLocation null/empty contracts
        assertEquals(null, GeckoAssetResolver.resolveAnimationLocation(null), "Null path resolves to null animation");
        assertEquals(null, GeckoAssetResolver.resolveAnimationLocation(""), "Empty path resolves to null animation");
        assertEquals(null, GeckoAssetResolver.resolveAnimationLocation("none"), " 'none' resolves to null animation");

        System.out.println("  [PASS] Invalid, null, empty, and unbaked model identifiers handled cleanly.");
    }

    /**
     * Test 4: Stress-test path traversal attempts and invalid namespaces on models and textures:
     * - Directory traversal (../../ traversal, backslashes, escape sequences)
     * - Invalid namespaces (uppercase, spaces, special symbols, colon abuse)
     * - Absolute filesystem paths (C:/Windows/..., /etc/passwd)
     */
    public static void testPathTraversalAttemptsOnModelsAndTextures() {
        System.out.println("\n--- Running Test 4: Path Traversal Attempts on Models & Textures ---");

        String[] traversalAttempts = {
                "../../secret.geo.json",
                "../../../etc/passwd",
                "..\\..\\windows\\win.ini",
                "customraces:../../escape.geo.json",
                "../../../../../../models/foo.geo.json",
                "/etc/shadow",
                "C:/Windows/System32/cmd.exe",
                "//unc/share/file.geo.json",
                "foo/../../bar/../../baz.geo.json"
        };

        RaceData race = new RaceData("traversal_race", "Traversal Race");
        race.enableWereRace = true;

        for (String traversal : traversalAttempts) {
            race.wereModelPath = traversal;
            race.wereTexturePath = traversal;

            // Model availability must be false (no unauthorized disk access or crashes)
            assertFalse(WereModelRenderer.isModelAvailable(race), "Traversal attempt '" + traversal + "' must not be available as a model");

            // Texture resolution must safely fall back without throwing
            ResourceLocation texLoc = GeckoAssetResolver.resolveTextureLocation(null, traversal);
            assertTrue(texLoc != null, "Traversal attempt '" + traversal + "' must resolve to a safe non-null texture location");
            assertFalse(texLoc.getPath().contains("missingno"), "Traversal texture must not resolve to missingno checkerboard");
        }

        // Malformed namespaces and colon abuse
        String[] malformedNamespaces = {
                "INVALID:MODEL.geo.json",
                "UPPER_NAMESPACE:model.geo.json",
                "space namespace:model.geo.json",
                "special@symbol:model.geo.json",
                "::double_colon.geo.json",
                ":no_namespace.geo.json",
                "customraces:::model.geo.json",
                "customraces:models/were/\0nullbyte.geo.json"
        };

        for (String malformed : malformedNamespaces) {
            race.wereModelPath = malformed;
            assertFalse(WereModelRenderer.isModelAvailable(race), "Malformed namespace '" + malformed + "' must not be available");

            ResourceLocation resolved = GeckoAssetResolver.resolveModelLocation(malformed);
            assertTrue(resolved != null, "Malformed model '" + malformed + "' should resolve to safe location or fallback");
        }

        System.out.println("  [PASS] Path traversal and malformed namespaces safely rejected with zero exploits.");
    }

    /**
     * Test 5: Verify player mesh suppression guardrails ("Never Invisible"):
     * - If model is missing, invalid, or unbaked, the player model is NEVER hidden.
     * - CustomRaceModelRenderer.updateModelPartVisibility keeps all 12+ parts visible.
     * - WereModelRenderer.renderWereForm returns false and explicitly restores base model visibility.
     */
    public static void testPlayerMeshSuppressionGuardrailsOnAdversarialModels() throws Exception {
        System.out.println("\n--- Running Test 5: Player Mesh Suppression Guardrails ---");

        PlayerModel<?> model = createMockPlayerModel();

        String[] adversarialPaths = {
                null,
                "",
                "none",
                "   ",
                "../../secret.geo.json",
                "nonexistent:model.geo.json",
                "unbaked_were_wolf.geo.json",
                "corrupt:::namespace.geo.json"
        };

        RaceData race = new RaceData("adversarial_were", "Adversarial Were");
        race.enableWereRace = true;

        for (String path : adversarialPaths) {
            race.wereModelPath = path;

            // 1. updateModelPartVisibility test
            CustomRaceModelRenderer.updateModelPartVisibility((PlayerModel) model, null, race);
            assertAllPlayerPartsVisible(model, "updateModelPartVisibility with path: '" + path + "'");

            // 2. renderWereForm execution test
            boolean rendered = WereModelRenderer.renderWereForm(null, null, 15728880, null, (PlayerModel) model, race, 0.0f, 0.0f);
            assertFalse(rendered, "renderWereForm must return false for adversarial path: '" + path + "'");
            assertAllPlayerPartsVisible(model, "renderWereForm with path: '" + path + "'");

            // 3. renderCustomRaceModel execution test
            CustomRaceModelRenderer.renderCustomRaceModel(null, null, 15728880, null, (PlayerModel) model, race, 0.0f, 0.0f);
            assertAllPlayerPartsVisible(model, "renderCustomRaceModel with path: '" + path + "'");
        }

        // 4. Null safety for PlayerModel
        WereModelRenderer.setBaseModelVisible(null, true);
        WereModelRenderer.setBaseModelVisible(null, false);
        CustomRaceModelRenderer.updateModelPartVisibility(null, null, race);
        CustomRaceModelRenderer.renderCustomRaceModel(null, null, 15728880, null, null, race, 0.0f, 0.0f);

        System.out.println("  [PASS] Player model is strictly protected against invisibility across all adversarial inputs.");
    }

    private static void assertAllPlayerPartsVisible(PlayerModel<?> model, String context) {
        assertTrue(model.head.visible, "head must be visible (" + context + ")");
        assertTrue(model.hat.visible, "hat must be visible (" + context + ")");
        assertTrue(model.body.visible, "body must be visible (" + context + ")");
        assertTrue(model.rightArm.visible, "rightArm must be visible (" + context + ")");
        assertTrue(model.leftArm.visible, "leftArm must be visible (" + context + ")");
        assertTrue(model.rightLeg.visible, "rightLeg must be visible (" + context + ")");
        assertTrue(model.leftLeg.visible, "leftLeg must be visible (" + context + ")");
        assertTrue(model.jacket.visible, "jacket must be visible (" + context + ")");
        assertTrue(model.rightSleeve.visible, "rightSleeve must be visible (" + context + ")");
        assertTrue(model.leftSleeve.visible, "leftSleeve must be visible (" + context + ")");
        assertTrue(model.rightPants.visible, "rightPants must be visible (" + context + ")");
        assertTrue(model.leftPants.visible, "leftPants must be visible (" + context + ")");
    }

    /**
     * Test 6: Verify texture resolution extremes, keyword interception, and zero checkerboards:
     * - Null, empty, whitespace, and "none" paths.
     * - All skin keywords ("skin", "player", "player_skin", etc.) case-insensitively.
     * - Corrupted strings, non-existent files, and long paths.
     * - Verify NO missingno texture is ever returned.
     */
    public static void testTextureResolutionExtremesAndFallbacks() {
        System.out.println("\n--- Running Test 6: Texture Resolution Extremes & Fallbacks ---");

        ResourceLocation expectedDefault = WereModelRenderer.DEFAULT_WERE_TEXTURE;

        // 1. Null, empty, whitespace, none
        String[] emptyInputs = {null, "", "   ", "\t", "none", "NONE", "  none  "};
        for (String input : emptyInputs) {
            ResourceLocation loc = GeckoAssetResolver.resolveTextureLocation(null, input);
            assertEquals(expectedDefault, loc, "Empty/none input '" + input + "' should fallback to default werewolf texture");
        }

        // 2. All 9 skin keywords (case variants)
        String[] keywords = {
                "skin", "SKIN", "Skin", "  skin  ",
                "player", "PLAYER", "Player",
                "player_skin", "PLAYER_SKIN",
                "skin_texture", "SKIN_TEXTURE",
                "dynamic_skin", "DYNAMIC_SKIN",
                "use_skin", "USE_SKIN",
                "dynamic", "DYNAMIC",
                "player_texture", "PLAYER_TEXTURE",
                "default_skin", "DEFAULT_SKIN"
        };
        for (String kw : keywords) {
            ResourceLocation loc = GeckoAssetResolver.resolveTextureLocation(null, kw);
            assertTrue(loc != null, "Keyword '" + kw + "' must resolve to a valid ResourceLocation");
            assertFalse(loc.getPath().contains("missingno"), "Keyword '" + kw + "' must not resolve to missingno");
        }

        // 3. Corrupted strings and invalid characters
        String[] corruptInputs = {
                "invalid::texture!@#.png",
                "textures/were/nonexistent_texture_xyz_123.png",
                "../escape.png",
                "C:/Windows/System32/drivers/etc/hosts",
                "a".repeat(2000) + ".png"
        };
        for (String corrupt : corruptInputs) {
            ResourceLocation loc = GeckoAssetResolver.resolveTextureLocation(null, corrupt);
            assertTrue(loc != null, "Corrupt input must resolve to a valid ResourceLocation");
            assertFalse(loc.getPath().contains("missingno"), "Corrupt input must not resolve to missingno");
        }

        // 4. Interface contract with Player argument
        ResourceLocation contractLoc = GeckoAssetResolver.resolveTexture(null, "textures/were/default_werewolf.png");
        assertTrue(contractLoc != null, "GeckoAssetResolver.resolveTexture contract method must return valid location");

        // 5. Cache clearing safety
        GeckoAssetResolver.clearCaches();

        System.out.println("  [PASS] Zero checkerboards verified across all texture inputs, keywords, and fallbacks.");
    }

    /**
     * Test 7: Verify procedural were-beast fallback resilience, hex color parsing,
     * and first-person camera suppression.
     */
    public static void testProceduralFallbackAndColorResilience() {
        System.out.println("\n--- Running Test 7: Procedural Fallback & Color Resilience ---");

        // 1. Color parsing resilience: null, invalid hex, short/long hex
        String[] invalidColors = {null, "", "none", "not_a_hex", "#12", "#12345", "#1234567", "#ZZZZZZ", "red", "rgb(255,0,0)"};
        for (String inv : invalidColors) {
            float[] rgb = PlayerRaceLayer.parseRGB(inv);
            assertTrue(rgb != null && rgb.length == 3, "parseRGB must always return float[3]");
            assertEqualsFloat(1.0f, rgb[0], 0.001f, "Invalid color fallback Red component must be 1.0f");
            assertEqualsFloat(1.0f, rgb[1], 0.001f, "Invalid color fallback Green component must be 1.0f");
            assertEqualsFloat(1.0f, rgb[2], 0.001f, "Invalid color fallback Blue component must be 1.0f");
        }

        // 2. Exact hex color parsing
        float[] red = PlayerRaceLayer.parseRGB("#FF0000");
        assertEqualsFloat(1.0f, red[0], 0.01f, "Red R");
        assertEqualsFloat(0.0f, red[1], 0.01f, "Red G");
        assertEqualsFloat(0.0f, red[2], 0.01f, "Red B");

        float[] cyan = PlayerRaceLayer.parseRGB("#00FFFF");
        assertEqualsFloat(0.0f, cyan[0], 0.01f, "Cyan R");
        assertEqualsFloat(1.0f, cyan[1], 0.01f, "Cyan G");
        assertEqualsFloat(1.0f, cyan[2], 0.01f, "Cyan B");

        float[] darkGray = PlayerRaceLayer.parseRGB("#333333");
        assertEqualsFloat(51.0f / 255.0f, darkGray[0], 0.01f, "DarkGray R");
        assertEqualsFloat(51.0f / 255.0f, darkGray[1], 0.01f, "DarkGray G");
        assertEqualsFloat(51.0f / 255.0f, darkGray[2], 0.01f, "DarkGray B");

        // 3. First-Person Camera Suppression Contract
        assertFalse(WereModelRenderer.isFirstPerson(null), "Null player is not in first-person");
        assertFalse(PlayerRaceLayer.isAttachmentSuppressed(null), "Null player does not suppress attachments");

        System.out.println("  [PASS] Procedural fallback color parsing and first-person suppression verified.");
    }

    /**
     * Test 8: Multithreaded concurrency stress hammer:
     * - 12 threads performing 1,000 iterations each (12,000 operations total).
     * - Scale clamping, transforms, texture resolution, model checks, and visibility toggling.
     * - Verifies 0 matrix leaks, 0 thread contention deadlocks, and 0 uncaught exceptions.
     */
    public static void testMassiveMultithreadedConcurrencyStress() throws Exception {
        System.out.println("\n--- Running Test 8: Massive Multithreaded Concurrency Stress Hammer ---");

        int threadCount = 12;
        int iterationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);
        Random random = new Random(42);

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    PlayerModel<?> threadModel = createMockPlayerModel();
                    PoseStack threadStack = new PoseStack();
                    int initialDepth = getStackDepth(threadStack);

                    for (int j = 0; j < iterationsPerThread; j++) {
                        int mode = (threadId + j) % 5;

                        if (mode == 0) {
                            // 1. Scale clamping & 9-DOF transforms
                            float[] testScales = {0.0f, -1.0f, Float.NaN, Float.POSITIVE_INFINITY, 1000.0f, 0.0001f, 1.5f};
                            float chosenScale = testScales[(j + threadId) % testScales.length];
                            PartTransformData pt = new PartTransformData(
                                    (float) (Math.sin(j) * 10.0),
                                    (float) (Math.cos(j) * 10.0),
                                    0.0f,
                                    j * 15.0f,
                                    j * 30.0f,
                                    0.0f,
                                    chosenScale, chosenScale, chosenScale
                            );

                            threadStack.pushPose();
                            try {
                                PlayerRaceLayer.applyPartTransforms(threadStack, pt);
                            } finally {
                                threadStack.popPose();
                            }

                            if (getStackDepth(threadStack) != initialDepth) {
                                errorCount.incrementAndGet();
                            }
                        } else if (mode == 1) {
                            // 2. Texture resolution and cache clearing
                            String[] paths = {"skin", "player", null, "", "textures/were/default_werewolf.png", "../../escape.png"};
                            String path = paths[(j + threadId) % paths.length];
                            ResourceLocation tex = GeckoAssetResolver.resolveTextureLocation(null, path);
                            if (tex == null) {
                                errorCount.incrementAndGet();
                            }
                            if (j % 200 == 0) {
                                GeckoAssetResolver.clearCaches();
                            }
                        } else if (mode == 2) {
                            // 3. Model availability and resolution
                            RaceData race = new RaceData("thread_race_" + threadId, "Thread Race");
                            race.enableWereRace = true;
                            race.wereModelPath = (j % 2 == 0) ? "none" : "../../traversal.geo.json";
                            boolean avail = WereModelRenderer.isModelAvailable(race);
                            if (avail) {
                                errorCount.incrementAndGet(); // Traversal or 'none' must NEVER be available
                            }
                        } else if (mode == 3) {
                            // 4. Model visibility guardrails
                            boolean visible = (j % 2 == 0);
                            WereModelRenderer.setBaseModelVisible(threadModel, visible);
                            if (threadModel.head.visible != visible || threadModel.body.visible != visible) {
                                errorCount.incrementAndGet();
                            }
                        } else {
                            // 5. Color parsing
                            float[] rgb = PlayerRaceLayer.parseRGB((j % 2 == 0) ? "#FF8800" : "invalid_hex");
                            if (rgb == null || rgb.length != 3) {
                                errorCount.incrementAndGet();
                            }
                        }
                    }
                } catch (Throwable t) {
                    System.err.println("[ERROR in thread " + threadId + "]: " + t.getMessage());
                    t.printStackTrace();
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean completed = latch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Multithreaded concurrency stress hammer timed out");
        assertEquals(0, errorCount.get(), "Zero errors expected during 12,000 concurrent adversarial operations");

        System.out.println("  [PASS] 12,000 concurrent adversarial operations executed with 0 errors.");
    }
}
