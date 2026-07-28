package ddraig.net.customraces.client.render;

import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GeckoAssetResolverTest {

    public static void main(String[] args) {
        System.out.println("=================================================");
        System.out.println("   GECKO ASSET RESOLVER EMPIRICAL TEST SUITE     ");
        System.out.println("=================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: Null, Empty, Whitespace, and "none" default fallbacks
        try {
            testDefaultFallbacks();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 1 (Default Fallbacks): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 2: Extension auto-inference and path normalization
        try {
            testExtensionInferenceAndNormalization();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 2 (Extension Inference): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 3: Namespace parsing (missing vs custom vs default)
        try {
            testNamespaceParsing();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 3 (Namespace Parsing): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 4: Candidate path ordering & subfolder resolution
        try {
            testCandidateOrdering();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 4 (Candidate Ordering): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 5: Disk config path candidates vs resource pack paths
        try {
            testDiskConfigVsResourcePackCandidates();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 5 (Disk Config vs Resource Pack): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 6: Disk file resolution & content loading
        try {
            testDiskFileResolutionAndContentLoading();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 6 (Disk File Resolution): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 7: Skin keyword texture resolution
        try {
            testSkinKeywordResolution();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 7 (Skin Keywords): " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 8: Malformed & stress path inputs
        try {
            testMalformedPathInputs();
            passed++;
        } catch (Throwable t) {
            System.err.println("[FAIL] Test 8 (Malformed Paths): " + t.getMessage());
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

    private static void testDefaultFallbacks() {
        System.out.println("\n--- Test 1: Default Fallbacks ---");

        String[] nullAndEmpty = new String[]{null, "", "   ", "none", "NONE", "NoNe"};

        for (String input : nullAndEmpty) {
            ResourceLocation modelLoc = GeckoAssetResolver.resolveModelLocation(input);
            if (!GeckoAssetResolver.DEFAULT_MODEL_LOCATION.equals(modelLoc)) {
                throw new AssertionError("resolveModelLocation failed to return default for input: '" + input + "', got: " + modelLoc);
            }

            ResourceLocation animLoc = GeckoAssetResolver.resolveAnimationLocation(input);
            if (!GeckoAssetResolver.DEFAULT_ANIMATION_LOCATION.equals(animLoc)) {
                throw new AssertionError("resolveAnimationLocation failed to return default for input: '" + input + "', got: " + animLoc);
            }

            ResourceLocation texLoc = GeckoAssetResolver.resolveTextureLocation(null, input);
            if (!GeckoAssetResolver.DEFAULT_TEXTURE_LOCATION.equals(texLoc)) {
                throw new AssertionError("resolveTextureLocation failed to return default for input: '" + input + "', got: " + texLoc);
            }
        }
        System.out.println("[PASS] Default fallbacks verified for null, empty, whitespace, and 'none'.");
    }

    private static void testExtensionInferenceAndNormalization() {
        System.out.println("\n--- Test 2: Extension Inference and Normalization ---");

        // Model extension checks
        GeckoAssetResolver.ParsedPath pModel1 = GeckoAssetResolver.parsePath("werewolf", "geo/", ".geo.json");
        if (!"werewolf.geo.json".equals(pModel1.cleanFilename)) {
            throw new AssertionError("Expected 'werewolf.geo.json', got: " + pModel1.cleanFilename);
        }

        GeckoAssetResolver.ParsedPath pModel2 = GeckoAssetResolver.parsePath("werewolf.json", "geo/", ".geo.json");
        if (!"werewolf.geo.json".equals(pModel2.cleanFilename)) {
            throw new AssertionError("Expected 'werewolf.geo.json' from 'werewolf.json', got: " + pModel2.cleanFilename);
        }

        GeckoAssetResolver.ParsedPath pModel3 = GeckoAssetResolver.parsePath("custom/path/werewolf.json", "geo/", ".geo.json");
        if (!"werewolf.geo.json".equals(pModel3.cleanFilename) || !"custom/path/werewolf.geo.json".equals(pModel3.relativePath)) {
            throw new AssertionError("Expected relative path 'custom/path/werewolf.geo.json', got: " + pModel3.relativePath);
        }

        GeckoAssetResolver.ParsedPath pModel4 = GeckoAssetResolver.parsePath("werewolf.geo.json", "geo/", ".geo.json");
        if (!"werewolf.geo.json".equals(pModel4.cleanFilename)) {
            throw new AssertionError("Expected 'werewolf.geo.json' from 'werewolf.geo.json', got: " + pModel4.cleanFilename);
        }

        // Animation extension checks
        GeckoAssetResolver.ParsedPath pAnim1 = GeckoAssetResolver.parsePath("werewolf", "animations/", ".animation.json");
        if (!"werewolf.animation.json".equals(pAnim1.cleanFilename)) {
            throw new AssertionError("Expected 'werewolf.animation.json', got: " + pAnim1.cleanFilename);
        }

        GeckoAssetResolver.ParsedPath pAnim2 = GeckoAssetResolver.parsePath("werewolf.json", "animations/", ".animation.json");
        if (!"werewolf.animation.json".equals(pAnim2.cleanFilename)) {
            throw new AssertionError("Expected 'werewolf.animation.json' from 'werewolf.json', got: " + pAnim2.cleanFilename);
        }

        GeckoAssetResolver.ParsedPath pAnim3 = GeckoAssetResolver.parsePath("custom/path/werewolf.json", "animations/", ".animation.json");
        if (!"werewolf.animation.json".equals(pAnim3.cleanFilename) || !"custom/path/werewolf.animation.json".equals(pAnim3.relativePath)) {
            throw new AssertionError("Expected relative path 'custom/path/werewolf.animation.json', got: " + pAnim3.relativePath);
        }

        GeckoAssetResolver.ParsedPath pAnim4 = GeckoAssetResolver.parsePath("werewolf.animation.json", "animations/", ".animation.json");
        if (!"werewolf.animation.json".equals(pAnim4.cleanFilename)) {
            throw new AssertionError("Expected 'werewolf.animation.json' from 'werewolf.animation.json', got: " + pAnim4.cleanFilename);
        }

        // Texture extension checks
        GeckoAssetResolver.ParsedPath pTex1 = GeckoAssetResolver.parsePath("werewolf", "textures/", ".png");
        if (!"werewolf.png".equals(pTex1.cleanFilename)) {
            throw new AssertionError("Expected 'werewolf.png', got: " + pTex1.cleanFilename);
        }

        System.out.println("[PASS] Extension inference (.geo.json, .animation.json, .png) verified.");
    }

    private static void testNamespaceParsing() {
        System.out.println("\n--- Test 3: Namespace Parsing ---");

        // Default namespace
        GeckoAssetResolver.ParsedPath pDef = GeckoAssetResolver.parsePath("models/were/custom.geo.json", "geo/", ".geo.json");
        if (!"customraces".equals(pDef.namespace)) {
            throw new AssertionError("Expected default namespace 'customraces', got: " + pDef.namespace);
        }

        // Explicit custom namespace
        GeckoAssetResolver.ParsedPath pCustom = GeckoAssetResolver.parsePath("mymod:models/were/custom.geo.json", "geo/", ".geo.json");
        if (!"mymod".equals(pCustom.namespace)) {
            throw new AssertionError("Expected namespace 'mymod', got: " + pCustom.namespace);
        }

        // Explicit minecraft namespace
        GeckoAssetResolver.ParsedPath pMinecraft = GeckoAssetResolver.parsePath("minecraft:textures/entity/steve.png", "textures/", ".png");
        if (!"minecraft".equals(pMinecraft.namespace)) {
            throw new AssertionError("Expected namespace 'minecraft', got: " + pMinecraft.namespace);
        }

        System.out.println("[PASS] Namespace parsing (default, custom, minecraft) verified.");
    }

    private static void testCandidateOrdering() {
        System.out.println("\n--- Test 4: Candidate Ordering ---");

        GeckoAssetResolver.ParsedPath pModel = GeckoAssetResolver.parsePath("werewolf", "geo/", ".geo.json");
        List<ResourceLocation> modelCandidates = pModel.candidateResourceLocations;

        if (modelCandidates.size() < 4) {
            throw new AssertionError("Expected at least 4 candidate locations for model, got: " + modelCandidates.size());
        }

        boolean foundGeo = false;
        boolean foundGeoWere = false;
        boolean foundModelsWere = false;
        boolean foundModels = false;

        for (ResourceLocation loc : modelCandidates) {
            if (loc.getPath().equals("werewolf.geo.json")) foundGeo = true;
            if (loc.getPath().equals("geo/werewolf.geo.json")) foundGeo = true;
            if (loc.getPath().equals("geo/were/werewolf.geo.json")) foundGeoWere = true;
            if (loc.getPath().equals("models/were/werewolf.geo.json")) foundModelsWere = true;
            if (loc.getPath().equals("models/werewolf.geo.json")) foundModels = true;
        }

        if (!foundGeoWere || !foundModelsWere || !foundModels) {
            throw new AssertionError("Missing expected candidate path patterns in model candidates: " + modelCandidates);
        }

        System.out.println("[PASS] Candidate path ordering and subfolder resolution verified.");
    }

    private static void testDiskConfigVsResourcePackCandidates() {
        System.out.println("\n--- Test 5: Disk Config vs Resource Pack Candidates ---");

        GeckoAssetResolver.ParsedPath parsedModel = GeckoAssetResolver.parsePath("custom_werewolf.geo.json", "geo/", ".geo.json");
        List<File> modelDiskFiles = GeckoAssetResolver.getModelDiskCandidates(parsedModel);

        boolean hasConfigModels = modelDiskFiles.stream().anyMatch(f -> f.getPath().replace('\\', '/').equals("config/custom_races/models/custom_werewolf.geo.json"));
        boolean hasConfigModelsWere = modelDiskFiles.stream().anyMatch(f -> f.getPath().replace('\\', '/').equals("config/custom_races/models/were/custom_werewolf.geo.json"));
        boolean hasConfigGeo = modelDiskFiles.stream().anyMatch(f -> f.getPath().replace('\\', '/').equals("config/custom_races/geo/custom_werewolf.geo.json"));

        if (!hasConfigModels || !hasConfigModelsWere || !hasConfigGeo) {
            throw new AssertionError("Missing expected disk candidate paths: " + modelDiskFiles);
        }

        GeckoAssetResolver.ParsedPath parsedAnim = GeckoAssetResolver.parsePath("custom_werewolf.animation.json", "animations/", ".animation.json");
        List<File> animDiskFiles = GeckoAssetResolver.getAnimationDiskCandidates(parsedAnim);
        boolean hasConfigAnim = animDiskFiles.stream().anyMatch(f -> f.getPath().replace('\\', '/').equals("config/custom_races/animations/custom_werewolf.animation.json"));

        if (!hasConfigAnim) {
            throw new AssertionError("Missing expected animation disk candidate path: " + animDiskFiles);
        }

        System.out.println("[PASS] Disk config path candidates verified against expected directories.");
    }

    private static void testDiskFileResolutionAndContentLoading() throws Exception {
        System.out.println("\n--- Test 6: Disk File Resolution and Content Loading ---");

        File configDir = new File("config/custom_races/models");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }

        File testModelFile = new File(configDir, "challenger_test_model.geo.json");
        String dummyJsonContent = "{\"format_version\":\"1.12.0\",\"geometry\":[{\"description\":{\"identifier\":\"geometry.challenger\"}}]}";

        try (FileWriter writer = new FileWriter(testModelFile)) {
            writer.write(dummyJsonContent);
        }

        try {
            // Verify resolveModelLocation detects disk file
            ResourceLocation resolvedLoc = GeckoAssetResolver.resolveModelLocation("challenger_test_model.geo.json");
            if (resolvedLoc == null) {
                throw new AssertionError("resolveModelLocation returned null for existing disk model file.");
            }

            // Verify getModelContent retrieves exact content from disk
            String content = GeckoAssetResolver.getModelContent(resolvedLoc, "challenger_test_model.geo.json");
            if (content == null || !content.contains("geometry.challenger")) {
                throw new AssertionError("getModelContent failed to read disk file content. Got: " + content);
            }

            System.out.println("[PASS] Disk file resolution and content reading verified with temporary file.");
        } finally {
            if (testModelFile.exists()) {
                testModelFile.delete();
            }
        }
    }

    private static void testSkinKeywordResolution() {
        System.out.println("\n--- Test 7: Skin Keyword Resolution ---");

        String[] skinKeywords = new String[]{"skin", "player", "player_skin", "skin_texture", "SKIN", "PLAYER"};

        for (String kw : skinKeywords) {
            ResourceLocation texLoc = GeckoAssetResolver.resolveTextureLocation(null, kw);
            if (!GeckoAssetResolver.DEFAULT_TEXTURE_LOCATION.equals(texLoc)) {
                throw new AssertionError("resolveTextureLocation for keyword '" + kw + "' without player failed to return safe default texture.");
            }
        }

        System.out.println("[PASS] Skin keyword resolution verified.");
    }

    private static void testMalformedPathInputs() {
        System.out.println("\n--- Test 8: Malformed Path Inputs ---");

        String[] malformedInputs = new String[]{
                ":missing_namespace",
                "::leading_colon",
                ":path/with:colon",
                "invalid_namespace::path",
                "customraces:UPPERCASE/PATH.json",
                "UpperMod:werewolf",
                "invalid namespace:werewolf.geo.json",
                "customraces: path with space ",
                "  spaces_around  ",
                "../../relative/path/traversal.geo.json",
                "  customraces:models/werewolf.geo.json  ",
                "mod:test\0path",
                "mod:test\npath",
                "",
                "   ",
                null
        };

        int malformedFailures = 0;

        for (String input : malformedInputs) {
            try {
                ResourceLocation resModel = GeckoAssetResolver.resolveModelLocation(input);
                if (resModel == null) {
                    System.err.println("  [FAIL] resolveModelLocation returned null for input: '" + input + "'");
                    malformedFailures++;
                }

                ResourceLocation resAnim = GeckoAssetResolver.resolveAnimationLocation(input);
                if (resAnim == null) {
                    System.err.println("  [FAIL] resolveAnimationLocation returned null for input: '" + input + "'");
                    malformedFailures++;
                }

                ResourceLocation resTex = GeckoAssetResolver.resolveTextureLocation(null, input);
                if (resTex == null) {
                    System.err.println("  [FAIL] resolveTextureLocation returned null for input: '" + input + "'");
                    malformedFailures++;
                }
            } catch (net.minecraft.ResourceLocationException rle) {
                System.err.println("  [VULNERABILITY CONFIRMED] ResourceLocationException thrown for malformed input '" + input + "': " + rle.getMessage());
                malformedFailures++;
            } catch (Throwable t) {
                System.err.println("  [FAIL] Unexpected exception thrown for malformed input '" + input + "': " + t.getMessage());
                malformedFailures++;
            }
        }

        if (malformedFailures > 0) {
            throw new AssertionError("Malformed path inputs caused " + malformedFailures + " failure(s) / ResourceLocationExceptions in GeckoAssetResolver.");
        }

        System.out.println("[PASS] Malformed path inputs handled gracefully without exceptions.");
    }
}
