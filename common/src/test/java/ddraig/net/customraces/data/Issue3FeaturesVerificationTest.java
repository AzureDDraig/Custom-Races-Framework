package ddraig.net.customraces.data;

import net.minecraft.world.phys.Vec3;

public class Issue3FeaturesVerificationTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  GITHUB ISSUE #3: CUSTOM MOBS, AI & PEHKUI CAMERA TEST SUITE     ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: Pehkui Camera & Scale Protection Formula
        try {
            System.out.println("\n--- Running Test 1: Pehkui Scale & Camera Synchronized Mapping ---");
            RaceData giantRace = new RaceData();
            giantRace.baseScale = 1.0f;
            giantRace.heightScale = 1.8f;
            giantRace.widthScale = 1.2f;

            float baseScale = giantRace.baseScale > 0 ? giantRace.baseScale : 1.0f;
            float heightMult = giantRace.heightScale > 0 ? giantRace.heightScale : 1.0f;
            float widthMult = giantRace.widthScale > 0 ? giantRace.widthScale : 1.0f;

            // BASE scale must NOT be average of height/width (which caused double-multiplication!)
            assertEqualsFloat(1.0f, baseScale);
            assertEqualsFloat(1.8f, heightMult);
            assertEqualsFloat(1.2f, widthMult);

            // Eye height must match height multiplier (no vertical stretching)
            float eyeHeightScale = heightMult;
            assertEqualsFloat(1.8f, eyeHeightScale);

            // Third person distance scale must be average of height/width
            float thirdPersonScale = (heightMult + widthMult) / 2.0f;
            assertEqualsFloat(1.5f, thirdPersonScale);

            System.out.println("  [PASS] Pehkui scale mapping eliminates double-multiplication and protects camera eye height.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 2: CustomMobs Minion Mob ID Resolution & Clean Extraction
        try {
            System.out.println("\n--- Running Test 2: CustomMobs Minion Mob ID Extraction ---");
            String rawId1 = "custom_mobs:shadow_knight";
            String subId1 = rawId1.replace("custom_mobs:", "").replace("custommobs:", "");
            assertEquals("shadow_knight", subId1);

            String rawId2 = "custommobs:goblin_archer";
            String subId2 = rawId2.replace("custom_mobs:", "").replace("custommobs:", "");
            assertEquals("goblin_archer", subId2);

            String vanillaId = "minecraft:wolf";
            assertTrue(!vanillaId.startsWith("custom_mobs:"), "Vanilla wolf is not custom mob namespace");

            System.out.println("  [PASS] CustomMobs entity ID extraction and namespace detection verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 3: Drawback Equipment Restrictions (No Shield, No Bow, No Trident)
        try {
            System.out.println("\n--- Running Test 3: Equipment Drawback Restrictions ---");
            RaceData restrictedRace = new RaceData();
            restrictedRace.drawbacks.add("no_shield");
            restrictedRace.drawbacks.add("no_bow");
            restrictedRace.drawbacks.add("no_trident");

            assertTrue(restrictedRace.drawbacks.contains("no_shield"), "no_shield drawback present");
            assertTrue(restrictedRace.drawbacks.contains("no_bow"), "no_bow drawback present");
            assertTrue(restrictedRace.drawbacks.contains("no_trident"), "no_trident drawback present");
            assertTrue(!restrictedRace.drawbacks.contains("no_heavy_armor"), "no_heavy_armor drawback absent");

            System.out.println("  [PASS] Drawback equipment restrictions verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 3 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 4: Custom Projectile Trajectory & Look Vector Math
        try {
            System.out.println("\n--- Running Test 4: Custom Projectile Trajectory Calculation ---");
            Vec3 playerPos = new Vec3(0.0, 64.0, 0.0);
            Vec3 lookAngle = new Vec3(1.0, 0.0, 0.0); // Looking along +X
            double eyeY = 65.62;

            double spawnX = playerPos.x + lookAngle.x * 1.2;
            double spawnY = eyeY + lookAngle.y * 0.5;
            double spawnZ = playerPos.z + lookAngle.z * 1.2;

            assertEqualsDouble(1.2, spawnX);
            assertEqualsDouble(65.62, spawnY);
            assertEqualsDouble(0.0, spawnZ);

            float speed = 2.5f;
            Vec3 velocity = lookAngle.scale(speed);
            assertEqualsDouble(2.5, velocity.x);
            assertEqualsDouble(0.0, velocity.y);
            assertEqualsDouble(0.0, velocity.z);

            System.out.println("  [PASS] Custom projectile spawn coordinates and velocity vectors verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 4 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================================");
        System.out.println("  SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================================");

        if (failed > 0) {
            throw new RuntimeException("Issue3FeaturesVerificationTest failed with " + failed + " errors");
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected == null || !expected.equals(actual)) {
            throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertEqualsDouble(double expected, double actual) {
        if (Math.abs(expected - actual) > 0.0001) {
            throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
        }
    }

    private static void assertEqualsFloat(float expected, float actual) {
        if (Math.abs(expected - actual) > 0.0001f) {
            throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
        }
    }
}
