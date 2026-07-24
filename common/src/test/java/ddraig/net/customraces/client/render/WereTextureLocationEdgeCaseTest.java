package ddraig.net.customraces.client.render;

import ddraig.net.customraces.data.RaceData;
import net.minecraft.resources.ResourceLocation;

/**
 * Adversarial empirical unit test suite for WereModelRenderer.getValidWereTextureLocation.
 * Validates edge case handling, fallback logic, keyword interception, path normalization,
 * and exception safety across 8 required edge cases plus extended adversarial scenarios.
 */
public class WereTextureLocationEdgeCaseTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("  M2 WERE TEXTURE LOCATION EDGE CASE TEST SUITE  ");
        System.out.println("=================================================");

        int passed = 0;
        int failed = 0;

        // 1. Required Task Test Cases
        try {
            testRequiredEdgeCaseInputs();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Required Edge Case Inputs Test: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // 2. Keyword Interception Variations
        try {
            testKeywordVariations();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Keyword Variations Test: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // 3. Path Normalization & Shorthand Syntax
        try {
            testPathNormalization();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Path Normalization Test: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // 4. Null Race and Null Player Handles
        try {
            testNullRaceAndPlayerHandles();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Null Race & Player Test: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // 5. Invalid Syntax & Special Chars Stress
        try {
            testInvalidSyntaxAndSpecialChars();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Invalid Syntax & Special Chars Test: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("=================================================");
        System.out.println("  SUMMARY: " + passed + " PASSED, " + failed + " FAILED  ");
        System.out.println("=================================================");

        if (failed > 0) {
            System.exit(1);
        }
    }

    /**
     * Empirical test for 8 required inputs specified in Task 1:
     * "SKIN", "  player  ", "", null, "none", "textures/were/custom.png", "invalid:path/with#bad@chars", "non_existent_file.png"
     */
    public static void testRequiredEdgeCaseInputs() {
        System.out.println("\n--- Testing 8 Required Edge Case Inputs ---");

        RaceData race = new RaceData();

        // 1. "SKIN"
        race.wereTexturePath = "SKIN";
        ResourceLocation locSkin = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertNotNull("SKIN", locSkin);
        assertEquals("SKIN fallback", WereModelRenderer.DEFAULT_WERE_TEXTURE, locSkin);
        System.out.println("  [PASS] 'SKIN' -> " + locSkin);

        // 2. "  player  "
        race.wereTexturePath = "  player  ";
        ResourceLocation locPlayer = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertNotNull("  player  ", locPlayer);
        assertEquals("  player   fallback", WereModelRenderer.DEFAULT_WERE_TEXTURE, locPlayer);
        System.out.println("  [PASS] '  player  ' -> " + locPlayer);

        // 3. ""
        race.wereTexturePath = "";
        ResourceLocation locEmpty = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertNotNull("Empty string", locEmpty);
        assertEquals("Empty string fallback", WereModelRenderer.DEFAULT_WERE_TEXTURE, locEmpty);
        System.out.println("  [PASS] '' -> " + locEmpty);

        // 4. null
        race.wereTexturePath = null;
        ResourceLocation locNull = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertNotNull("Null path", locNull);
        assertEquals("Null path fallback", WereModelRenderer.DEFAULT_WERE_TEXTURE, locNull);
        System.out.println("  [PASS] null -> " + locNull);

        // 5. "none"
        race.wereTexturePath = "none";
        ResourceLocation locNone = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertNotNull("none", locNone);
        assertEquals("none fallback", WereModelRenderer.DEFAULT_WERE_TEXTURE, locNone);
        System.out.println("  [PASS] 'none' -> " + locNone);

        // 6. "textures/were/custom.png"
        race.wereTexturePath = "textures/were/custom.png";
        ResourceLocation locCustom = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertNotNull("textures/were/custom.png", locCustom);
        assertEquals("textures/were/custom.png resolution", "customraces:textures/were/custom.png", locCustom.toString());
        System.out.println("  [PASS] 'textures/were/custom.png' -> " + locCustom);

        // 7. "invalid:path/with#bad@chars"
        race.wereTexturePath = "invalid:path/with#bad@chars";
        ResourceLocation locInvalid = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertNotNull("invalid:path/with#bad@chars", locInvalid);
        assertEquals("invalid syntax fallback", WereModelRenderer.DEFAULT_WERE_TEXTURE, locInvalid);
        System.out.println("  [PASS] 'invalid:path/with#bad@chars' -> " + locInvalid);

        // 8. "non_existent_file.png"
        race.wereTexturePath = "non_existent_file.png";
        ResourceLocation locNonExistent = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertNotNull("non_existent_file.png", locNonExistent);
        // Normalized path should be valid syntax: customraces:textures/non_existent_file.png
        assertEquals("non_existent_file.png normalized path", "customraces:textures/non_existent_file.png", locNonExistent.toString());
        System.out.println("  [PASS] 'non_existent_file.png' -> " + locNonExistent);
    }

    /**
     * Test case-insensitive keyword interception variations: "PLAYER_SKIN", "skin_texture", "  PLAYER  ", "NONE".
     */
    public static void testKeywordVariations() {
        System.out.println("\n--- Testing Keyword Interception Variations ---");

        RaceData race = new RaceData();

        String[] keywords = new String[]{
                "PLAYER_SKIN",
                "  skin_texture  ",
                "  PLAYER  ",
                "SKIN",
                "NONE",
                "  none  "
        };

        for (String kw : keywords) {
            race.wereTexturePath = kw;
            ResourceLocation loc = WereModelRenderer.getValidWereTextureLocation(null, race);
            assertNotNull("Keyword '" + kw + "'", loc);
            assertEquals("Keyword fallback for '" + kw + "'", WereModelRenderer.DEFAULT_WERE_TEXTURE, loc);
        }

        System.out.println("  [PASS] All keyword variations handled cleanly with default texture fallback.");
    }

    /**
     * Test path normalization for shorthand paths missing namespace, missing `textures/` prefix, or missing `.png` suffix.
     */
    public static void testPathNormalization() {
        System.out.println("\n--- Testing Path Normalization ---");

        RaceData race = new RaceData();

        // No namespace, no textures/ prefix, no .png suffix
        race.wereTexturePath = "were/direwolf";
        ResourceLocation loc1 = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertEquals("Normalization 'were/direwolf'", "customraces:textures/were/direwolf.png", loc1.toString());

        // Namespace included, but missing textures/ prefix and .png suffix
        race.wereTexturePath = "customraces:were/direwolf";
        ResourceLocation loc2 = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertEquals("Normalization 'customraces:were/direwolf'", "customraces:textures/were/direwolf.png", loc2.toString());

        // Already has textures/ and .png
        race.wereTexturePath = "my_namespace:textures/were/custom_skin.png";
        ResourceLocation loc3 = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertEquals("Full path 'my_namespace:textures/were/custom_skin.png'", "my_namespace:textures/were/custom_skin.png", loc3.toString());

        System.out.println("  [PASS] Path normalization correctly formatted namespaces, prefixes, and suffixes.");
    }

    /**
     * Test null race object and null player parameter safety.
     */
    public static void testNullRaceAndPlayerHandles() {
        System.out.println("\n--- Testing Null Race and Player Handles ---");

        ResourceLocation loc1 = WereModelRenderer.getValidWereTextureLocation(null, (RaceData) null);
        assertNotNull("Null race object", loc1);
        assertEquals("Null race fallback", WereModelRenderer.DEFAULT_WERE_TEXTURE, loc1);

        RaceData race = new RaceData();
        race.wereTexturePath = "customraces:textures/were/test.png";
        ResourceLocation loc2 = WereModelRenderer.getValidWereTextureLocation(null, race);
        assertNotNull("Null player object", loc2);

        System.out.println("  [PASS] Null race data and null player handles executed without exception.");
    }

    /**
     * Test illegal character handling, multi-colon strings, control chars, spaces.
     */
    public static void testInvalidSyntaxAndSpecialChars() {
        System.out.println("\n--- Testing Invalid Syntax and Special Characters ---");

        RaceData race = new RaceData();

        String[] badInputs = new String[]{
                "invalid::doublecolon",
                "namespace:path with spaces/file.png",
                "namespace:path/with/control\nchar",
                "namespace:path/with/unicode/\u0000bad",
                "!!!invalid_namespace!!!:textures/were/wolf.png",
                "customraces:textures/were/wolf?.png"
        };

        for (String badInput : badInputs) {
            race.wereTexturePath = badInput;
            ResourceLocation loc = WereModelRenderer.getValidWereTextureLocation(null, race);
            assertNotNull("Bad input '" + badInput + "'", loc);
            // ResourceLocation.tryParse fails for invalid chars, falling back safely to DEFAULT_WERE_TEXTURE
            assertEquals("Fallback for bad input '" + badInput + "'", WereModelRenderer.DEFAULT_WERE_TEXTURE, loc);
        }

        System.out.println("  [PASS] Malformed resource location strings caught by ResourceLocation.tryParse with safe fallback.");
    }

    private static void assertNotNull(String msg, Object obj) {
        if (obj == null) {
            throw new AssertionError("Expected non-null for: " + msg);
        }
    }

    private static void assertEquals(String msg, Object expected, Object actual) {
        if (expected == null && actual == null) return;
        if (expected != null && expected.equals(actual)) return;
        throw new AssertionError("Assertion failed for " + msg + ": expected [" + expected + "] but got [" + actual + "]");
    }
}
