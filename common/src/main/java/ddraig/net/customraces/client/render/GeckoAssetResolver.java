package ddraig.net.customraces.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Dedicated asset resolution helper class for Custom Races.
 * Cleanly resolves models, textures, and animation files across both disk config paths
 * (config/custom_races/models/, textures/, animations/) and mod resource pack paths
 * (assets/customraces/geo/, textures/, animations/).
 */
public class GeckoAssetResolver {

    public static final String DEFAULT_NAMESPACE = "customraces";

    public static final ResourceLocation DEFAULT_MODEL_LOCATION = new ResourceLocation(DEFAULT_NAMESPACE, "models/were/default_werewolf.geo.json");
    public static final ResourceLocation DEFAULT_TEXTURE_LOCATION = new ResourceLocation(DEFAULT_NAMESPACE, "textures/were/default_werewolf.png");
    public static final ResourceLocation DEFAULT_ANIMATION_LOCATION = new ResourceLocation(DEFAULT_NAMESPACE, "animations/were/default_werewolf.animation.json");

    private static final Map<String, ResourceLocation> DYNAMIC_TEXTURE_CACHE = new ConcurrentHashMap<>();

    public static void clearCaches() {
        DYNAMIC_TEXTURE_CACHE.clear();
    }

    /**
     * Resolves a GeckoLib model ResourceLocation from raw path string.
     * Searches client resource manager and disk config locations.
     */
    public static ResourceLocation resolveModelLocation(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty() || "none".equalsIgnoreCase(rawPath.trim())) {
            return null;
        }

        String path = rawPath.trim();
        ParsedPath parsed = parsePath(path, "geo/", ".geo.json");

        // 1. Try candidate resource locations on client resource manager
        for (ResourceLocation candidate : parsed.candidateResourceLocations) {
            if (isResourcePresentOnClient(candidate)) {
                return candidate;
            }
        }

        // 2. Check candidate disk files
        for (File diskFile : getModelDiskCandidates(parsed)) {
            if (diskFile.exists() && diskFile.isFile()) {
                return parsed.primaryLocation;
            }
        }

        return parsed.primaryLocation;
    }

    /**
     * Resolves texture ResourceLocation from raw path string and optional player context.
     * Intercepts player skin keywords, checks resource manager and dynamic disk texture files.
     */
    public static ResourceLocation resolveTextureLocation(AbstractClientPlayer player, String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty() || "none".equalsIgnoreCase(rawPath.trim())) {
            return getSafeDefaultTexture(player);
        }

        String path = rawPath.trim();
        String lowerPath = path.toLowerCase(java.util.Locale.ROOT);

        if ("skin".equals(lowerPath) || "player".equals(lowerPath) || "player_skin".equals(lowerPath) || "skin_texture".equals(lowerPath) || "dynamic_skin".equals(lowerPath) || "use_skin".equals(lowerPath) || "dynamic".equals(lowerPath) || "player_texture".equals(lowerPath) || "default_skin".equals(lowerPath)) {
            if (player != null) {
                ResourceLocation skinLoc = player.getSkinTextureLocation();
                if (skinLoc != null) {
                    return skinLoc;
                }
            }
            return getSafeDefaultTexture(player);
        }

        ParsedPath parsed = parsePath(path, "textures/", ".png");

        for (ResourceLocation candidate : parsed.candidateResourceLocations) {
            if (isResourcePresentOnClient(candidate)) {
                return candidate;
            }
        }

        ResourceLocation diskLoc = loadDiskTextureDynamic(path, parsed);
        if (diskLoc != null) {
            return diskLoc;
        }

        return getSafeDefaultTexture(player);
    }

    /**
     * Resolves animation ResourceLocation from raw path string.
     */
    public static ResourceLocation resolveAnimationLocation(String rawPath) {
        if (rawPath == null || rawPath.trim().isEmpty() || "none".equalsIgnoreCase(rawPath.trim())) {
            return null;
        }

        String path = rawPath.trim();
        ParsedPath parsed = parsePath(path, "animations/", ".animation.json");

        for (ResourceLocation candidate : parsed.candidateResourceLocations) {
            if (isResourcePresentOnClient(candidate)) {
                return candidate;
            }
        }

        for (File diskFile : getAnimationDiskCandidates(parsed)) {
            if (diskFile.exists() && diskFile.isFile()) {
                return parsed.primaryLocation;
            }
        }

        return parsed.primaryLocation;
    }

    /**
     * Reads content string for a model from disk file candidates or resource manager.
     */
    public static String getModelContent(ResourceLocation modelLoc, String rawPath) {
        ParsedPath parsed = parsePath(rawPath != null ? rawPath : (modelLoc != null ? modelLoc.toString() : ""), "geo/", ".geo.json");
        if (modelLoc != null && !parsed.candidateResourceLocations.contains(modelLoc)) {
            parsed.candidateResourceLocations.add(0, modelLoc);
        }

        for (File file : getModelDiskCandidates(parsed)) {
            if (file.exists() && file.isFile()) {
                try {
                    return Files.readString(file.toPath());
                } catch (Throwable ignored) {}
            }
        }

        for (ResourceLocation loc : parsed.candidateResourceLocations) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.getResourceManager() != null) {
                    var resOpt = mc.getResourceManager().getResource(loc);
                    if (resOpt.isPresent()) {
                        try (InputStream is = resOpt.get().open();
                             java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {
                            return br.lines().collect(Collectors.joining("\n"));
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    /**
     * Reads content string for an animation from disk file candidates or resource manager.
     */
    public static String getAnimationContent(ResourceLocation animLoc, String rawPath) {
        ParsedPath parsed = parsePath(rawPath != null ? rawPath : (animLoc != null ? animLoc.toString() : ""), "animations/", ".animation.json");
        if (animLoc != null && !parsed.candidateResourceLocations.contains(animLoc)) {
            parsed.candidateResourceLocations.add(0, animLoc);
        }

        for (File file : getAnimationDiskCandidates(parsed)) {
            if (file.exists() && file.isFile()) {
                try {
                    return Files.readString(file.toPath());
                } catch (Throwable ignored) {}
            }
        }

        for (ResourceLocation loc : parsed.candidateResourceLocations) {
            try {
                Minecraft mc = Minecraft.getInstance();
                if (mc != null && mc.getResourceManager() != null) {
                    var resOpt = mc.getResourceManager().getResource(loc);
                    if (resOpt.isPresent()) {
                        try (InputStream is = resOpt.get().open();
                             java.io.BufferedReader br = new java.io.BufferedReader(new java.io.InputStreamReader(is, StandardCharsets.UTF_8))) {
                            return br.lines().collect(Collectors.joining("\n"));
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }

        return null;
    }

    public static boolean isResourcePresentOnClient(ResourceLocation loc) {
        if (loc == null) return false;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                return mc.getResourceManager().getResource(loc).isPresent();
            }
        } catch (Throwable ignored) {}
        return true;
    }

    private static ResourceLocation getSafeDefaultTexture(AbstractClientPlayer player) {
        if (isResourcePresentOnClient(DEFAULT_TEXTURE_LOCATION)) {
            return DEFAULT_TEXTURE_LOCATION;
        }
        if (player != null) {
            ResourceLocation skinLoc = player.getSkinTextureLocation();
            if (skinLoc != null) {
                return skinLoc;
            }
        }
        return DEFAULT_TEXTURE_LOCATION;
    }

    private static ResourceLocation loadDiskTextureDynamic(String rawPath, ParsedPath parsed) {
        String cleanName = parsed.cleanFilename;
        if (DYNAMIC_TEXTURE_CACHE.containsKey(cleanName)) {
            return DYNAMIC_TEXTURE_CACHE.get(cleanName);
        }

        for (File file : getTextureDiskCandidates(parsed, rawPath)) {
            if (file.exists() && file.isFile()) {
                try (InputStream is = new FileInputStream(file)) {
                    NativeImage nativeImage = NativeImage.read(is);
                    DynamicTexture dynamicTexture = new DynamicTexture(nativeImage);
                    ResourceLocation loc = new ResourceLocation(DEFAULT_NAMESPACE, "dynamic_were_texture/" + cleanName.toLowerCase().replaceAll("[^a-z0-9_.-]", "_"));
                    Minecraft.getInstance().getTextureManager().register(loc, dynamicTexture);
                    DYNAMIC_TEXTURE_CACHE.put(cleanName, loc);
                    return loc;
                } catch (Throwable t) {
                    System.err.println("[CustomRaces] Failed to load dynamic disk texture: " + file.getAbsolutePath() + " -> " + t.getMessage());
                }
            }
        }
        return null;
    }

    public static class ParsedPath {
        public final String namespace;
        public final String relativePath;
        public final String cleanFilename;
        public final ResourceLocation primaryLocation;
        public final List<ResourceLocation> candidateResourceLocations;

        public ParsedPath(String namespace, String relativePath, String cleanFilename, ResourceLocation primaryLocation, List<ResourceLocation> candidateResourceLocations) {
            this.namespace = namespace;
            this.relativePath = relativePath;
            this.cleanFilename = cleanFilename;
            this.primaryLocation = primaryLocation;
            this.candidateResourceLocations = candidateResourceLocations;
        }
    }

    private static void addCandidate(List<ResourceLocation> candidates, String namespace, String path) {
        if (namespace == null || path == null) return;
        String ns = namespace.trim();
        String p = path.trim();
        if (ns.isEmpty()) ns = DEFAULT_NAMESPACE;
        try {
            ResourceLocation loc = ResourceLocation.tryParse(ns + ":" + p);
            if (loc != null && !candidates.contains(loc)) {
                candidates.add(loc);
            }
        } catch (Throwable ignored) {}
    }

    private static ResourceLocation getDefaultLocation(String defaultSubfolderPrefix) {
        if ("textures/".equals(defaultSubfolderPrefix)) {
            return DEFAULT_TEXTURE_LOCATION;
        } else if ("animations/".equals(defaultSubfolderPrefix)) {
            return DEFAULT_ANIMATION_LOCATION;
        }
        return DEFAULT_MODEL_LOCATION;
    }

    private static boolean isValidNamespace(String namespace) {
        if (namespace == null || namespace.isEmpty()) return false;
        for (int i = 0; i < namespace.length(); i++) {
            char c = namespace.charAt(i);
            if (c != '_' && c != '-' && (c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '.') {
                return false;
            }
        }
        return true;
    }

    private static boolean isValidPath(String path) {
        if (path == null || path.isEmpty()) return false;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c != '_' && c != '-' && (c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '/' && c != '.') {
                return false;
            }
        }
        return true;
    }

    public static ParsedPath parsePath(String rawPath, String defaultSubfolderPrefix, String defaultExtension) {
        if (rawPath == null) rawPath = "";
        String trimmed = rawPath.trim();

        String namespace;
        String pathWithoutNamespace;
        int colonIdx = trimmed.indexOf(':');
        if (colonIdx >= 0) {
            namespace = trimmed.substring(0, colonIdx);
            pathWithoutNamespace = trimmed.substring(colonIdx + 1);
        } else {
            namespace = DEFAULT_NAMESPACE;
            pathWithoutNamespace = trimmed;
        }

        if (namespace.trim().isEmpty()) {
            namespace = DEFAULT_NAMESPACE;
        }

        String lowerRel = pathWithoutNamespace.toLowerCase(java.util.Locale.ROOT);
        String lowerExt = defaultExtension.toLowerCase(java.util.Locale.ROOT);

        String normalizedRelPath = pathWithoutNamespace;
        if (!lowerRel.endsWith(lowerExt)) {
            if (lowerExt.equals(".geo.json") && lowerRel.endsWith(".json")) {
                normalizedRelPath = pathWithoutNamespace.substring(0, pathWithoutNamespace.length() - 5) + ".geo.json";
            } else if (lowerExt.equals(".animation.json") && lowerRel.endsWith(".json")) {
                normalizedRelPath = pathWithoutNamespace.substring(0, pathWithoutNamespace.length() - 5) + ".animation.json";
            } else if (!lowerRel.contains(".")) {
                normalizedRelPath = pathWithoutNamespace + defaultExtension;
            }
        }

        String filename = normalizedRelPath.replaceAll(".*/", "");

        if (!isValidNamespace(namespace) || !isValidPath(pathWithoutNamespace)) {
            ResourceLocation fallback = getDefaultLocation(defaultSubfolderPrefix);
            return new ParsedPath(namespace, normalizedRelPath, filename, fallback, new ArrayList<>());
        }

        List<ResourceLocation> candidates = new ArrayList<>();

        if (lowerRel.startsWith(lowerExt) || lowerRel.startsWith(defaultSubfolderPrefix) || lowerRel.startsWith("models/")) {
            addCandidate(candidates, namespace, normalizedRelPath);
            addCandidate(candidates, namespace, defaultSubfolderPrefix + filename);
        } else {
            addCandidate(candidates, namespace, defaultSubfolderPrefix + normalizedRelPath);
            addCandidate(candidates, namespace, defaultSubfolderPrefix + filename);
            addCandidate(candidates, namespace, normalizedRelPath);
        }

        addCandidate(candidates, namespace, defaultSubfolderPrefix + "were/" + filename);

        if (defaultSubfolderPrefix.equals("geo/")) {
            addCandidate(candidates, namespace, "models/were/" + filename);
            addCandidate(candidates, namespace, "models/" + filename);
        } else if (defaultSubfolderPrefix.equals("animations/")) {
            addCandidate(candidates, namespace, "animations/were/" + filename);
        } else if (defaultSubfolderPrefix.equals("textures/")) {
            addCandidate(candidates, namespace, "textures/were/" + filename);
        }

        ResourceLocation primaryLoc = null;
        if (!candidates.isEmpty()) {
            primaryLoc = candidates.get(0);
        } else {
            primaryLoc = getDefaultLocation(defaultSubfolderPrefix);
        }

        return new ParsedPath(namespace, normalizedRelPath, filename, primaryLoc, candidates);
    }

    public static List<File> getModelDiskCandidates(ParsedPath parsed) {
        List<File> files = new ArrayList<>();
        files.add(new File("config/custom_races/models/" + parsed.cleanFilename));
        files.add(new File("config/custom_races/models/were/" + parsed.cleanFilename));
        files.add(new File("config/custom_races/geo/" + parsed.cleanFilename));
        files.add(new File("config/custom_races/" + parsed.relativePath));
        files.add(new File(parsed.relativePath));
        return files;
    }

    public static List<File> getTextureDiskCandidates(ParsedPath parsed, String rawPath) {
        List<File> files = new ArrayList<>();
        files.add(new File("config/custom_races/textures/" + parsed.cleanFilename));
        files.add(new File("config/custom_races/textures/were/" + parsed.cleanFilename));
        files.add(new File("config/custom_races/" + parsed.relativePath));
        if (rawPath != null && !rawPath.trim().isEmpty()) {
            files.add(new File(rawPath.trim()));
        }
        return files;
    }

    public static List<File> getAnimationDiskCandidates(ParsedPath parsed) {
        List<File> files = new ArrayList<>();
        files.add(new File("config/custom_races/animations/" + parsed.cleanFilename));
        files.add(new File("config/custom_races/animations/were/" + parsed.cleanFilename));
        files.add(new File("config/custom_races/" + parsed.relativePath));
        files.add(new File(parsed.relativePath));
        return files;
    }
}
