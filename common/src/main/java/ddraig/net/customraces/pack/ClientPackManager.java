package ddraig.net.customraces.pack;

import ddraig.net.customraces.client.render.GeckoLibCacheInjector;
import ddraig.net.customraces.client.render.WereModelRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.FallbackResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Client-side resource pack manager.
 * Handles SHA-1 content-addressed caching, singleplayer bypass, asynchronous download,
 * and dynamic in-memory injection into Minecraft's ResourceManager and PackRepository.
 */
public class ClientPackManager {

    public static final Path DEFAULT_CACHE_DIR = Paths.get("config/custom_races/cache");

    private static Path customCacheDir = null;
    private static String currentMountedSha1 = null;
    private static Path currentMountedPackPath = null;
    private static boolean packMounted = false;

    // Test overrides / bypass suppliers
    private static BooleanSupplier singleplayerOverride = null;
    private static Supplier<Path> localPackPathOverride = null;

    private static final ExecutorService DOWNLOAD_EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "CustomRaces-ClientPack-Downloader");
        t.setDaemon(true);
        return t;
    });

    /**
     * Primary entry point invoked by ClientPacketHandler when a ServerPackInfoPacket is received.
     *
     * @param packUrl   the URL to download the pack from (if network download is required)
     * @param sha1Hash  the expected SHA-1 hash of the pack (40 lowercase hex characters)
     * @param sizeBytes the byte size of the pack
     * @param required  whether the pack is required
     */
    public static synchronized void handleServerPack(String packUrl, String sha1Hash, long sizeBytes, boolean required) {
        if (sha1Hash == null || sha1Hash.trim().isEmpty()) {
            System.err.println("[CustomRaces] Received ServerPackInfoPacket with null or empty SHA-1 hash.");
            return;
        }

        String normalizedSha1 = sha1Hash.trim().toLowerCase();
        if (normalizedSha1.length() != 40 || !normalizedSha1.matches("^[0-9a-f]{40}$")) {
            System.err.println("[CustomRaces] Received invalid SHA-1 hash format: " + sha1Hash);
            return;
        }

        // 1. Check if already mounted with identical hash
        if (packMounted && normalizedSha1.equalsIgnoreCase(currentMountedSha1)
                && currentMountedPackPath != null && Files.exists(currentMountedPackPath)) {
            System.out.println("[CustomRaces] Pack with SHA-1 " + normalizedSha1 + " is already mounted. Skipping.");
            return;
        }

        // 2. Singleplayer / LAN Bypass: mount directly from local disk if available
        if (isSingleplayer()) {
            Path localPackPath = getLocalServerPackPath();
            if (localPackPath != null && Files.isRegularFile(localPackPath)) {
                try {
                    String localSha1 = ServerPackManager.calculateSha1(localPackPath);
                    if (normalizedSha1.equalsIgnoreCase(localSha1)) {
                        System.out.println("[CustomRaces] Singleplayer/LAN bypass: mounting local pack directly from " + localPackPath);
                        mountPack(localPackPath, normalizedSha1);
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("[CustomRaces] Failed to verify local singleplayer pack SHA-1: " + e.getMessage());
                }
            }
        }

        // 3. Local Cache Check: skip download if valid cached file exists
        if (isPackCached(normalizedSha1)) {
            Path cachedFile = getCacheFilePath(normalizedSha1);
            System.out.println("[CustomRaces] Found valid cached pack at " + cachedFile + ". Skipping download.");
            mountPack(cachedFile, normalizedSha1);
            return;
        }

        // 4. Download from URL asynchronously
        if (packUrl == null || packUrl.trim().isEmpty()) {
            System.err.println("[CustomRaces] Cannot download pack: URL is empty or null and pack is not cached.");
            return;
        }

        System.out.println("[CustomRaces] Downloading server pack from " + packUrl + " (SHA-1: " + normalizedSha1 + ", " + sizeBytes + " bytes)...");
        downloadPackAsync(packUrl, normalizedSha1);
    }

    /**
     * Checks if a valid pack with the given SHA-1 exists in the local cache.
     * Deletes corrupted cache files if the hash does not match.
     */
    public static boolean isPackCached(String sha1Hash) {
        if (sha1Hash == null || sha1Hash.length() != 40) return false;
        Path cacheFile = getCacheFilePath(sha1Hash.toLowerCase());
        if (!Files.isRegularFile(cacheFile)) return false;

        try {
            String calculated = ServerPackManager.calculateSha1(cacheFile);
            if (sha1Hash.equalsIgnoreCase(calculated)) {
                return true;
            } else {
                System.err.println("[CustomRaces] Cache file corrupted for " + sha1Hash + " (calculated: " + calculated + "). Deleting.");
                Files.deleteIfExists(cacheFile);
                return false;
            }
        } catch (IOException e) {
            System.err.println("[CustomRaces] Error reading cached pack file: " + e.getMessage());
            return false;
        }
    }

    /**
     * Asynchronously downloads the pack from the given URL and validates the SHA-1 checksum.
     *
     * @param packUrl  the HTTP download URL
     * @param sha1Hash the expected SHA-1 checksum
     * @return CompletableFuture completing with true if download and validation succeeded
     */
    public static CompletableFuture<Boolean> downloadPackAsync(String packUrl, String sha1Hash) {
        return CompletableFuture.supplyAsync(() -> downloadAndPromote(packUrl, sha1Hash), DOWNLOAD_EXECUTOR)
                .thenApply(success -> {
                    if (Boolean.TRUE.equals(success)) {
                        Path cachedPath = getCacheFilePath(sha1Hash);
                        mountPack(cachedPath, sha1Hash);
                        return true;
                    }
                    return false;
                });
    }

    /**
     * Synchronously downloads the pack to a .tmp file, verifies SHA-1, and atomically renames.
     *
     * @param packUrl  the HTTP download URL
     * @param sha1Hash the expected SHA-1 checksum
     * @return true if download and validation succeeded
     */
    public static boolean downloadAndPromote(String packUrl, String sha1Hash) {
        if (packUrl == null || packUrl.trim().isEmpty()) {
            System.err.println("[CustomRaces] downloadAndPromote failed: invalid packUrl");
            return false;
        }

        String expectedSha1 = sha1Hash.trim().toLowerCase();
        Path cacheDir = getCacheDirectory();
        Path tempFile = getTempFilePath(expectedSha1);
        Path targetFile = getCacheFilePath(expectedSha1);

        HttpURLConnection connection = null;
        try {
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }

            Files.deleteIfExists(tempFile);

            URL url = new URL(packUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);
            connection.connect();

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                System.err.println("[CustomRaces] Failed to download pack: HTTP " + responseCode + " from " + packUrl);
                return false;
            }

            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream in = connection.getInputStream();
                 OutputStream out = Files.newOutputStream(tempFile)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    digest.update(buffer, 0, read);
                }
            }

            // Verify SHA-1
            byte[] hashBytes = digest.digest();
            StringBuilder sb = new StringBuilder(40);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            String calculatedSha1 = sb.toString();

            if (!expectedSha1.equalsIgnoreCase(calculatedSha1)) {
                System.err.println("[CustomRaces] Downloaded pack checksum mismatch! Expected: "
                        + expectedSha1 + ", calculated: " + calculatedSha1);
                Files.deleteIfExists(tempFile);
                return false;
            }

            // Atomic rename from temp to target
            Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            System.out.println("[CustomRaces] Downloaded and cached dynamic pack: " + targetFile + " (SHA-1: " + calculatedSha1 + ")");
            return true;

        } catch (Exception e) {
            System.err.println("[CustomRaces] Error downloading resource pack from " + packUrl + ": " + e.getMessage());
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ignored) {}
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Mounts the pack into Minecraft's ResourceManager and registers it into PackRepository.
     * Ensures execution on client main thread when Minecraft is running.
     *
     * @param packPath the local zip path to mount
     * @param sha1Hash the SHA-1 checksum of the pack
     */
    public static synchronized void mountPack(Path packPath, String sha1Hash) {
        currentMountedSha1 = sha1Hash;
        currentMountedPackPath = packPath;
        packMounted = true;

        Minecraft mc = null;
        try {
            mc = Minecraft.getInstance();
        } catch (Throwable ignored) {}

        if (mc == null) {
            // Headless / Unit test environment
            GeckoLibCacheInjector.onPackMounted(packPath);
            WereModelRenderer.clearCaches();
            return;
        }

        Runnable task = () -> {
            try {
                applyDynamicMount(Minecraft.getInstance(), packPath, sha1Hash);
                GeckoLibCacheInjector.onPackMounted(packPath);
                WereModelRenderer.clearCaches();
                System.out.println("[CustomRaces] Dynamic server pack mounted in-memory: " + packPath);
            } catch (Throwable t) {
                System.err.println("[CustomRaces] Error during dynamic pack mount: " + t.getMessage());
                t.printStackTrace();
            }
        };

        if (mc.isSameThread()) {
            task.run();
        } else {
            mc.execute(task);
        }
    }

    /**
     * Directly injects the pack into MultiPackResourceManager and PackRepository.
     */
    public static void applyDynamicMount(Minecraft mc, Path packPath, String sha1Hash) {
        if (mc == null || packPath == null || !Files.isRegularFile(packPath)) {
            return;
        }

        String packId = "customraces_dynamic";
        File file = packPath.toFile();
        FilePackResources packResources = new FilePackResources(packId, file, false);

        // 1. Dynamic In-Memory Mount into ResourceManager
        try {
            ResourceManager rm = mc.getResourceManager();
            MultiPackResourceManager mprm = extractMultiPackResourceManager(rm);
            if (mprm != null) {
                injectIntoMultiPackResourceManager(mprm, packResources);
            }
        } catch (Throwable t) {
            System.err.println("[CustomRaces] Failed to inject into ResourceManager: " + t.getMessage());
        }

        // 2. Register into PackRepository for persistence across manual F3+T reloads
        try {
            PackRepository repo = mc.getResourcePackRepository();
            if (repo != null) {
                registerIntoPackRepository(repo, packPath);
            }
        } catch (Throwable t) {
            System.err.println("[CustomRaces] Failed to register into PackRepository: " + t.getMessage());
        }
    }

    private static MultiPackResourceManager extractMultiPackResourceManager(ResourceManager rm) {
        if (rm instanceof MultiPackResourceManager mprm) {
            return mprm;
        }
        if (rm instanceof ReloadableResourceManager) {
            try {
                // Find field of type CloseableResourceManager or named "resources"
                for (Field f : ReloadableResourceManager.class.getDeclaredFields()) {
                    if (CloseableResourceManager.class.isAssignableFrom(f.getType())) {
                        f.setAccessible(true);
                        Object val = f.get(rm);
                        if (val instanceof MultiPackResourceManager mprm) {
                            return mprm;
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private static void injectIntoMultiPackResourceManager(MultiPackResourceManager mprm, FilePackResources packResources) {
        try {
            // Find Map<String, FallbackResourceManager> namespacedManagers
            Map<String, FallbackResourceManager> managers = null;
            for (Field f : MultiPackResourceManager.class.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(mprm);
                    if (val instanceof Map) {
                        managers = (Map<String, FallbackResourceManager>) val;
                        break;
                    }
                }
            }

            if (managers != null) {
                Set<String> namespaces = packResources.getNamespaces(PackType.CLIENT_RESOURCES);
                if (namespaces == null || namespaces.isEmpty()) {
                    namespaces = Set.of("customraces");
                }

                for (String ns : namespaces) {
                    FallbackResourceManager fallback = managers.get(ns);
                    if (fallback == null) {
                        fallback = new FallbackResourceManager(PackType.CLIENT_RESOURCES, ns);
                        managers.put(ns, fallback);
                    }
                    fallback.push(packResources);
                }
            }

            // Find List<PackResources> packs
            for (Field f : MultiPackResourceManager.class.getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    Object val = f.get(mprm);
                    if (val instanceof List) {
                        List<PackResources> list = (List<PackResources>) val;
                        if (!list.contains(packResources)) {
                            list.add(packResources);
                        }
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            System.err.println("[CustomRaces] Error injecting pack resources into MultiPackResourceManager: " + t.getMessage());
        }
    }

    private static void registerIntoPackRepository(PackRepository repo, Path packPath) {
        try {
            RepositorySource customSource = consumer -> {
                try {
                    Pack pack = Pack.readMetaAndCreate(
                            "customraces_dynamic",
                            Component.literal("Custom Races Dynamic Server Pack"),
                            true,
                            id -> new FilePackResources(id, packPath.toFile(), false),
                            PackType.CLIENT_RESOURCES,
                            Pack.Position.TOP,
                            PackSource.SERVER
                    );
                    if (pack != null) {
                        consumer.accept(pack);
                    }
                } catch (Throwable t) {
                    System.err.println("[CustomRaces] Failed to create dynamic Pack in RepositorySource: " + t.getMessage());
                }
            };

            // Inject into repo.sources
            Field sourcesField = null;
            for (Field f : PackRepository.class.getDeclaredFields()) {
                if (Set.class.isAssignableFrom(f.getType())) {
                    sourcesField = f;
                    break;
                }
            }

            if (sourcesField != null) {
                sourcesField.setAccessible(true);
                Set<RepositorySource> sources = (Set<RepositorySource>) sourcesField.get(repo);
                Set<RepositorySource> updatedSources = new HashSet<>(sources != null ? sources : Collections.emptySet());
                updatedSources.add(customSource);
                sourcesField.set(repo, Collections.unmodifiableSet(updatedSources));
            }
        } catch (Throwable t) {
            System.err.println("[CustomRaces] Error registering dynamic RepositorySource into PackRepository: " + t.getMessage());
        }
    }

    public static boolean isSingleplayer() {
        if (singleplayerOverride != null) {
            return singleplayerOverride.getAsBoolean();
        }
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null) {
                return mc.isSingleplayer();
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public static Path getLocalServerPackPath() {
        if (localPackPathOverride != null) {
            return localPackPathOverride.get();
        }
        return ServerPackManager.getGeneratedPackPath();
    }

    public static Path getCacheDirectory() {
        return customCacheDir != null ? customCacheDir : DEFAULT_CACHE_DIR;
    }

    public static void setCacheDirectory(Path dir) {
        customCacheDir = dir;
    }

    public static Path getCacheFilePath(String sha1Hash) {
        return getCacheDirectory().resolve("pack-" + sha1Hash.toLowerCase() + ".zip");
    }

    public static Path getTempFilePath(String sha1Hash) {
        return getCacheDirectory().resolve("pack-" + sha1Hash.toLowerCase() + ".zip.tmp");
    }

    public static boolean isPackMounted() {
        return packMounted;
    }

    public static String getCurrentMountedSha1() {
        return currentMountedSha1;
    }

    public static Path getMountedPackPath() {
        return currentMountedPackPath;
    }

    public static void setSingleplayerOverride(BooleanSupplier override) {
        singleplayerOverride = override;
    }

    public static void setLocalPackPathOverride(Supplier<Path> override) {
        localPackPathOverride = override;
    }

    public static synchronized void resetForTesting() {
        customCacheDir = null;
        currentMountedSha1 = null;
        currentMountedPackPath = null;
        packMounted = false;
        singleplayerOverride = null;
        localPackPathOverride = null;
        GeckoLibCacheInjector.resetForTesting();
    }
}
