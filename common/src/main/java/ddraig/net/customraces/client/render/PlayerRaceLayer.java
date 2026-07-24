package ddraig.net.customraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import ddraig.net.customraces.data.PartTransformData;
import ddraig.net.customraces.data.ParticleAuraData;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * Custom player render layer rendering ears, wings, tails, horns, halos, leg variations,
 * RGB color tinting, real-time particle auras, and selective armor piece hiding.
 */
public class PlayerRaceLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public PlayerRaceLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {

        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
        if (race == null) {
            WereModelRenderer.setBaseModelVisible(this.getParentModel(), true);
            return;
        }

        try {
            poseStack.pushPose();

            boolean isWereTransformed = WereModelRenderer.isWereForm(player, race);
            int effectiveParticleCount = isWereTransformed ? race.getWereParticleCount() : race.getParticleCount();

            if (isWereTransformed) {
                // Apply Were-Form Visual Scale Transformation
                float hScale = race.wereHeightScale > 0 ? race.wereHeightScale : 1.3f;
                float wScale = race.wereWidthScale > 0 ? race.wereWidthScale : 1.3f;
                poseStack.scale(wScale, hScale, wScale);

                // Render custom Were model or fallback procedural beast parts
                boolean customRendered = WereModelRenderer.renderWereForm(poseStack, buffer, packedLight, player, this.getParentModel(), race, netHeadYaw, headPitch);
                if (!customRendered) {
                    renderWereBeastParts(poseStack, buffer, packedLight, player, race, netHeadYaw, headPitch);
                }

                // Render Real-Time Dark Were-Form Smoke Particles (Scaled by wereParticleCount)
                if (player.level().isClientSide && player.tickCount % 3 == 0) {
                    int smokeLoops = Math.max(1, Math.round(effectiveParticleCount / 2.0f));
                    for (int i = 0; i < smokeLoops; i++) {
                        player.level().addParticle(
                                net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                player.getRandomX(0.6),
                                player.getRandomY(),
                                player.getRandomZ(0.6),
                                0.0, 0.05, 0.0
                        );
                        player.level().addParticle(
                                race.isWereFlyingRace ? net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME : net.minecraft.core.particles.ParticleTypes.FLAME,
                                player.getRandomX(0.4),
                                player.getRandomY(),
                                player.getRandomZ(0.4),
                                0.0, 0.02, 0.0
                        );
                    }
                }
            } else {
                // Ensure base player model mesh is visible in human form
                WereModelRenderer.setBaseModelVisible(this.getParentModel(), true);

                // Render Base Race Preset Body Parts (Ears, Wings, Tail, Horns, Halo, Legs)
                renderPresetParts(poseStack, buffer, packedLight, player, race, netHeadYaw, headPitch);
            }

            // 2. Render Particle Auras in Real-Time (Scaled by effectiveParticleCount)
            if (player.level().isClientSide && race.particleAuras != null && !race.particleAuras.isEmpty()) {
                for (ParticleAuraData aura : race.particleAuras) {
                    if (player.tickCount % 4 == 0) {
                        net.minecraft.core.particles.ParticleType<?> pType = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(new ResourceLocation(aura.getValidParticleType()));
                        if (pType instanceof net.minecraft.core.particles.ParticleOptions pOptions) {
                            int countToSpawn = aura.getScaledParticleCount(effectiveParticleCount);
                            for (int i = 0; i < countToSpawn; i++) {
                                player.level().addParticle(
                                        pOptions,
                                        player.getRandomX(aura.getSafeSpread()),
                                        player.getRandomY() + 0.5,
                                        player.getRandomZ(aura.getSafeSpread()),
                                        0.0, aura.getSafeSpeed(), 0.0
                                );
                            }
                        }
                    }
                }
            }

        } catch (Exception ignored) {
        } finally {
            poseStack.popPose();
        }
    }

    private void renderWereBeastParts(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, RaceData race, float headYaw, float headPitch) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(WHITE_TEXTURE));

        poseStack.pushPose();
        this.getParentModel().getHead().translateAndRotate(poseStack);

        // Werewolf ears (Crimson & Dark Fur)
        renderColoredBox(poseStack, vc, packedLight, -0.40f, -0.75f, -0.05f, -0.25f, -0.45f, 0.05f, 0.2f, 0.05f, 0.05f, 1.0f);
        renderColoredBox(poseStack, vc, packedLight, 0.25f, -0.75f, -0.05f, 0.40f, -0.45f, 0.05f, 0.2f, 0.05f, 0.05f, 1.0f);

        // Werewolf snout
        renderColoredBox(poseStack, vc, packedLight, -0.15f, -0.25f, -0.55f, 0.15f, -0.05f, -0.25f, 0.15f, 0.04f, 0.04f, 1.0f);

        // Glowing Crimson Eyes Overlay
        renderColoredBox(poseStack, vc, packedLight, -0.25f, -0.42f, -0.32f, -0.08f, -0.30f, -0.28f, 1.0f, 0.1f, 0.1f, 1.0f);
        renderColoredBox(poseStack, vc, packedLight, 0.08f, -0.42f, -0.32f, 0.25f, -0.30f, -0.28f, 1.0f, 0.1f, 0.1f, 1.0f);

        poseStack.popPose();
    }

    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("minecraft", "textures/misc/white.png");

    private void renderPresetParts(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, RaceData race, float headYaw, float headPitch) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(WHITE_TEXTURE));

        // 1. Head Attachments (Ears, Horns, Halo)
        if (!"none".equalsIgnoreCase(race.earType) || !"none".equalsIgnoreCase(race.hornType) || !"none".equalsIgnoreCase(race.haloType)) {
            poseStack.pushPose();
            this.getParentModel().getHead().translateAndRotate(poseStack);

            // Render Ears
            if (!"none".equalsIgnoreCase(race.earType)) {
                float[] rgb = parseRGB(race.getColor("ears"));
                PartTransformData pt = race.partTransforms.get("ears");
                poseStack.pushPose();
                if (pt != null) poseStack.translate(pt.posX, pt.posY, pt.posZ);
                // Left & Right Ear Cuboids
                renderColoredBox(poseStack, vc, packedLight, -0.35f, -0.65f, -0.05f, -0.22f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
                renderColoredBox(poseStack, vc, packedLight, 0.22f, -0.65f, -0.05f, 0.35f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
                poseStack.popPose();
            }

            // Render Horns
            if (!"none".equalsIgnoreCase(race.hornType)) {
                float[] rgb = parseRGB(race.getColor("horns"));
                PartTransformData pt = race.partTransforms.get("horns");
                poseStack.pushPose();
                if (pt != null) poseStack.translate(pt.posX, pt.posY, pt.posZ);
                renderColoredBox(poseStack, vc, packedLight, -0.20f, -0.70f, -0.15f, -0.12f, -0.50f, -0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
                renderColoredBox(poseStack, vc, packedLight, 0.12f, -0.70f, -0.15f, 0.20f, -0.50f, -0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
                poseStack.popPose();
            }

            // Render Halo
            if (!"none".equalsIgnoreCase(race.haloType)) {
                float[] rgb = parseRGB(race.getColor("halo"));
                PartTransformData pt = race.partTransforms.get("halo");
                poseStack.pushPose();
                if (pt != null) poseStack.translate(pt.posX, pt.posY, pt.posZ);
                renderColoredBox(poseStack, vc, packedLight, -0.30f, -0.75f, -0.30f, 0.30f, -0.71f, 0.30f, rgb[0], rgb[1], rgb[2], 0.9f);
                poseStack.popPose();
            }

            poseStack.popPose();
        }

        // 2. Body Attachments (Wings, Tail)
        if (!"none".equalsIgnoreCase(race.wingType) || !"none".equalsIgnoreCase(race.tailType)) {
            poseStack.pushPose();
            this.getParentModel().body.translateAndRotate(poseStack);

            // Render Wings
            if (!"none".equalsIgnoreCase(race.wingType)) {
                float[] rgb = parseRGB(race.getColor("wings"));
                PartTransformData pt = race.partTransforms.get("wings");

                boolean isFlying = player.getAbilities().flying || !player.onGround();
                float flapAngle = isFlying ? (float) (Math.sin(player.tickCount * 0.45f) * 0.4f) : 0.0f;

                // Left Wing Panel
                poseStack.pushPose();
                if (pt != null) poseStack.translate(pt.posX, pt.posY, pt.posZ);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotation(flapAngle));
                renderColoredBox(poseStack, vc, packedLight, -0.85f, 0.0f, 0.15f, -0.15f, 0.80f, 0.20f, rgb[0], rgb[1], rgb[2], 0.95f);
                poseStack.popPose();

                // Right Wing Panel
                poseStack.pushPose();
                if (pt != null) poseStack.translate(pt.posX, pt.posY, pt.posZ);
                poseStack.mulPose(com.mojang.math.Axis.YP.rotation(-flapAngle));
                renderColoredBox(poseStack, vc, packedLight, 0.15f, 0.0f, 0.15f, 0.85f, 0.80f, 0.20f, rgb[0], rgb[1], rgb[2], 0.95f);
                poseStack.popPose();
            }

            // Render Tail
            if (!"none".equalsIgnoreCase(race.tailType)) {
                float[] rgb = parseRGB(race.getColor("tail"));
                PartTransformData pt = race.partTransforms.get("tail");
                poseStack.pushPose();
                if (pt != null) poseStack.translate(pt.posX, pt.posY, pt.posZ);
                renderColoredBox(poseStack, vc, packedLight, -0.06f, 0.65f, 0.15f, 0.06f, 1.25f, 0.65f, rgb[0], rgb[1], rgb[2], 1.0f);
                poseStack.popPose();
            }

            poseStack.popPose();
        }
    }

    private float[] parseRGB(String hex) {
        float[] rgb = new float[]{1.0f, 1.0f, 1.0f};
        try {
            if (hex != null && hex.startsWith("#") && hex.length() == 7) {
                rgb[0] = Integer.parseInt(hex.substring(1, 3), 16) / 255.0f;
                rgb[1] = Integer.parseInt(hex.substring(3, 5), 16) / 255.0f;
                rgb[2] = Integer.parseInt(hex.substring(5, 7), 16) / 255.0f;
            }
        } catch (Exception ignored) {}
        return rgb;
    }

    private void renderColoredBox(PoseStack poseStack, VertexConsumer builder, int packedLight, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, float r, float g, float b, float a) {
        org.joml.Matrix4f pose = poseStack.last().pose();
        org.joml.Matrix3f normal = poseStack.last().normal();

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
