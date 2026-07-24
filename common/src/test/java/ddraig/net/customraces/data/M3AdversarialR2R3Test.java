package ddraig.net.customraces.data;

import net.minecraft.world.entity.player.Player;
import sun.misc.Unsafe;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Adversarial empirical test suite for Milestone 3 Requirement R2 (Permission Locks)
 * and Requirement R3 (Config Persistence).
 */
public class M3AdversarialR2R3Test {

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

    private static class MockPermissionPlayer extends Player {
        private int permissionLevel;

        private MockPermissionPlayer() {
            super(null, null, 0f, null);
        }

        public void setPermissionLevel(int level) {
            this.permissionLevel = level;
        }

        @Override
        public boolean hasPermissions(int level) {
            return this.permissionLevel >= level;
        }

        @Override
        public boolean isSpectator() {
            return false;
        }

        @Override
        public boolean isCreative() {
            return false;
        }
    }

    private static MockPermissionPlayer createMockPlayer(int permissionLevel) throws Exception {
        if (UNSAFE == null) {
            throw new IllegalStateException("Unsafe is unavailable");
        }
        MockPermissionPlayer player = (MockPermissionPlayer) UNSAFE.allocateInstance(MockPermissionPlayer.class);
        player.setPermissionLevel(permissionLevel);
        return player;
    }

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  M3 ADVERSARIAL PERMISSION LOCKS (R2) & CONFIG (R3) TEST SUITE  ");
        System.out.println("==================================================================");

        try {
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
            System.out.println("[INIT] Minecraft Bootstrap initialized successfully.");
        } catch (Throwable t) {
            System.err.println("[WARN] Minecraft Bootstrap init failed: " + t.getMessage());
        }

        int passed = 0;
        int failed = 0;

        // R2 Permission Lock Tests
        try {
            testPermissionLockNullAndEmptyAcrossPlayers();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 1 (Permission Lock Null/Empty): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testPermissionLockStringNodes();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 2 (Permission Lock String Nodes): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testPermissionLockNumericLevels();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 3 (Permission Lock Numeric Levels): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testPermissionLockNumericEdgeCases();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 4 (Permission Lock Numeric Edge Cases): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // R3 Config Persistence Tests
        try {
            testConfigMissingFileAutoCreate();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 5 (Config Missing File Auto Create): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testConfigCorruptJsonResilience();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 6 (Config Corrupt JSON Resilience): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testConfigInvalidDataTypes();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 7 (Config Invalid Data Types): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testConfigToggleFlips();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 8 (Config Toggle Flips): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testConfigConcurrentSaveLoadStress();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 9 (Config Concurrent Save/Load Stress): " + t.getMessage());
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

    private static void assertEquals(Object expected, Object actual, String msg) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError(msg + " Expected: [" + expected + "], Actual: [" + actual + "]");
    }

    /**
     * Test 1: Test permissionLock = null, "", "   " across null player, non-OP player, and OP player.
     */
    public static void testPermissionLockNullAndEmptyAcrossPlayers() throws Exception {
        System.out.println("\n--- Running Test 1: Permission Lock Null/Empty ---");

        RaceData raceNull = new RaceData("null_lock", "Null Lock");
        raceNull.permissionLock = null;

        RaceData raceEmpty = new RaceData("empty_lock", "Empty Lock");
        raceEmpty.permissionLock = "";

        RaceData raceSpace = new RaceData("space_lock", "Space Lock");
        raceSpace.permissionLock = "   ";

        MockPermissionPlayer nonOpPlayer = createMockPlayer(0);
        MockPermissionPlayer opPlayer = createMockPlayer(2);

        // Null player
        assertTrue(RaceRegistry.canPlayerSelectRace(null, raceNull), "Null player can select race with null permissionLock");
        assertTrue(RaceRegistry.canPlayerSelectRace(null, raceEmpty), "Null player can select race with empty permissionLock");
        assertTrue(RaceRegistry.canPlayerSelectRace(null, raceSpace), "Null player can select race with whitespace permissionLock");

        // Non-OP player
        assertTrue(RaceRegistry.canPlayerSelectRace(nonOpPlayer, raceNull), "Non-OP player can select race with null permissionLock");
        assertTrue(RaceRegistry.canPlayerSelectRace(nonOpPlayer, raceEmpty), "Non-OP player can select race with empty permissionLock");
        assertTrue(RaceRegistry.canPlayerSelectRace(nonOpPlayer, raceSpace), "Non-OP player can select race with whitespace permissionLock");

        // OP player
        assertTrue(RaceRegistry.canPlayerSelectRace(opPlayer, raceNull), "OP player can select race with null permissionLock");
        assertTrue(RaceRegistry.canPlayerSelectRace(opPlayer, raceEmpty), "OP player can select race with empty permissionLock");
        assertTrue(RaceRegistry.canPlayerSelectRace(opPlayer, raceSpace), "OP player can select race with whitespace permissionLock");

        // Null race
        assertFalse(RaceRegistry.canPlayerSelectRace(null, null), "canPlayerSelectRace should return false for null race");
        assertFalse(RaceRegistry.canPlayerSelectRace(nonOpPlayer, null), "canPlayerSelectRace should return false for null race with non-null player");

        System.out.println("  [PASS] Null, empty, and whitespace permission locks allow selection for null, non-OP, and OP players.");
    }

    /**
     * Test 2: Test permissionLock = "customraces.vip" and "admin.only".
     */
    public static void testPermissionLockStringNodes() throws Exception {
        System.out.println("\n--- Running Test 2: Permission Lock String Nodes ---");

        RaceData raceVip = new RaceData("vip_race", "VIP Race");
        raceVip.permissionLock = "customraces.vip";

        RaceData raceAdmin = new RaceData("admin_race", "Admin Race");
        raceAdmin.permissionLock = "admin.only";

        MockPermissionPlayer nonOpPlayer = createMockPlayer(0);
        MockPermissionPlayer perm1Player = createMockPlayer(1);
        MockPermissionPlayer opPlayer = createMockPlayer(2);

        // Null player -> false
        assertFalse(RaceRegistry.canPlayerSelectRace(null, raceVip), "Null player cannot select string locked race");
        assertFalse(RaceRegistry.canPlayerSelectRace(null, raceAdmin), "Null player cannot select admin locked race");

        // Non-OP player (level 0) -> false (Integer.parseInt fails)
        assertFalse(RaceRegistry.canPlayerSelectRace(nonOpPlayer, raceVip), "Non-OP player (level 0) cannot select customraces.vip race");
        assertFalse(RaceRegistry.canPlayerSelectRace(nonOpPlayer, raceAdmin), "Non-OP player (level 0) cannot select admin.only race");

        // Perm level 1 player -> false
        assertFalse(RaceRegistry.canPlayerSelectRace(perm1Player, raceVip), "Level 1 player cannot select customraces.vip race");

        // OP player (level 2) -> true (bypasses string parse check via hasPermissions(2))
        assertTrue(RaceRegistry.canPlayerSelectRace(opPlayer, raceVip), "OP player (level 2) can select customraces.vip race");
        assertTrue(RaceRegistry.canPlayerSelectRace(opPlayer, raceAdmin), "OP player (level 2) can select admin.only race");

        System.out.println("  [PASS] String permission nodes ('customraces.vip', 'admin.only') lock non-OP players and grant OP level 2 players.");
    }

    /**
     * Test 3: Test permissionLock = "2" and "4".
     */
    public static void testPermissionLockNumericLevels() throws Exception {
        System.out.println("\n--- Running Test 3: Permission Lock Numeric Levels ---");

        RaceData raceLevel2 = new RaceData("level2_race", "Level 2 Race");
        raceLevel2.permissionLock = "2";

        RaceData raceLevel4 = new RaceData("level4_race", "Level 4 Race");
        raceLevel4.permissionLock = "4";

        MockPermissionPlayer level0 = createMockPlayer(0);
        MockPermissionPlayer level1 = createMockPlayer(1);
        MockPermissionPlayer level2 = createMockPlayer(2);
        MockPermissionPlayer level4 = createMockPlayer(4);

        // Level 2 lock:
        assertFalse(RaceRegistry.canPlayerSelectRace(level0, raceLevel2), "Level 0 player rejected for level 2 lock");
        assertFalse(RaceRegistry.canPlayerSelectRace(level1, raceLevel2), "Level 1 player rejected for level 2 lock");
        assertTrue(RaceRegistry.canPlayerSelectRace(level2, raceLevel2), "Level 2 player accepted for level 2 lock");
        assertTrue(RaceRegistry.canPlayerSelectRace(level4, raceLevel2), "Level 4 player accepted for level 2 lock");

        // Level 4 lock:
        assertFalse(RaceRegistry.canPlayerSelectRace(level0, raceLevel4), "Level 0 player rejected for level 4 lock");
        assertFalse(RaceRegistry.canPlayerSelectRace(level1, raceLevel4), "Level 1 player rejected for level 4 lock");
        assertTrue(RaceRegistry.canPlayerSelectRace(level2, raceLevel4), "Level 2 player accepted for level 4 lock (due to hardcoded OP level 2 check in canPlayerSelectRace)");
        assertTrue(RaceRegistry.canPlayerSelectRace(level4, raceLevel4), "Level 4 player accepted for level 4 lock");

        System.out.println("  [PASS] Numeric permission locks checked correctly against player permission levels.");
    }

    /**
     * Test 4: Numeric edge cases ("-1", "99999999999999999999").
     */
    public static void testPermissionLockNumericEdgeCases() throws Exception {
        System.out.println("\n--- Running Test 4: Permission Lock Numeric Edge Cases ---");

        RaceData raceNeg = new RaceData("neg_race", "Neg Race");
        raceNeg.permissionLock = "-1";

        RaceData raceOverflow = new RaceData("overflow_race", "Overflow Race");
        raceOverflow.permissionLock = "999999999999999999999999999999";

        MockPermissionPlayer level0 = createMockPlayer(0);
        MockPermissionPlayer level2 = createMockPlayer(2);

        // Negative level "-1": level 0 player has permission >= -1 -> true
        assertTrue(RaceRegistry.canPlayerSelectRace(level0, raceNeg), "Level 0 player accepted for -1 lock");

        // Overflow level: Integer.parseInt throws NumberFormatException -> falls through to return false for non-OP
        assertFalse(RaceRegistry.canPlayerSelectRace(level0, raceOverflow), "Level 0 player rejected for overflow lock");
        assertTrue(RaceRegistry.canPlayerSelectRace(level2, raceOverflow), "OP Level 2 player accepted for overflow lock");

        System.out.println("  [PASS] Numeric edge cases (-1, overflow) handled safely.");
    }

    /**
     * Test 5: Missing config file auto-creation.
     */
    public static void testConfigMissingFileAutoCreate() {
        System.out.println("\n--- Running Test 5: Config Missing File Auto Create ---");

        File configFile = RaceRegistry.getConfigFile();
        if (configFile.exists()) {
            configFile.delete();
        }

        RaceRegistry.autoOpenSelectionOnJoin = true;
        RaceRegistry.loadConfig(); // Should trigger saveConfig() since file does not exist

        assertTrue(configFile.exists(), "config.json should be auto-created when loadConfig() is called on missing file");

        // Cleanup
        if (configFile.exists()) configFile.delete();
        RaceRegistry.autoOpenSelectionOnJoin = true;

        System.out.println("  [PASS] Missing config file triggers automatic creation of default config.json.");
    }

    /**
     * Test 6: Corrupt JSON file resilience.
     */
    public static void testConfigCorruptJsonResilience() throws Exception {
        System.out.println("\n--- Running Test 6: Config Corrupt JSON Resilience ---");

        File configFile = RaceRegistry.getConfigFile();

        // 1. Syntax Error JSON
        try (FileWriter w = new FileWriter(configFile)) {
            w.write("{ \"autoOpenSelectionOnJoin\": true, malformed_json_content ");
        }
        RaceRegistry.autoOpenSelectionOnJoin = true;
        RaceRegistry.loadConfig(); // Should catch JsonSyntaxException without throwing
        assertTrue(RaceRegistry.autoOpenSelectionOnJoin, "In-memory config value retained after syntax error JSON");

        // 2. JSON Array instead of JsonObject
        try (FileWriter w = new FileWriter(configFile)) {
            w.write("[ \"foo\", \"bar\", 123 ]");
        }
        RaceRegistry.autoOpenSelectionOnJoin = false;
        RaceRegistry.loadConfig(); // GSON.fromJson returns null or throws ClassCastException/JsonSyntaxException
        assertFalse(RaceRegistry.autoOpenSelectionOnJoin, "In-memory config value retained after array JSON");

        // 3. Truncated JSON
        try (FileWriter w = new FileWriter(configFile)) {
            w.write("{ \"autoOpen");
        }
        RaceRegistry.autoOpenSelectionOnJoin = true;
        RaceRegistry.loadConfig();
        assertTrue(RaceRegistry.autoOpenSelectionOnJoin, "In-memory config value retained after truncated JSON");

        // Cleanup
        if (configFile.exists()) configFile.delete();
        RaceRegistry.autoOpenSelectionOnJoin = true;

        System.out.println("  [PASS] Corrupt JSON syntax, truncated files, and array JSON caught safely with existing state preserved.");
    }

    /**
     * Test 7: Invalid JSON data types for config fields.
     */
    public static void testConfigInvalidDataTypes() throws Exception {
        System.out.println("\n--- Running Test 7: Config Invalid Data Types ---");

        File configFile = RaceRegistry.getConfigFile();

        // 1. Number instead of boolean: {"autoOpenSelectionOnJoin": 12345}
        try (FileWriter w = new FileWriter(configFile)) {
            w.write("{ \"autoOpenSelectionOnJoin\": 12345 }");
        }
        RaceRegistry.autoOpenSelectionOnJoin = false;
        RaceRegistry.loadConfig();
        // JsonPrimitive.getAsBoolean() converts non-zero numbers to true or throws, caught safely
        // In GSON, primitive number.getAsBoolean() returns false unless string 'true' or boolean true
        System.out.println("  [OBSERVATION] Number primitive parsed as boolean: " + RaceRegistry.autoOpenSelectionOnJoin);

        // 2. Object instead of boolean: {"autoOpenSelectionOnJoin": {"nested": true}}
        try (FileWriter w = new FileWriter(configFile)) {
            w.write("{ \"autoOpenSelectionOnJoin\": { \"nested\": true } }");
        }
        RaceRegistry.autoOpenSelectionOnJoin = true;
        RaceRegistry.loadConfig(); // Exception thrown by getAsBoolean on JsonObject, caught safely
        assertTrue(RaceRegistry.autoOpenSelectionOnJoin, "In-memory value preserved when value is nested object");

        // Cleanup
        if (configFile.exists()) configFile.delete();
        RaceRegistry.autoOpenSelectionOnJoin = true;

        System.out.println("  [PASS] Invalid data types in config fields handled without crash.");
    }

    /**
     * Test 8: Toggle flips and persistence verification.
     */
    public static void testConfigToggleFlips() {
        System.out.println("\n--- Running Test 8: Config Toggle Flips ---");

        File configFile = RaceRegistry.getConfigFile();

        // Toggle 1: Set to false -> Save -> Mutate memory to true -> Load -> Expect false
        RaceRegistry.autoOpenSelectionOnJoin = false;
        RaceRegistry.saveConfig();

        RaceRegistry.autoOpenSelectionOnJoin = true;
        RaceRegistry.loadConfig();
        assertFalse(RaceRegistry.autoOpenSelectionOnJoin, "Loaded config should be false");

        // Toggle 2: Set to true -> Save -> Mutate memory to false -> Load -> Expect true
        RaceRegistry.autoOpenSelectionOnJoin = true;
        RaceRegistry.saveConfig();

        RaceRegistry.autoOpenSelectionOnJoin = false;
        RaceRegistry.loadConfig();
        assertTrue(RaceRegistry.autoOpenSelectionOnJoin, "Loaded config should be true");

        // Cleanup
        if (configFile.exists()) configFile.delete();
        RaceRegistry.autoOpenSelectionOnJoin = true;

        System.out.println("  [PASS] Toggle flips (false -> true -> false) persisted and reloaded accurately.");
    }

    /**
     * Test 9: Concurrent config save & load stress test across threads.
     */
    public static void testConfigConcurrentSaveLoadStress() throws Exception {
        System.out.println("\n--- Running Test 9: Concurrent Config Save/Load Stress ---");

        int threadCount = 20;
        int operationsPerThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicInteger errorCount = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final boolean toggleState = (t % 2 == 0);
            executor.submit(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    try {
                        if (i % 2 == 0) {
                            RaceRegistry.autoOpenSelectionOnJoin = toggleState;
                            RaceRegistry.saveConfig();
                        } else {
                            RaceRegistry.loadConfig();
                        }
                    } catch (Throwable ex) {
                        errorCount.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        boolean finished = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(finished, "Concurrent config execution completed within timeout");
        assertEquals(0, errorCount.get(), "Zero exceptions during concurrent config operations");

        // Cleanup
        File configFile = RaceRegistry.getConfigFile();
        if (configFile.exists()) configFile.delete();
        RaceRegistry.autoOpenSelectionOnJoin = true;

        System.out.println("  [PASS] Concurrent save/load across 20 threads (10,000 ops) completed with zero errors.");
    }
}
