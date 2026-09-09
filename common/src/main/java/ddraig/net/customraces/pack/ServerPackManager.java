package ddraig.net.customraces.pack;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Server-side dynamic resource pack generator.
 * Scans server configuration asset directories (models, textures, animations),
 * packages them into customraces-server-pack.zip with pack.mcmeta format 15,
 * and computes a 40-character lowercase hexadecimal SHA-1 checksum.
 */
public class ServerPackManager {

    public static final String PACK_FILE_NAME = "customraces-server-pack.zip";
    public static final Path DEFAULT_CONFIG_DIR = Paths.get("config/custom_races");
    public static final Path DEFAULT_PACK_PATH = DEFAULT_CONFIG_DIR.resolve(PACK_FILE_NAME);
    public static final int PACK_FORMAT = 15;
    public static final String PACK_DESCRIPTION = "Custom Races Dynamic Server Assets";

    private static Path currentPackPath = DEFAULT_PACK_PATH;
    private static String currentPackSha1 = "";
    private static long currentPackSizeBytes = 0;

    /**
     * Builds the resource pack using the default configuration directory and default output zip path.
     */
    public static synchronized void buildPack() {
        buildPack(DEFAULT_CONFIG_DIR, DEFAULT_PACK_PATH);
    }

    /**
     * Builds the resource pack from a specified base config directory to a specified target zip file.
     *
     * @param configBaseDir the base directory containing models, textures, animations
     * @param targetZipPath the output zip file path
     */
    public static synchronized void buildPack(Path configBaseDir, Path targetZipPath) {
        try {
            if (targetZipPath.getParent() != null && !Files.exists(targetZipPath.getParent())) {
                Files.createDirectories(targetZipPath.getParent());
            }

            Path tempZipPath = targetZipPath.resolveSibling(targetZipPath.getFileName().toString() + ".tmp");

            try (OutputStream fos = Files.newOutputStream(tempZipPath);
                 ZipOutputStream zos = new ZipOutputStream(fos)) {

                Set<String> addedEntries = new HashSet<>();

                // 1. Generate pack.mcmeta
                String mcmetaContent = "{\n" +
                        "  \"pack\": {\n" +
                        "    \"pack_format\": " + PACK_FORMAT + ",\n" +
                        "    \"description\": \"" + PACK_DESCRIPTION + "\"\n" +
                        "  }\n" +
                        "}\n";
                addZipEntry(zos, addedEntries, "pack.mcmeta", mcmetaContent.getBytes(StandardCharsets.UTF_8));

                // 2. Scan models and mirror into assets/customraces/geo/ and assets/customraces/models/
                Path modelsDir = configBaseDir.resolve("models");
                if (Files.isDirectory(modelsDir)) {
                    try (Stream<Path> stream = Files.walk(modelsDir)) {
                        stream.filter(Files::isRegularFile)
                              .filter(p -> !p.getFileName().toString().startsWith(".") && !p.getFileName().toString().endsWith(".tmp"))
                              .sorted(Comparator.comparing(Path::toString))
                              .forEach(modelFile -> {
                                  try {
                                      String rel = normalizeRelativePath(modelsDir.relativize(modelFile));
                                      byte[] bytes = Files.readAllBytes(modelFile);
                                      // Mirror to both geo/ and models/ for full GeckoLib resolution
                                      addZipEntry(zos, addedEntries, "assets/customraces/geo/" + rel, bytes);
                                      addZipEntry(zos, addedEntries, "assets/customraces/models/" + rel, bytes);
                                  } catch (IOException e) {
                                      System.err.println("[CustomRaces] Failed to read model file: " + modelFile + " - " + e.getMessage());
                                  }
                              });
                    }
                }

                // 3. Scan textures and place into assets/customraces/textures/
                Path texturesDir = configBaseDir.resolve("textures");
                if (Files.isDirectory(texturesDir)) {
                    try (Stream<Path> stream = Files.walk(texturesDir)) {
                        stream.filter(Files::isRegularFile)
                              .filter(p -> !p.getFileName().toString().startsWith(".") && !p.getFileName().toString().endsWith(".tmp"))
                              .sorted(Comparator.comparing(Path::toString))
                              .forEach(textureFile -> {
                                  try {
                                      String rel = normalizeRelativePath(texturesDir.relativize(textureFile));
                                      byte[] bytes = Files.readAllBytes(textureFile);
                                      addZipEntry(zos, addedEntries, "assets/customraces/textures/" + rel, bytes);
                                  } catch (IOException e) {
                                      System.err.println("[CustomRaces] Failed to read texture file: " + textureFile + " - " + e.getMessage());
                                  }
                              });
                    }
                }

                // 4. Scan animations and place into assets/customraces/animations/
                Path animsDir = configBaseDir.resolve("animations");
                if (Files.isDirectory(animsDir)) {
                    try (Stream<Path> stream = Files.walk(animsDir)) {
                        stream.filter(Files::isRegularFile)
                              .filter(p -> !p.getFileName().toString().startsWith(".") && !p.getFileName().toString().endsWith(".tmp"))
                              .sorted(Comparator.comparing(Path::toString))
                              .forEach(animFile -> {
                                  try {
                                      String rel = normalizeRelativePath(animsDir.relativize(animFile));
                                      byte[] bytes = Files.readAllBytes(animFile);
                                      addZipEntry(zos, addedEntries, "assets/customraces/animations/" + rel, bytes);
                                  } catch (IOException e) {
                                      System.err.println("[CustomRaces] Failed to read animation file: " + animFile + " - " + e.getMessage());
                                  }
                              });
                    }
                }

                zos.finish();
            }

            // Atomic rename from temp to target zip
            Files.move(tempZipPath, targetZipPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            // Compute SHA-1 and update cached metadata
            currentPackPath = targetZipPath;
            currentPackSizeBytes = Files.size(targetZipPath);
            currentPackSha1 = calculateSha1(targetZipPath);

            System.out.println("[CustomRaces] Generated dynamic server pack at " + targetZipPath
                    + " (" + currentPackSizeBytes + " bytes, SHA-1: " + currentPackSha1 + ")");
        } catch (Exception e) {
            System.err.println("[CustomRaces] Error building server pack: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addZipEntry(ZipOutputStream zos, Set<String> addedEntries, String entryName, byte[] data) throws IOException {
        String normalizedName = entryName.replace('\\', '/');
        if (addedEntries.add(normalizedName)) {
            ZipEntry entry = new ZipEntry(normalizedName);
            entry.setTime(0L);
            zos.putNextEntry(entry);
            zos.write(data);
            zos.closeEntry();
        }
    }

    private static String normalizeRelativePath(Path path) {
        String pathStr = path.toString().replace('\\', '/');
        while (pathStr.startsWith("/")) {
            pathStr = pathStr.substring(1);
        }
        return pathStr;
    }

    /**
     * Calculates the 40-character lowercase hexadecimal SHA-1 checksum of a file.
     *
     * @param file the path to the file
     * @return 40-character lowercase hex string
     * @throws IOException if an I/O error occurs reading the file
     */
    public static String calculateSha1(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            try (InputStream is = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            byte[] hash = digest.digest();
            StringBuilder sb = new StringBuilder(40);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 digest algorithm not found", e);
        }
    }

    public static Path getGeneratedPackPath() {
        return currentPackPath;
    }

    public static String getPackSha1() {
        if ((currentPackSha1 == null || currentPackSha1.isEmpty()) && hasPack()) {
            try {
                currentPackSha1 = calculateSha1(currentPackPath);
            } catch (IOException ignored) {}
        }
        return currentPackSha1 != null ? currentPackSha1 : "";
    }

    public static long getPackSizeBytes() {
        if (currentPackSizeBytes <= 0 && hasPack()) {
            try {
                currentPackSizeBytes = Files.size(currentPackPath);
            } catch (IOException ignored) {}
        }
        return currentPackSizeBytes;
    }

    public static boolean hasPack() {
        return currentPackPath != null && Files.isRegularFile(currentPackPath);
    }
}
