package ddraig.net.customraces.pack;

import ddraig.net.customraces.client.render.GeckoLibCacheInjector;
import ddraig.net.customraces.network.ServerPackInfoPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Comprehensive unit and regression test suite for Milestone M2:
 * - ServerPackInfoPacket serialization and deserialization
 * - Client-side SHA-1 cache detection and validation
 * - Corrupted cache file cleanup and recovery
 * - Singleplayer / LAN direct filesystem mount bypass
 * - Asynchronous HTTP background download and atomic promotion
 * - Checksum mismatch and corrupted download rejection
 * - Network failure / HTTP error handling
 * - Already mounted deduplication guard
 */
public class ClientPackManagerTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("       CLIENT PACK MANAGER & HANDSHAKE VERIFICATION SUITE         ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: ServerPackInfoPacket serialization and deserialization
        try {
            System.out.println("\n--- Running Test 1: Packet Serialization & Deserialization ---");
            testPacketSerializationAndDeserialization();
            System.out.println("  [PASS] Packet Serialization & Deserialization verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 2: Local cache detection and validation
        try {
            System.out.println("\n--- Running Test 2: Local Cache Detection & Validation ---");
            testCacheDetectionAndValidation();
            System.out.println("  [PASS] Local Cache Detection & Validation verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 3: Corrupted cache file detection and cleanup
        try {
            System.out.println("\n--- Running Test 3: Corrupted Cache Detection & Cleanup ---");
            testCorruptedCacheDetectionAndCleanup();
            System.out.println("  [PASS] Corrupted Cache Detection & Cleanup verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 3 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 4: Singleplayer / LAN direct mount bypass
        try {
            System.out.println("\n--- Running Test 4: Singleplayer / LAN Direct Mount Bypass ---");
            testSingleplayerBypass();
            System.out.println("  [PASS] Singleplayer / LAN Direct Mount Bypass verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 4 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 5: Asynchronous HTTP download and atomic promotion
        try {
            System.out.println("\n--- Running Test 5: Async HTTP Download & Atomic Promotion ---");
            testAsyncDownloadAndPromotion();
            System.out.println("  [PASS] Async HTTP Download & Atomic Promotion verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 5 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 6: Checksum mismatch and corrupted download rejection
        try {
            System.out.println("\n--- Running Test 6: Checksum Mismatch & Download Rejection ---");
            testCorruptedDownloadRejection();
            System.out.println("  [PASS] Checksum Mismatch & Download Rejection verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 6 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 7: HTTP error handling (e.g. 404 or connection refused)
        try {
            System.out.println("\n--- Running Test 7: HTTP Error Handling ---");
            testHttpErrorHandling();
            System.out.println("  [PASS] HTTP Error Handling verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 7 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 8: Already mounted deduplication guard
        try {
            System.out.println("\n--- Running Test 8: Already Mounted Deduplication Guard ---");
            testAlreadyMountedGuard();
            System.out.println("  [PASS] Already Mounted Deduplication Guard verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 8 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================================");
        System.out.println("  CLIENT PACK MANAGER TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================================");

        if (failed > 0) {
            throw new RuntimeException("ClientPackManagerTest failed with " + failed + " failures!");
        }
    }

    private static void testPacketSerializationAndDeserialization() {
        // Standard packet
        String testUrl = "http://192.168.1.50:25585/customraces-server-pack.zip";
        String testSha1 = "3f5ddb467385ae7c29db092d06261c23a57fd227";
        long testSize = 987654321L;
        boolean testReq = true;

        ServerPackInfoPacket packet = new ServerPackInfoPacket(testUrl, testSha1, testSize, testReq);
        assertEquals(testUrl, packet.getPackUrl(), "Packet URL getter");
        assertEquals(testSha1, packet.getSha1Hash(), "Packet SHA-1 getter");
        assertEquals(testSize, packet.getSizeBytes(), "Packet sizeBytes getter");
        assertEquals(testReq, packet.isRequired(), "Packet required getter");

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);

        ServerPackInfoPacket decoded = ServerPackInfoPacket.decode(buf);
        assertEquals(testUrl, decoded.getPackUrl(), "Decoded URL must match");
        assertEquals(testSha1, decoded.getSha1Hash(), "Decoded SHA-1 must match");
        assertEquals(testSize, decoded.getSizeBytes(), "Decoded sizeBytes must match");
        assertEquals(testReq, decoded.isRequired(), "Decoded required must match");

        // Edge case: uppercase SHA-1 normalized to lowercase
        ServerPackInfoPacket upperPacket = new ServerPackInfoPacket(testUrl, "3F5DDB467385AE7C29DB092D06261C23A57FD227", 0L, false);
        assertEquals("3f5ddb467385ae7c29db092d06261c23a57fd227", upperPacket.getSha1Hash(), "Uppercase SHA-1 must normalize to lowercase");

        // Edge case: null inputs
        ServerPackInfoPacket nullPacket = new ServerPackInfoPacket(null, null, -1L, false);
        assertEquals("", nullPacket.getPackUrl(), "Null URL must normalize to empty string");
        assertEquals("", nullPacket.getSha1Hash(), "Null SHA-1 must normalize to empty string");
        assertEquals(-1L, nullPacket.getSizeBytes(), "Negative size preserved");
        assertEquals(false, nullPacket.isRequired(), "Required boolean preserved");

        FriendlyByteBuf nullBuf = new FriendlyByteBuf(Unpooled.buffer());
        nullPacket.encode(nullBuf);
        ServerPackInfoPacket decodedNull = ServerPackInfoPacket.decode(nullBuf);
        assertEquals("", decodedNull.getPackUrl(), "Decoded null URL must be empty");
        assertEquals("", decodedNull.getSha1Hash(), "Decoded null SHA-1 must be empty");
    }

    private static void testCacheDetectionAndValidation() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCacheDir = Files.createTempDirectory("customraces_cache_test");
        ClientPackManager.setCacheDirectory(tempCacheDir);

        String dummySha1 = "aabbccddeeff00112233445566778899aabbccdd";
        assertTrue(!ClientPackManager.isPackCached(dummySha1), "isPackCached should be false for non-existent file");

        // Create a real valid zip in cache directory
        Path targetPack = tempCacheDir.resolve("pack-" + dummySha1 + ".zip");
        createDummyZip(targetPack, "pack.mcmeta", "{\"pack\":{\"pack_format\":15,\"description\":\"Test\"}}");

        // Calculate real SHA-1 of the zip we just made
        String realSha1 = ServerPackManager.calculateSha1(targetPack);
        Path realNamedPack = tempCacheDir.resolve("pack-" + realSha1 + ".zip");
        Files.move(targetPack, realNamedPack);

        assertTrue(ClientPackManager.isPackCached(realSha1), "isPackCached should be true for existing valid file");

        // Test handleServerPack mounting from cache without downloading
        ClientPackManager.handleServerPack("http://invalid.should.not.be.contacted:1234/pack.zip", realSha1, Files.size(realNamedPack), false);

        assertTrue(ClientPackManager.isPackMounted(), "Pack should be marked mounted");
        assertEquals(realSha1, ClientPackManager.getCurrentMountedSha1(), "Mounted SHA-1 should match");
        assertEquals(realNamedPack, ClientPackManager.getMountedPackPath(), "Mounted pack path should point to cached zip");
        assertEquals(realNamedPack, GeckoLibCacheInjector.getLastMountedPackPath(), "GeckoLibCacheInjector should be notified with cached path");
    }

    private static void testCorruptedCacheDetectionAndCleanup() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCacheDir = Files.createTempDirectory("customraces_corrupt_cache");
        ClientPackManager.setCacheDirectory(tempCacheDir);

        String targetSha1 = "11223344556677889900aabbccddeeff11223344";
        Path corruptedFile = tempCacheDir.resolve("pack-" + targetSha1 + ".zip");

        // Write junk bytes that do not match targetSha1
        Files.writeString(corruptedFile, "completely corrupted contents that will not match target SHA-1");
        assertTrue(Files.exists(corruptedFile), "Corrupted file must exist prior to check");

        boolean cached = ClientPackManager.isPackCached(targetSha1);
        assertTrue(!cached, "isPackCached must return false for corrupted file");
        assertTrue(!Files.exists(corruptedFile), "Corrupted file must be automatically deleted from disk");
    }

    private static void testSingleplayerBypass() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCacheDir = Files.createTempDirectory("customraces_sp_cache");
        ClientPackManager.setCacheDirectory(tempCacheDir);

        // Configure singleplayer bypass override
        ClientPackManager.setSingleplayerOverride(() -> true);

        // Create a mock local server pack
        Path tempServerDir = Files.createTempDirectory("customraces_sp_server");
        Path localServerPack = tempServerDir.resolve("customraces-server-pack.zip");
        createDummyZip(localServerPack, "assets/customraces/models/test.geo.json", "{\"test\": true}");
        String localSha1 = ServerPackManager.calculateSha1(localServerPack);

        ClientPackManager.setLocalPackPathOverride(() -> localServerPack);

        // Call handleServerPack with a completely bogus URL that would fail if accessed
        ClientPackManager.handleServerPack("http://invalid.nonexistent.domain:65534/pack.zip", localSha1, Files.size(localServerPack), false);

        assertTrue(ClientPackManager.isPackMounted(), "Pack should be mounted via singleplayer bypass");
        assertEquals(localSha1, ClientPackManager.getCurrentMountedSha1(), "Mounted SHA-1 must match local pack");
        assertEquals(localServerPack, ClientPackManager.getMountedPackPath(), "Mounted path must be local server pack");
        assertEquals(localServerPack, GeckoLibCacheInjector.getLastMountedPackPath(), "GeckoLibCacheInjector must receive local path");
    }

    private static void testAsyncDownloadAndPromotion() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCacheDir = Files.createTempDirectory("customraces_async_cache");
        ClientPackManager.setCacheDirectory(tempCacheDir);
        ClientPackManager.setSingleplayerOverride(() -> false);

        // Generate server pack and start HTTP server
        Path tempServerDir = Files.createTempDirectory("customraces_async_server");
        Path serverPack = tempServerDir.resolve("customraces-server-pack.zip");
        Files.createDirectories(tempServerDir.resolve("models"));
        Files.writeString(tempServerDir.resolve("models/model.geo.json"), "{\"model\": \"test\"}");
        ServerPackManager.buildPack(tempServerDir, serverPack);

        boolean started = ServerPackHttpServer.start(0);
        assertTrue(started, "HTTP server must start on ephemeral port");

        try {
            String downloadUrl = ServerPackHttpServer.getDownloadUrl("127.0.0.1");
            String packSha1 = ServerPackManager.getPackSha1();
            long packSize = ServerPackManager.getPackSizeBytes();

            // Run async download
            Boolean result = ClientPackManager.downloadPackAsync(downloadUrl, packSha1).get(10, TimeUnit.SECONDS);
            assertTrue(Boolean.TRUE.equals(result), "Async download future must complete successfully with true");

            Path expectedCacheFile = ClientPackManager.getCacheFilePath(packSha1);
            assertTrue(Files.exists(expectedCacheFile), "Cache zip file must exist after download and atomic rename");
            assertEquals(packSize, Files.size(expectedCacheFile), "Cache file size must match server file");

            Path tempFile = ClientPackManager.getTempFilePath(packSha1);
            assertTrue(!Files.exists(tempFile), "Temporary .tmp file must not remain on disk after promotion");

            assertTrue(ClientPackManager.isPackMounted(), "Pack should be marked mounted after download");
            assertEquals(packSha1, ClientPackManager.getCurrentMountedSha1(), "Current mounted SHA-1 must match downloaded SHA-1");
            assertEquals(expectedCacheFile, GeckoLibCacheInjector.getLastMountedPackPath(), "GeckoLibCacheInjector should have mounted cached zip");

        } finally {
            ServerPackHttpServer.stop();
        }
    }

    private static void testCorruptedDownloadRejection() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCacheDir = Files.createTempDirectory("customraces_corrupt_download");
        ClientPackManager.setCacheDirectory(tempCacheDir);
        ClientPackManager.setSingleplayerOverride(() -> false);

        Path tempServerDir = Files.createTempDirectory("customraces_corrupt_server");
        Path serverPack = tempServerDir.resolve("customraces-server-pack.zip");
        createDummyZip(serverPack, "test.txt", "hello world");
        ServerPackManager.buildPack(tempServerDir, serverPack);

        boolean started = ServerPackHttpServer.start(0);
        assertTrue(started, "HTTP server must start");

        try {
            String downloadUrl = ServerPackHttpServer.getDownloadUrl("127.0.0.1");
            String mismatchedSha1 = "ffffffffffffffffffffffffffffffffffffffff";

            boolean success = ClientPackManager.downloadAndPromote(downloadUrl, mismatchedSha1);
            assertTrue(!success, "downloadAndPromote must fail on checksum mismatch");

            Path targetCacheFile = ClientPackManager.getCacheFilePath(mismatchedSha1);
            assertTrue(!Files.exists(targetCacheFile), "Target cache file must not be created on checksum mismatch");

            Path tempFile = ClientPackManager.getTempFilePath(mismatchedSha1);
            assertTrue(!Files.exists(tempFile), "Temp file must be cleaned up on checksum mismatch");

        } finally {
            ServerPackHttpServer.stop();
        }
    }

    private static void testHttpErrorHandling() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCacheDir = Files.createTempDirectory("customraces_err_cache");
        ClientPackManager.setCacheDirectory(tempCacheDir);

        // Port 65534 where nothing is listening
        String nonexistentUrl = "http://127.0.0.1:65534/nonexistent.zip";
        String sha1 = "abcdef0123456789abcdef0123456789abcdef01";

        boolean success = ClientPackManager.downloadAndPromote(nonexistentUrl, sha1);
        assertTrue(!success, "downloadAndPromote must return false on connection refused/failure");

        Path targetCacheFile = ClientPackManager.getCacheFilePath(sha1);
        assertTrue(!Files.exists(targetCacheFile), "Target cache file must not exist");
    }

    private static void testAlreadyMountedGuard() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempPack = Files.createTempFile("test_already_mounted", ".zip");
        String sha1 = "1234567890123456789012345678901234567890";

        ClientPackManager.mountPack(tempPack, sha1);
        int initialMounts = GeckoLibCacheInjector.getMountCount();
        assertTrue(initialMounts > 0, "Initial mount count should be > 0");

        // Invoking handleServerPack with identical SHA-1 should skip re-mount
        ClientPackManager.handleServerPack("http://example.com/pack.zip", sha1, 100L, false);
        int subsequentMounts = GeckoLibCacheInjector.getMountCount();

        assertEquals(initialMounts, subsequentMounts, "Mount count must not increment when same pack is already mounted");
    }

    private static void createDummyZip(Path zipPath, String entryName, String content) throws IOException {
        if (zipPath.getParent() != null && !Files.exists(zipPath.getParent())) {
            Files.createDirectories(zipPath.getParent());
        }
        try (OutputStream fos = Files.newOutputStream(zipPath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
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

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError("[FAIL] " + message + " - Expected: <" + expected + ">, but got: <" + actual + ">");
        }
    }
}
