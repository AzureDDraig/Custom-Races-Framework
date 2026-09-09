package ddraig.net.customraces.pack;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import ddraig.net.customraces.data.RaceRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Embedded lightweight HTTP server for streaming the server-side dynamic resource pack
 * to connecting clients.
 * Uses Java 17's built-in {@link HttpServer} with zero external dependencies.
 */
public class ServerPackHttpServer {

    public static final int DEFAULT_PORT = 25585;
    public static final String PACK_ENDPOINT = "/customraces-server-pack.zip";

    private static HttpServer server = null;
    private static ExecutorService executor = null;
    private static int activePort = -1;
    private static boolean running = false;

    /**
     * Starts the HTTP server on the configured port.
     *
     * @param port the TCP port to bind to (use 0 for ephemeral port)
     * @return true if started successfully, false if port bind failed
     */
    public static synchronized boolean start(int port) {
        if (running && server != null && activePort == port && port != 0) {
            System.out.println("[CustomRaces] ServerPackHttpServer is already running on port " + activePort);
            return true;
        }

        stop();

        try {
            InetSocketAddress address = new InetSocketAddress(port);
            server = HttpServer.create(address, 0);

            // Handler for /customraces-server-pack.zip
            server.createContext(PACK_ENDPOINT, new PackHttpHandler());

            // Fallback handler for root status check
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if (PACK_ENDPOINT.equals(path)) {
                    return;
                }
                byte[] response = "Custom Races Dynamic Resource Pack Server Active\n".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
                exchange.close();
            });

            // Daemon thread pool so HTTP server threads don't prevent JVM termination
            executor = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "CustomRaces-HttpServer-Worker");
                t.setDaemon(true);
                return t;
            });
            server.setExecutor(executor);
            server.start();

            activePort = server.getAddress().getPort();
            running = true;

            System.out.println("[CustomRaces] ServerPackHttpServer started successfully on port " + activePort);
            System.out.println("[CustomRaces] Server pack URL: " + getDownloadUrl());
            return true;
        } catch (Exception e) {
            System.err.println("[CustomRaces] Failed to bind ServerPackHttpServer on port " + port + ": " + e.getMessage());
            stop();
            return false;
        }
    }

    /**
     * Alias for start(int port) matching interface contract.
     */
    public static void startServer(int port) {
        start(port);
    }

    /**
     * Stops the running HTTP server and cleans up executors.
     */
    public static synchronized void stop() {
        if (server != null) {
            try {
                server.stop(0);
            } catch (Exception e) {
                System.err.println("[CustomRaces] Error stopping HttpServer: " + e.getMessage());
            }
            server = null;
        }

        if (executor != null) {
            try {
                executor.shutdownNow();
            } catch (Exception e) {
                System.err.println("[CustomRaces] Error shutting down HttpServer executor: " + e.getMessage());
            }
            executor = null;
        }

        running = false;
        activePort = -1;
    }

    /**
     * Alias for stop() matching interface contract.
     */
    public static void stopServer() {
        stop();
    }

    public static boolean isRunning() {
        return running && server != null;
    }

    public static int getPort() {
        return activePort > 0 ? activePort : (server != null ? server.getAddress().getPort() : -1);
    }

    /**
     * Returns the full HTTP download URL for the dynamic resource pack.
     */
    public static String getDownloadUrl() {
        String host = RaceRegistry.getServerPackHost();
        return getDownloadUrl(host);
    }

    /**
     * Returns the full HTTP download URL using the given hostname or IP.
     *
     * @param host the hostname or IP address
     * @return the HTTP URL string
     */
    public static String getDownloadUrl(String host) {
        int port = getPort();
        if (port <= 0) {
            port = RaceRegistry.getServerPackPort();
        }
        String resolvedHost = (host != null && !host.trim().isEmpty()) ? host.trim() : "localhost";
        return "http://" + resolvedHost + ":" + port + PACK_ENDPOINT;
    }

    private static class PackHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String method = exchange.getRequestMethod();
                if (!"GET".equalsIgnoreCase(method) && !"HEAD".equalsIgnoreCase(method)) {
                    exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                    return;
                }

                if (!ServerPackManager.hasPack()) {
                    byte[] notFound = "Resource pack has not been generated yet.".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                    exchange.sendResponseHeaders(404, notFound.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(notFound);
                    }
                    return;
                }

                Path packPath = ServerPackManager.getGeneratedPackPath();
                long fileSize = Files.size(packPath);
                String sha1 = ServerPackManager.getPackSha1();

                exchange.getResponseHeaders().set("Content-Type", "application/zip");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + ServerPackManager.PACK_FILE_NAME + "\"");
                exchange.getResponseHeaders().set("X-Checksum-SHA1", sha1);
                exchange.getResponseHeaders().set("Cache-Control", "no-cache, no-store, must-revalidate");

                if ("HEAD".equalsIgnoreCase(method)) {
                    exchange.sendResponseHeaders(200, -1);
                } else {
                    exchange.sendResponseHeaders(200, fileSize);
                    try (OutputStream os = exchange.getResponseBody();
                         InputStream is = Files.newInputStream(packPath)) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                    }
                }
            } finally {
                exchange.close();
            }
        }
    }
}
