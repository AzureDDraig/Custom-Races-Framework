package ddraig.net.customraces.client;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

public class ClientSuggestionsHelper {

    public static void addClientDimensions(List<String> dimensions) {
        try {
            if (Minecraft.getInstance() != null && Minecraft.getInstance().level != null) {
                var registry = Minecraft.getInstance().level.registryAccess().registry(Registries.DIMENSION_TYPE);
                if (registry.isPresent()) {
                    for (ResourceLocation dim : registry.get().keySet()) {
                        if (!dimensions.contains(dim.toString())) {
                            dimensions.add(dim.toString());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void addClientBiomes(List<String> biomes) {
        try {
            if (Minecraft.getInstance() != null && Minecraft.getInstance().level != null) {
                var registry = Minecraft.getInstance().level.registryAccess().registry(Registries.BIOME);
                if (registry.isPresent()) {
                    for (ResourceLocation biome : registry.get().keySet()) {
                        if (!biomes.contains(biome.toString())) {
                            biomes.add(biome.toString());
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void addClientSounds(List<String> sounds) {
        try {
            if (Minecraft.getInstance() != null && Minecraft.getInstance().getSoundManager() != null) {
                Collection<ResourceLocation> available = Minecraft.getInstance().getSoundManager().getAvailableSounds();
                if (available != null) {
                    for (ResourceLocation loc : available) {
                        String str = loc.toString();
                        if (!sounds.contains(str)) {
                            sounds.add(str);
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        try {
            File soundsDir = new File("config/custom_races/sounds");
            if (soundsDir.exists() && soundsDir.isDirectory()) {
                File[] files = soundsDir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().endsWith(".ogg") || f.getName().endsWith(".wav")) {
                            String name = f.getName().substring(0, f.getName().lastIndexOf('.'));
                            String fullLoc = "customraces:sounds/" + name;
                            if (!sounds.contains(fullLoc)) {
                                sounds.add(fullLoc);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static void addClientAnimationSuggestions(String cleanPath, List<String> results, Gson gson) {
        try {
            if (Minecraft.getInstance() != null) {
                ResourceLocation rl = cleanPath.contains(":")
                        ? new ResourceLocation(cleanPath)
                        : new ResourceLocation("customraces", "animations/" + cleanPath);
                var res = Minecraft.getInstance().getResourceManager().getResource(rl);
                if (res.isPresent()) {
                    try (InputStreamReader isr = new InputStreamReader(res.get().open(), StandardCharsets.UTF_8)) {
                        JsonObject json = gson.fromJson(isr, JsonObject.class);
                        if (json != null && json.has("animations") && json.get("animations").isJsonObject()) {
                            JsonObject animsObj = json.getAsJsonObject("animations");
                            for (String key : animsObj.keySet()) {
                                if (!results.contains(key)) {
                                    results.add(key);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
