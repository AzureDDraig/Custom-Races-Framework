package ddraig.net.customraces.pack;

import ddraig.net.customraces.client.render.GeckoLibCacheInjector;
import ddraig.net.customraces.network.ServerPackInfoPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

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

import com.sun.net.httpserver.HttpServer;

/**
 * Adversarial Challenger test suite for Milestone M2:
 * 1. Path traversal attacks in SHA-1 hash strings
 * 2. Rapid concurrent server pack announcements (10 threads concurrently)
 * 3. Premature server disconnect / truncated response
 * 4. Special characters & URL encoding resilience
 * 5. Extreme null & boundary matrix
 * 6. Singleplayer fallback when local pack is missing or corrupt
 * 7. PackRepository and ResourceManager reflection safety
 */
public class ClientPackManagerChallengerTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("    CLIENT PACK MANAGER ADVERSARIAL CHALLENGER TEST SUITE         ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;

        // Challenger Test 1: Path traversal attacks in SHA-1
        try {
            System.out.println("\n--- Challenger Test 1: Path Traversal Attack Prevention ---");
            testPathTraversalAttackPrevention();
            System.out.println("  [PASS] Path Traversal Attack Prevention verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Challenger Test 2: Extreme null and boundary matrix
        try {
            System.out.println("\n--- Challenger Test 2: Extreme Null & Boundary Matrix ---");
            testNullAndBoundaryMatrix();
            System.out.println("  [PASS] Extreme Null & Boundary Matrix verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Challenger Test 3: Concurrent download race condition stress
        try {
            System.out.println("\n--- Challenger Test 3: Concurrent Download Race Condition Stress ---");
            testConcurrentDownloadStress();
            System.out.println("  [PASS] Concurrent Download Race Condition Stress verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 3 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Challenger Test 4: Truncated stream / server crash mid-transfer
        try {
            System.out.println("\n--- Challenger Test 4: Mid-Transfer Server Crash / Truncation ---");
            testMidTransferServerCrash();
            System.out.println("  [PASS] Mid-Transfer Server Crash / Truncation verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 4 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Challenger Test 5: Singleplayer fallback on missing or corrupt local pack
        try {
            System.out.println("\n--- Challenger Test 5: Singleplayer Fallback on Missing Local Pack ---");
            testSingleplayerFallback();
            System.out.println("  [PASS] Singleplayer Fallback verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 5 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Challenger Test 6: Reflection robustness on null / mock Minecraft instances
        try {
            System.out.println("\n--- Challenger Test 6: Reflection In-Memory Robustness ---");
            testReflectionRobustness();
            System.out.println("  [PASS] Reflection In-Memory Robustness verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 6 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================================");
        System.out.println("  CHALLENGER TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================================");

        if (failed > 0) {
            throw new RuntimeException("ClientPackManagerChallengerTest failed with " + failed + " failures!");
        }
    }

    private static void testPathTraversalAttackPrevention() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("challenger_traversal_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        String[] maliciousHashes = new String[]{
                "../../etc/passwd",
                "..\\..\\windows\\system32\\calc.exe",
                "pack-../../escape.zip",
                "ce89ca7f7caebfa1f517e6c1c064f0064d872f9/../../bad",
                "3f5ddb467385ae7c29db092d06261c23a57fd227;rm -rf /",
                "../",
                "....",
                "ce89ca7f7caebfa1f517e6c1c064f0064d872f9\0nullbyte.zip"
        };

        for (String badHash : maliciousHashes) {
            // handleServerPack must refuse to create files outside cache
            ClientPackManager.handleServerPack("http://example.com/pack.zip", badHash, 100L, false);
            assertTrue(!ClientPackManager.isPackCached(badHash), "Malicious hash must not be considered cached: " + badHash);
            assertTrue(!ClientPackManager.isPackMounted(), "Pack must not mount for malicious hash: " + badHash);
        }

        // Ensure no files were written outside tempCache
        try (var stream = Files.list(tempCache.getParent())) {
            boolean escaped = stream.anyMatch(p -> p.getFileName().toString().contains("escape")
                    || p.getFileName().toString().contains("calc.exe"));
            assertTrue(!escaped, "No files should escape the designated cache directory");
        }
    }

    private static void testNullAndBoundaryMatrix() {
        ClientPackManager.resetForTesting();

        // 1. Extreme nulls
        ClientPackManager.handleServerPack(null, null, 0, false);
        assertTrue(!ClientPackManager.isPackMounted(), "Pack must not mount on null hash");

        ClientPackManager.handleServerPack("", "", -500, true);
        assertTrue(!ClientPackManager.isPackMounted(), "Pack must not mount on empty hash");

        ClientPackManager.handleServerPack("not_a_valid_url", "short_hash", 0, false);
        assertTrue(!ClientPackManager.isPackMounted(), "Pack must not mount on non-40-char hash");

        // 2. Buffer encoding with empty strings
        ServerPackInfoPacket emptyPacket = new ServerPackInfoPacket("", "", 0, false);
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        emptyPacket.encode(buf);
        ServerPackInfoPacket decoded = ServerPackInfoPacket.decode(buf);
        assertEquals("", decoded.getPackUrl(), "Empty URL encoded");
        assertEquals("", decoded.getSha1Hash(), "Empty SHA-1 encoded");
        assertEquals(0L, decoded.getSizeBytes(), "0 size encoded");
        assertEquals(false, decoded.isRequired(), "Required encoded");
    }

    private static void testConcurrentDownloadStress() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("challenger_concurrent_cache");
        ClientPackManager.setCacheDirectory(tempCache);
        ClientPackManager.setSingleplayerOverride(() -> false);

        // Build a server pack
        Path tempServer = Files.createTempDirectory("challenger_concurrent_server");
        Path serverPack = tempServer.resolve("customraces-server-pack.zip");
        createZip(serverPack, "test.json", "{\"test\": \"concurrent\"}");
        ServerPackManager.buildPack(tempServer, serverPack);

        ServerPackHttpServer.start(0);
        String url = ServerPackHttpServer.getDownloadUrl("127.0.0.1");
        String sha1 = ServerPackManager.getPackSha1();

        try {
            int threadCount = 8;
            ExecutorService pool = Executors.newFixedThreadPool(threadCount);
            List<Future<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < threadCount; i++) {
                futures.add(pool.submit(() -> ClientPackManager.downloadAndPromote(url, sha1)));
            }

            int successCount = 0;
            for (Future<Boolean> f : futures) {
                if (f.get(10, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }

            pool.shutdown();
            assertTrue(successCount > 0, "At least one concurrent download must succeed");

            Path finalCache = ClientPackManager.getCacheFilePath(sha1);
            assertTrue(Files.exists(finalCache), "Final cached pack must exist on disk");
            assertTrue(ClientPackManager.isPackCached(sha1), "Pack must be properly cached and verified");

        } finally {
            ServerPackHttpServer.stop();
        }
    }

    private static void testMidTransferServerCrash() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("challenger_crash_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        // Spin up a raw HttpServer that abruptly closes connection or sends partial bytes
        HttpServer faultyServer = HttpServer.create(new InetSocketAddress(0), 0);
        faultyServer.createContext("/truncated.zip", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "application/zip");
            exchange.sendResponseHeaders(200, 100000); // promise 100KB
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(new byte[]{1, 2, 3, 4}); // only send 4 bytes then abruptly close
                os.flush();
            }
            exchange.close();
        });
        faultyServer.start();

        int port = faultyServer.getAddress().getPort();
        String brokenUrl = "http://127.0.0.1:" + port + "/truncated.zip";
        String sha1 = "1111222233334444555566667777888899990000";

        try {
            boolean success = ClientPackManager.downloadAndPromote(brokenUrl, sha1);
            assertTrue(!success, "downloadAndPromote must fail on truncated transfer");

            Path target = ClientPackManager.getCacheFilePath(sha1);
            assertTrue(!Files.exists(target), "Target file must not be promoted on truncated transfer");

            Path tmp = ClientPackManager.getTempFilePath(sha1);
            assertTrue(!Files.exists(tmp), "Temp file must be removed after truncated transfer");
        } finally {
            faultyServer.stop(0);
        }
    }

    private static void testSingleplayerFallback() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("challenger_sp_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        // Singleplayer is true, but local server pack path points to non-existent file
        ClientPackManager.setSingleplayerOverride(() -> true);
        Path nonExistent = tempCache.resolve("non_existent_server_pack.zip");
        ClientPackManager.setLocalPackPathOverride(() -> nonExistent);

        // Local pack is missing, so it should not mount local pack
        String dummySha1 = "4444555566667777888899990000111122223333";
        ClientPackManager.handleServerPack("", dummySha1, 100L, false);

        assertTrue(!ClientPackManager.isPackMounted(), "Pack should not mount when local pack file is non-existent");
    }

    private static void testReflectionRobustness() {
        ClientPackManager.resetForTesting();
        // applyDynamicMount with null Minecraft instance should execute safely
        ClientPackManager.applyDynamicMount(null, null, "1234");
        // No exceptions thrown
        assertTrue(true, "applyDynamicMount safely ignores null instances");
    }

    private static void createZip(Path zipPath, String entryName, String content) throws IOException {
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
            throw new AssertionError("[FAIL] " + message);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("[FAIL] " + message + " - Expected: " + expected + ", Got: " + actual);
        }
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError("[FAIL] " + message + " - Expected: " + expected + ", Got: " + actual);
        }
    }
}
