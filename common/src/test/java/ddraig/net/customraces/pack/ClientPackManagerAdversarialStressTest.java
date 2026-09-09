package ddraig.net.customraces.pack;

import ddraig.net.customraces.client.render.GeckoLibCacheInjector;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Empirical Adversarial Stress Test Suite for Milestone M2:
 * 1. Deep Checksum Tampering & Disk Cache Purge
 * 2. Local Singleplayer File Tampering & Missing File Resilience
 * 3. HTTP Error Codes (500, 404, 403, 204) Non-200 Status Rejection
 * 4. Systematic Stream Truncation at Exact Chunk Boundaries (0B, 1B, 128B, 8192B, 8193B)
 * 5. Comprehensive Path Traversal Matrix (Backslashes, Forward slashes, URL-encoded, ADS, Windows paths)
 * 6. URL Scheme Spoofing & Malformed URLs (file://, ftp://, malformed protocols)
 * 7. Multi-Threaded Cache Contention Stress
 */
public class ClientPackManagerAdversarialStressTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   M2 CLIENT PACK MANAGER ADVERSARIAL STRESS TEST SUITE           ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        // Test 1: Deep Checksum Tampering & Disk Cache Purge
        try {
            System.out.println("\n--- Stress Test 1: Checksum Tampering & Disk Cache Purge ---");
            testChecksumTamperingAndPurge();
            System.out.println("  [PASS] Checksum Tampering & Disk Cache Purge verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 2: Local Singleplayer File Tampering & Missing File Resilience
        try {
            System.out.println("\n--- Stress Test 2: Singleplayer File Tampering & Missing File ---");
            testSingleplayerTamperingAndMissing();
            System.out.println("  [PASS] Singleplayer File Tampering & Missing File verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 3: HTTP Error Codes (500, 404, 403, 204) Non-200 Status Rejection
        try {
            System.out.println("\n--- Stress Test 3: HTTP Error Codes & Non-200 Status Rejection ---");
            testHttpErrorCodes();
            System.out.println("  [PASS] HTTP Error Codes & Non-200 Status Rejection verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 3 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 4: Systematic Stream Truncation at Exact Chunk Boundaries
        try {
            System.out.println("\n--- Stress Test 4: Systematic Stream Truncation at Chunk Boundaries ---");
            testStreamTruncationBoundaries();
            System.out.println("  [PASS] Systematic Stream Truncation at Chunk Boundaries verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 4 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 5: Comprehensive Path Traversal Matrix
        try {
            System.out.println("\n--- Stress Test 5: Comprehensive Path Traversal Matrix ---");
            testComprehensivePathTraversalMatrix();
            System.out.println("  [PASS] Comprehensive Path Traversal Matrix verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 5 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 6: URL Scheme Spoofing & Malformed URLs
        try {
            System.out.println("\n--- Stress Test 6: URL Scheme Spoofing & Malformed URLs ---");
            testUrlSchemeSpoofing();
            System.out.println("  [PASS] URL Scheme Spoofing & Malformed URLs verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 6 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 7: Multi-Threaded Cache Contention Stress
        try {
            System.out.println("\n--- Stress Test 7: Multi-Threaded Cache Contention Stress ---");
            testMultiThreadedCacheContention();
            System.out.println("  [PASS] Multi-Threaded Cache Contention Stress verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 7 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================================");
        System.out.println("  ADVERSARIAL STRESS TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================================");

        if (failed > 0) {
            throw new RuntimeException("ClientPackManagerAdversarialStressTest failed with " + failed + " failures!");
        }
    }

    private static void testChecksumTamperingAndPurge() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("stress_tamper_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        // Generate a valid zip file
        Path tempZip = Files.createTempFile("valid_source", ".zip");
        createDummyZip(tempZip, "assets/customraces/models/were.geo.json", "{\"valid\": true}");
        String expectedSha1 = ServerPackManager.calculateSha1(tempZip);

        Path cacheFile = tempCache.resolve("pack-" + expectedSha1 + ".zip");
        Files.copy(tempZip, cacheFile);
        assertTrue(ClientPackManager.isPackCached(expectedSha1), "Pack must initially be recognized as cached");

        // 1. Mutate byte at start
        byte[] bytes = Files.readAllBytes(cacheFile);
        bytes[0] = (byte) (bytes[0] ^ 0xFF);
        Files.write(cacheFile, bytes);

        assertTrue(!ClientPackManager.isPackCached(expectedSha1), "Tampered start byte must cause isPackCached to return false");
        assertTrue(!Files.exists(cacheFile), "Tampered file must be purged from cache disk");

        // 2. Mutate byte in the middle
        Files.copy(tempZip, cacheFile);
        bytes = Files.readAllBytes(cacheFile);
        bytes[bytes.length / 2] = (byte) (bytes[bytes.length / 2] ^ 0xFF);
        Files.write(cacheFile, bytes);

        assertTrue(!ClientPackManager.isPackCached(expectedSha1), "Tampered middle byte must cause isPackCached to return false");
        assertTrue(!Files.exists(cacheFile), "Tampered file must be purged from cache disk");

        // 3. Mutate byte at end
        Files.copy(tempZip, cacheFile);
        bytes = Files.readAllBytes(cacheFile);
        bytes[bytes.length - 1] = (byte) (bytes[bytes.length - 1] ^ 0xFF);
        Files.write(cacheFile, bytes);

        assertTrue(!ClientPackManager.isPackCached(expectedSha1), "Tampered end byte must cause isPackCached to return false");
        assertTrue(!Files.exists(cacheFile), "Tampered file must be purged from cache disk");

        // 4. Test handleServerPack when cache is tampered
        Files.copy(tempZip, cacheFile);
        bytes = Files.readAllBytes(cacheFile);
        bytes[0] = (byte) (bytes[0] ^ 0xAA);
        Files.write(cacheFile, bytes);

        ClientPackManager.handleServerPack("", expectedSha1, bytes.length, false);
        assertTrue(!ClientPackManager.isPackMounted(), "handleServerPack must NOT mount tampered cache file");
        assertTrue(!Files.exists(cacheFile), "Tampered cache file must be purged during handleServerPack");
    }

    private static void testSingleplayerTamperingAndMissing() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("stress_sp_cache");
        ClientPackManager.setCacheDirectory(tempCache);
        ClientPackManager.setSingleplayerOverride(() -> true);

        // Scenario A: Local pack file exists, but its hash does NOT match the packet SHA-1
        Path localDir = Files.createTempDirectory("stress_sp_local");
        Path localPack = localDir.resolve("customraces-server-pack.zip");
        createDummyZip(localPack, "test.txt", "local data");
        ClientPackManager.setLocalPackPathOverride(() -> localPack);

        String legitimateLocalSha1 = ServerPackManager.calculateSha1(localPack);
        String mismatchedSha1 = "00112233445566778899aabbccddeeff00112233";

        // Dispatch with mismatched SHA-1 and no URL (offline)
        ClientPackManager.handleServerPack("", mismatchedSha1, 100L, false);
        assertTrue(!ClientPackManager.isPackMounted(), "Singleplayer must NOT mount local pack if SHA-1 does not match");
        assertEquals(null, ClientPackManager.getCurrentMountedSha1(), "Mounted SHA-1 must remain null");

        // Scenario B: Local pack was deleted after startup
        Files.deleteIfExists(localPack);
        ClientPackManager.handleServerPack("", legitimateLocalSha1, 100L, false);
        assertTrue(!ClientPackManager.isPackMounted(), "Singleplayer must safely handle deleted/missing local pack");

        // Scenario C: Local pack is a directory instead of a regular file
        Path dummyDirPack = localDir.resolve("dir_pack.zip");
        Files.createDirectories(dummyDirPack);
        ClientPackManager.setLocalPackPathOverride(() -> dummyDirPack);
        ClientPackManager.handleServerPack("", legitimateLocalSha1, 100L, false);
        assertTrue(!ClientPackManager.isPackMounted(), "Singleplayer must safely ignore non-regular-file pack path");
    }

    private static void testHttpErrorCodes() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("stress_http_errors_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        int[] errorCodes = new int[]{500, 404, 403, 204, 502, 503};

        for (int code : errorCodes) {
            server.createContext("/status" + code, exchange -> {
                exchange.sendResponseHeaders(code, 0);
                exchange.close();
            });
        }
        server.start();

        int port = server.getAddress().getPort();
        String sha1 = "abcdefabcdefabcdefabcdefabcdefabcdefabcd";

        try {
            for (int code : errorCodes) {
                String url = "http://127.0.0.1:" + port + "/status" + code;
                boolean success = ClientPackManager.downloadAndPromote(url, sha1);
                assertTrue(!success, "downloadAndPromote must fail for HTTP status " + code);
                assertTrue(!Files.exists(ClientPackManager.getCacheFilePath(sha1)), "Cache file must not exist for status " + code);
                assertTrue(!Files.exists(ClientPackManager.getTempFilePath(sha1)), "Temp file must not exist for status " + code);
            }
        } finally {
            server.stop(0);
        }
    }

    private static void testStreamTruncationBoundaries() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("stress_truncation_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        // Boundary chunk sizes to test: 0 bytes, 1 byte, 128 bytes, 8192 bytes (exact buffer size), 8193 bytes
        int[] truncatedSizes = new int[]{0, 1, 128, 8192, 8193};

        for (int truncSize : truncatedSizes) {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/trunc", exchange -> {
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, 65536); // declare 64KB
                try (OutputStream os = exchange.getResponseBody()) {
                    if (truncSize > 0) {
                        byte[] data = new byte[truncSize];
                        for (int i = 0; i < truncSize; i++) data[i] = (byte) (i % 256);
                        os.write(data);
                        os.flush();
                    }
                }
                exchange.close();
            });
            server.start();

            int port = server.getAddress().getPort();
            String url = "http://127.0.0.1:" + port + "/trunc";
            String sha1 = "1234567890123456789012345678901234567890";

            try {
                boolean success = ClientPackManager.downloadAndPromote(url, sha1);
                assertTrue(!success, "Truncation at size " + truncSize + " must fail");
                assertTrue(!Files.exists(ClientPackManager.getCacheFilePath(sha1)), "Target file must not exist for truncSize " + truncSize);
                assertTrue(!Files.exists(ClientPackManager.getTempFilePath(sha1)), "Temp file must be cleaned up for truncSize " + truncSize);
            } finally {
                server.stop(0);
            }
        }
    }

    private static void testComprehensivePathTraversalMatrix() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("stress_traversal_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        String[] traversalVectors = new String[]{
                "../escape",
                "..\\escape",
                "....//escape",
                "....\\\\escape",
                "/etc/shadow",
                "C:\\Windows\\System32\\drivers\\etc\\hosts",
                "D:/data/secret.txt",
                "%2e%2e%2f%2e%2e%2fescape",
                "%2e%2e%5c%2e%2e%5cescape",
                "pack.zip:stream",
                "pack\u0000hidden.zip",
                "3f5ddb467385ae7c29db092d06261c23a57fd227/../evil",
                "3f5ddb467385ae7c29db092d06261c23a57fd227\\..\\evil",
                "3f5ddb467385ae7c29db092d06261c23a57fd227\r\nHeader: Injection",
                "3f5ddb467385ae7c29db092d06261c23a57fd227\0.zip"
        };

        for (String vector : traversalVectors) {
            ClientPackManager.handleServerPack("http://127.0.0.1/pack.zip", vector, 100L, false);
            assertTrue(!ClientPackManager.isPackCached(vector), "Vector must not be cached: " + vector);
            assertTrue(!ClientPackManager.isPackMounted(), "Pack must not mount for traversal vector: " + vector);
        }

        // Verify parent directory contains 0 escaped files
        try (var stream = Files.list(tempCache.getParent())) {
            boolean escaped = stream.anyMatch(p -> p.getFileName().toString().contains("escape")
                    || p.getFileName().toString().contains("evil")
                    || p.getFileName().toString().contains("secret"));
            assertTrue(!escaped, "No files should have escaped to cache parent directory");
        }
    }

    private static void testUrlSchemeSpoofing() {
        ClientPackManager.resetForTesting();
        Path tempCache;
        try {
            tempCache = Files.createTempDirectory("stress_scheme_cache");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        ClientPackManager.setCacheDirectory(tempCache);

        String validSha1 = "3f5ddb467385ae7c29db092d06261c23a57fd227";

        String[] badUrls = new String[]{
                "file:///C:/Windows/System32/calc.exe",
                "ftp://evil.com/pack.zip",
                "jar:file:/something.jar!/entry",
                "gopher://gopher.floodgap.com/",
                "unknown_protocol://test.com/pack.zip",
                "://malformed",
                "htt p://spaces.com/pack.zip"
        };

        for (String badUrl : badUrls) {
            boolean success = ClientPackManager.downloadAndPromote(badUrl, validSha1);
            assertTrue(!success, "downloadAndPromote must fail cleanly on unsupported/malformed URL: " + badUrl);
            assertTrue(!Files.exists(ClientPackManager.getCacheFilePath(validSha1)), "Cache file must not exist for bad URL: " + badUrl);
            assertTrue(!Files.exists(ClientPackManager.getTempFilePath(validSha1)), "Temp file must not exist for bad URL: " + badUrl);
        }
    }

    private static void testMultiThreadedCacheContention() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("stress_contention_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        Path testPack = tempCache.resolve("source.zip");
        createDummyZip(testPack, "data.txt", "contention test content");
        String sha1 = ServerPackManager.calculateSha1(testPack);

        Path cachedFile = ClientPackManager.getCacheFilePath(sha1);
        Files.copy(testPack, cachedFile);

        int threads = 12;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Boolean>> futures = new ArrayList<>();

        // Concurrently query isPackCached, getCacheFilePath, and handleServerPack
        for (int i = 0; i < 40; i++) {
            futures.add(pool.submit(() -> ClientPackManager.isPackCached(sha1)));
            futures.add(pool.submit(() -> {
                ClientPackManager.handleServerPack("http://invalid:1234/pack.zip", sha1, 100L, false);
                return ClientPackManager.isPackMounted();
            }));
        }

        for (Future<Boolean> f : futures) {
            Boolean res = f.get(10, TimeUnit.SECONDS);
            assertTrue(Boolean.TRUE.equals(res), "Concurrent operation should complete successfully");
        }

        pool.shutdown();
        assertTrue(ClientPackManager.isPackMounted(), "Pack should remain safely mounted");
        assertEquals(sha1, ClientPackManager.getCurrentMountedSha1(), "Mounted SHA-1 should match");
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
}
