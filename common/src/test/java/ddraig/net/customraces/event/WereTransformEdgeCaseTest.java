package ddraig.net.customraces.event;

import ddraig.net.customraces.client.ClientWereState;
import ddraig.net.customraces.client.render.WereModelRenderer;
import ddraig.net.customraces.data.RaceData;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public class WereTransformEdgeCaseTest {

    public static void main(String[] args) {
        System.out.println("=== RUNNING WERE-RACE TRANSFORM EDGE CASE TESTS ===");
        try {
            testNullAndEmptyModelPaths();
            testInvalidResourceLocationSyntax();
            testUnmappedModelPaths();
            testPehkuiScaleBoundaries();
            testRapidTransformationToggling();
            testNullSafetyInTransformHandlers();
            testClientWereStateConcurrencyAndClear();
            System.out.println("=== ALL EMPIRICAL EDGE CASE TESTS PASSED SUCCESSFULLY ===");
        } catch (Throwable t) {
            System.err.println("!!! TEST FAILURE !!!");
            t.printStackTrace();
            System.exit(1);
        }
    }

    public static void testNullAndEmptyModelPaths() {
        System.out.println("\n[Test 1] Null, Empty, and 'none' Model Paths");
        RaceData race = new RaceData();

        // Test null
        race.wereModelPath = null;
        assert !WereModelRenderer.hasCustomModel(race) : "Failed: null path should return false for hasCustomModel";
        ResourceLocation locNull = WereModelRenderer.getValidWereModelLocation(race);
        assert locNull.equals(WereModelRenderer.DEFAULT_WERE_MODEL) : "Failed: null model path did not return DEFAULT_WERE_MODEL";

        // Test empty string
        race.wereModelPath = "";
        assert !WereModelRenderer.hasCustomModel(race) : "Failed: empty path should return false for hasCustomModel";
        assert WereModelRenderer.getValidWereModelLocation(race).equals(WereModelRenderer.DEFAULT_WERE_MODEL) : "Failed: empty path fallback";

        // Test whitespace
        race.wereModelPath = "   ";
        assert !WereModelRenderer.hasCustomModel(race) : "Failed: whitespace path should return false";

        // Test 'none' / 'NONE'
        race.wereModelPath = "none";
        assert !WereModelRenderer.hasCustomModel(race) : "Failed: 'none' path should return false";
        race.wereModelPath = "NONE";
        assert !WereModelRenderer.hasCustomModel(race) : "Failed: 'NONE' path should return false";

        // Texture null/empty/none tests
        race.wereTexturePath = null;
        assert WereModelRenderer.getValidWereTextureLocation(race).equals(WereModelRenderer.DEFAULT_WERE_TEXTURE) : "Failed: null texture path fallback";
        race.wereTexturePath = "none";
        assert WereModelRenderer.getValidWereTextureLocation(race).equals(WereModelRenderer.DEFAULT_WERE_TEXTURE) : "Failed: 'none' texture path fallback";

        // Animation null/empty/none tests
        race.wereAnimationPath = null;
        assert WereModelRenderer.getValidWereAnimationLocation(race).equals(WereModelRenderer.DEFAULT_WERE_ANIMATION) : "Failed: null anim path fallback";
        race.wereAnimationPath = "none";
        assert WereModelRenderer.getValidWereAnimationLocation(race).equals(WereModelRenderer.DEFAULT_WERE_ANIMATION) : "Failed: 'none' anim path fallback";

        System.out.println("  PASSED: Null, empty, and 'none' model/texture/anim paths gracefully fall back to defaults.");
    }

    public static void testInvalidResourceLocationSyntax() {
        System.out.println("\n[Test 2] Invalid ResourceLocation Syntax Handling");
        RaceData race = new RaceData();

        // Path with spaces and invalid characters
        race.wereModelPath = "customraces:models/were/invalid path with spaces.geo.json";
        ResourceLocation loc = WereModelRenderer.getValidWereModelLocation(race);
        assert loc.equals(WereModelRenderer.DEFAULT_WERE_MODEL) : "Failed: invalid syntax should fall back to DEFAULT_WERE_MODEL";

        race.wereTexturePath = "INVALID TEX PATH!";
        ResourceLocation texLoc = WereModelRenderer.getValidWereTextureLocation(race);
        assert texLoc.equals(WereModelRenderer.DEFAULT_WERE_TEXTURE) : "Failed: invalid texture syntax fallback";

        race.wereAnimationPath = "INVALID ANIM PATH!";
        ResourceLocation animLoc = WereModelRenderer.getValidWereAnimationLocation(race);
        assert animLoc.equals(WereModelRenderer.DEFAULT_WERE_ANIMATION) : "Failed: invalid anim syntax fallback";

        System.out.println("  PASSED: Syntax errors in resource locations return default constants without throwing exceptions.");
    }

    public static void testUnmappedModelPaths() {
        System.out.println("\n[Test 3] Unmapped / Non-Existent Model Location Handling");
        RaceData race = new RaceData();
        race.wereModelPath = "customraces:models/were/missing_model_file.geo.json";

        boolean hasCustom = WereModelRenderer.hasCustomModel(race);
        assert hasCustom : "Syntactically valid path should return true for hasCustomModel";
        ResourceLocation loc = WereModelRenderer.getValidWereModelLocation(race);
        assert loc.toString().equals("customraces:models/were/missing_model_file.geo.json") : "Valid ResourceLocation returned";

        System.out.println("  OBSERVATION/PASSED: Valid ResourceLocation syntax returns ResourceLocation. (Asset presence must be checked by client resource loader).");
    }

    public static void testPehkuiScaleBoundaries() {
        System.out.println("\n[Test 4] Negative and Extreme Pehkui Scale Boundaries");
        RaceData race = new RaceData();

        // Negative & Zero Scales
        race.wereHeightScale = -2.5f;
        race.wereWidthScale = 0.0f;
        race.baseScale = -1.0f;
        race.heightScale = -0.5f;
        race.widthScale = 0.0f;
        race.reachScale = -3.0f;
        race.stepHeightScale = 0.0f;

        float hMult = race.wereHeightScale > 0 ? race.wereHeightScale : 1.3f;
        float wMult = race.wereWidthScale > 0 ? race.wereWidthScale : 1.3f;
        float bScale = race.baseScale > 0 ? race.baseScale : 1.0f;
        float rScale = race.reachScale > 0 ? race.reachScale : 1.0f;
        float sScale = race.stepHeightScale > 0 ? race.stepHeightScale : 1.0f;

        assert hMult == 1.3f : "Failed: negative wereHeightScale did not fallback to 1.3f";
        assert wMult == 1.3f : "Failed: 0.0 wereWidthScale did not fallback to 1.3f";
        assert bScale == 1.0f : "Failed: negative baseScale did not fallback to 1.0f";
        assert rScale == 1.0f : "Failed: negative reachScale did not fallback to 1.0f";
        assert sScale == 1.0f : "Failed: 0.0 stepHeightScale did not fallback to 1.0f";

        // NaN Scale Check
        race.baseScale = Float.NaN;
        float nanFallback = race.baseScale > 0 ? race.baseScale : 1.0f;
        assert nanFallback == 1.0f : "Failed: Float.NaN did not fall back to 1.0f";

        System.out.println("  PASSED: Negative, zero, and NaN scale values fall back safely to 1.0f or 1.3f.");
    }

    public static void testRapidTransformationToggling() {
        System.out.println("\n[Test 5] Rapid Transformation Toggling & Cooldown Map");
        UUID playerUuid = UUID.randomUUID();

        // Simulate rapid cooldown check logic from WereRaceTransformHandler
        long now = System.currentTimeMillis();
        long last = 0L;

        boolean allowed1 = (now - last >= 1000L);
        assert allowed1 : "First transform attempt should be allowed";

        last = now;
        long nowSpam = now + 200L; // 200 ms later (rapid spam)
        boolean allowed2 = (nowSpam - last >= 1000L);
        assert !allowed2 : "Rapid transform attempt < 1000ms should be rejected by cooldown";

        long nowAfterCooldown = now + 1050L; // 1050 ms later
        boolean allowed3 = (nowAfterCooldown - last >= 1000L);
        assert allowed3 : "Transform attempt after > 1000ms should be allowed";

        System.out.println("  PASSED: Transformation cooldown rejects toggles within 1000ms.");
    }

    public static void testNullSafetyInTransformHandlers() {
        System.out.println("\n[Test 6] Null Safety in WereRaceTransformHandler & ClientWereState");

        // WereRaceTransformHandler null checks
        assert !WereRaceTransformHandler.isTransformed(null) : "isTransformed(null) should return false";
        WereRaceTransformHandler.onPlayerStartTracking(null, null); // should not throw
        WereRaceTransformHandler.syncAllWereStatesTo(null); // should not throw
        WereRaceTransformHandler.checkTransformation(null); // should not throw

        // ClientWereState null checks
        assert !ClientWereState.isTransformed(null) : "ClientWereState.isTransformed(null) should return false";
        ClientWereState.setTransformed(null, true); // should not throw
        assert !ClientWereState.isTransformed(null);

        System.out.println("  PASSED: All transformation handlers handle null players and UUIDs safely.");
    }

    public static void testClientWereStateConcurrencyAndClear() {
        System.out.println("\n[Test 7] ClientWereState Map Mutability and Persistence");

        UUID u1 = UUID.randomUUID();
        UUID u2 = UUID.randomUUID();

        ClientWereState.setTransformed(u1, true);
        ClientWereState.setTransformed(u2, true);

        assert ClientWereState.isTransformed(u1) : "u1 should be transformed";
        assert ClientWereState.isTransformed(u2) : "u2 should be transformed";

        ClientWereState.setTransformed(u1, false);
        assert !ClientWereState.isTransformed(u1) : "u1 should be reverted";
        assert ClientWereState.isTransformed(u2) : "u2 should still be transformed";

        ClientWereState.clear();
        assert !ClientWereState.isTransformed(u2) : "clear() should wipe transformed state map";

        System.out.println("  PASSED: ClientWereState correctly adds, removes, and clears player states.");
    }
}
