package ddraig.net.customraces.data;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Scans config/custom_races/models/parts/ for custom part subfolders and model files.
 */
public class CustomPartScanner {
    private static final List<String> discoveredCustomParts = new ArrayList<>();

    public static File getPartsDirectory() {
        File dir = new File("config/custom_races/models/parts");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir;
    }

    public static void scan() {
        discoveredCustomParts.clear();
        File dir = getPartsDirectory();
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        discoveredCustomParts.add(file.getName());
                    } else if (file.getName().endsWith(".json") || file.getName().endsWith(".geo.json") || file.getName().endsWith(".obj")) {
                        String name = file.getName();
                        int dot = name.indexOf('.');
                        if (dot > 0) {
                            name = name.substring(0, dot);
                        }
                        if (!discoveredCustomParts.contains(name)) {
                            discoveredCustomParts.add(name);
                        }
                    }
                }
            }
        }
        try {
            for (String modelId : ddraig.net.azureframelib.resource.AzureResourceManager.getDiscoveredModels()) {
                if (modelId == null) continue;
                String clean = modelId;
                if (clean.contains(":")) {
                    clean = clean.substring(clean.indexOf(':') + 1);
                }
                if (clean.startsWith("models/")) {
                    clean = clean.substring("models/".length());
                } else if (clean.startsWith("geo/")) {
                    clean = clean.substring("geo/".length());
                }
                if (clean.startsWith("parts/")) {
                    String partName = clean.substring("parts/".length());
                    int dot = partName.indexOf('.');
                    if (dot > 0) {
                        partName = partName.substring(0, dot);
                    }
                    if (!partName.isEmpty() && !discoveredCustomParts.contains(partName)) {
                        discoveredCustomParts.add(partName);
                    }
                }
            }
        } catch (Throwable ignored) {}
        Collections.sort(discoveredCustomParts);
    }

    public static List<String> getDiscoveredCustomParts() {
        if (discoveredCustomParts.isEmpty()) {
            scan();
        }
        return Collections.unmodifiableList(discoveredCustomParts);
    }
}
