package ddraig.net.customraces.data;

import net.minecraft.nbt.CompoundTag;

/**
 * Empirical Unit Verification Test Suite for Milestone 4 (Requirement R4):
 * 1. Body Part Presets & Extra Legs (legType, legCount, customPartId) NBT Serialization.
 * 2. RGB Color Map (bodyPartColors) NBT Roundtrip.
 * 3. Part Transform Map (posX/Y/Z, rotPitch/Yaw/Roll, scaleX/Y/Z) NBT Roundtrip.
 * 4. PartTransformData safe scaling boundary validation.
 */
public class M4PresetAuditVerificationTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  M4 DYNAMIC BODY PART MODEL PRESET AUDIT & VERIFICATION TEST SUITE  ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        try {
            testPresetFieldsNBTRoundtrip();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 1 (Preset Fields NBT Roundtrip): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testPartTransformSafeScaleBoundaries();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 2 (Safe Scale Boundaries): " + t.getMessage());
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

    public static void testPresetFieldsNBTRoundtrip() {
        System.out.println("\n--- Running Test 1: Body Part Presets & Transforms NBT Roundtrip ---");

        RaceData race = new RaceData("test_kitsune", "Test Kitsune");
        race.earType = "cat";
        race.wingType = "feathered";
        race.tailType = "dog";
        race.hornType = "ram";
        race.haloType = "angel";
        race.legType = "spider";
        race.legCount = 8;
        race.customPartId = "fox_tails";

        race.setColor("ears", "#FF5555");
        race.setColor("legs", "#00FF00");

        PartTransformData earsPt = race.getTransform("ears");
        earsPt.posX = 1.2f;
        earsPt.posY = -0.5f;
        earsPt.posZ = 0.3f;
        earsPt.rotPitch = 45.0f;
        earsPt.rotYaw = 90.0f;
        earsPt.rotRoll = 15.0f;
        earsPt.scaleX = 1.5f;
        earsPt.scaleY = 2.0f;
        earsPt.scaleZ = 0.8f;

        PartTransformData legsPt = race.getTransform("legs");
        legsPt.posX = 0.0f;
        legsPt.posY = 0.5f;
        legsPt.posZ = -0.2f;
        legsPt.rotPitch = 12.0f;
        legsPt.scaleX = 1.2f;

        CompoundTag nbt = race.toNBT(new CompoundTag());

        RaceData loaded = new RaceData();
        loaded.fromNBT(nbt);

        assertEquals("cat", loaded.earType, "earType roundtrip");
        assertEquals("feathered", loaded.wingType, "wingType roundtrip");
        assertEquals("dog", loaded.tailType, "tailType roundtrip");
        assertEquals("ram", loaded.hornType, "hornType roundtrip");
        assertEquals("angel", loaded.haloType, "haloType roundtrip");
        assertEquals("spider", loaded.legType, "legType roundtrip");
        assertEquals(8, loaded.legCount, "legCount roundtrip");
        assertEquals("fox_tails", loaded.customPartId, "customPartId roundtrip");

        assertEquals("#FF5555", loaded.getColor("ears"), "ears color roundtrip");
        assertEquals("#00FF00", loaded.getColor("legs"), "legs color roundtrip");

        PartTransformData loadedEarsPt = loaded.getTransform("ears");
        assertEqualsFloat(1.2f, loadedEarsPt.posX, 0.001f, "ears posX");
        assertEqualsFloat(-0.5f, loadedEarsPt.posY, 0.001f, "ears posY");
        assertEqualsFloat(0.3f, loadedEarsPt.posZ, 0.001f, "ears posZ");
        assertEqualsFloat(45.0f, loadedEarsPt.rotPitch, 0.001f, "ears rotPitch");
        assertEqualsFloat(90.0f, loadedEarsPt.rotYaw, 0.001f, "ears rotYaw");
        assertEqualsFloat(15.0f, loadedEarsPt.rotRoll, 0.001f, "ears rotRoll");
        assertEqualsFloat(1.5f, loadedEarsPt.scaleX, 0.001f, "ears scaleX");
        assertEqualsFloat(2.0f, loadedEarsPt.scaleY, 0.001f, "ears scaleY");
        assertEqualsFloat(0.8f, loadedEarsPt.scaleZ, 0.001f, "ears scaleZ");

        PartTransformData loadedLegsPt = loaded.getTransform("legs");
        assertEqualsFloat(0.5f, loadedLegsPt.posY, 0.001f, "legs posY");
        assertEqualsFloat(12.0f, loadedLegsPt.rotPitch, 0.001f, "legs rotPitch");
        assertEqualsFloat(1.2f, loadedLegsPt.scaleX, 0.001f, "legs scaleX");

        System.out.println("  [PASS] All body part presets, colors, and 9-DOF transform maps serialized and restored accurately via NBT.");
    }

    public static void testPartTransformSafeScaleBoundaries() {
        System.out.println("\n--- Running Test 2: PartTransformData Safe Scale Boundaries ---");

        PartTransformData pt = new PartTransformData();

        pt.scaleX = -5.0f;
        pt.scaleY = 0.0001f;
        pt.scaleZ = 100.0f;

        assertEqualsFloat(1.0f, pt.getSafeScaleX(), 0.001f, "Negative scale fallback");
        assertEqualsFloat(0.01f, pt.getSafeScaleY(), 0.001f, "Min scale clamp");
        assertEqualsFloat(5.0f, pt.getSafeScaleZ(), 0.001f, "Max scale clamp");

        System.out.println("  [PASS] Scale clamping bounds [0.01f, 5.0f] and negative/zero fallbacks validated.");
    }
}
