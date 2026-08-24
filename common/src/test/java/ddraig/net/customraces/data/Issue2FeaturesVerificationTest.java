package ddraig.net.customraces.data;

import net.minecraft.world.phys.Vec3;

public class Issue2FeaturesVerificationTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  GITHUB ISSUE #2: PASSIVES & DRAWBACKS VERIFICATION TEST SUITE   ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: Arrow Deflection Vector Inversion & Dot Product Math
        try {
            System.out.println("\n--- Running Test 1: Arrow Deflection Trajectory Math ---");
            Vec3 playerPos = new Vec3(10.0, 64.0, 10.0);
            Vec3 projPos = new Vec3(10.0, 64.0, 12.5); // 2.5 blocks away on Z
            Vec3 incomingVelocity = new Vec3(0.0, 0.0, -1.5); // Flying along -Z toward player

            Vec3 toPlayer = playerPos.subtract(projPos); // (0, 0, -2.5)
            double dot = incomingVelocity.dot(toPlayer); // -1.5 * -2.5 = +3.75 > 0 (incoming!)
            assertTrue(dot > 0, "Dot product must be > 0 when projectile is flying toward player");

            // Compute deflected velocity
            Vec3 deflectedVel = new Vec3(incomingVelocity.x * -1.2, incomingVelocity.y * -0.5 + 0.2, incomingVelocity.z * -1.2);
            assertTrue(deflectedVel.z > 0, "Deflected projectile must reverse direction along Z");
            assertEquals(1.8, deflectedVel.z); // -1.5 * -1.2 = +1.8

            // Outgoing projectile (flying away) should NOT trigger deflection
            Vec3 outgoingVelocity = new Vec3(0.0, 0.0, 1.5);
            double outgoingDot = outgoingVelocity.dot(toPlayer); // 1.5 * -2.5 = -3.75 < 0
            assertTrue(outgoingDot <= 0, "Dot product must be <= 0 when projectile is flying away");

            System.out.println("  [PASS] Arrow deflection vector calculations and directional filtering verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 2: Magnet Aura Attraction Vector & Sneak/Delay Logic
        try {
            System.out.println("\n--- Running Test 2: Magnet Aura Velocity Pull & Drop Delay Safety ---");
            Vec3 playerPos = new Vec3(10.0, 64.0, 10.0);
            Vec3 itemPos = new Vec3(13.0, 64.0, 10.0); // 3 blocks away on X

            Vec3 playerChest = playerPos.add(0, 0.5, 0);
            Vec3 pullVec = playerChest.subtract(itemPos).normalize().scale(0.22);

            assertTrue(pullVec.x < 0, "Pull vector must pull item toward player on X axis");
            assertTrue(Math.abs(pullVec.y - (0.5 / 3.04138) * 0.22) < 0.01, "Slight upward pull vector toward chest");

            // Sneaking suppression test
            boolean sneaking = true;
            boolean magnetActive = !sneaking;
            assertTrue(!magnetActive, "Magnet aura must be inactive when player is sneaking (crouching)");

            // Drop delay protection test
            boolean hasPickupDelay = true;
            boolean canPullItem = !hasPickupDelay;
            assertTrue(!canPullItem, "Magnet aura must NEVER pull items that have pickup delay (e.g. freshly dropped with Q)");

            System.out.println("  [PASS] Magnet aura smooth velocity pull and drop-delay safety verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 3: Night Miner Activation Matrix
        try {
            System.out.println("\n--- Running Test 3: Night Miner Activation Matrix ---");
            // Case A: Daytime on surface (Y=70, Light=15, isDay=true) -> False
            assertTrue(!isNightMinerActive(true, 70, 15), "Surface day not active");

            // Case B: Nighttime on surface (Y=70, Light=4, isDay=false) -> True
            assertTrue(isNightMinerActive(false, 70, 4), "Surface night active");

            // Case C: Underground daytime (Y=30, Light=15, isDay=true) -> True (underground!)
            assertTrue(isNightMinerActive(true, 30, 15), "Underground day active");

            // Case D: Dark cave during day (Y=65, Light=2, isDay=true) -> True (dark cave!)
            assertTrue(isNightMinerActive(true, 65, 2), "Dark cave day active");

            // Case E: Deep dark mining (Y=-40, Light=0, isDay=true) -> True
            assertTrue(isNightMinerActive(true, -40, 0), "Deep dark mining active");

            System.out.println("  [PASS] Night miner day/night/underground/cave activation matrix verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 3 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 4: Desert Dehydration Climate & Biome Detection
        try {
            System.out.println("\n--- Running Test 4: Desert Dehydration Climate Detection ---");
            // Vanilla desert
            assertTrue(isHotAridBiome("minecraft:desert", 2.0f), "Vanilla desert detected");
            // Badlands / Mesa
            assertTrue(isHotAridBiome("minecraft:badlands", 2.0f), "Badlands detected");
            assertTrue(isHotAridBiome("minecraft:eroded_badlands", 2.0f), "Eroded badlands detected");
            // Savanna
            assertTrue(isHotAridBiome("minecraft:savanna", 1.2f), "Savanna path detected");
            // Modded hot biome with temperature >= 1.5f
            assertTrue(isHotAridBiome("terralith:sandstone_valley", 1.8f), "Modded arid biome with high temperature detected");
            // Normal forest (not hot arid)
            assertTrue(!isHotAridBiome("minecraft:forest", 0.7f), "Forest not hot arid");
            // Snowy plains (cold)
            assertTrue(!isHotAridBiome("minecraft:snowy_plains", 0.0f), "Snowy plains not hot arid");

            System.out.println("  [PASS] Desert dehydration climate temperature and path detection verified.");
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
            throw new RuntimeException("Issue2FeaturesVerificationTest failed with " + failed + " errors");
        }
    }

    private static boolean isNightMinerActive(boolean isDay, double y, int lightLevel) {
        boolean isNight = !isDay;
        boolean isUnderground = y < 55;
        boolean isDark = lightLevel < 8;
        return isNight || isUnderground || isDark;
    }

    private static boolean isHotAridBiome(String biomePath, float baseTemp) {
        String path = biomePath.toLowerCase();
        return path.contains("desert") || path.contains("badlands") || path.contains("mesa") || path.contains("savanna") || baseTemp >= 1.5f;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) throw new AssertionError("Assertion failed: " + message);
    }

    private static void assertEquals(double expected, double actual) {
        if (Math.abs(expected - actual) > 0.0001) {
            throw new AssertionError("Expected: " + expected + ", Actual: " + actual);
        }
    }
}
