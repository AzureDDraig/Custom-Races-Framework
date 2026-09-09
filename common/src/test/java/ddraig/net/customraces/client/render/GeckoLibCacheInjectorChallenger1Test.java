package ddraig.net.customraces.client.render;

import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Challenger M3-1 Empirical Adversarial Stress Test Suite:
 * 1. Corrupt zip files (truncated headers, zero bytes, random garbage data, truncated entries).
 * 2. Path traversal attempts in zip entries (../../ traversal, backslashes, escape sequences).
 * 3. Non-standard namespaces (uppercase, mixed case, illegal characters, empty namespaces).
 * 4. Malformed geo JSON (missing format_version, missing geometry, empty strings, invalid syntax, arrays/primitives).
 * 5. Malformed animation JSON (missing animations, null entries, non-object types, invalid syntax).
 * 6. isModelBaked and isAnimationBaked nulls, empty strings, non-existent locations, and alias symmetry.
 * 7. GeckoLibWereRenderer edge and adversarial reflection contracts.
 * 8. High-concurrency multithreaded adversarial stress hammer.
 */
public class GeckoLibCacheInjectorChallenger1Test {

    private static final String VALID_GEO_JSON = "{"
            + "\"format_version\": \"1.12.0\","
            + "\"geometry\": [{"
            + "  \"description\": {\"identifier\": \"geometry.were_challenger\", \"texture_width\": 64, \"texture_height\": 64},"
            + "  \"bones\": [{\"name\": \"head\", \"pivot\": [0, 24, 0]}]"
            + "}]"
            + "}";

    private static final String VALID_ANIM_JSON = "{"
            + "\"format_version\": \"1.8.0\","
            + "\"animations\": {\"animation.were_challenger.idle\": {\"loop\": true}}"
            + "}";

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  CHALLENGER M3-1 EMPIRICAL ADVERSARIAL STRESS TEST SUITE         ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        // Group 1: Corrupt ZIP files stress tests
        try { testCorruptZipZeroBytes(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testCorruptZipZeroBytes: " + t.getMessage()); t.printStackTrace(); failed++; }

        try { testCorruptZipTruncatedHeaders(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testCorruptZipTruncatedHeaders: " + t.getMessage()); t.printStackTrace(); failed++; }

        try { testCorruptZipRandomGarbageStreams(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testCorruptZipRandomGarbageStreams: " + t.getMessage()); t.printStackTrace(); failed++; }

        try { testCorruptZipNullAndInvalidPaths(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testCorruptZipNullAndInvalidPaths: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Group 2: Path Traversal & Zip Entry Path Attacks
        try { testPathTraversalAttacksInZipEntries(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testPathTraversalAttacksInZipEntries: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Group 3: Non-Standard Namespaces & Malformed Directory Entries
        try { testNonStandardNamespacesInZip(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testNonStandardNamespacesInZip: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Group 4: Malformed Geo JSON & Model Baking Stress
        try { testMalformedGeoJsonSyntaxAndEmpty(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testMalformedGeoJsonSyntaxAndEmpty: " + t.getMessage()); t.printStackTrace(); failed++; }

        try { testMalformedGeoJsonPackResilience(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testMalformedGeoJsonPackResilience: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Group 5: Malformed Animation JSON & Baking Stress
        try { testMalformedAnimationJsonSyntaxAndEmpty(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testMalformedAnimationJsonSyntaxAndEmpty: " + t.getMessage()); t.printStackTrace(); failed++; }

        try { testMalformedAnimationJsonPackResilience(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testMalformedAnimationJsonPackResilience: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Group 6: isModelBaked and isAnimationBaked Null & Edge Cases
        try { testIsModelBakedAndIsAnimationBakedNullAndEdgeCases(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testIsModelBakedAndIsAnimationBakedNullAndEdgeCases: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Group 7: GeckoLibWereRenderer Edge & Adversarial Contracts
        try { testGeckoLibWereRendererEdgeAndAdversarialContracts(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testGeckoLibWereRendererEdgeAndAdversarialContracts: " + t.getMessage()); t.printStackTrace(); failed++; }

        // Group 8: High Concurrency Adversarial Hammer
        try { testHighConcurrencyAdversarialHammer(); passed++; }
        catch (Throwable t) { System.err.println("[FAIL] testHighConcurrencyAdversarialHammer: " + t.getMessage()); t.printStackTrace(); failed++; }

        System.out.println("==================================================================");
        System.out.println("  CHALLENGER M3-1 SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================================");

        if (failed > 0) {
            System.err.println("ADVERSARIAL CHALLENGE FAILED: Found " + failed + " bug(s) / failure(s)!");
            System.exit(1);
        } else {
            System.out.println("ADVERSARIAL CHALLENGE PASSED: All adversarial stress tests defended!");
        }
    }

    // --------------------------------------------------------------------------
    // Group 1: Corrupt ZIP files stress tests
    // --------------------------------------------------------------------------

    public static void testCorruptZipZeroBytes() throws IOException {
        System.out.println("\n--- [Challenger 1.1] Corrupt Zip: 0-byte file ---");
        Path zeroFile = Files.createTempFile("zero_byte_pack", ".zip");
        try {
            int injected = GeckoLibCacheInjector.injectFromPack(zeroFile);
            assertEquals(0, injected, "0-byte zip file must return 0 injected assets");
            System.out.println("  [PASS] 0-byte zip file handled safely with 0 injected assets.");
        } finally {
            Files.deleteIfExists(zeroFile);
        }
    }

    public static void testCorruptZipTruncatedHeaders() throws IOException {
        System.out.println("\n--- [Challenger 1.2] Corrupt Zip: Truncated headers (1b, 4b, 10b, 22b) ---");
        byte[][] truncatedPayloads = new byte[][] {
                new byte[] { 0x50 }, // 1 byte
                new byte[] { 0x50, 0x4B, 0x03, 0x04 }, // 4 bytes partial PK header
                new byte[] { 0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00, 0x08, 0x00 }, // 10 bytes
                new byte[] { 0x50, 0x4B, 0x05, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 } // 22 bytes partial EOCD
        };

        for (int i = 0; i < truncatedPayloads.length; i++) {
            Path p = Files.createTempFile("truncated_hdr_" + i, ".zip");
            try {
                Files.write(p, truncatedPayloads[i]);
                int injected = GeckoLibCacheInjector.injectFromPack(p);
                assertEquals(0, injected, "Truncated zip payload [" + i + "] must return 0 injected assets");
            } finally {
                Files.deleteIfExists(p);
            }
        }
        System.out.println("  [PASS] All truncated zip header variations handled safely without crashing.");
    }

    public static void testCorruptZipRandomGarbageStreams() throws IOException {
        System.out.println("\n--- [Challenger 1.3] Corrupt Zip: Random garbage data streams ---");
        Random rng = new Random(0xDEADBEEF);
        int[] sizes = { 64, 1024, 65536 };

        for (int size : sizes) {
            Path p = Files.createTempFile("garbage_" + size, ".zip");
            try {
                byte[] garbage = new byte[size];
                rng.nextBytes(garbage);
                Files.write(p, garbage);
                int injected = GeckoLibCacheInjector.injectFromPack(p);
                assertEquals(0, injected, "Garbage data (" + size + " bytes) must return 0 injected assets");
            } finally {
                Files.deleteIfExists(p);
            }
        }
        System.out.println("  [PASS] Random garbage streams up to 64KB handled safely.");
    }

    public static void testCorruptZipNullAndInvalidPaths() {
        System.out.println("\n--- [Challenger 1.4] Corrupt Zip: Null, directory, and non-existent paths ---");
        // Null path
        int resNull = GeckoLibCacheInjector.injectFromPack(null);
        assertEquals(0, resNull, "Null pack path must return 0");

        // Non-existent path
        Path nonExistent = Path.of("non_existent_pack_" + System.currentTimeMillis() + ".zip");
        int resNonExist = GeckoLibCacheInjector.injectFromPack(nonExistent);
        assertEquals(0, resNonExist, "Non-existent path must return 0");

        // Directory path
        Path dirPath = Path.of(System.getProperty("java.io.tmpdir"));
        int resDir = GeckoLibCacheInjector.injectFromPack(dirPath);
        assertEquals(0, resDir, "Directory path must return 0");

        System.out.println("  [PASS] Null, non-existent, and directory paths handled safely.");
    }

    // --------------------------------------------------------------------------
    // Group 2: Path Traversal & Zip Entry Path Attacks
    // --------------------------------------------------------------------------

    public static void testPathTraversalAttacksInZipEntries() throws Exception {
        System.out.println("\n--- [Challenger 2] Path Traversal Attacks in Zip Entries ---");
        GeckoLibCacheInjector.resetForTesting();

        Path traversalZip = Files.createTempFile("traversal_attack", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(traversalZip.toFile()))) {
            // Traversal entry 1: standard ../../
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/../../traversal.geo.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Traversal entry 2: backslash ..\..\
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/..\\..\\win_traversal.geo.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Traversal entry 3: prefix ../
            zos.putNextEntry(new ZipEntry("../assets/customraces/geo/escape.geo.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Traversal entry 4: root escape ../
            zos.putNextEntry(new ZipEntry("assets/../../escape2.geo.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Traversal entry 5: animation traversal
            zos.putNextEntry(new ZipEntry("assets/customraces/animations/../../traversal.animation.json"));
            zos.write(VALID_ANIM_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Traversal entry 6: embedded .. within path
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/subdir/../hidden_escape.geo.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        try {
            int injected = GeckoLibCacheInjector.injectFromPack(traversalZip);
            assertEquals(0, injected, "All traversal entries must be rejected (injected count must be 0)");

            assertFalse(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "traversal.geo.json")),
                    "Traversal model must NOT be baked");
            assertFalse(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/traversal.geo.json")),
                    "Traversal canonical model must NOT be baked");
            assertFalse(GeckoLibCacheInjector.isAnimationBaked(new ResourceLocation("customraces", "animations/traversal.animation.json")),
                    "Traversal animation must NOT be baked");

            System.out.println("  [PASS] All 6 path traversal attack variations successfully rejected.");
        } finally {
            Files.deleteIfExists(traversalZip);
        }
    }

    // --------------------------------------------------------------------------
    // Group 3: Non-Standard Namespaces & Malformed Directory Entries
    // --------------------------------------------------------------------------

    public static void testNonStandardNamespacesInZip() throws Exception {
        System.out.println("\n--- [Challenger 3] Non-Standard Namespaces and Malformed Paths ---");
        GeckoLibCacheInjector.resetForTesting();

        Path zip = Files.createTempFile("namespace_test", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip.toFile()))) {
            // Uppercase namespace -> should be lowercased to 'customraces'
            zos.putNextEntry(new ZipEntry("assets/CUSTOMRACES/geo/upper_were.geo.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Mixed-case namespace -> should be lowercased to 'mymod_were'
            zos.putNextEntry(new ZipEntry("assets/MyMod_Were/geo/mixed_were.geo.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Invalid namespace with spaces -> should be skipped safely without throwing unhandled ResourceLocationException
            zos.putNextEntry(new ZipEntry("assets/invalid namespace with space/geo/bad.geo.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Invalid namespace with colon -> should be skipped safely
            zos.putNextEntry(new ZipEntry("assets/invalid:colon/geo/bad.geo.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Malformed entry: directory entry
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/"));
            zos.closeEntry();

            // Malformed entry: non-json file in geo folder
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/readme.txt"));
            zos.write("Some text".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Malformed entry: non-geo/animation folder
            zos.putNextEntry(new ZipEntry("assets/customraces/other/test.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        try {
            int injected = GeckoLibCacheInjector.injectFromPack(zip);
            assertEquals(2, injected, "Expected exactly 2 valid models injected (upper and mixed case), invalid skipped");

            assertTrue(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/upper_were.geo.json")),
                    "Uppercase namespace must be normalized to lowercase and baked");
            assertTrue(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("mymod_were", "geo/mixed_were.geo.json")),
                    "Mixed case namespace must be normalized to lowercase and baked");

            System.out.println("  [PASS] Namespaces correctly normalized and illegal namespace characters safely skipped.");
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    // --------------------------------------------------------------------------
    // Group 4: Malformed Geo JSON & Model Baking Stress
    // --------------------------------------------------------------------------

    public static void testMalformedGeoJsonSyntaxAndEmpty() {
        System.out.println("\n--- [Challenger 4.1] Malformed Geo JSON: Syntax errors and empty strings ---");
        ResourceLocation loc = new ResourceLocation("customraces", "geo/stress.geo.json");

        String[] badInputs = {
                null,
                "",
                "    ",
                "\t\n\r",
                "{ incomplete json syntax : [",
                "not a json object at all",
                "[1, 2, 3]",
                "12345",
                "\"string primitive\"",
                "true",
                "false",
                "{ \"format_version\": "
        };

        for (int i = 0; i < badInputs.length; i++) {
            Object baked = GeckoLibCacheInjector.bakeModelFromJson(loc, badInputs[i]);
            assertNull(baked, "Input [" + i + "] must return null from bakeModelFromJson");
        }
        System.out.println("  [PASS] All 12 invalid syntax/empty inputs safely returned null without unhandled exception.");
    }

    public static void testMalformedGeoJsonPackResilience() throws Exception {
        System.out.println("\n--- [Challenger 4.2] Malformed Geo JSON: Pack resilience under bad JSON files ---");
        GeckoLibCacheInjector.resetForTesting();

        Path zip = Files.createTempFile("bad_geo_pack", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip.toFile()))) {
            // 1. Valid model
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/valid_model.geo.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 2. Syntax error model
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/bad_syntax.geo.json"));
            zos.write("{ \"geometry\": [ broken syntax".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 3. Empty string model
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/empty_file.geo.json"));
            zos.write("".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 4. Array model
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/array_file.geo.json"));
            zos.write("[1, 2, 3]".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        try {
            int injected = GeckoLibCacheInjector.injectFromPack(zip);
            assertEquals(1, injected, "Pack containing 1 valid model and 3 malformed models must inject exactly 1");

            assertTrue(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/valid_model.geo.json")),
                    "Valid model in pack must be baked");
            assertFalse(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/bad_syntax.geo.json")),
                    "Bad syntax model must not be baked");
            assertFalse(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/empty_file.geo.json")),
                    "Empty model must not be baked");

            System.out.println("  [PASS] Pack scanning survived malformed JSON entries and correctly injected valid assets.");
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    // --------------------------------------------------------------------------
    // Group 5: Malformed Animation JSON & Baking Stress
    // --------------------------------------------------------------------------

    public static void testMalformedAnimationJsonSyntaxAndEmpty() {
        System.out.println("\n--- [Challenger 5.1] Malformed Animation JSON: Syntax errors and null entries ---");
        ResourceLocation loc = new ResourceLocation("customraces", "animations/stress.animation.json");

        String[] badAnimInputs = {
                null,
                "",
                "    ",
                "\t\n",
                "{ incomplete anim syntax : [",
                "not json",
                "[1, 2, 3]",
                "99999",
                "\"primitive string\"",
                "true",
                "{\"format_version\": \"1.8.0\", \"animations\": null}", // null animations block
                "{\"format_version\": \"1.8.0\", \"animations\": [1, 2, 3]}" // array animations block
        };

        for (int i = 0; i < badAnimInputs.length; i++) {
            Object baked = GeckoLibCacheInjector.bakeAnimationsFromJson(loc, badAnimInputs[i]);
            assertNull(baked, "Input [" + i + "] must return null from bakeAnimationsFromJson");
        }
        System.out.println("  [PASS] All 12 invalid animation inputs safely returned null without unhandled exception.");
    }

    public static void testMalformedAnimationJsonPackResilience() throws Exception {
        System.out.println("\n--- [Challenger 5.2] Malformed Animation JSON: Pack resilience under bad files ---");
        GeckoLibCacheInjector.resetForTesting();

        Path zip = Files.createTempFile("bad_anim_pack", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip.toFile()))) {
            // 1. Valid animation
            zos.putNextEntry(new ZipEntry("assets/customraces/animations/valid_anim.animation.json"));
            zos.write(VALID_ANIM_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 2. Syntax error animation
            zos.putNextEntry(new ZipEntry("assets/customraces/animations/bad_syntax.animation.json"));
            zos.write("{ \"animations\": [ broken syntax".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 3. Null animations block
            zos.putNextEntry(new ZipEntry("assets/customraces/animations/null_anim.animation.json"));
            zos.write("{\"format_version\": \"1.8.0\", \"animations\": null}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // 4. Empty file
            zos.putNextEntry(new ZipEntry("assets/customraces/animations/empty.animation.json"));
            zos.write("".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        try {
            int injected = GeckoLibCacheInjector.injectFromPack(zip);
            assertEquals(1, injected, "Pack containing 1 valid animation and 3 malformed animations must inject exactly 1");

            assertTrue(GeckoLibCacheInjector.isAnimationBaked(new ResourceLocation("customraces", "animations/valid_anim.animation.json")),
                    "Valid animation must be baked");
            assertFalse(GeckoLibCacheInjector.isAnimationBaked(new ResourceLocation("customraces", "animations/bad_syntax.animation.json")),
                    "Bad syntax animation must not be baked");
            assertFalse(GeckoLibCacheInjector.isAnimationBaked(new ResourceLocation("customraces", "animations/null_anim.animation.json")),
                    "Null animation must not be baked");

            System.out.println("  [PASS] Animation pack scanning survived bad entries and correctly injected valid assets.");
        } finally {
            Files.deleteIfExists(zip);
        }
    }

    // --------------------------------------------------------------------------
    // Group 6: isModelBaked and isAnimationBaked Null & Edge Cases
    // --------------------------------------------------------------------------

    public static void testIsModelBakedAndIsAnimationBakedNullAndEdgeCases() {
        System.out.println("\n--- [Challenger 6] isModelBaked & isAnimationBaked Nulls, Empty Strings, and Aliases ---");
        GeckoLibCacheInjector.resetForTesting();

        // 1. Null handling
        assertFalse(GeckoLibCacheInjector.isModelBaked(null), "isModelBaked(null) must return false");
        assertFalse(GeckoLibCacheInjector.isAnimationBaked(null), "isAnimationBaked(null) must return false");

        // 2. Empty / edge ResourceLocations
        ResourceLocation emptyLoc = new ResourceLocation("customraces", "");
        assertFalse(GeckoLibCacheInjector.isModelBaked(emptyLoc), "Empty path model must return false");
        assertFalse(GeckoLibCacheInjector.isAnimationBaked(emptyLoc), "Empty path animation must return false");

        // 3. Non-existent locations
        ResourceLocation nonExistentModel = new ResourceLocation("customraces", "geo/non_existent.geo.json");
        ResourceLocation nonExistentAnim = new ResourceLocation("customraces", "animations/non_existent.animation.json");
        ResourceLocation nonExistentNs = new ResourceLocation("unknown_namespace_404", "geo/dummy.geo.json");
        assertFalse(GeckoLibCacheInjector.isModelBaked(nonExistentModel), "Non-existent model must return false");
        assertFalse(GeckoLibCacheInjector.isAnimationBaked(nonExistentAnim), "Non-existent animation must return false");
        assertFalse(GeckoLibCacheInjector.isModelBaked(nonExistentNs), "Non-existent namespace model must return false");

        // 4. Unusual path prefixes (geo/, animations/, extra slashes)
        assertFalse(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/")), "geo/ folder path must return false");
        assertFalse(GeckoLibCacheInjector.isAnimationBaked(new ResourceLocation("customraces", "animations/")), "animations/ folder path must return false");
        assertFalse(GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo///slash.geo.json")), "Triple slash path must return false");

        // 5. Bidirectional alias resolution (Canonical -> Shorthand)
        ResourceLocation canonicalModel = new ResourceLocation("customraces", "geo/beast.geo.json");
        ResourceLocation shortModel = new ResourceLocation("customraces", "beast.geo.json");
        GeckoLibCacheInjector.injectModel(canonicalModel, "dummy_beast_model");
        assertTrue(GeckoLibCacheInjector.isModelBaked(canonicalModel), "Canonical model must be baked");
        assertTrue(GeckoLibCacheInjector.isModelBaked(shortModel), "Shorthand alias must resolve canonical model");

        // 6. Bidirectional alias resolution (Shorthand -> Canonical)
        ResourceLocation shortModel2 = new ResourceLocation("customraces", "wolf.geo.json");
        ResourceLocation canonicalModel2 = new ResourceLocation("customraces", "geo/wolf.geo.json");
        GeckoLibCacheInjector.injectModel(shortModel2, "dummy_wolf_model");
        assertTrue(GeckoLibCacheInjector.isModelBaked(shortModel2), "Shorthand model must be baked");
        assertTrue(GeckoLibCacheInjector.isModelBaked(canonicalModel2), "Canonical alias must resolve shorthand model");

        // 7. Bidirectional animation alias resolution (Canonical -> Shorthand)
        ResourceLocation canonicalAnim = new ResourceLocation("customraces", "animations/howl.animation.json");
        ResourceLocation shortAnim = new ResourceLocation("customraces", "howl.animation.json");
        GeckoLibCacheInjector.injectAnimations(canonicalAnim, "dummy_howl_anim");
        assertTrue(GeckoLibCacheInjector.isAnimationBaked(canonicalAnim), "Canonical animation must be baked");
        assertTrue(GeckoLibCacheInjector.isAnimationBaked(shortAnim), "Shorthand alias must resolve canonical animation");

        // 8. Bidirectional animation alias resolution (Shorthand -> Canonical)
        ResourceLocation shortAnim2 = new ResourceLocation("customraces", "roar.animation.json");
        ResourceLocation canonicalAnim2 = new ResourceLocation("customraces", "animations/roar.animation.json");
        GeckoLibCacheInjector.injectAnimations(shortAnim2, "dummy_roar_anim");
        assertTrue(GeckoLibCacheInjector.isAnimationBaked(shortAnim2), "Shorthand animation must be baked");
        assertTrue(GeckoLibCacheInjector.isAnimationBaked(canonicalAnim2), "Canonical alias must resolve shorthand animation");

        System.out.println("  [PASS] isModelBaked and isAnimationBaked nulls, empty strings, and bidirectional aliases verified.");
    }

    // --------------------------------------------------------------------------
    // Group 7: GeckoLibWereRenderer Edge & Adversarial Contracts
    // --------------------------------------------------------------------------

    public static void testGeckoLibWereRendererEdgeAndAdversarialContracts() throws IOException {
        System.out.println("\n--- [Challenger 7] GeckoLibWereRenderer Edge & Adversarial Contracts ---");
        ResourceLocation loc = new ResourceLocation("customraces", "geo/renderer_test.geo.json");
        ResourceLocation animLoc = new ResourceLocation("customraces", "animations/renderer_test.animation.json");

        // 1. Null parameters
        assertNull(GeckoLibWereRenderer.bakeModelFromFile(null), "bakeModelFromFile(null) must return null");
        assertNull(GeckoLibWereRenderer.bakeModelFromFile(null, null), "bakeModelFromFile(null, null) must return null");
        assertNull(GeckoLibWereRenderer.bakeModelFromFile(loc, null), "bakeModelFromFile with non-existent file must return null");
        assertNull(GeckoLibWereRenderer.bakeModelFromFile(loc, "non/existent/path.geo.json"), "bakeModelFromFile non-existent path must return null");

        // 2. Temp file with malformed JSON on disk
        Path badModelFile = Files.createTempFile("bad_model", ".geo.json");
        try {
            Files.writeString(badModelFile, "{ invalid json on disk : ");
            Object baked = GeckoLibWereRenderer.bakeModelFromFile(loc, badModelFile.toAbsolutePath().toString());
            assertNull(baked, "bakeModelFromFile with malformed disk file must return null");
        } finally {
            Files.deleteIfExists(badModelFile);
        }

        // 3. Temp file with 0 bytes on disk
        Path zeroModelFile = Files.createTempFile("zero_model", ".geo.json");
        try {
            Object baked = GeckoLibWereRenderer.bakeModelFromFile(loc, zeroModelFile.toAbsolutePath().toString());
            assertNull(baked, "bakeModelFromFile with 0-byte file must return null");
        } finally {
            Files.deleteIfExists(zeroModelFile);
        }

        // 4. Animation null parameters
        assertNull(GeckoLibWereRenderer.bakeAnimationsFromFile(null), "bakeAnimationsFromFile(null) must return null");
        assertNull(GeckoLibWereRenderer.bakeAnimationsFromFile(null, null), "bakeAnimationsFromFile(null, null) must return null");
        assertNull(GeckoLibWereRenderer.bakeAnimationsFromFile(animLoc, null), "bakeAnimationsFromFile non-existent must return null");
        assertNull(GeckoLibWereRenderer.bakeAnimationsFromFile(animLoc, "non/existent/path.animation.json"), "bakeAnimationsFromFile non-existent path must return null");

        // 5. Temp file with malformed animation JSON on disk
        Path badAnimFile = Files.createTempFile("bad_anim", ".animation.json");
        try {
            Files.writeString(badAnimFile, "{ invalid animation json : ");
            Object baked = GeckoLibWereRenderer.bakeAnimationsFromFile(animLoc, badAnimFile.toAbsolutePath().toString());
            assertNull(baked, "bakeAnimationsFromFile with malformed disk file must return null");
        } finally {
            Files.deleteIfExists(badAnimFile);
        }

        System.out.println("  [PASS] GeckoLibWereRenderer edge and adversarial reflection contracts executed safely.");
    }

    // --------------------------------------------------------------------------
    // Group 8: High Concurrency Adversarial Hammer
    // --------------------------------------------------------------------------

    public static void testHighConcurrencyAdversarialHammer() throws Exception {
        System.out.println("\n--- [Challenger 8] High Concurrency Adversarial Stress Hammer ---");
        Path validZip = Files.createTempFile("hammer_valid", ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(validZip.toFile()))) {
            zos.putNextEntry(new ZipEntry("assets/customraces/geo/hammer.geo.json"));
            zos.write(VALID_GEO_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
            zos.putNextEntry(new ZipEntry("assets/customraces/animations/hammer.animation.json"));
            zos.write(VALID_ANIM_JSON.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        Path corruptZip = Files.createTempFile("hammer_corrupt", ".zip");
        Files.write(corruptZip, new byte[] { 0x50, 0x4B, 0x01, 0x02, 0x03, 0x04 });

        try {
            int threads = 8;
            int iterationsPerThread = 150;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            AtomicInteger errorCount = new AtomicInteger(0);

            for (int t = 0; t < threads; t++) {
                final int threadId = t;
                pool.submit(() -> {
                    try {
                        for (int i = 0; i < iterationsPerThread; i++) {
                            switch (threadId % 4) {
                                case 0 -> {
                                    GeckoLibCacheInjector.injectFromPack(i % 2 == 0 ? validZip : corruptZip);
                                    GeckoLibCacheInjector.isModelBaked(new ResourceLocation("customraces", "geo/hammer.geo.json"));
                                }
                                case 1 -> {
                                    ResourceLocation loc = new ResourceLocation("customraces", "geo/hammer_" + i + ".geo.json");
                                    GeckoLibCacheInjector.bakeModelFromJson(loc, i % 2 == 0 ? VALID_GEO_JSON : "{ broken json : ");
                                    GeckoLibCacheInjector.isModelBaked(loc);
                                }
                                case 2 -> {
                                    ResourceLocation animLoc = new ResourceLocation("customraces", "animations/hammer_" + i + ".animation.json");
                                    GeckoLibCacheInjector.bakeAnimationsFromJson(animLoc, i % 2 == 0 ? VALID_ANIM_JSON : "[1,2,3]");
                                    GeckoLibCacheInjector.isAnimationBaked(animLoc);
                                }
                                case 3 -> {
                                    GeckoLibCacheInjector.isModelBaked(null);
                                    GeckoLibCacheInjector.isAnimationBaked(null);
                                    GeckoLibCacheInjector.ensureModifiableModelMap();
                                    GeckoLibCacheInjector.ensureModifiableAnimationMap();
                                    if (i % 50 == 0) {
                                        GeckoLibCacheInjector.clearInternalCaches();
                                    }
                                }
                            }
                        }
                    } catch (Throwable t1) {
                        System.err.println("Thread " + threadId + " encountered error: " + t1.getMessage());
                        t1.printStackTrace();
                        errorCount.incrementAndGet();
                    }
                });
            }

            pool.shutdown();
            boolean finished = pool.awaitTermination(20, TimeUnit.SECONDS);
            assertTrue(finished, "High concurrency hammer must finish within 20 seconds");
            assertEquals(0, errorCount.get(), "Zero exceptions expected during high concurrency hammer");

            System.out.println("  [PASS] High concurrency hammer passed (8 threads x 150 iterations = 1200 operations, 0 errors).");
        } finally {
            Files.deleteIfExists(validZip);
            Files.deleteIfExists(corruptZip);
        }
    }

    // --------------------------------------------------------------------------
    // Assertion Helpers
    // --------------------------------------------------------------------------

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("ASSERTION FAILED: " + message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("ASSERTION FAILED: " + message);
        }
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError("ASSERTION FAILED: " + message + " (expected: " + expected + ", got: " + actual + ")");
        }
    }

    private static void assertNull(Object obj, String message) {
        if (obj != null) {
            throw new AssertionError("ASSERTION FAILED: " + message + " (expected null, got: " + obj + ")");
        }
    }
}
