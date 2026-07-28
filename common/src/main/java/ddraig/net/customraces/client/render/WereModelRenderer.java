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
    public static final ResourceLocation DEFAULT_WERE_MODEL = GeckoAssetResolver.DEFAULT_MODEL_LOCATION;
    public static final ResourceLocation DEFAULT_WERE_TEXTURE = GeckoAssetResolver.DEFAULT_TEXTURE_LOCATION;
    public static final ResourceLocation DEFAULT_WERE_ANIMATION = GeckoAssetResolver.DEFAULT_ANIMATION_LOCATION;

    private static final Set<String> LOGGED_WARNINGS = new HashSet<>();

    public static void clearCaches() {
        GeckoAssetResolver.clearCaches();
        DYNAMIC_TEXTURE_CACHE.clear();
        LOGGED_WARNINGS.clear();
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            java.lang.reflect.Method getModelsMethod = cacheClass.getMethod("getBakedModels");
            java.util.Map<?, ?> models = (java.util.Map<?, ?>) getModelsMethod.invoke(null);
            if (models != null) {
                models.keySet().removeIf(key -> {
                    if (key instanceof ResourceLocation loc) {
                        return loc.getNamespace().equals("customraces");
                    }
                    return key.toString().startsWith("customraces:");
                });
            }
            java.lang.reflect.Method getAnimsMethod = cacheClass.getMethod("getBakedAnimations");
            java.util.Map<?, ?> anims = (java.util.Map<?, ?>) getAnimsMethod.invoke(null);
            if (anims != null) {
                anims.keySet().removeIf(key -> {
                    if (key instanceof ResourceLocation loc) {
                        return loc.getNamespace().equals("customraces");
                    }
                    return key.toString().startsWith("customraces:");
                });
            }
        } catch (Throwable ignored) {}
    }

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

    public static boolean isModelAvailable(RaceData race) {
        if (!hasCustomModel(race)) return false;
        ResourceLocation loc = getValidWereModelLocation(race);
        return GeckoLibWereRenderer.isModelPresent(loc, race != null ? race.wereModelPath : null);
    }

    public static ResourceLocation getValidWereModelLocation(RaceData race) {
        return GeckoAssetResolver.resolveModelLocation(race != null ? race.wereModelPath : null);
    }

    public static ResourceLocation getValidWereTextureLocation(AbstractClientPlayer player, RaceData race) {
        return GeckoAssetResolver.resolveTextureLocation(player, race != null ? race.wereTexturePath : null);
    }

    private static final java.util.Map<String, ResourceLocation> DYNAMIC_TEXTURE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    public static ResourceLocation getValidWereTextureLocation(RaceData race) {
        return getValidWereTextureLocation(null, race);
    }

    public static boolean isResourcePresentOnClient(ResourceLocation loc) {
        return GeckoAssetResolver.isResourcePresentOnClient(loc);
    }

    public static ResourceLocation getValidWereAnimationLocation(RaceData race) {
        return GeckoAssetResolver.resolveAnimationLocation(race != null ? race.wereAnimationPath : null);
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
        try {
            java.lang.reflect.Field cloakField = null;
            try {
                cloakField = PlayerModel.class.getDeclaredField("cloak");
            } catch (NoSuchFieldException e) {
                try {
                    cloakField = PlayerModel.class.getDeclaredField("f_103374_");
                } catch (NoSuchFieldException ignored) {}
            }
            if (cloakField != null) {
                cloakField.setAccessible(true);
                net.minecraft.client.model.geom.ModelPart cloak = (net.minecraft.client.model.geom.ModelPart) cloakField.get(model);
                if (cloak != null) cloak.visible = visible;
            }
        } catch (Throwable ignored) {}
        try {
            java.lang.reflect.Field earField = null;
            try {
                earField = PlayerModel.class.getDeclaredField("ear");
            } catch (NoSuchFieldException e) {
                try {
                    earField = PlayerModel.class.getDeclaredField("f_103375_");
                } catch (NoSuchFieldException ignored) {}
            }
            if (earField != null) {
                earField.setAccessible(true);
                net.minecraft.client.model.geom.ModelPart ear = (net.minecraft.client.model.geom.ModelPart) earField.get(model);
                if (ear != null) ear.visible = visible;
            }
        } catch (Throwable ignored) {}
    }

    public static boolean renderWereForm(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> parentModel, RaceData race, float netHeadYaw, float headPitch) {
        if (!isWereForm(player, race)) {
            setBaseModelVisible(parentModel, true);
            return false;
        }

        if (hasCustomModel(race)) {
            ResourceLocation modelLoc = getValidWereModelLocation(race);
            ResourceLocation textureLoc = getValidWereTextureLocation(player, race);
            ResourceLocation animLoc = getValidWereAnimationLocation(race);

            boolean rendered = false;
            try {
                rendered = renderGeckoLibWereModel(poseStack, buffer, packedLight, player, parentModel, modelLoc, textureLoc, animLoc, netHeadYaw, headPitch);
            } catch (Throwable t) {
                rendered = false;
            }

            if (!rendered) {
                // If GeckoLib model fails to bake, load, or render, restore base player model mesh safely
                setBaseModelVisible(parentModel, true);
                return false;
            }

            // Hide human player model mesh ONLY when custom model rendered successfully
            setBaseModelVisible(parentModel, false);
            return true;
        } else {
            // Keep player model visible for procedural overlay fallback
            setBaseModelVisible(parentModel, true);
            return false;
        }
    }

    private static boolean renderGeckoLibWereModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> parentModel, ResourceLocation modelLoc, ResourceLocation textureLoc, ResourceLocation animLoc, float netHeadYaw, float headPitch) {
        RaceData race = ddraig.net.customraces.data.RaceRegistry.getPlayerRace(player.getUUID());
        return GeckoLibWereRenderer.renderGeckoModel(poseStack, buffer, packedLight, player, race, modelLoc, textureLoc, animLoc, netHeadYaw, headPitch);
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
