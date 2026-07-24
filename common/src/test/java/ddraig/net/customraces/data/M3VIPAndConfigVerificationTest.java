package ddraig.net.customraces.data;

import net.minecraft.nbt.CompoundTag;
import java.io.File;

public class M3VIPAndConfigVerificationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  M3 VIP LOCK & CONFIG VERIFICATION TEST SUITE  ");
        System.out.println("=================================================");

        int passed = 0;
        int failed = 0;

        try {
            testPermissionLockNBTSerialization();
            System.out.println("[PASS] Test 1: PermissionLock NBT Serialization");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 1: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testPermissionLockDefaults();
            System.out.println("[PASS] Test 2: PermissionLock Defaults");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 2: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testCanPlayerSelectRaceNullAndEmpty();
            System.out.println("[PASS] Test 3: canPlayerSelectRace Null & Empty Checks");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 3: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testCanPlayerSelectRaceLockedNullPlayer();
            System.out.println("[PASS] Test 4: canPlayerSelectRace Locked Null Player Check");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 4: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testConfigSaveAndLoad();
            System.out.println("[PASS] Test 5: Config Save & Load Persistence");
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 5: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("=================================================");
        System.out.println("  RESULTS: " + passed + " Passed, " + failed + " Failed  ");
        System.out.println("=================================================");

        if (failed > 0) {
            throw new RuntimeException("M3 VIP & Config Test Suite failed with " + failed + " error(s).");
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
        throw new AssertionError(msg + " Expected: " + expected + ", Actual: " + actual);
    }

    public static void testPermissionLockNBTSerialization() {
        RaceData race = new RaceData("vip_elf", "VIP Elf");
        race.permissionLock = "customraces.vip";

        CompoundTag tag = new CompoundTag();
        race.toNBT(tag);

        assertEquals("customraces.vip", tag.getString("permissionLock"), "NBT string permissionLock mismatch");

        RaceData loaded = new RaceData("dummy", "Dummy");
        loaded.fromNBT(tag);

        assertEquals("customraces.vip", loaded.permissionLock, "Deserialized permissionLock mismatch");
    }

    public static void testPermissionLockDefaults() {
        RaceData race = new RaceData("default_race", "Default Race");
        race.permissionLock = null;
        race.initDefaults();

        assertEquals("", race.permissionLock, "permissionLock null check failed");
    }

    public static void testCanPlayerSelectRaceNullAndEmpty() {
        assertFalse(RaceRegistry.canPlayerSelectRace(null, null), "canPlayerSelectRace should return false for null race");

        RaceData race = new RaceData("open_race", "Open Race");
        race.permissionLock = "";
        assertTrue(RaceRegistry.canPlayerSelectRace(null, race), "canPlayerSelectRace should return true for empty permissionLock");

        race.permissionLock = "   ";
        assertTrue(RaceRegistry.canPlayerSelectRace(null, race), "canPlayerSelectRace should return true for whitespace permissionLock");
    }

    public static void testCanPlayerSelectRaceLockedNullPlayer() {
        RaceData race = new RaceData("locked_race", "Locked Race");
        race.permissionLock = "customraces.vip";
        assertFalse(RaceRegistry.canPlayerSelectRace(null, race), "canPlayerSelectRace should return false for null player on locked race");
    }

    public static void testConfigSaveAndLoad() {
        File configFile = RaceRegistry.getConfigFile();
        if (configFile.exists()) {
            configFile.delete();
        }
        RaceRegistry.autoOpenSelectionOnJoin = false;
        RaceRegistry.saveConfig();

        assertTrue(configFile.exists(), "config.json should be created after saveConfig()");

        // Reset in-memory value
        RaceRegistry.autoOpenSelectionOnJoin = true;

        RaceRegistry.loadConfig();
        assertFalse(RaceRegistry.autoOpenSelectionOnJoin, "loadConfig() should load false for autoOpenSelectionOnJoin");

        // Clean up
        if (configFile.exists()) {
            configFile.delete();
        }
        RaceRegistry.autoOpenSelectionOnJoin = true;
    }
}
