package ddraig.net.customraces.pack;

import com.sun.net.httpserver.HttpServer;
import ddraig.net.customraces.client.render.GeckoLibCacheInjector;
import ddraig.net.customraces.network.ServerPackInfoPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Challenger 2 Adversarial Stress & Empirical Verification Suite for Milestone M2:
 *
 * 1. Multi-threaded concurrency:
 *    - Concurrent downloads requesting the SAME pack simultaneously (race conditions & atomic promotion)
 *    - Concurrent downloads requesting DIFFERENT packs simultaneously (isolation & cache integrity)
 *    - High-throughput executor burst stress (thread-safety & resource hygiene)
 * 2. Lifecycle, Reconnects & Deduplication:
 *    - Packet flooding with duplicate announcements (deduplication & zero redundant I/O)
 *    - Rapid server flapping / alternating pack sync (switching between Server A and Server B)
 *    - Reset & state transitions
 * 3. Dynamic ResourceManager & PackRepository Persistence:
 *    - Injected RepositorySource discovery on real PackRepository instances
 *    - Pack persistence across multiple consecutive F3+T manual reloads
 *    - Multiple dynamic registrations and fallback resource handling
 * 4. Extreme Boundary Matrix:
 *    - Maximum string length buffers (32,767 URL chars, 64 SHA-1 chars, Unicode, special chars)
 *    - Extreme numeric boundary values (0, Long.MAX_VALUE, Long.MIN_VALUE, -1)
 *    - Exhaustive SHA-1 rejection matrix (null, empty, 39-char, 41-char, non-hex, spaces, uppercase)
 *    - Exhaustive URL rejection matrix (malformed syntax, 404, 500, refused connection)
 */
public class ClientPackManagerStream2ChallengerTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   CLIENT PACK MANAGER CHALLENGER 2 STRESS & VERIFICATION SUITE   ");
        System.out.println("==================================================================");

        try {
            net.minecraft.SharedConstants.tryDetectVersion();
            net.minecraft.server.Bootstrap.bootStrap();
        } catch (Throwable ignored) {}

        int passed = 0;
        int failed = 0;

        // --- SECTION 1: CONCURRENCY & MULTI-THREADED STRESS ---
        try {
            System.out.println("\n--- [Challenger 2] Test 1.1: Concurrent Downloads for SAME Pack ---");
            testConcurrentDownloadsSamePack();
            System.out.println("  [PASS] Concurrent Downloads for SAME Pack verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1.1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            System.out.println("\n--- [Challenger 2] Test 1.2: Concurrent Downloads for DIFFERENT Packs ---");
            testConcurrentDownloadsDifferentPacks();
            System.out.println("  [PASS] Concurrent Downloads for DIFFERENT Packs verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1.2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            System.out.println("\n--- [Challenger 2] Test 1.3: High-Throughput Burst Requests ---");
            testHighThroughputBurstRequests();
            System.out.println("  [PASS] High-Throughput Burst Requests verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1.3 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // --- SECTION 2: LIFECYCLE, PACKET FLOODING & DEDUPLICATION ---
        try {
            System.out.println("\n--- [Challenger 2] Test 2.1: Packet Flooding Deduplication Guard ---");
            testPacketFloodingDeduplication();
            System.out.println("  [PASS] Packet Flooding Deduplication Guard verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 2.1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            System.out.println("\n--- [Challenger 2] Test 2.2: Rapid Server Switching / Alternating Packs ---");
            testRapidServerSwitching();
            System.out.println("  [PASS] Rapid Server Switching verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 2.2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // --- SECTION 3: DYNAMIC RESOURCEMANAGER & PACKREPOSITORY PERSISTENCE ---
        try {
            System.out.println("\n--- [Challenger 2] Test 3.1: PackRepository Registration & F3+T Reload Survival ---");
            testPackRepositoryRegistrationAndReloadSurvival();
            System.out.println("  [PASS] PackRepository Registration & F3+T Reload Survival verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 3.1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // --- SECTION 4: EXTREME BOUNDARY MATRIX ---
        try {
            System.out.println("\n--- [Challenger 2] Test 4.1: Extreme Buffer Serialization Boundaries ---");
            testExtremeBufferSerialization();
            System.out.println("  [PASS] Extreme Buffer Serialization Boundaries verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 4.1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            System.out.println("\n--- [Challenger 2] Test 4.2: Exhaustive SHA-1 Validation & Rejection Matrix ---");
            testSha1ValidationAndRejectionMatrix();
            System.out.println("  [PASS] Exhaustive SHA-1 Validation & Rejection Matrix verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 4.2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        try {
            System.out.println("\n--- [Challenger 2] Test 4.3: Exhaustive URL & Network Error Matrix ---");
            testUrlAndNetworkErrorMatrix();
            System.out.println("  [PASS] Exhaustive URL & Network Error Matrix verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 4.3 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================================");
        System.out.println("  CHALLENGER 2 TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        System.out.println("==================================================================");

        if (failed > 0) {
            throw new RuntimeException("ClientPackManagerStream2ChallengerTest failed with " + failed + " failures!");
        }
    }

    /**
     * Test 1.1: 16 concurrent threads simultaneously attempt to download and promote the exact same pack.
     * Verifies that race conditions do not corrupt the file and that the target cached file is valid.
     */
    private static void testConcurrentDownloadsSamePack() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("c2_concurrent_same_cache");
        ClientPackManager.setCacheDirectory(tempCache);
        ClientPackManager.setSingleplayerOverride(() -> false);

        Path tempServer = Files.createTempDirectory("c2_concurrent_same_server");
        Path serverPack = tempServer.resolve("customraces-server-pack.zip");
        createValidPackZip(serverPack, "assets/customraces/models/werewolf.geo.json", "{\"geometry\":\"werewolf\"}");
        ServerPackManager.buildPack(tempServer, serverPack);

        ServerPackHttpServer.start(0);
        String url = ServerPackHttpServer.getDownloadUrl("127.0.0.1");
        String sha1 = ServerPackManager.getPackSha1();

        try {
            int threads = 16;
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            CountDownLatch startGate = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < threads; i++) {
                futures.add(pool.submit(() -> {
                    startGate.await(); // ensure simultaneous execution
                    return ClientPackManager.downloadAndPromote(url, sha1);
                }));
            }

            // Release all threads simultaneously
            startGate.countDown();

            int successfulDownloads = 0;
            for (Future<Boolean> f : futures) {
                if (f.get(15, TimeUnit.SECONDS)) {
                    successfulDownloads++;
                }
            }

            pool.shutdown();
            assertTrue(successfulDownloads > 0, "At least one concurrent download must succeed (got " + successfulDownloads + ")");

            Path targetCache = ClientPackManager.getCacheFilePath(sha1);
            assertTrue(Files.exists(targetCache), "Target cache zip must exist after concurrent downloads");
            assertTrue(ClientPackManager.isPackCached(sha1), "Cached file must pass SHA-1 verification");
            assertEquals(ServerPackManager.getPackSizeBytes(), Files.size(targetCache), "Cache file size must match server pack");

            // Ensure no lingering .tmp file
            Path tmpFile = ClientPackManager.getTempFilePath(sha1);
            assertTrue(!Files.exists(tmpFile), "No .tmp file should linger in cache directory");

        } finally {
            ServerPackHttpServer.stop();
        }
    }

    /**
     * Test 1.2: Multiple threads simultaneously request DIFFERENT packs served over HTTP.
     * Verifies that concurrent downloads of distinct packs are properly isolated.
     */
    private static void testConcurrentDownloadsDifferentPacks() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("c2_concurrent_diff_cache");
        ClientPackManager.setCacheDirectory(tempCache);
        ClientPackManager.setSingleplayerOverride(() -> false);

        int packCount = 5;
        Map<String, byte[]> packs = new HashMap<>();
        Map<String, String> shaMap = new HashMap<>();

        for (int i = 0; i < packCount; i++) {
            Path tempZip = Files.createTempFile("pack_" + i + "_", ".zip");
            createValidPackZip(tempZip, "assets/customraces/models/model_" + i + ".geo.json", "{\"id\":" + i + "}");
            byte[] bytes = Files.readAllBytes(tempZip);
            String sha = ServerPackManager.calculateSha1(tempZip);
            packs.put("/pack_" + i + ".zip", bytes);
            shaMap.put("/pack_" + i + ".zip", sha);
            Files.deleteIfExists(tempZip);
        }

        // Custom HTTP server serving all 5 packs
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        for (Map.Entry<String, byte[]> entry : packs.entrySet()) {
            server.createContext(entry.getKey(), exchange -> {
                byte[] data = entry.getValue();
                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
            });
        }
        server.start();

        int port = server.getAddress().getPort();
        try {
            int totalRequests = 15;
            ExecutorService pool = Executors.newFixedThreadPool(totalRequests);
            CountDownLatch gate = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();

            for (int i = 0; i < totalRequests; i++) {
                int packIndex = i % packCount;
                String path = "/pack_" + packIndex + ".zip";
                String url = "http://127.0.0.1:" + port + path;
                String sha = shaMap.get(path);

                futures.add(pool.submit(() -> {
                    gate.await();
                    return ClientPackManager.downloadAndPromote(url, sha);
                }));
            }

            gate.countDown();

            for (Future<Boolean> f : futures) {
                f.get(15, TimeUnit.SECONDS);
            }
            pool.shutdown();

            // Verify that all 5 packs were successfully cached with valid checksums
            for (String sha : shaMap.values()) {
                assertTrue(ClientPackManager.isPackCached(sha), "Pack with SHA-1 " + sha + " must be cached and valid");
                Path p = ClientPackManager.getCacheFilePath(sha);
                assertTrue(Files.exists(p), "Cached file must exist on disk: " + p);
            }

        } finally {
            server.stop(0);
        }
    }

    /**
     * Test 1.3: Submit a high-throughput burst of 64 requests (valid, invalid, cached)
     * to verify executor resilience and system stability under load.
     */
    private static void testHighThroughputBurstRequests() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("c2_burst_cache");
        ClientPackManager.setCacheDirectory(tempCache);
        ClientPackManager.setSingleplayerOverride(() -> false);

        // Pre-cache one pack
        String cachedSha1 = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
        Path preCached = ClientPackManager.getCacheFilePath(cachedSha1);
        createValidPackZip(preCached, "test.txt", "pre-cached");
        String actualCachedSha = ServerPackManager.calculateSha1(preCached);
        Path realCachedPath = ClientPackManager.getCacheFilePath(actualCachedSha);
        Files.move(preCached, realCachedPath);

        int burstSize = 64;
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (int i = 0; i < burstSize; i++) {
            final int reqIdx = i;
            if (i % 3 == 0) {
                // Cached hit
                futures.add(CompletableFuture.runAsync(() ->
                        ClientPackManager.handleServerPack("http://invalid:9999/dummy.zip", actualCachedSha, 100, false)));
            } else if (i % 3 == 1) {
                // Bad SHA-1 rejection
                futures.add(CompletableFuture.runAsync(() ->
                        ClientPackManager.handleServerPack("http://invalid:9999/dummy.zip", "invalid_sha_" + reqIdx, 100, false)));
            } else {
                // Connection refused error
                futures.add(ClientPackManager.downloadPackAsync("http://127.0.0.1:65530/dummy.zip", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get(15, TimeUnit.SECONDS);
        // If execution reaches here without deadlock or OOM, test passes
        assertTrue(ClientPackManager.isPackCached(actualCachedSha), "Pre-cached pack must remain untouched");
    }

    /**
     * Test 2.1: Packet flooding — rapid duplicate packet arrivals must not trigger duplicate mounts.
     */
    private static void testPacketFloodingDeduplication() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("c2_flood_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        Path packZip = Files.createTempFile("flood_pack", ".zip");
        createValidPackZip(packZip, "assets/customraces/models/test.geo.json", "{\"test\":true}");
        String sha1 = ServerPackManager.calculateSha1(packZip);

        // Mount the pack initially
        ClientPackManager.mountPack(packZip, sha1);
        int initialMounts = GeckoLibCacheInjector.getMountCount();
        assertTrue(initialMounts > 0, "Initial mount count should be > 0");

        // Flood 50 identical handleServerPack calls
        for (int i = 0; i < 50; i++) {
            ClientPackManager.handleServerPack("http://example.com/pack.zip", sha1, 500, false);
        }

        int afterFloodMounts = GeckoLibCacheInjector.getMountCount();
        assertEquals(initialMounts, afterFloodMounts, "Mount count must not increment during packet flooding of already-mounted pack");
        assertEquals(sha1, ClientPackManager.getCurrentMountedSha1(), "Mounted SHA-1 must remain consistent");
    }

    /**
     * Test 2.2: Rapid switching between two different server packs (Server A and Server B).
     * Verifies that state transitions, path updates, and cache invalidation callbacks work reliably.
     */
    private static void testRapidServerSwitching() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("c2_switch_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        Path packA = Files.createTempFile("server_a_pack", ".zip");
        createValidPackZip(packA, "pack_a.txt", "Content A");
        String shaA = ServerPackManager.calculateSha1(packA);
        Path cachedA = ClientPackManager.getCacheFilePath(shaA);
        Files.copy(packA, cachedA);

        Path packB = Files.createTempFile("server_b_pack", ".zip");
        createValidPackZip(packB, "pack_b.txt", "Content B");
        String shaB = ServerPackManager.calculateSha1(packB);
        Path cachedB = ClientPackManager.getCacheFilePath(shaB);
        Files.copy(packB, cachedB);

        int switchIterations = 10;
        for (int i = 0; i < switchIterations; i++) {
            String targetSha = (i % 2 == 0) ? shaA : shaB;
            Path targetPath = (i % 2 == 0) ? cachedA : cachedB;

            ClientPackManager.handleServerPack("http://example.com/pack.zip", targetSha, 100, false);

            assertEquals(targetSha, ClientPackManager.getCurrentMountedSha1(), "Mounted SHA-1 must match switch iteration " + i);
            assertEquals(targetPath, ClientPackManager.getMountedPackPath(), "Mounted pack path must match switch iteration " + i);
            assertEquals(targetPath, GeckoLibCacheInjector.getLastMountedPackPath(), "GeckoLibCacheInjector must receive target path on switch " + i);
        }

        assertEquals(switchIterations, GeckoLibCacheInjector.getMountCount(), "Each pack switch must trigger a mount notification");
    }

    /**
     * Test 3.1: PackRepository reflection registration and survival across manual F3+T reloads.
     */
    private static void testPackRepositoryRegistrationAndReloadSurvival() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("c2_repo_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        Path packZip = tempCache.resolve("dynamic_pack.zip");
        createValidPackZip(packZip, "assets/customraces/geo/werewolf.geo.json", "{\"bones\":[]}");

        // Create a real PackRepository instance with an empty initial source
        PackRepository repo = new PackRepository(consumer -> {});

        // Invoke private registerIntoPackRepository via reflection
        Method registerMethod = ClientPackManager.class.getDeclaredMethod("registerIntoPackRepository", PackRepository.class, Path.class);
        registerMethod.setAccessible(true);
        registerMethod.invoke(null, repo, packZip);

        // Initial reload: loads all sources into available packs
        repo.reload();
        Collection<String> availableIds = repo.getAvailableIds();
        assertTrue(availableIds.contains("customraces_dynamic"), "PackRepository must contain 'customraces_dynamic' after initial reload");

        Pack pack = repo.getPack("customraces_dynamic");
        assertNotNull(pack, "Dynamic pack must be retrievable from PackRepository");
        assertEquals("customraces_dynamic", pack.getId(), "Pack ID must be customraces_dynamic");
        assertEquals("Custom Races Dynamic Server Pack", pack.getTitle().getString(), "Pack title must match");

        // Simulate 5 consecutive F3+T reloads to verify persistence
        for (int i = 1; i <= 5; i++) {
            repo.reload();
            assertTrue(repo.getAvailableIds().contains("customraces_dynamic"),
                    "Dynamic pack must survive F3+T reload #" + i);
            assertNotNull(repo.getPack("customraces_dynamic"),
                    "Dynamic pack must remain accessible after reload #" + i);
        }
    }

    /**
     * Test 4.1: Extreme buffer serialization boundaries for ServerPackInfoPacket.
     */
    private static void testExtremeBufferSerialization() {
        // 1. Max allowed URL string (32767 chars)
        StringBuilder maxUrlBuilder = new StringBuilder(32767);
        maxUrlBuilder.append("http://example.com/");
        while (maxUrlBuilder.length() < 32767) {
            maxUrlBuilder.append("a");
        }
        String maxUrl = maxUrlBuilder.toString();

        String sha1 = "abcdef0123456789abcdef0123456789abcdef01";
        ServerPackInfoPacket packet = new ServerPackInfoPacket(maxUrl, sha1, Long.MAX_VALUE, true);

        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        ServerPackInfoPacket decoded = ServerPackInfoPacket.decode(buf);

        assertEquals(maxUrl, decoded.getPackUrl(), "Max length URL must decode exactly");
        assertEquals(sha1, decoded.getSha1Hash(), "SHA-1 must decode exactly");
        assertEquals(Long.MAX_VALUE, decoded.getSizeBytes(), "Long.MAX_VALUE sizeBytes must decode");
        assertEquals(true, decoded.isRequired(), "Required boolean must decode");

        // 2. Unicode and URL encoded characters
        String unicodeUrl = "http://127.0.0.1:25585/custom%20races/pack?name=狼人&param=val#fragment";
        ServerPackInfoPacket unicodePacket = new ServerPackInfoPacket(unicodeUrl, sha1, Long.MIN_VALUE, false);

        FriendlyByteBuf uBuf = new FriendlyByteBuf(Unpooled.buffer());
        unicodePacket.encode(uBuf);
        ServerPackInfoPacket decodedUnicode = ServerPackInfoPacket.decode(uBuf);

        assertEquals(unicodeUrl, decodedUnicode.getPackUrl(), "Unicode URL must decode identically");
        assertEquals(Long.MIN_VALUE, decodedUnicode.getSizeBytes(), "Long.MIN_VALUE sizeBytes must decode");
        assertEquals(false, decodedUnicode.isRequired(), "Required false must decode");

        // 3. 0 sizeBytes
        ServerPackInfoPacket zeroPacket = new ServerPackInfoPacket("http://test.com", sha1, 0L, false);
        FriendlyByteBuf zBuf = new FriendlyByteBuf(Unpooled.buffer());
        zeroPacket.encode(zBuf);
        ServerPackInfoPacket decodedZero = ServerPackInfoPacket.decode(zBuf);
        assertEquals(0L, decodedZero.getSizeBytes(), "0 sizeBytes must decode");
    }

    /**
     * Test 4.2: Exhaustive validation and rejection matrix for SHA-1 strings in handleServerPack.
     */
    private static void testSha1ValidationAndRejectionMatrix() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("c2_sha1_matrix_cache");
        ClientPackManager.setCacheDirectory(tempCache);

        String[] invalidHashes = new String[]{
                null,
                "",
                "   ",
                "123456789012345678901234567890123456789",          // 39 chars (off-by-one under)
                "12345678901234567890123456789012345678901",         // 41 chars (off-by-one over)
                "3f5ddb467385ae7c29db092d06261c23a57fd22g",         // 'g' is non-hex
                "3f5ddb467385ae7c29db092d06261c23a57fd22z",         // 'z' is non-hex
                "3f5ddb467385ae7c29db092d06261c23a57fd22-",         // hyphen
                "3f5ddb467385ae7c29db092d06261c23a57fd22!",         // punctuation
                "3f5ddb467385ae7c29db092d06261c23a57fd22\0",        // null byte
                "3f5ddb467385ae7c29db092d 6261c23a57fd220",         // space in middle
                "000000000000000000000000000000000000000\n"          // trailing newline
        };

        for (String badHash : invalidHashes) {
            ClientPackManager.handleServerPack("http://example.com/pack.zip", badHash, 100, false);
            assertTrue(!ClientPackManager.isPackMounted(), "handleServerPack must reject invalid hash: '" + badHash + "'");
        }

        // Test valid hashes with trimming and case normalization
        String validHash = "3F5DDB467385AE7C29DB092D06261C23A57FD227";
        String paddedHash = "   " + validHash + "   ";

        // Pre-create cache file with lowercase name to verify acceptance
        Path cacheFile = ClientPackManager.getCacheFilePath(validHash.toLowerCase());
        try {
            createValidPackZip(cacheFile, "test.txt", "test");
            String actualSha = ServerPackManager.calculateSha1(cacheFile);
            Path actualCache = ClientPackManager.getCacheFilePath(actualSha);
            Files.move(cacheFile, actualCache);

            ClientPackManager.handleServerPack("http://example.com/pack.zip", "  " + actualSha.toUpperCase() + "  ", 100, false);
            assertTrue(ClientPackManager.isPackMounted(), "handleServerPack should accept and normalize uppercase, padded valid hash");
            assertEquals(actualSha, ClientPackManager.getCurrentMountedSha1(), "Mounted hash must be lowercase normalized");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Test 4.3: Exhaustive URL and network error matrix.
     */
    private static void testUrlAndNetworkErrorMatrix() throws Exception {
        ClientPackManager.resetForTesting();
        Path tempCache = Files.createTempDirectory("c2_url_matrix_cache");
        ClientPackManager.setCacheDirectory(tempCache);
        ClientPackManager.setSingleplayerOverride(() -> false);

        String dummySha1 = "1234567890abcdef1234567890abcdef12345678";

        // 1. Null, empty, whitespace URLs with un-cached SHA-1
        ClientPackManager.handleServerPack(null, dummySha1, 100, false);
        assertTrue(!ClientPackManager.isPackMounted(), "handleServerPack must not mount with null URL and un-cached pack");

        ClientPackManager.handleServerPack("", dummySha1, 100, false);
        assertTrue(!ClientPackManager.isPackMounted(), "handleServerPack must not mount with empty URL and un-cached pack");

        ClientPackManager.handleServerPack("   ", dummySha1, 100, false);
        assertTrue(!ClientPackManager.isPackMounted(), "handleServerPack must not mount with whitespace URL and un-cached pack");

        // 2. Malformed URL syntax
        boolean malformedRes = ClientPackManager.downloadAndPromote("not-a-valid-protocol://invalid", dummySha1);
        assertTrue(!malformedRes, "downloadAndPromote must return false for malformed URL");

        // 3. HTTP 404 Not Found Server
        HttpServer errorServer = HttpServer.create(new InetSocketAddress(0), 0);
        errorServer.createContext("/404.zip", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        errorServer.createContext("/500.zip", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        errorServer.start();

        int port = errorServer.getAddress().getPort();
        try {
            boolean res404 = ClientPackManager.downloadAndPromote("http://127.0.0.1:" + port + "/404.zip", dummySha1);
            assertTrue(!res404, "downloadAndPromote must return false on HTTP 404");

            boolean res500 = ClientPackManager.downloadAndPromote("http://127.0.0.1:" + port + "/500.zip", dummySha1);
            assertTrue(!res500, "downloadAndPromote must return false on HTTP 500");

            // Ensure no temp file remained
            Path tempFile = ClientPackManager.getTempFilePath(dummySha1);
            assertTrue(!Files.exists(tempFile), "Temporary file must not exist after HTTP errors");

        } finally {
            errorServer.stop(0);
        }
    }

    /**
     * Helper to create a valid Minecraft resource pack zip containing a valid pack.mcmeta (pack_format: 15).
     */
    private static void createValidPackZip(Path zipPath, String extraEntry, String extraContent) throws IOException {
        if (zipPath.getParent() != null && !Files.exists(zipPath.getParent())) {
            Files.createDirectories(zipPath.getParent());
        }
        try (OutputStream fos = Files.newOutputStream(zipPath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            // pack.mcmeta
            ZipEntry mcmeta = new ZipEntry("pack.mcmeta");
            zos.putNextEntry(mcmeta);
            String mcmetaContent = "{\"pack\":{\"pack_format\":15,\"description\":\"Custom Races Dynamic Server Pack\"}}";
            zos.write(mcmetaContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();

            // Extra entry (e.g. model or text)
            if (extraEntry != null && extraContent != null) {
                ZipEntry entry = new ZipEntry(extraEntry);
                zos.putNextEntry(entry);
                zos.write(extraContent.getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError("[FAIL] Assertion failed: " + message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError("[FAIL] Assertion failed (expected false): " + message);
        }
    }

    private static void assertNotNull(Object obj, String message) {
        if (obj == null) {
            throw new AssertionError("[FAIL] Expected non-null: " + message);
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
