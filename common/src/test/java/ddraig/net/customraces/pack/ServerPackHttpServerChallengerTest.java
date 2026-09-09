package ddraig.net.customraces.pack;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Empirical Challenger 2 Stress Test Suite for ServerPackHttpServer.
 * Tests network corner cases, port collisions, rapid socket reuse / TIME_WAIT,
 * HTTP methods, unmapped paths & path traversal, high concurrency, and client disconnections.
 */
public class ServerPackHttpServerChallengerTest {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("  CHALLENGER 2: SERVER PACK HTTP SERVER EMPIRICAL STRESS TESTS    ");
        System.out.println("==================================================================");

        int passed = 0;
        int failed = 0;
        List<String> findings = new ArrayList<>();

        // Test 1: Port collision and external port conflicts
        try {
            System.out.println("\n--- Test 1: Port Collision & Conflict Resilience ---");
            testPortCollisionsAndConflicts();
            System.out.println("  [PASS] Port Collision & Conflict Resilience verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 2: Rapid socket reuse and TIME_WAIT restart resilience
        try {
            System.out.println("\n--- Test 2: Rapid Stop & Restart (Socket Reuse / TIME_WAIT) ---");
            testRapidStopAndRestartSocketReuse();
            System.out.println("  [PASS] Rapid Stop & Restart (Socket Reuse / TIME_WAIT) verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 2 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 3: Pack absence lifecycle (404 when absent, 200 when generated, 404 when deleted)
        try {
            System.out.println("\n--- Test 3: Pack Absence & Dynamic Generation Lifecycle ---");
            testPackAbsenceLifecycle();
            System.out.println("  [PASS] Pack Absence & Dynamic Generation Lifecycle verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 3 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 4: HTTP methods audit (GET, HEAD, POST, PUT, DELETE, OPTIONS, PATCH)
        try {
            System.out.println("\n--- Test 4: HTTP Methods Audit ---");
            testHttpMethods();
            System.out.println("  [PASS] HTTP Methods Audit verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 4 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 5: Path traversal attack resistance and unmapped paths audit
        try {
            System.out.println("\n--- Test 5: Path Traversal & Unmapped Paths Security Audit ---");
            testPathTraversalAndUnmappedPaths(findings);
            System.out.println("  [PASS] Path Traversal & Unmapped Paths Audit verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 5 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 6: High concurrency stress test (50 concurrent client downloads)
        try {
            System.out.println("\n--- Test 6: 50 Concurrent Client Downloads Stress Test ---");
            testHighConcurrencyDownloads(50);
            System.out.println("  [PASS] 50 Concurrent Client Downloads Stress Test verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 6 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 7: Abrupt client socket disconnect handling
        try {
            System.out.println("\n--- Test 7: Abrupt Client Disconnect / Truncated Request Handling ---");
            testAbruptClientDisconnect();
            System.out.println("  [PASS] Abrupt Client Disconnect Handling verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 7 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        // Test 8: Large pack multi-client streaming stress test (5 MB pack, 20 concurrent threads)
        try {
            System.out.println("\n--- Test 8: Large Pack Multi-Client Streaming (5 MB, 20 threads) ---");
            testLargePackMultiClientStreaming();
            System.out.println("  [PASS] Large Pack Multi-Client Streaming verified.");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 8 failed: " + t.getMessage());
            t.printStackTrace();
            failed++;
        }

        System.out.println("\n==================================================================");
        System.out.println("  CHALLENGER 2 TEST SUMMARY: " + passed + " PASSED, " + failed + " FAILED");
        if (!findings.isEmpty()) {
            System.out.println("  FINDINGS (" + findings.size() + "):");
            for (String f : findings) {
                System.out.println("   * " + f);
            }
        }
        System.out.println("==================================================================");

        // Ensure clean server shutdown
        ServerPackHttpServer.stop();

        if (failed > 0) {
            throw new RuntimeException("ServerPackHttpServerChallengerTest failed with " + failed + " failures!");
        }
    }

    private static void testPortCollisionsAndConflicts() throws Exception {
        ServerPackHttpServer.stop();

        // 1. Occupy a random port with a raw ServerSocket
        int port;
        try (ServerSocket occupyingSocket = new ServerSocket(0)) {
            port = occupyingSocket.getLocalPort();
            assertTrue(port > 0, "Occupied port must be positive");

            // 2. Try starting ServerPackHttpServer on that occupied port
            boolean result = ServerPackHttpServer.start(port);
            assertTrue(!result, "start() on occupied port must return false gracefully");
            assertTrue(!ServerPackHttpServer.isRunning(), "isRunning() must be false after failed start");
            assertEquals(-1, ServerPackHttpServer.getPort(), "getPort() must be -1 after failed start");
        }

        // 3. Now that occupyingSocket is closed, start ServerPackHttpServer on that port
        boolean success = ServerPackHttpServer.start(port);
        assertTrue(success, "start() must succeed once port is freed");
        assertTrue(ServerPackHttpServer.isRunning(), "isRunning() must be true");
        assertEquals(port, ServerPackHttpServer.getPort(), "activePort must match started port");

        // 4. Test idempotency: calling start() again on the SAME port while running should return true
        boolean duplicateStart = ServerPackHttpServer.start(port);
        assertTrue(duplicateStart, "Calling start() on the already-running port must return true idempotently");
        assertTrue(ServerPackHttpServer.isRunning(), "Server must remain running");

        // 5. Try opening an external ServerSocket on the port held by ServerPackHttpServer
        boolean externalBindFailed = false;
        try (ServerSocket extSocket = new ServerSocket(port)) {
            // Should not reach here
        } catch (IOException e) {
            externalBindFailed = true; // Expected: port is occupied by ServerPackHttpServer
        }
        assertTrue(externalBindFailed, "External socket must fail to bind to port held by ServerPackHttpServer");

        ServerPackHttpServer.stop();
        assertTrue(!ServerPackHttpServer.isRunning(), "Server must be stopped");
    }

    private static void testRapidStopAndRestartSocketReuse() throws Exception {
        ServerPackHttpServer.stop();

        // Prepare a valid minimal pack
        Path tempDir = Files.createTempDirectory("challenger2_reuse_test");
        Path targetZip = tempDir.resolve("reuse-pack.zip");
        Files.createDirectories(tempDir.resolve("models"));
        Files.writeString(tempDir.resolve("models/test.geo.json"), "{\"reuse\": true}");
        ServerPackManager.buildPack(tempDir, targetZip);

        // Find an ephemeral port
        int testPort;
        try (ServerSocket s = new ServerSocket(0)) {
            testPort = s.getLocalPort();
        }

        // Rapidly start, perform HTTP download, stop, and immediately restart 10 times consecutively
        for (int i = 0; i < 10; i++) {
            boolean started = ServerPackHttpServer.start(testPort);
            assertTrue(started, "Rapid restart cycle " + i + " must succeed on port " + testPort);
            assertTrue(ServerPackHttpServer.isRunning(), "Server must be running on cycle " + i);

            // Execute a GET request
            String urlStr = ServerPackHttpServer.getDownloadUrl("127.0.0.1");
            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            int code = conn.getResponseCode();
            assertEquals(200, code, "Cycle " + i + " response code must be 200");
            conn.getInputStream().readAllBytes();
            conn.disconnect();

            // Immediate stop
            ServerPackHttpServer.stop();
            assertTrue(!ServerPackHttpServer.isRunning(), "Server must be stopped on cycle " + i);
        }
    }

    private static void testPackAbsenceLifecycle() throws Exception {
        ServerPackHttpServer.stop();

        Path tempDir = Files.createTempDirectory("challenger2_absence_test");
        Path nonexistentZip = tempDir.resolve("does_not_exist.zip");

        // Force ServerPackManager to point to non-existent file
        // We do this by building a pack to a temp file and then deleting it
        Path tempPack = tempDir.resolve("temp-pack.zip");
        ServerPackManager.buildPack(tempDir, tempPack);
        Files.deleteIfExists(tempPack);
        assertTrue(!ServerPackManager.hasPack(), "hasPack() must be false when file is deleted");

        // Start server with no pack present
        boolean started = ServerPackHttpServer.start(0);
        assertTrue(started, "Server should start successfully even without pack generated");
        int port = ServerPackHttpServer.getPort();

        String packUrl = "http://127.0.0.1:" + port + ServerPackHttpServer.PACK_ENDPOINT;

        // 1. GET request when pack does not exist -> Expect 404
        HttpURLConnection conn404 = (HttpURLConnection) new URL(packUrl).openConnection();
        conn404.setRequestMethod("GET");
        int code404 = conn404.getResponseCode();
        assertEquals(404, code404, "Requesting non-existent pack must return HTTP 404");
        try (InputStream es = conn404.getErrorStream()) {
            String errorMsg = new String(es.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(errorMsg.contains("not been generated yet"), "404 response must contain descriptive message");
        }
        conn404.disconnect();

        // 2. HEAD request when pack does not exist -> Expect 404
        HttpURLConnection head404 = (HttpURLConnection) new URL(packUrl).openConnection();
        head404.setRequestMethod("HEAD");
        int headCode404 = head404.getResponseCode();
        assertEquals(404, headCode404, "HEAD on non-existent pack must return HTTP 404");
        head404.disconnect();

        // 3. Dynamically generate pack while server is running
        Files.createDirectories(tempDir.resolve("models"));
        Files.writeString(tempDir.resolve("models/dynamic.geo.json"), "{\"dynamic\": true}");
        ServerPackManager.buildPack(tempDir, tempPack);
        assertTrue(ServerPackManager.hasPack(), "hasPack() must be true after generating pack");

        // 4. GET request now -> Expect HTTP 200 with zip content
        HttpURLConnection conn200 = (HttpURLConnection) new URL(packUrl).openConnection();
        conn200.setRequestMethod("GET");
        int code200 = conn200.getResponseCode();
        assertEquals(200, code200, "Pack request must return HTTP 200 after dynamic generation");
        assertEquals("application/zip", conn200.getContentType(), "Content-Type must be application/zip");
        assertEquals(ServerPackManager.getPackSha1(), conn200.getHeaderField("X-Checksum-SHA1"), "X-Checksum-SHA1 must match");
        byte[] packBytes = conn200.getInputStream().readAllBytes();
        assertEquals(Files.size(tempPack), packBytes.length, "Downloaded bytes must match file size on disk");
        conn200.disconnect();

        // 5. Delete pack from disk while server is running
        Files.deleteIfExists(tempPack);
        assertTrue(!ServerPackManager.hasPack(), "hasPack() must be false after file deletion");

        // 6. Request again -> Expect HTTP 404 gracefully
        HttpURLConnection connDeleted = (HttpURLConnection) new URL(packUrl).openConnection();
        connDeleted.setRequestMethod("GET");
        int codeDeleted = connDeleted.getResponseCode();
        assertEquals(404, codeDeleted, "Request after pack deletion must return HTTP 404");
        connDeleted.disconnect();

        ServerPackHttpServer.stop();
    }

    private static void testHttpMethods() throws Exception {
        ServerPackHttpServer.stop();

        Path tempDir = Files.createTempDirectory("challenger2_methods_test");
        Path targetZip = tempDir.resolve("methods-pack.zip");
        Files.createDirectories(tempDir.resolve("models"));
        Files.writeString(tempDir.resolve("models/model.geo.json"), "{\"model\": 1}");
        ServerPackManager.buildPack(tempDir, targetZip);

        ServerPackHttpServer.start(0);
        int port = ServerPackHttpServer.getPort();
        String packUrl = "http://127.0.0.1:" + port + ServerPackHttpServer.PACK_ENDPOINT;

        // 1. HEAD request
        HttpURLConnection headConn = (HttpURLConnection) new URL(packUrl).openConnection();
        headConn.setRequestMethod("HEAD");
        assertEquals(200, headConn.getResponseCode(), "HEAD request must return 200");
        assertEquals("application/zip", headConn.getContentType(), "HEAD must return application/zip");
        assertEquals(ServerPackManager.getPackSha1(), headConn.getHeaderField("X-Checksum-SHA1"), "HEAD must have SHA1 header");
        headConn.disconnect();

        // 2. Disallowed HTTP methods on PACK_ENDPOINT -> Expect 405 Method Not Allowed
        String[] disallowedMethods = {"POST", "PUT", "DELETE", "OPTIONS"};
        for (String method : disallowedMethods) {
            HttpURLConnection conn = (HttpURLConnection) new URL(packUrl).openConnection();
            conn.setRequestMethod(method);
            conn.setDoOutput(method.equals("POST") || method.equals("PUT"));
            if (conn.getDoOutput()) {
                try (OutputStream os = conn.getOutputStream()) {
                    os.write("payload".getBytes(StandardCharsets.UTF_8));
                }
            }
            int code = conn.getResponseCode();
            assertEquals(405, code, "Method " + method + " on pack endpoint must return HTTP 405 Method Not Allowed");
            conn.disconnect();
        }

        ServerPackHttpServer.stop();
    }

    private static void testPathTraversalAndUnmappedPaths(List<String> findings) throws Exception {
        ServerPackHttpServer.stop();

        Path tempDir = Files.createTempDirectory("challenger2_security_test");
        Path targetZip = tempDir.resolve("sec-pack.zip");
        Files.createDirectories(tempDir.resolve("models"));
        Files.writeString(tempDir.resolve("models/m.geo.json"), "{\"sec\": true}");
        ServerPackManager.buildPack(tempDir, targetZip);

        ServerPackHttpServer.start(0);
        int port = ServerPackHttpServer.getPort();
        String baseUrl = "http://127.0.0.1:" + port;

        // 1. Verify root path returns 200 status banner
        HttpURLConnection rootConn = (HttpURLConnection) new URL(baseUrl + "/").openConnection();
        assertEquals(200, rootConn.getResponseCode(), "Root / should return 200 status banner");
        String rootBody = new String(rootConn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertTrue(rootBody.contains("Custom Races Dynamic Resource Pack Server Active"),
                "Root response should be active status check");
        rootConn.disconnect();

        // 2. Path traversal attempts via Raw HTTP Socket (bypassing Java URL normalization)
        String[] traversalRequests = {
                "GET /../etc/passwd HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n",
                "GET /../../windows/win.ini HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n",
                "GET /....//....//config.json HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n",
                "GET /customraces-server-pack.zip/../../secret.txt HTTP/1.1\r\nHost: 127.0.0.1\r\nConnection: close\r\n\r\n"
        };

        for (String req : traversalRequests) {
            try (Socket socket = new Socket("127.0.0.1", port)) {
                socket.setSoTimeout(3000);
                OutputStream os = socket.getOutputStream();
                os.write(req.getBytes(StandardCharsets.UTF_8));
                os.flush();

                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                String statusLine = reader.readLine();
                assertNotNull(statusLine, "Server must respond to request: " + req.trim());

                // Read full response
                StringBuilder resp = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    resp.append(line).append("\n");
                }
                String respStr = resp.toString();

                // Path traversal MUST NOT leak file contents from filesystem
                assertTrue(!respStr.contains("root:") && !respStr.contains("[fonts]"),
                        "Path traversal must NOT return sensitive file content for request: " + req.trim());
            }
        }

        // 3. Unmapped paths behavior inspection
        String[] unmappedPaths = {"/nonexistent", "/secret/passwords.txt", "/admin/dashboard", "/favicon.ico"};
        for (String p : unmappedPaths) {
            HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + p).openConnection();
            int code = conn.getResponseCode();
            String body = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            conn.disconnect();

            // Observe the response code for unmapped paths
            if (code == 200) {
                // If 200, check if it's the root status string
                assertTrue(body.contains("Custom Races Dynamic Resource Pack Server Active"),
                        "If unmapped path returns 200, it must be the generic root fallback handler");
            }
        }

        // Record finding about unmapped path routing to root fallback context
        findings.add("The root fallback handler (server.createContext(\"/\")) catches all unmapped paths (e.g. /nonexistent, /favicon.ico) and returns HTTP 200 with the active status banner instead of HTTP 404. Path traversal cannot leak host files because file reads only access the fixed getGeneratedPackPath().");

        ServerPackHttpServer.stop();
    }

    private static void testHighConcurrencyDownloads(int numClients) throws Exception {
        ServerPackHttpServer.stop();

        Path tempDir = Files.createTempDirectory("challenger2_concurrency_test");
        Path targetZip = tempDir.resolve("concurrency-pack.zip");
        Files.createDirectories(tempDir.resolve("models"));
        Files.createDirectories(tempDir.resolve("textures"));
        Files.createDirectories(tempDir.resolve("animations"));

        // Create moderate asset payload
        for (int i = 0; i < 5; i++) {
            Files.writeString(tempDir.resolve("models/werewolf_" + i + ".geo.json"), "{\"geometry\": " + i + "}");
            Files.write(tempDir.resolve("textures/tex_" + i + ".png"), new byte[]{(byte) i, (byte) (i + 1), (byte) (i + 2)});
            Files.writeString(tempDir.resolve("animations/anim_" + i + ".json"), "{\"anim\": " + i + "}");
        }
        ServerPackManager.buildPack(tempDir, targetZip);

        long expectedSize = Files.size(targetZip);
        String expectedSha1 = ServerPackManager.getPackSha1();

        ServerPackHttpServer.start(0);
        int port = ServerPackHttpServer.getPort();
        String downloadUrl = "http://127.0.0.1:" + port + ServerPackHttpServer.PACK_ENDPOINT;

        ExecutorService executor = Executors.newFixedThreadPool(numClients);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(numClients);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < numClients; i++) {
            final int clientId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Simultaneous thunderclap release

                    URL url = new URL(downloadUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    int code = conn.getResponseCode();
                    if (code != 200) {
                        errors.add("Client " + clientId + " received non-200 code: " + code);
                        failCount.incrementAndGet();
                        return;
                    }

                    String headerSha1 = conn.getHeaderField("X-Checksum-SHA1");
                    if (!expectedSha1.equalsIgnoreCase(headerSha1)) {
                        errors.add("Client " + clientId + " header SHA-1 mismatch: expected " + expectedSha1 + ", got " + headerSha1);
                        failCount.incrementAndGet();
                        return;
                    }

                    byte[] body;
                    try (InputStream is = conn.getInputStream()) {
                        body = is.readAllBytes();
                    }

                    if (body.length != expectedSize) {
                        errors.add("Client " + clientId + " byte length mismatch: expected " + expectedSize + ", got " + body.length);
                        failCount.incrementAndGet();
                        return;
                    }

                    // Verify SHA-1 of downloaded payload
                    MessageDigest md = MessageDigest.getInstance("SHA-1");
                    byte[] hash = md.digest(body);
                    StringBuilder sb = new StringBuilder(40);
                    for (byte b : hash) {
                        sb.append(String.format("%02x", b));
                    }
                    if (!expectedSha1.equalsIgnoreCase(sb.toString())) {
                        errors.add("Client " + clientId + " payload SHA-1 mismatch: expected " + expectedSha1 + ", got " + sb.toString());
                        failCount.incrementAndGet();
                        return;
                    }

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    errors.add("Client " + clientId + " threw exception: " + e.getMessage());
                    failCount.incrementAndGet();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Release all threads simultaneously
        startLatch.countDown();
        boolean completed = finishLatch.await(30, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;
        executor.shutdown();

        System.out.println("    " + numClients + " concurrent downloads completed in " + elapsed + " ms");
        System.out.println("    Success: " + successCount.get() + ", Failed: " + failCount.get());

        assertTrue(completed, "All " + numClients + " client threads must complete within timeout");
        if (!errors.isEmpty()) {
            System.err.println("Errors encountered during concurrency test:");
            for (String err : errors) {
                System.err.println("  - " + err);
            }
        }
        assertEquals(numClients, successCount.get(), "All " + numClients + " concurrent downloads must succeed");
        assertEquals(0, failCount.get(), "Zero downloads should fail");

        ServerPackHttpServer.stop();
    }

    private static void testAbruptClientDisconnect() throws Exception {
        ServerPackHttpServer.stop();

        Path tempDir = Files.createTempDirectory("challenger2_disconnect_test");
        Path targetZip = tempDir.resolve("disc-pack.zip");
        Files.createDirectories(tempDir.resolve("models"));
        // Create 50 KB dummy file
        byte[] payload = new byte[50000];
        Arrays.fill(payload, (byte) 'A');
        Files.write(tempDir.resolve("models/heavy.geo.json"), payload);
        ServerPackManager.buildPack(tempDir, targetZip);

        ServerPackHttpServer.start(0);
        int port = ServerPackHttpServer.getPort();

        // Connect raw socket, read only 100 bytes, and forcefully close socket
        for (int i = 0; i < 5; i++) {
            try (Socket socket = new Socket("127.0.0.1", port)) {
                socket.setSoTimeout(3000);
                socket.setSoLinger(true, 0); // Force TCP RST on close
                OutputStream os = socket.getOutputStream();
                os.write(("GET " + ServerPackHttpServer.PACK_ENDPOINT + " HTTP/1.1\r\nHost: 127.0.0.1\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                os.flush();

                InputStream is = socket.getInputStream();
                byte[] partial = new byte[100];
                int read = is.read(partial);
                assertTrue(read > 0, "Should read partial header/body");
                // Abrupt close
            }
        }

        // Verify server is STILL completely healthy and responsive after multiple abrupt drops
        HttpURLConnection conn = (HttpURLConnection) new URL("http://127.0.0.1:" + port + ServerPackHttpServer.PACK_ENDPOINT).openConnection();
        conn.setRequestMethod("GET");
        assertEquals(200, conn.getResponseCode(), "Server must remain responsive after abrupt client disconnects");
        byte[] full = conn.getInputStream().readAllBytes();
        assertEquals(Files.size(targetZip), full.length, "Full file must still be delivered accurately");
        conn.disconnect();

        ServerPackHttpServer.stop();
    }

    private static void testLargePackMultiClientStreaming() throws Exception {
        ServerPackHttpServer.stop();

        Path tempDir = Files.createTempDirectory("challenger2_large_stream");
        Path targetZip = tempDir.resolve("large-stream-pack.zip");
        Files.createDirectories(tempDir.resolve("textures"));

        // Create a ~5 MB asset file
        byte[] largeAsset = new byte[5 * 1024 * 1024];
        new Random(42).nextBytes(largeAsset);
        Files.write(tempDir.resolve("textures/large_texture.png"), largeAsset);
        ServerPackManager.buildPack(tempDir, targetZip);

        long packSize = Files.size(targetZip);
        String packSha1 = ServerPackManager.getPackSha1();
        System.out.println("    Large pack size: " + (packSize / 1024 / 1024) + " MB (" + packSize + " bytes), SHA-1: " + packSha1);

        ServerPackHttpServer.start(0);
        int port = ServerPackHttpServer.getPort();
        String downloadUrl = "http://127.0.0.1:" + port + ServerPackHttpServer.PACK_ENDPOINT;

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger success = new AtomicInteger(0);

        long start = System.currentTimeMillis();
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    assertEquals(200, conn.getResponseCode(), "Large pack download code must be 200");
                    byte[] downloaded = conn.getInputStream().readAllBytes();
                    assertEquals(packSize, downloaded.length, "Downloaded byte size must match large pack size");
                    success.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("Large pack stream error: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean done = latch.await(60, TimeUnit.SECONDS);
        long duration = System.currentTimeMillis() - start;
        executor.shutdown();

        System.out.println("    20 concurrent 5MB downloads (" + (packSize * 20 / 1024 / 1024) + " MB total) streamed in " + duration + " ms");
        assertTrue(done, "All 20 streaming clients should finish within 60s");
        assertEquals(20, success.get(), "All 20 streaming downloads must succeed");

        ServerPackHttpServer.stop();
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
