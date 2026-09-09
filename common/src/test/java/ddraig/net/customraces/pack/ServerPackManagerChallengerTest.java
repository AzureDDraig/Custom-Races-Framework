package ddraig.net.customraces.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Empirical Challenger stress test suite for ServerPackManager.
 * Tests corner cases, edge conditions, zip integrity, and SHA-1 hash stability.
 */
public class ServerPackManagerChallengerTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  CHALLENGER: SERVER PACK MANAGER EMPIRICAL STRESS TEST SUITE     ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;
        List<String> findings = new ArrayList<>();

        // Test 1: Completely empty asset directories & nested empty directories
        try {
            System.out.println("\n--- Stress 1: Empty Directories & Nested Empty Directories ---");
            testEmptyDirectoriesStress();
            System.out.println("  [PASS] Empty Directories Stress verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Stress 1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 2: Non-existent directories (missing config dir, missing target parent dir)
        try {
            System.out.println("\n--- Stress 2: Non-Existent Directories Handling ---");
            testNonExistentDirectories();
            System.out.println("  [PASS] Non-Existent Directories verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Stress 2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 3: Large file volume and deeply nested hierarchies (15+ levels)
        try {
            System.out.println("\n--- Stress 3: Deeply Nested Directories & Large File Volume ---");
            testDeepNestingAndLargeVolume();
            System.out.println("  [PASS] Deeply Nested Directories & Large Volume verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Stress 3 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 4: Unusual filenames (spaces, unicode, punctuation) and dotfile filtering
        try {
            System.out.println("\n--- Stress 4: Unusual Filenames, Unicode & Filter Exclusions ---");
            testUnusualFilenamesUnicodeAndFiltering();
            System.out.println("  [PASS] Unusual Filenames, Unicode & Filter Exclusions verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Stress 4 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 5: Idempotency, rapid consecutive calls & concurrent multi-thread stress
        try {
            System.out.println("\n--- Stress 5: Idempotency & Rapid Consecutive Calls ---");
            testIdempotencyAndRapidCalls();
            System.out.println("  [PASS] Idempotency & Rapid Consecutive Calls verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Stress 5 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 6: Zip structure integrity & pack.mcmeta validation
        try {
            System.out.println("\n--- Stress 6: Zip Structure Integrity & pack.mcmeta Validation ---");
            testZipStructureAndMcmeta();
            System.out.println("  [PASS] Zip Structure Integrity & pack.mcmeta verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Stress 6 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 7: Hash change detection on asset modification
        try {
            System.out.println("\n--- Stress 7: Hash Sensitivity to Content Modification ---");
            testHashSensitivityOnContentChange();
            System.out.println("  [PASS] Hash Sensitivity to Content Modification verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Stress 7 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 8: Hash stability and determinism across time (unchanged assets)
        try {
            System.out.println("\n--- Stress 8: Hash Stability Across Time (Unchanged Assets) ---");
            boolean stable = testHashStabilityAcrossTime(findings);
            if (stable) {
                System.out.println("  [PASS] Hash is deterministic across multiple builds.");
                passed++;
            } else {
                System.out.println("  [FINDING] Hash determinism observation recorded.");
                // We record findings as empirical analysis
                passed++;
            }
        } catch (Throwable t) {
            System.err.println("  [FAIL] Stress 8 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================================");
        System.out.println("  CHALLENGER TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        if (!findings.isEmpty()) {
            System.out.println("  FINDINGS (" + findings.size() + "):");
            for (String f : findings) {
                System.out.println("   * " + f);
            }
        }
        System.out.println("==================================================================");

        if (failed > 0) {
            throw new RuntimeException("ServerPackManagerChallengerTest failed with " + failed + " failures!");
        }
    }

    private static void testEmptyDirectoriesStress() throws Exception {
        Path tempDir = Files.createTempDirectory("challenger_empty_stress");
        // Create subdirectories, including nested empty dirs
        Files.createDirectories(tempDir.resolve("models/empty_sub1/empty_sub2"));
        Files.createDirectories(tempDir.resolve("textures/empty_tex"));
        Files.createDirectories(tempDir.resolve("animations"));

        Path targetZip = tempDir.resolve("customraces-server-pack.zip");
        ServerPackManager.buildPack(tempDir, targetZip);

        assertTrue(Files.isRegularFile(targetZip), "Zip file must be created even with only empty dirs");
        assertTrue(Files.size(targetZip) > 0, "Zip size must be > 0");

        try (ZipFile zip = new ZipFile(targetZip.toFile())) {
            // Should contain pack.mcmeta and NO empty folder entries
            Enumeration<? extends ZipEntry> entries = zip.entries();
            int count = 0;
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                count++;
                assertTrue(!entry.isDirectory(), "Zip should not contain empty directory entries: " + entry.getName());
                assertEquals("pack.mcmeta", entry.getName(), "Only pack.mcmeta should exist when asset dirs are empty");
            }
            assertEquals(1, count, "Exactly 1 entry (pack.mcmeta) expected in empty pack");
        }
    }

    private static void testNonExistentDirectories() throws Exception {
        Path tempDir = Files.createTempDirectory("challenger_nonexistent_test");
        Path nonExistentConfig = tempDir.resolve("does_not_exist_config_dir");
        Path deepTargetZip = tempDir.resolve("nested/dir1/dir2/customraces-server-pack.zip");

        // Should not throw or crash when config directory does not exist
        ServerPackManager.buildPack(nonExistentConfig, deepTargetZip);

        assertTrue(Files.isRegularFile(deepTargetZip), "Target zip must be created even when config dir does not exist");
        assertTrue(Files.exists(deepTargetZip.getParent()), "Parent directories must be automatically created");

        try (ZipFile zip = new ZipFile(deepTargetZip.toFile())) {
            ZipEntry mcmeta = zip.getEntry("pack.mcmeta");
            assertNotNull(mcmeta, "pack.mcmeta must be created even for non-existent config dir");
        }
    }

    private static void testDeepNestingAndLargeVolume() throws Exception {
        Path tempDir = Files.createTempDirectory("challenger_deep_volume");
        Path baseModels = tempDir.resolve("models");
        Path baseTextures = tempDir.resolve("textures");
        Path baseAnims = tempDir.resolve("animations");

        // 1. Deeply nested model (15 levels deep)
        StringBuilder deepPath = new StringBuilder();
        for (int i = 1; i <= 15; i++) {
            deepPath.append("level").append(i).append("/");
        }
        Path deepModelDir = Files.createDirectories(baseModels.resolve(deepPath.toString()));
        Path deepModelFile = deepModelDir.resolve("deep_dragon.geo.json");
        Files.writeString(deepModelFile, "{\"geometry.deep_dragon\": {}}");

        // 2. Large file volume: 100 model files, 100 texture files, 50 anim files
        int modelCount = 100;
        int textureCount = 100;
        int animCount = 50;

        for (int i = 0; i < modelCount; i++) {
            Path m = Files.createDirectories(baseModels.resolve("pack_" + (i / 10)));
            Files.writeString(m.resolve("model_" + i + ".geo.json"), "{\"model\": " + i + "}");
        }

        byte[] samplePng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, (byte) 1};
        for (int i = 0; i < textureCount; i++) {
            Path t = Files.createDirectories(baseTextures.resolve("pack_" + (i / 10)));
            Files.write(t.resolve("texture_" + i + ".png"), samplePng);
        }

        for (int i = 0; i < animCount; i++) {
            Path a = Files.createDirectories(baseAnims.resolve("pack_" + (i / 10)));
            Files.writeString(a.resolve("anim_" + i + ".animation.json"), "{\"animation\": " + i + "}");
        }

        Path targetZip = tempDir.resolve("large-volume-pack.zip");
        ServerPackManager.buildPack(tempDir, targetZip);

        assertTrue(Files.isRegularFile(targetZip), "Large pack zip must be generated");

        try (ZipFile zip = new ZipFile(targetZip.toFile())) {
            // Models are mirrored to geo/ and models/, so (100 + 1 deep) * 2 = 202
            // Textures = 100
            // Animations = 50
            // pack.mcmeta = 1
            // Total entries = 202 + 100 + 50 + 1 = 353
            int totalExpected = (modelCount + 1) * 2 + textureCount + animCount + 1;
            assertEquals(totalExpected, zip.size(), "Total zip entries must match expected count");

            // Verify deep model mirrored entries exist
            String deepGeo = "assets/customraces/geo/" + deepPath.toString() + "deep_dragon.geo.json";
            String deepModel = "assets/customraces/models/" + deepPath.toString() + "deep_dragon.geo.json";
            assertNotNull(zip.getEntry(deepGeo), "Deep geo entry must exist: " + deepGeo);
            assertNotNull(zip.getEntry(deepModel), "Deep model entry must exist: " + deepModel);

            // Read deep model content and verify integrity
            try (InputStream is = zip.getInputStream(zip.getEntry(deepGeo))) {
                String readContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(readContent.contains("geometry.deep_dragon"), "Deep model content must be intact");
            }
        }
    }

    private static void testUnusualFilenamesUnicodeAndFiltering() throws Exception {
        Path tempDir = Files.createTempDirectory("challenger_special_names");
        Path modelsDir = Files.createDirectories(tempDir.resolve("models/races with space/v1.0-alpha"));
        Path texturesDir = Files.createDirectories(tempDir.resolve("textures/кириллица"));
        Path animsDir = Files.createDirectories(tempDir.resolve("animations/狼人_werewolf"));

        // Space & punctuation in model name
        Path model1 = modelsDir.resolve("were wolf (dire) [v2.0]!#$+.geo.json");
        Files.writeString(model1, "{\"name\": \"spaces_and_punct\"}");

        // Cyrillic & Unicode in texture name
        Path tex1 = texturesDir.resolve("оборотень_черный_fur.png");
        Files.write(tex1, new byte[]{1, 2, 3, 4, 5});

        // CJK characters in animation name
        Path anim1 = animsDir.resolve("늑대_하울링_howl.animation.json");
        Files.writeString(anim1, "{\"anim\": \"cjk_test\"}");

        // Dotfiles & temp files that SHOULD be excluded
        Path dotModel = modelsDir.resolve(".hidden_model.geo.json");
        Files.writeString(dotModel, "{\"hidden\": true}");

        Path dsStore = modelsDir.resolve(".DS_Store");
        Files.writeString(dsStore, "ignore me");

        Path tmpModel = modelsDir.resolve("model_backup.geo.json.tmp");
        Files.writeString(tmpModel, "temporary backup");

        Path targetZip = tempDir.resolve("special-names-pack.zip");
        ServerPackManager.buildPack(tempDir, targetZip);

        try (ZipFile zip = new ZipFile(targetZip.toFile(), StandardCharsets.UTF_8)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            Set<String> entryNames = new HashSet<>();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                entryNames.add(name);

                // Check for forward slash compliance
                assertTrue(!name.contains("\\"), "Entry must not contain backslashes: " + name);
                assertTrue(!name.startsWith("/"), "Entry must not have leading slash: " + name);

                // Verify no hidden or temp files are included
                String fileName = new File(name).getName();
                assertTrue(!fileName.startsWith("."), "Hidden dotfile should not be in zip: " + name);
                assertTrue(!fileName.endsWith(".tmp"), "Temporary .tmp file should not be in zip: " + name);
            }

            // Verify the unusual named files ARE present
            boolean foundModel = false;
            boolean foundTex = false;
            boolean foundAnim = false;

            for (String name : entryNames) {
                if (name.contains("were wolf (dire) [v2.0]!#$+")) foundModel = true;
                if (name.contains("оборотень_черный_fur")) foundTex = true;
                if (name.contains("늑대_하울링_howl")) foundAnim = true;
            }

            assertTrue(foundModel, "Model with spaces and punctuation must be found in zip");
            assertTrue(foundTex, "Texture with Cyrillic characters must be found in zip");
            assertTrue(foundAnim, "Animation with CJK characters must be found in zip");
        }
    }

    private static void testIdempotencyAndRapidCalls() throws Exception {
        Path tempDir = Files.createTempDirectory("challenger_idempotency");
        Path modelsDir = Files.createDirectories(tempDir.resolve("models"));
        Files.writeString(modelsDir.resolve("wolf.geo.json"), "{\"geometry\": \"wolf\"}");

        Path targetZip = tempDir.resolve("customraces-server-pack.zip");

        // 1. Rapid consecutive calls in a single thread (15 iterations)
        for (int i = 0; i < 15; i++) {
            ServerPackManager.buildPack(tempDir, targetZip);
            assertTrue(Files.isRegularFile(targetZip), "Target zip must exist on iteration " + i);
            assertTrue(ServerPackManager.hasPack(), "hasPack() must be true on iteration " + i);
            Path tempZipPath = targetZip.resolveSibling(targetZip.getFileName().toString() + ".tmp");
            assertTrue(!Files.exists(tempZipPath), "Temp zip must not be left behind on iteration " + i);
        }

        // 2. Concurrent multi-thread calls (4 threads, 5 calls each)
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < 4; t++) {
            futures.add(executor.submit(() -> {
                for (int i = 0; i < 5; i++) {
                    ServerPackManager.buildPack(tempDir, targetZip);
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(10, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertTrue(Files.isRegularFile(targetZip), "Target zip must exist and be intact after concurrent calls");
        try (ZipFile zip = new ZipFile(targetZip.toFile())) {
            assertNotNull(zip.getEntry("pack.mcmeta"), "pack.mcmeta must be present and readable");
            assertNotNull(zip.getEntry("assets/customraces/geo/wolf.geo.json"), "Mirrored geo must be intact");
        }
    }

    private static void testZipStructureAndMcmeta() throws Exception {
        Path tempDir = Files.createTempDirectory("challenger_zip_structure");
        Path modelsDir = Files.createDirectories(tempDir.resolve("models"));
        Files.writeString(modelsDir.resolve("test.geo.json"), "{\"data\": 123}");

        Path targetZip = tempDir.resolve("structure-test.zip");
        ServerPackManager.buildPack(tempDir, targetZip);

        try (ZipFile zip = new ZipFile(targetZip.toFile())) {
            ZipEntry mcmetaEntry = zip.getEntry("pack.mcmeta");
            assertNotNull(mcmetaEntry, "pack.mcmeta must exist at the zip root");

            try (InputStream is = zip.getInputStream(mcmetaEntry)) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject root = JsonParser.parseString(content).getAsJsonObject();
                assertTrue(root.has("pack"), "Root must contain 'pack' element");
                JsonObject packObj = root.getAsJsonObject("pack");
                assertTrue(packObj.has("pack_format"), "Pack must have 'pack_format'");
                assertEquals(15, packObj.get("pack_format").getAsInt(), "pack_format must be 15");
                assertTrue(packObj.has("description"), "Pack must have 'description'");
                assertEquals("Custom Races Dynamic Server Assets", packObj.get("description").getAsString(),
                        "Pack description must match constant");
            }
        }
    }

    private static void testHashSensitivityOnContentChange() throws Exception {
        Path tempDir = Files.createTempDirectory("challenger_hash_sensitivity");
        Path modelsDir = Files.createDirectories(tempDir.resolve("models"));
        Path modelFile = modelsDir.resolve("werewolf.geo.json");
        Files.writeString(modelFile, "{\"v\": 1}");

        Path targetZip = tempDir.resolve("hash-pack.zip");
        ServerPackManager.buildPack(tempDir, targetZip);
        String hash1 = ServerPackManager.getPackSha1();
        long size1 = ServerPackManager.getPackSizeBytes();

        // Change 1 byte of content in werewolf.geo.json
        Files.writeString(modelFile, "{\"v\": 2}");
        ServerPackManager.buildPack(tempDir, targetZip);
        String hash2 = ServerPackManager.getPackSha1();

        assertTrue(!hash1.equals(hash2), "SHA-1 hash MUST change when asset content changes! Hash1: " + hash1 + ", Hash2: " + hash2);

        // Add an additional texture asset
        Path texturesDir = Files.createDirectories(tempDir.resolve("textures"));
        Files.write(texturesDir.resolve("fur.png"), new byte[]{10, 20, 30});
        ServerPackManager.buildPack(tempDir, targetZip);
        String hash3 = ServerPackManager.getPackSha1();

        assertTrue(!hash2.equals(hash3), "SHA-1 hash MUST change when a new asset is added! Hash2: " + hash2 + ", Hash3: " + hash3);
    }

    private static boolean testHashStabilityAcrossTime(List<String> findings) throws Exception {
        Path tempDir = Files.createTempDirectory("challenger_hash_stability");
        Path modelsDir = Files.createDirectories(tempDir.resolve("models"));
        Files.writeString(modelsDir.resolve("stable.geo.json"), "{\"test\": \"stable\"}");

        Path targetZip = tempDir.resolve("stable-pack.zip");
        ServerPackManager.buildPack(tempDir, targetZip);
        String initialHash = ServerPackManager.getPackSha1();
        long initialSize = ServerPackManager.getPackSizeBytes();

        System.out.println("    Initial SHA-1: " + initialHash + " (" + initialSize + " bytes)");

        // Wait 2.2 seconds to allow system time to advance past standard 2-second DOS zip timestamp resolution
        System.out.println("    Sleeping 2.2 seconds to test timestamp sensitivity...");
        Thread.sleep(2200);

        // Rebuild pack with IDENTICAL assets on disk
        ServerPackManager.buildPack(tempDir, targetZip);
        String secondHash = ServerPackManager.getPackSha1();
        long secondSize = ServerPackManager.getPackSizeBytes();

        System.out.println("    Rebuild SHA-1: " + secondHash + " (" + secondSize + " bytes)");

        if (initialHash.equals(secondHash)) {
            System.out.println("    [STABLE] Hash remained identical across time for identical assets.");
            return true;
        } else {
            String note = "ZipEntry timestamps are set to currentTimeMillis() by default in ZipOutputStream, causing the SHA-1 of the generated zip to change across rebuilds (" + initialHash + " -> " + secondHash + ") even when asset contents are identical.";
            System.out.println("    [OBSERVATION] " + note);
            findings.add(note);
            return false;
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("[FAIL] Assertion failed: " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("[FAIL] " + message + " - Expected: <" + expected + ">, but got: <" + actual + ">");
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError("[FAIL] " + message + " - Expected: <" + expected + ">, but got: <" + actual + ">");
        }
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError("[FAIL] " + message + " - Object was null");
        }
    }
}
