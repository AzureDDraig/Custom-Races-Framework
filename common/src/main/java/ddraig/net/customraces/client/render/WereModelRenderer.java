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

    public static ResourceLocation getValidWereTextureLocation(RaceData race) {
        if (race == null || race.wereTexturePath == null || race.wereTexturePath.trim().isEmpty() || "none".equalsIgnoreCase(race.wereTexturePath.trim())) {
            return DEFAULT_WERE_TEXTURE;
        }
        String path = race.wereTexturePath.trim();
        ResourceLocation loc = ResourceLocation.tryParse(path);
        if (loc == null) {
            if (LOGGED_WARNINGS.add("texture:" + path)) {
                System.err.println("[CustomRaces] Invalid Were texture path '" + path + "', falling back to default: " + DEFAULT_WERE_TEXTURE);
            }
            return DEFAULT_WERE_TEXTURE;
        }
        return loc;
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

            ResourceLocation textureLoc = getValidWereTextureLocation(race);
            renderCustomWereMesh(poseStack, buffer, packedLight, player, parentModel, textureLoc, netHeadYaw, headPitch);
            return true;
        } else {
            // Keep player model visible for procedural overlay fallback
            setBaseModelVisible(parentModel, true);
            return false;
        }
    }

    private static void renderCustomWereMesh(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> parentModel, ResourceLocation textureLoc, float headYaw, float headPitch) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(textureLoc));

        poseStack.pushPose();

        // Render Werebeast Head Overlay
        poseStack.pushPose();
        parentModel.head.translateAndRotate(poseStack);
        renderBox(poseStack, vc, packedLight, -0.45f, -0.80f, -0.45f, 0.45f, 0.10f, 0.45f);
        // Snout
        renderBox(poseStack, vc, packedLight, -0.20f, -0.30f, -0.75f, 0.20f, -0.05f, -0.45f);
        // Ears
        renderBox(poseStack, vc, packedLight, -0.42f, -1.05f, -0.10f, -0.22f, -0.75f, 0.10f);
        renderBox(poseStack, vc, packedLight, 0.22f, -1.05f, -0.10f, 0.42f, -0.75f, 0.10f);
        poseStack.popPose();

        // Werebeast Body & Limbs Overlay
        poseStack.pushPose();
        parentModel.body.translateAndRotate(poseStack);
        renderBox(poseStack, vc, packedLight, -0.45f, 0.0f, -0.30f, 0.45f, 1.25f, 0.30f);
        poseStack.popPose();

        poseStack.pushPose();
        parentModel.rightArm.translateAndRotate(poseStack);
        renderBox(poseStack, vc, packedLight, -0.30f, -0.20f, -0.25f, 0.15f, 1.25f, 0.25f);
        poseStack.popPose();

        poseStack.pushPose();
        parentModel.leftArm.translateAndRotate(poseStack);
        renderBox(poseStack, vc, packedLight, -0.15f, -0.20f, -0.25f, 0.30f, 1.25f, 0.25f);
        poseStack.popPose();

        poseStack.pushPose();
        parentModel.rightLeg.translateAndRotate(poseStack);
        renderBox(poseStack, vc, packedLight, -0.25f, 0.0f, -0.25f, 0.25f, 1.25f, 0.25f);
        poseStack.popPose();

        poseStack.pushPose();
        parentModel.leftLeg.translateAndRotate(poseStack);
        renderBox(poseStack, vc, packedLight, -0.25f, 0.0f, -0.25f, 0.25f, 1.25f, 0.25f);
        poseStack.popPose();

        poseStack.popPose();
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
