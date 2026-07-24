package ddraig.net.customraces.client.render;

import ddraig.net.customraces.data.ParticleAuraData;
import ddraig.net.customraces.data.RaceData;
import net.minecraft.nbt.CompoundTag;

public class M3ParticleConfigVerificationTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  M3 EMPIRICAL PARTICLE CONFIG TEST SUITE  ");
        System.out.println("=================================================");

        int passed = 0;
        int failed = 0;

        try {
            testRaceDataParticleDefaultsAndGetters();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 1 (Particle Defaults & Getters): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testRaceDataNBTSerialization();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 2 (NBT Serialization): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testParticleAuraScalingLogic();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 3 (Particle Aura Scaling): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            testParticleCountEdgeCases();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 4 (Particle Count Edge Cases): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("=================================================");
        System.out.println("  SUMMARY: " + passed + " PASSED, " + failed + " FAILED  ");
        System.out.println("=================================================");

        if (failed > 0) {
            throw new RuntimeException("M3 Particle Config Verification Test Failed!");
        }
    }

    public static void testRaceDataParticleDefaultsAndGetters() {
        System.out.println("\n--- Running Test 1: RaceData Particle Defaults & Getters ---");
        RaceData race = new RaceData("test_race", "Test Race");

        if (race.getParticleCount() != 5) {
            throw new AssertionError("Expected default particleCount to be 5, got: " + race.getParticleCount());
        }
        if (race.getWereParticleCount() != 10) {
            throw new AssertionError("Expected default wereParticleCount to be 10, got: " + race.getWereParticleCount());
        }

        race.setParticleCount(15);
        race.setWereParticleCount(25);

        if (race.getParticleCount() != 15) {
            throw new AssertionError("Expected particleCount to be 15, got: " + race.getParticleCount());
        }
        if (race.getWereParticleCount() != 25) {
            throw new AssertionError("Expected wereParticleCount to be 25, got: " + race.getWereParticleCount());
        }

        System.out.println("[PASS] Default values and getters/setters validated.");
    }

    public static void testRaceDataNBTSerialization() {
        System.out.println("\n--- Running Test 2: RaceData NBT Serialization ---");
        RaceData race = new RaceData("dragon", "Dragonkin");
        race.setParticleCount(12);
        race.setWereParticleCount(30);

        CompoundTag tag = new CompoundTag();
        race.toNBT(tag);

        if (!tag.contains("particleCount") || tag.getInt("particleCount") != 12) {
            throw new AssertionError("NBT failed to serialize particleCount properly: " + tag);
        }
        if (!tag.contains("wereParticleCount") || tag.getInt("wereParticleCount") != 30) {
            throw new AssertionError("NBT failed to serialize wereParticleCount properly: " + tag);
        }

        RaceData deserialized = new RaceData();
        deserialized.fromNBT(tag);

        if (deserialized.getParticleCount() != 12) {
            throw new AssertionError("Expected deserialized particleCount 12, got: " + deserialized.getParticleCount());
        }
        if (deserialized.getWereParticleCount() != 30) {
            throw new AssertionError("Expected deserialized wereParticleCount 30, got: " + deserialized.getWereParticleCount());
        }

        System.out.println("[PASS] NBT toNBT and fromNBT serialization cycle verified.");
    }

    public static void testParticleAuraScalingLogic() {
        System.out.println("\n--- Running Test 3: Particle Aura Scaling Logic ---");
        ParticleAuraData aura = new ParticleAuraData("minecraft:flame", 1.0f, 0.05f, 0.5f);

        // Base particleCount = 5 -> scaled count = 1
        int scaledBase = aura.getScaledParticleCount(5);
        if (scaledBase != 1) {
            throw new AssertionError("Expected scaled particle count 1 for count=5, got: " + scaledBase);
        }

        // particleCount = 10 -> scaled count = 2
        int scaled10 = aura.getScaledParticleCount(10);
        if (scaled10 != 2) {
            throw new AssertionError("Expected scaled particle count 2 for count=10, got: " + scaled10);
        }

        // particleCount = 25 -> scaled count = 5
        int scaled25 = aura.getScaledParticleCount(25);
        if (scaled25 != 5) {
            throw new AssertionError("Expected scaled particle count 5 for count=25, got: " + scaled25);
        }

        System.out.println("[PASS] ParticleAuraData scaling calculation verified.");
    }

    public static void testParticleCountEdgeCases() {
        System.out.println("\n--- Running Test 4: Particle Count Edge Cases ---");
        RaceData race = new RaceData();
        race.particleCount = -10;
        race.wereParticleCount = 0;
        race.initDefaults();

        if (race.getParticleCount() != 5) {
            throw new AssertionError("Expected fallback 5 for negative particleCount, got: " + race.getParticleCount());
        }
        if (race.getWereParticleCount() != 10) {
            throw new AssertionError("Expected fallback 10 for zero wereParticleCount, got: " + race.getWereParticleCount());
        }

        ParticleAuraData aura = new ParticleAuraData("minecraft:soul_fire_flame", 2.0f, 0.05f, 0.5f);
        int scaledZero = aura.getScaledParticleCount(0);
        if (scaledZero != 2) {
            throw new AssertionError("Expected fallback scaling for 0 particleCount, got: " + scaledZero);
        }

        System.out.println("[PASS] Edge cases and negative/zero fallbacks validated.");
    }
}
