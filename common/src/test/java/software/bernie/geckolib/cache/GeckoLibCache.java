package software.bernie.geckolib.cache;

import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;

public class GeckoLibCache {
    public static Map<ResourceLocation, Object> MODELS = Collections.emptyMap();
    public static Map<ResourceLocation, Object> ANIMATIONS = Collections.emptyMap();

    public static Map<ResourceLocation, Object> getBakedModels() {
        return MODELS;
    }

    public static Map<ResourceLocation, Object> getBakedAnimations() {
        return ANIMATIONS;
    }
}
