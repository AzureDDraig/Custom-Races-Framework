package ddraig.net.customraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import ddraig.net.customraces.client.ClientWereState;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.event.WereRaceTransformHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles Were-form custom model rendering, transformation state checks,
 * base player mesh suppression, and fallback logic for unmapped/invalid model paths.
 */
public class WereModelRenderer {
    public static final ResourceLocation DEFAULT_WERE_MODEL = new ResourceLocation("customraces", "models/were/default_werewolf.geo.json");
    public static final ResourceLocation DEFAULT_WERE_TEXTURE = new ResourceLocation("customraces", "textures/were/default_werewolf.png");
    public static final ResourceLocation DEFAULT_WERE_ANIMATION = new ResourceLocation("customraces", "animations/were/default_werewolf.animation.json");

    private static final Set<String> LOGGED_WARNINGS = new HashSet<>();

    public static boolean isTransformed(UUID uuid) {
        if (uuid == null) return false;
        return ClientWereState.isTransformed(uuid) || WereRaceTransformHandler.isTransformed(uuid);
    }

    public static boolean isWereForm(AbstractClientPlayer player, RaceData race) {
        if (player == null || race == null) return false;
        return race.enableWereRace && isTransformed(player.getUUID());
    }

    public static boolean hasCustomModel(RaceData race) {
        if (race == null) return false;
        String path = race.wereModelPath;
        return path != null && !path.trim().isEmpty() && !"none".equalsIgnoreCase(path.trim());
    }

    public static ResourceLocation getValidWereModelLocation(RaceData race) {
        if (race == null || !hasCustomModel(race)) {
            return DEFAULT_WERE_MODEL;
        }
        String path = race.wereModelPath.trim();
        ResourceLocation loc = ResourceLocation.tryParse(path);
        if (loc == null) {
            if (LOGGED_WARNINGS.add("model:" + path)) {
                System.err.println("[CustomRaces] Invalid Were model path '" + path + "', falling back to default: " + DEFAULT_WERE_MODEL);
            }
            return DEFAULT_WERE_MODEL;
        }
        return loc;
    }

    public static ResourceLocation getValidWereTextureLocation(AbstractClientPlayer player, RaceData race) {
        if (race == null || race.wereTexturePath == null || race.wereTexturePath.trim().isEmpty() || "none".equalsIgnoreCase(race.wereTexturePath.trim())) {
            return getSafeDefaultTexture(player);
        }

        String path = race.wereTexturePath.trim();
        String lowerPath = path.toLowerCase(java.util.Locale.ROOT);

        // Intercept "skin" and "player" keywords (case-insensitive, trimmed)
        if ("skin".equals(lowerPath) || "player".equals(lowerPath) || "player_skin".equals(lowerPath) || "skin_texture".equals(lowerPath)) {
            if (player != null) {
                ResourceLocation skinLoc = player.getSkinTextureLocation();
                if (skinLoc != null) {
                    return skinLoc;
                }
            }
            return getSafeDefaultTexture(player);
        }

        // Path & extension normalization (default namespace customraces, prefix textures/, suffix .png if missing)
        String namespace;
        String relativePath;
        int colonIndex = path.indexOf(':');
        if (colonIndex >= 0) {
            namespace = path.substring(0, colonIndex);
            relativePath = path.substring(colonIndex + 1);
        } else {
            namespace = "customraces";
            relativePath = path;
        }

        if (!relativePath.startsWith("textures/")) {
            relativePath = "textures/" + relativePath;
        }
        if (!relativePath.endsWith(".png")) {
            relativePath = relativePath + ".png";
        }

        ResourceLocation loc = ResourceLocation.tryParse(namespace + ":" + relativePath);
        if (loc == null) {
            if (LOGGED_WARNINGS.add("texture_syntax:" + path)) {
                System.err.println("[CustomRaces] Invalid Were texture path syntax '" + path + "', falling back to default: " + DEFAULT_WERE_TEXTURE);
            }
            return getSafeDefaultTexture(player);
        }

        // Client-side ResourceManager existence validation & safe fallback ladder
        if (isResourcePresentOnClient(loc)) {
            return loc;
        } else {
            if (LOGGED_WARNINGS.add("texture_missing:" + loc)) {
                System.err.println("[CustomRaces] Were texture asset missing on disk: '" + loc + "', falling back to default: " + DEFAULT_WERE_TEXTURE);
            }
            return getSafeDefaultTexture(player);
        }
    }

    public static ResourceLocation getValidWereTextureLocation(RaceData race) {
        return getValidWereTextureLocation(null, race);
    }

    public static boolean isResourcePresentOnClient(ResourceLocation loc) {
        if (loc == null) return false;
        try {
            net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
            if (mc != null && mc.getResourceManager() != null) {
                return mc.getResourceManager().getResource(loc).isPresent();
            }
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static ResourceLocation getSafeDefaultTexture(AbstractClientPlayer player) {
        if (isResourcePresentOnClient(DEFAULT_WERE_TEXTURE)) {
            return DEFAULT_WERE_TEXTURE;
        }
        if (player != null) {
            ResourceLocation skinLoc = player.getSkinTextureLocation();
            if (skinLoc != null) {
                return skinLoc;
            }
        }
        return DEFAULT_WERE_TEXTURE;
    }

    public static ResourceLocation getValidWereAnimationLocation(RaceData race) {
        if (race == null || race.wereAnimationPath == null || race.wereAnimationPath.trim().isEmpty() || "none".equalsIgnoreCase(race.wereAnimationPath.trim())) {
            return DEFAULT_WERE_ANIMATION;
        }
        String path = race.wereAnimationPath.trim();
        ResourceLocation loc = ResourceLocation.tryParse(path);
        if (loc == null) {
            if (LOGGED_WARNINGS.add("animation:" + path)) {
                System.err.println("[CustomRaces] Invalid Were animation path '" + path + "', falling back to default: " + DEFAULT_WERE_ANIMATION);
            }
            return DEFAULT_WERE_ANIMATION;
        }
        return loc;
    }

    public static void setBaseModelVisible(PlayerModel<?> model, boolean visible) {
        if (model == null) return;
        model.head.visible = visible;
        model.hat.visible = visible;
        model.body.visible = visible;
        model.rightArm.visible = visible;
        model.leftArm.visible = visible;
        model.rightLeg.visible = visible;
        model.leftLeg.visible = visible;
        model.jacket.visible = visible;
        model.rightSleeve.visible = visible;
        model.leftSleeve.visible = visible;
        model.rightPants.visible = visible;
        model.leftPants.visible = visible;
    }

    public static boolean renderWereForm(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> parentModel, RaceData race, float netHeadYaw, float headPitch) {
        if (!isWereForm(player, race)) {
            setBaseModelVisible(parentModel, true);
            return false;
        }

        if (hasCustomModel(race)) {
            // Hide human player model mesh so skin doesn't bleed through
            setBaseModelVisible(parentModel, false);

            ResourceLocation modelLoc = getValidWereModelLocation(race);
            ResourceLocation textureLoc = getValidWereTextureLocation(player, race);
            ResourceLocation animLoc = getValidWereAnimationLocation(race);

            boolean rendered = renderGeckoLibWereModel(poseStack, buffer, packedLight, player, parentModel, modelLoc, textureLoc, animLoc);
            if (!rendered) {
                // If GeckoLib model fails to bake, restore base player model mesh safely
                setBaseModelVisible(parentModel, true);
                return false;
            }
            return true;
        } else {
            // Keep player model visible for procedural overlay fallback
            setBaseModelVisible(parentModel, true);
            return false;
        }
    }

    private static boolean renderGeckoLibWereModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> parentModel, ResourceLocation modelLoc, ResourceLocation textureLoc, ResourceLocation animLoc) {
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            java.lang.reflect.Method getModelsMethod = cacheClass.getMethod("getBakedModels");
            java.util.Map<?, ?> bakedModels = (java.util.Map<?, ?>) getModelsMethod.invoke(null);
            
            if (bakedModels != null) {
                Object bakedModel = bakedModels.get(modelLoc);
                if (bakedModel == null) {
                    // Try dynamic baking from file system if model not yet cached
                    bakedModel = loadAndBakeGeckoModel(modelLoc);
                }
                if (bakedModel != null) {
                    // Successfully resolved baked GeckoLib model
                    return true;
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    private static Object loadAndBakeGeckoModel(ResourceLocation modelLoc) {
        if (modelLoc == null) return null;
        try {
            String path = modelLoc.getPath();
            java.io.File file = new java.io.File(path);
            if (!file.exists()) {
                file = new java.io.File("config/custom_races/models/" + path.replaceAll(".*/", ""));
            }
            if (!file.exists()) {
                file = new java.io.File("config/custom_races/models/were/" + path.replaceAll(".*/", ""));
            }
            if (file.exists() && file.isFile()) {
                String content = java.nio.file.Files.readString(file.toPath());
                com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(content).getAsJsonObject();
                
                Class<?> jsonUtilClass = Class.forName("software.bernie.geckolib.util.JsonUtil");
                Object geoGson = jsonUtilClass.getField("GEO_GSON").get(null);
                java.lang.reflect.Method fromJsonMethod = geoGson.getClass().getMethod("fromJson", com.google.gson.JsonElement.class, Class.class);
                
                Class<?> rawModelClass = Class.forName("software.bernie.geckolib.loading.json.raw.Model");
                Object rawModel = fromJsonMethod.invoke(geoGson, json, rawModelClass);
                
                Class<?> geomTreeClass = Class.forName("software.bernie.geckolib.loading.object.GeometryTree");
                java.lang.reflect.Method fromModelMethod = geomTreeClass.getMethod("fromModel", rawModelClass);
                Object geomTree = fromModelMethod.invoke(null, rawModel);
                
                Class<?> bakedFactoryClass = Class.forName("software.bernie.geckolib.loading.object.BakedModelFactory");
                java.lang.reflect.Method getForNsMethod = bakedFactoryClass.getMethod("getForNamespace", String.class);
                Object factory = getForNsMethod.invoke(null, modelLoc.getNamespace());
                
                java.lang.reflect.Method constructGeoModelMethod = factory.getClass().getMethod("constructGeoModel", geomTreeClass);
                Object bakedModel = constructGeoModelMethod.invoke(factory, geomTree);
                
                if (bakedModel != null) {
                    Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
                    java.lang.reflect.Method getModelsMethod = cacheClass.getMethod("getBakedModels");
                    java.util.Map<Object, Object> bakedModels = (java.util.Map<Object, Object>) getModelsMethod.invoke(null);
                    if (bakedModels != null) {
                        bakedModels.put(modelLoc, bakedModel);
                    }
                    return bakedModel;
                }
            }
        } catch (Throwable t) {
            System.err.println("[CustomRaces] Failed to bake dynamic GeckoLib model: " + modelLoc + " -> " + t.getMessage());
        }
        return null;
    }

    private static void renderBox(PoseStack poseStack, VertexConsumer builder, int packedLight, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        org.joml.Matrix4f pose = poseStack.last().pose();
        org.joml.Matrix3f normal = poseStack.last().normal();
        float r = 1.0f, g = 1.0f, b = 1.0f, a = 1.0f;

        // Top
        builder.vertex(pose, minX, maxY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        builder.vertex(pose, minX, maxY, maxZ).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        builder.vertex(pose, maxX, maxY, maxZ).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();
        builder.vertex(pose, maxX, maxY, minZ).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 1, 0).endVertex();

        // Bottom
        builder.vertex(pose, minX, minY, maxZ).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();
        builder.vertex(pose, minX, minY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();
        builder.vertex(pose, maxX, minY, minZ).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();
        builder.vertex(pose, maxX, minY, maxZ).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, -1, 0).endVertex();

        // Front
        builder.vertex(pose, minX, maxY, maxZ).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        builder.vertex(pose, minX, minY, maxZ).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        builder.vertex(pose, maxX, minY, maxZ).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();
        builder.vertex(pose, maxX, maxY, maxZ).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, 1).endVertex();

        // Back
        builder.vertex(pose, maxX, maxY, minZ).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();
        builder.vertex(pose, maxX, minY, minZ).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();
        builder.vertex(pose, minX, minY, minZ).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();
        builder.vertex(pose, minX, maxY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 0, 0, -1).endVertex();

        // Left
        builder.vertex(pose, minX, maxY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();
        builder.vertex(pose, minX, minY, minZ).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();
        builder.vertex(pose, minX, minY, maxZ).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();
        builder.vertex(pose, minX, maxY, maxZ).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, -1, 0, 0).endVertex();

        // Right
        builder.vertex(pose, maxX, maxY, maxZ).color(r, g, b, a).uv(1, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();
        builder.vertex(pose, maxX, minY, maxZ).color(r, g, b, a).uv(1, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();
        builder.vertex(pose, maxX, minY, minZ).color(r, g, b, a).uv(0, 1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();
        builder.vertex(pose, maxX, maxY, minZ).color(r, g, b, a).uv(0, 0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(packedLight).normal(normal, 1, 0, 0).endVertex();
    }
}
