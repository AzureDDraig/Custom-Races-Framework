package ddraig.net.customraces.pack;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ServerPackManagerTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   SERVER PACK MANAGER & HTTP SERVER VERIFICATION TEST SUITE      ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: Pack zip generation with sample asset directories and mirroring
        try {
            System.out.println("\n--- Running Test 1: Pack Generation & Asset Mirroring ---");
            testPackGenerationWithAssets();
            System.out.println("  [PASS] Pack Generation & Asset Mirroring verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 2: Validate pack.mcmeta structure and pack_format == 15
        try {
            System.out.println("\n--- Running Test 2: pack.mcmeta Structure & Format 15 ---");
            testPackMcmetaStructure();
            System.out.println("  [PASS] pack.mcmeta Structure & Format 15 verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 3: Validate SHA-1 format (40-char lowercase hex) and consistency
        try {
            System.out.println("\n--- Running Test 3: SHA-1 Checksum Validation ---");
            testSha1ChecksumValidation();
            System.out.println("  [PASS] SHA-1 Checksum Validation verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 3 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 4: Test empty directories handling
        try {
            System.out.println("\n--- Running Test 4: Empty Directories Handling ---");
            testEmptyDirectoriesHandling();
            System.out.println("  [PASS] Empty Directories Handling verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 4 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 5: Special characters and nested path handling
        try {
            System.out.println("\n--- Running Test 5: Special Characters & Nested Paths ---");
            testSpecialCharactersAndNestedPaths();
            System.out.println("  [PASS] Special Characters & Nested Paths verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 5 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 6: ServerPackHttpServer Lifecycle, HTTP 200 Download Response, & Clean Stop
        try {
            System.out.println("\n--- Running Test 6: HTTP Server Lifecycle & Download ---");
            testHttpServerLifecycleAndDownload();
            System.out.println("  [PASS] HTTP Server Lifecycle & Download verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 6 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 7: Port bind conflict graceful handling
        try {
            System.out.println("\n--- Running Test 7: Port Bind Conflict Resilience ---");
            testPortBindConflictGracefulHandling();
            System.out.println("  [PASS] Port Bind Conflict Resilience verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 7 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================================");
        System.out.println("  SERVER PACK MANAGER TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================================");

        if (failed > 0) {
            throw new RuntimeException("ServerPackManagerTest failed with " + failed + " failures!");
        }
    }

    private static void testPackGenerationWithAssets() throws Exception {
        Path tempDir = Files.createTempDirectory("customraces_pack_test");
        Path modelsDir = Files.createDirectories(tempDir.resolve("models/were"));
        Path texturesDir = Files.createDirectories(tempDir.resolve("textures/were"));
        Path animsDir = Files.createDirectories(tempDir.resolve("animations/were"));

        Path modelFile = modelsDir.resolve("werewolf.geo.json");
        Files.writeString(modelFile, "{\"geometry\": \"werewolf_model\"}");

        Path rootModelFile = tempDir.resolve("models").resolve("horns.geo.json");
        Files.writeString(rootModelFile, "{\"geometry\": \"horns_model\"}");

        byte[] fakePngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3, 4};
        Path textureFile = texturesDir.resolve("werewolf.png");
        Files.write(textureFile, fakePngBytes);

        Path animFile = animsDir.resolve("werewolf.animation.json");
        Files.writeString(animFile, "{\"animations\": {\"animation.werewolf.walk\": {}}}");

        Path targetZip = tempDir.resolve("customraces-server-pack.zip");

        ServerPackManager.buildPack(tempDir, targetZip);

        assertTrue(Files.exists(targetZip), "Target zip must be created");
        assertTrue(Files.size(targetZip) > 0, "Target zip must not be empty");

        // Inspect zip content
        Map<String, Long> entries = new HashMap<>();
        try (ZipFile zipFile = new ZipFile(targetZip.toFile())) {
            Enumeration<? extends ZipEntry> it = zipFile.entries();
            while (it.hasMoreElements()) {
                ZipEntry entry = it.nextElement();
                entries.put(entry.getName(), entry.getSize());
            }

            assertTrue(entries.containsKey("pack.mcmeta"), "pack.mcmeta must exist in zip");
            // Check mirroring: both geo/ and models/
            assertTrue(entries.containsKey("assets/customraces/geo/were/werewolf.geo.json"), "geo/ were model must exist");
            assertTrue(entries.containsKey("assets/customraces/models/were/werewolf.geo.json"), "models/ were model must exist");
            assertTrue(entries.containsKey("assets/customraces/geo/horns.geo.json"), "geo/ root model must exist");
            assertTrue(entries.containsKey("assets/customraces/models/horns.geo.json"), "models/ root model must exist");

            // Check textures
            assertTrue(entries.containsKey("assets/customraces/textures/were/werewolf.png"), "textures/ were texture must exist");

            // Check animations
            assertTrue(entries.containsKey("assets/customraces/animations/were/werewolf.animation.json"), "animations/ were animation must exist");

            // Verify content integrity
            ZipEntry animEntry = zipFile.getEntry("assets/customraces/animations/were/werewolf.animation.json");
            try (InputStream is = zipFile.getInputStream(animEntry)) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                assertTrue(content.contains("animation.werewolf.walk"), "Animation content must be intact");
            }
        }
    }

    private static void testPackMcmetaStructure() throws Exception {
        Path tempDir = Files.createTempDirectory("customraces_mcmeta_test");
        Path targetZip = tempDir.resolve("mcmeta-test.zip");

        ServerPackManager.buildPack(tempDir, targetZip);

        try (ZipFile zipFile = new ZipFile(targetZip.toFile())) {
            ZipEntry mcmetaEntry = zipFile.getEntry("pack.mcmeta");
            assertNotNull(mcmetaEntry, "pack.mcmeta must be present");

            try (InputStream is = zipFile.getInputStream(mcmetaEntry)) {
                String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(content).getAsJsonObject();
                assertTrue(json.has("pack"), "mcmeta must have 'pack' root object");
                JsonObject packObj = json.getAsJsonObject("pack");
                assertTrue(packObj.has("pack_format"), "mcmeta must specify 'pack_format'");
                assertEquals(15, packObj.get("pack_format").getAsInt(), "pack_format must be 15 for MC 1.20.1");
                assertTrue(packObj.has("description"), "mcmeta must specify 'description'");
                assertTrue(!packObj.get("description").getAsString().isEmpty(), "description must not be empty");
            }
        }
    }

    private static void testSha1ChecksumValidation() throws Exception {
        Path tempDir = Files.createTempDirectory("customraces_sha1_test");
        Path targetZip = tempDir.resolve("sha1-test.zip");

        ServerPackManager.buildPack(tempDir, targetZip);

        String packSha1 = ServerPackManager.getPackSha1();
        assertNotNull(packSha1, "SHA-1 must not be null");
        assertEquals(40, packSha1.length(), "SHA-1 must be exactly 40 characters");
        assertTrue(packSha1.matches("^[0-9a-f]{40}$"), "SHA-1 must be lowercase hexadecimal: " + packSha1);

        // Independently calculate SHA-1
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] zipBytes = Files.readAllBytes(targetZip);
        byte[] hash = digest.digest(zipBytes);
        StringBuilder sb = new StringBuilder(40);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        String independentSha1 = sb.toString();

        assertEquals(independentSha1, packSha1, "ServerPackManager SHA-1 must match independent calculation");
    }

    private static void testEmptyDirectoriesHandling() throws Exception {
        Path tempDir = Files.createTempDirectory("customraces_empty_test");
        Files.createDirectories(tempDir.resolve("models"));
        Files.createDirectories(tempDir.resolve("textures"));
        Files.createDirectories(tempDir.resolve("animations"));

        Path targetZip = tempDir.resolve("empty-pack.zip");

        ServerPackManager.buildPack(tempDir, targetZip);

        assertTrue(Files.exists(targetZip), "Empty pack must still generate valid zip file");
        assertTrue(ServerPackManager.hasPack(), "hasPack() must return true after pack creation");
        assertTrue(ServerPackManager.getPackSizeBytes() > 0, "Pack size in bytes must be > 0");
        assertEquals(40, ServerPackManager.getPackSha1().length(), "SHA-1 must be 40 chars");

        try (ZipFile zipFile = new ZipFile(targetZip.toFile())) {
            assertNotNull(zipFile.getEntry("pack.mcmeta"), "pack.mcmeta must exist in empty pack");
        }
    }

    private static void testSpecialCharactersAndNestedPaths() throws Exception {
        Path tempDir = Files.createTempDirectory("customraces_special_chars");
        Path modelsDir = Files.createDirectories(tempDir.resolve("models/sub dir-1/v2_beta"));
        Path texturesDir = Files.createDirectories(tempDir.resolve("textures/test (1)"));

        Path modelFile = modelsDir.resolve("cool model (special).geo.json");
        Files.writeString(modelFile, "{\"name\": \"special_chars_test\"}");

        Path textureFile = texturesDir.resolve("texture-sharp#1.png");
        Files.write(textureFile, new byte[]{1, 2, 3});

        Path targetZip = tempDir.resolve("special-chars-pack.zip");
        ServerPackManager.buildPack(tempDir, targetZip);

        try (ZipFile zipFile = new ZipFile(targetZip.toFile())) {
            Enumeration<? extends ZipEntry> it = zipFile.entries();
            boolean foundModel = false;
            boolean foundTexture = false;

            while (it.hasMoreElements()) {
                ZipEntry entry = it.nextElement();
                String name = entry.getName();
                // Ensure forward slash separators
                assertTrue(!name.contains("\\"), "Zip entry must use forward slashes only: " + name);
                if (name.contains("cool model (special).geo.json")) {
                    foundModel = true;
                }
                if (name.contains("texture-sharp#1.png")) {
                    foundTexture = true;
                }
            }

            assertTrue(foundModel, "Model with special characters and nested path must be found");
            assertTrue(foundTexture, "Texture with special characters must be found");
        }
    }

    private static void testHttpServerLifecycleAndDownload() throws Exception {
        // Ensure a pack is built first
        Path tempDir = Files.createTempDirectory("customraces_http_test");
        Path targetZip = tempDir.resolve("http-test-pack.zip");
        Files.createDirectories(tempDir.resolve("models"));
        Files.writeString(tempDir.resolve("models/test.geo.json"), "{\"test\": true}");
        ServerPackManager.buildPack(tempDir, targetZip);

        // Start HTTP server on ephemeral port (0)
        boolean started = ServerPackHttpServer.start(0);
        assertTrue(started, "HTTP server must start successfully on ephemeral port");
        assertTrue(ServerPackHttpServer.isRunning(), "isRunning() must return true");

        int port = ServerPackHttpServer.getPort();
        assertTrue(port > 0, "Bound port must be > 0");

        String downloadUrl = ServerPackHttpServer.getDownloadUrl("127.0.0.1");
        assertTrue(downloadUrl.startsWith("http://127.0.0.1:" + port + "/customraces-server-pack.zip"),
                "Download URL format incorrect: " + downloadUrl);

        // Test GET request
        URL url = new URL(downloadUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int responseCode = conn.getResponseCode();
        assertEquals(200, responseCode, "HTTP response code must be 200");
        assertEquals("application/zip", conn.getContentType(), "Content-Type must be application/zip");

        String sha1Header = conn.getHeaderField("X-Checksum-SHA1");
        assertNotNull(sha1Header, "X-Checksum-SHA1 header must be present");
        assertEquals(ServerPackManager.getPackSha1(), sha1Header, "Header SHA-1 must match pack SHA-1");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (InputStream is = conn.getInputStream()) {
            byte[] buffer = new byte[4096];
            int r;
            while ((r = is.read(buffer)) != -1) {
                baos.write(buffer, 0, r);
            }
        }
        conn.disconnect();

        byte[] downloaded = baos.toByteArray();
        assertEquals(Files.size(targetZip), downloaded.length, "Downloaded byte size must match file on disk");

        // Verify SHA-1 of downloaded payload
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(downloaded);
        StringBuilder sb = new StringBuilder(40);
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        assertEquals(ServerPackManager.getPackSha1(), sb.toString(), "Downloaded payload SHA-1 must match pack SHA-1");

        // Test HEAD request
        HttpURLConnection headConn = (HttpURLConnection) url.openConnection();
        headConn.setRequestMethod("HEAD");
        int headCode = headConn.getResponseCode();
        assertEquals(200, headCode, "HEAD response code must be 200");
        assertEquals("application/zip", headConn.getContentType(), "HEAD Content-Type must be application/zip");
        headConn.disconnect();

        // Clean stop
        ServerPackHttpServer.stop();
        assertTrue(!ServerPackHttpServer.isRunning(), "isRunning() must return false after stop()");
    }

    private static void testPortBindConflictGracefulHandling() throws Exception {
        // Open a local socket to occupy a port
        try (ServerSocket socket = new ServerSocket(0)) {
            int occupiedPort = socket.getLocalPort();
            assertTrue(occupiedPort > 0, "Occupied port must be > 0");

            // Attempting to bind to the occupied port should return false gracefully without throwing
            boolean result = ServerPackHttpServer.start(occupiedPort);
            assertTrue(!result, "Binding to already-occupied port should fail gracefully and return false");
            assertTrue(!ServerPackHttpServer.isRunning(), "Server should not be running after failed bind");
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

    private static void assertEquals(long expected, long actual, String message) {
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
