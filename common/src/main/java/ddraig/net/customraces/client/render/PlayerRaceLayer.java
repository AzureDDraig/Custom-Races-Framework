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
        try {
            this.getParentModel().getHead().translateAndRotate(poseStack);

            // Werewolf ears (Crimson & Dark Fur)
            renderColoredBox(poseStack, vc, packedLight, -0.40f, -0.75f, -0.05f, -0.25f, -0.45f, 0.05f, 0.2f, 0.05f, 0.05f, 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.25f, -0.75f, -0.05f, 0.40f, -0.45f, 0.05f, 0.2f, 0.05f, 0.05f, 1.0f);

            // Werewolf snout
            renderColoredBox(poseStack, vc, packedLight, -0.15f, -0.25f, -0.55f, 0.15f, -0.05f, -0.25f, 0.15f, 0.04f, 0.04f, 1.0f);

            // Glowing Crimson Eyes Overlay
            renderColoredBox(poseStack, vc, packedLight, -0.25f, -0.42f, -0.32f, -0.08f, -0.30f, -0.28f, 1.0f, 0.1f, 0.1f, 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.08f, -0.42f, -0.32f, 0.25f, -0.30f, -0.28f, 1.0f, 0.1f, 0.1f, 1.0f);
        } finally {
            poseStack.popPose();
        }
    }

    private static final ResourceLocation WHITE_TEXTURE = new ResourceLocation("minecraft", "textures/misc/white.png");

    private void applyPartTransforms(PoseStack poseStack, PartTransformData pt) {
        if (pt == null) return;
        poseStack.translate(pt.posX, pt.posY, pt.posZ);
        if (pt.rotPitch != 0.0f) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotation((float) Math.toRadians(pt.rotPitch)));
        }
        if (pt.rotYaw != 0.0f) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotation((float) Math.toRadians(pt.rotYaw)));
        }
        if (pt.rotRoll != 0.0f) {
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotation((float) Math.toRadians(pt.rotRoll)));
        }
        poseStack.scale(pt.getSafeScaleX(), pt.getSafeScaleY(), pt.getSafeScaleZ());
    }

    private void renderPresetParts(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, RaceData race, float headYaw, float headPitch) {
        VertexConsumer vc = buffer.getBuffer(RenderType.entityCutoutNoCull(WHITE_TEXTURE));

        // 1. Head Attachments (Ears, Horns, Halo)
        if (!"none".equalsIgnoreCase(race.earType) || !"none".equalsIgnoreCase(race.hornType) || !"none".equalsIgnoreCase(race.haloType)) {
            poseStack.pushPose();
            try {
                this.getParentModel().getHead().translateAndRotate(poseStack);

                // Render Ears
                if (!"none".equalsIgnoreCase(race.earType)) {
                    float[] rgb = parseRGB(race.getColor("ears"));
                    PartTransformData pt = race.partTransforms.get("ears");
                    poseStack.pushPose();
                    try {
                        applyPartTransforms(poseStack, pt);
                        renderEarsGeometry(poseStack, vc, packedLight, race.earType, rgb);
                    } finally {
                        poseStack.popPose();
                    }
                }

                // Render Horns
                if (!"none".equalsIgnoreCase(race.hornType)) {
                    float[] rgb = parseRGB(race.getColor("horns"));
                    PartTransformData pt = race.partTransforms.get("horns");
                    poseStack.pushPose();
                    try {
                        applyPartTransforms(poseStack, pt);
                        renderHornsGeometry(poseStack, vc, packedLight, race.hornType, rgb);
                    } finally {
                        poseStack.popPose();
                    }
                }

                // Render Halo
                if (!"none".equalsIgnoreCase(race.haloType)) {
                    float[] rgb = parseRGB(race.getColor("halo"));
                    PartTransformData pt = race.partTransforms.get("halo");
                    poseStack.pushPose();
                    try {
                        applyPartTransforms(poseStack, pt);
                        renderHaloGeometry(poseStack, vc, packedLight, race.haloType, rgb);
                    } finally {
                        poseStack.popPose();
                    }
                }
            } finally {
                poseStack.popPose();
            }
        }

        // 2. Body Attachments (Wings, Tail, Extra Legs, Custom Part)
        boolean hasWings = !"none".equalsIgnoreCase(race.wingType);
        boolean hasTail = !"none".equalsIgnoreCase(race.tailType);
        boolean hasExtraLegs = !"human".equalsIgnoreCase(race.legType) && race.legCount > 2;
        boolean hasCustomPart = !"none".equalsIgnoreCase(race.customPartId);

        if (hasWings || hasTail || hasExtraLegs || hasCustomPart) {
            poseStack.pushPose();
            try {
                this.getParentModel().body.translateAndRotate(poseStack);

                // Render Wings
                if (hasWings) {
                    float[] rgb = parseRGB(race.getColor("wings"));
                    PartTransformData pt = race.partTransforms.get("wings");

                    boolean isFlying = player.getAbilities().flying || !player.onGround();
                    float flapAngle = isFlying ? (float) (Math.sin(player.tickCount * 0.45f) * 0.4f) : 0.0f;

                    // Left Wing Panel
                    poseStack.pushPose();
                    try {
                        applyPartTransforms(poseStack, pt);
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotation(flapAngle));
                        renderWingGeometry(poseStack, vc, packedLight, race.wingType, rgb, true);
                    } finally {
                        poseStack.popPose();
                    }

                    // Right Wing Panel
                    poseStack.pushPose();
                    try {
                        applyPartTransforms(poseStack, pt);
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotation(-flapAngle));
                        renderWingGeometry(poseStack, vc, packedLight, race.wingType, rgb, false);
                    } finally {
                        poseStack.popPose();
                    }
                }

                // Render Tail
                if (hasTail) {
                    float[] rgb = parseRGB(race.getColor("tail"));
                    PartTransformData pt = race.partTransforms.get("tail");
                    poseStack.pushPose();
                    try {
                        applyPartTransforms(poseStack, pt);
                        renderTailGeometry(poseStack, vc, packedLight, race.tailType, rgb);
                    } finally {
                        poseStack.popPose();
                    }
                }

                // Render Extra Legs (Preset #6)
                if (hasExtraLegs) {
                    float[] rgb = parseRGB(race.getColor("legs"));
                    PartTransformData pt = race.partTransforms.get("legs");
                    poseStack.pushPose();
                    try {
                        applyPartTransforms(poseStack, pt);
                        renderExtraLegsGeometry(poseStack, vc, packedLight, race.legType, race.legCount, rgb);
                    } finally {
                        poseStack.popPose();
                    }
                }

                // Render Custom Part
                if (hasCustomPart) {
                    float[] rgb = parseRGB(race.getColor("custom"));
                    PartTransformData pt = race.partTransforms.get("custom");
                    poseStack.pushPose();
                    try {
                        applyPartTransforms(poseStack, pt);
                        renderColoredBox(poseStack, vc, packedLight, -0.20f, -0.20f, -0.20f, 0.20f, 0.20f, 0.20f, rgb[0], rgb[1], rgb[2], 1.0f);
                    } finally {
                        poseStack.popPose();
                    }
                }
            } finally {
                poseStack.popPose();
            }
        }
    }

    private void renderEarsGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String earType, float[] rgb) {
        String type = earType.toLowerCase();
        if ("dog".equals(type)) {
            // Dog: Floppy/slanted ears
            renderColoredBox(poseStack, vc, packedLight, -0.42f, -0.50f, -0.05f, -0.25f, -0.10f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.25f, -0.50f, -0.05f, 0.42f, -0.10f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else if ("cat".equals(type)) {
            // Cat: Pointed ears with inner ear contrast
            renderColoredBox(poseStack, vc, packedLight, -0.35f, -0.75f, -0.05f, -0.20f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.20f, -0.75f, -0.05f, 0.35f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
            // Inner ear accents (slightly lighter)
            renderColoredBox(poseStack, vc, packedLight, -0.32f, -0.70f, -0.06f, -0.23f, -0.45f, -0.05f, Math.min(1.0f, rgb[0] + 0.2f), Math.min(1.0f, rgb[1] + 0.2f), Math.min(1.0f, rgb[2] + 0.2f), 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.23f, -0.70f, -0.06f, 0.32f, -0.45f, -0.05f, Math.min(1.0f, rgb[0] + 0.2f), Math.min(1.0f, rgb[1] + 0.2f), Math.min(1.0f, rgb[2] + 0.2f), 1.0f);
        } else if ("dragon".equals(type)) {
            // Dragon: Webbed fin-like flared ears
            renderColoredBox(poseStack, vc, packedLight, -0.55f, -0.50f, -0.05f, -0.25f, -0.38f, 0.05f, rgb[0], rgb[1], rgb[2], 0.95f);
            renderColoredBox(poseStack, vc, packedLight, 0.25f, -0.50f, -0.05f, 0.55f, -0.38f, 0.05f, rgb[0], rgb[1], rgb[2], 0.95f);
        } else if ("bunny".equals(type)) {
            // Bunny: Long upright rabbit ears
            renderColoredBox(poseStack, vc, packedLight, -0.30f, -1.10f, -0.05f, -0.18f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.18f, -1.10f, -0.05f, 0.30f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else {
            // Standard/Default Ear Cuboids
            renderColoredBox(poseStack, vc, packedLight, -0.35f, -0.65f, -0.05f, -0.22f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.22f, -0.65f, -0.05f, 0.35f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
        }
    }

    private void renderHornsGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String hornType, float[] rgb) {
        String type = hornType.toLowerCase();
        if ("demon".equals(type)) {
            // Demon: Steep curved backward horns
            renderColoredBox(poseStack, vc, packedLight, -0.22f, -0.90f, -0.15f, -0.12f, -0.50f, -0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.12f, -0.90f, -0.15f, 0.22f, -0.50f, -0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else if ("ram".equals(type)) {
            // Ram: Wide curling horns wrapping outward around head sides
            renderColoredBox(poseStack, vc, packedLight, -0.45f, -0.60f, -0.20f, -0.18f, -0.40f, 0.10f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.18f, -0.60f, -0.20f, 0.45f, -0.40f, 0.10f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else if ("dragon".equals(type)) {
            // Dragon: Swept-back multi-pronged spiky horns
            renderColoredBox(poseStack, vc, packedLight, -0.20f, -0.80f, 0.00f, -0.10f, -0.50f, 0.30f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.10f, -0.80f, 0.00f, 0.20f, -0.50f, 0.30f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else if ("unicorn".equals(type)) {
            // Unicorn: Single center forehead horn
            renderColoredBox(poseStack, vc, packedLight, -0.05f, -0.95f, -0.30f, 0.05f, -0.45f, -0.18f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else {
            // Standard Horn Cuboids
            renderColoredBox(poseStack, vc, packedLight, -0.20f, -0.70f, -0.15f, -0.12f, -0.50f, -0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.12f, -0.70f, -0.15f, 0.20f, -0.50f, -0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
        }
    }

    private void renderHaloGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String haloType, float[] rgb) {
        String type = haloType.toLowerCase();
        if ("angel".equals(type)) {
            // Angel: Floating luminous ring
            renderColoredBox(poseStack, vc, packedLight, -0.35f, -0.80f, -0.35f, 0.35f, -0.76f, 0.35f, rgb[0], rgb[1], rgb[2], 0.95f);
        } else if ("flower".equals(type)) {
            // Flower: Halo ring surrounded by 4 floral petal accents
            renderColoredBox(poseStack, vc, packedLight, -0.32f, -0.78f, -0.32f, 0.32f, -0.74f, 0.32f, rgb[0], rgb[1], rgb[2], 0.9f);
            renderColoredBox(poseStack, vc, packedLight, -0.38f, -0.80f, -0.05f, -0.30f, -0.72f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.30f, -0.80f, -0.05f, 0.38f, -0.72f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, -0.05f, -0.80f, -0.38f, 0.05f, -0.72f, -0.30f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, -0.05f, -0.80f, 0.30f, 0.05f, -0.72f, 0.38f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else if ("demon".equals(type)) {
            // Demon: Dark spiky ring halo
            renderColoredBox(poseStack, vc, packedLight, -0.30f, -0.75f, -0.30f, 0.30f, -0.71f, 0.30f, rgb[0], rgb[1], rgb[2], 0.9f);
            renderColoredBox(poseStack, vc, packedLight, -0.28f, -0.82f, -0.28f, -0.24f, -0.75f, -0.24f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, 0.24f, -0.82f, -0.28f, 0.28f, -0.75f, -0.24f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else {
            // Standard Halo Ring
            renderColoredBox(poseStack, vc, packedLight, -0.30f, -0.75f, -0.30f, 0.30f, -0.71f, 0.30f, rgb[0], rgb[1], rgb[2], 0.9f);
        }
    }

    private void renderWingGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String wingType, float[] rgb, boolean isLeft) {
        String type = wingType.toLowerCase();
        if ("feathered".equals(type)) {
            if (isLeft) {
                renderColoredBox(poseStack, vc, packedLight, -0.85f, -0.10f, 0.15f, -0.15f, 0.70f, 0.20f, rgb[0], rgb[1], rgb[2], 0.95f);
                renderColoredBox(poseStack, vc, packedLight, -0.95f, 0.20f, 0.16f, -0.25f, 0.95f, 0.21f, rgb[0] * 0.9f, rgb[1] * 0.9f, rgb[2] * 0.9f, 0.95f);
            } else {
                renderColoredBox(poseStack, vc, packedLight, 0.15f, -0.10f, 0.15f, 0.85f, 0.70f, 0.20f, rgb[0], rgb[1], rgb[2], 0.95f);
                renderColoredBox(poseStack, vc, packedLight, 0.25f, 0.20f, 0.16f, 0.95f, 0.95f, 0.21f, rgb[0] * 0.9f, rgb[1] * 0.9f, rgb[2] * 0.9f, 0.95f);
            }
        } else if ("dragon".equals(type)) {
            if (isLeft) {
                renderColoredBox(poseStack, vc, packedLight, -0.90f, 0.0f, 0.15f, -0.15f, 0.85f, 0.20f, rgb[0], rgb[1], rgb[2], 0.90f);
                renderColoredBox(poseStack, vc, packedLight, -0.88f, -0.05f, 0.14f, -0.17f, 0.10f, 0.21f, rgb[0] * 0.7f, rgb[1] * 0.7f, rgb[2] * 0.7f, 1.0f);
            } else {
                renderColoredBox(poseStack, vc, packedLight, 0.15f, 0.0f, 0.15f, 0.90f, 0.85f, 0.20f, rgb[0], rgb[1], rgb[2], 0.90f);
                renderColoredBox(poseStack, vc, packedLight, 0.17f, -0.05f, 0.14f, 0.88f, 0.10f, 0.21f, rgb[0] * 0.7f, rgb[1] * 0.7f, rgb[2] * 0.7f, 1.0f);
            }
        } else {
            if (isLeft) {
                renderColoredBox(poseStack, vc, packedLight, -0.85f, 0.0f, 0.15f, -0.15f, 0.80f, 0.20f, rgb[0], rgb[1], rgb[2], 0.95f);
            } else {
                renderColoredBox(poseStack, vc, packedLight, 0.15f, 0.0f, 0.15f, 0.85f, 0.80f, 0.20f, rgb[0], rgb[1], rgb[2], 0.95f);
            }
        }
    }

    private void renderTailGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String tailType, float[] rgb) {
        String type = tailType.toLowerCase();
        if ("dog".equals(type)) {
            // Dog: Bushy upward tail
            renderColoredBox(poseStack, vc, packedLight, -0.08f, 0.50f, 0.15f, 0.08f, 1.05f, 0.45f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else if ("cat".equals(type)) {
            // Cat: Slender long tail box
            renderColoredBox(poseStack, vc, packedLight, -0.04f, 0.60f, 0.15f, 0.04f, 1.35f, 0.35f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else if ("camel".equals(type)) {
            // Camel: Hump / tail tuft
            renderColoredBox(poseStack, vc, packedLight, -0.15f, 0.20f, 0.15f, 0.15f, 0.60f, 0.45f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else if ("fish".equals(type)) {
            // Fish: Tail body with caudal fin span
            renderColoredBox(poseStack, vc, packedLight, -0.04f, 0.65f, 0.15f, 0.04f, 1.20f, 0.50f, rgb[0], rgb[1], rgb[2], 1.0f);
            renderColoredBox(poseStack, vc, packedLight, -0.25f, 1.00f, 0.45f, 0.25f, 1.30f, 0.52f, rgb[0], rgb[1], rgb[2], 0.95f);
        } else if ("dragon".equals(type)) {
            // Dragon: Thick spiky tail
            renderColoredBox(poseStack, vc, packedLight, -0.10f, 0.60f, 0.15f, 0.10f, 1.30f, 0.70f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else {
            // Standard rear vertical tail
            renderColoredBox(poseStack, vc, packedLight, -0.06f, 0.65f, 0.15f, 0.06f, 1.25f, 0.65f, rgb[0], rgb[1], rgb[2], 1.0f);
        }
    }

    private void renderExtraLegsGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String legType, int legCount, float[] rgb) {
        String type = legType != null ? legType.toLowerCase() : "human";
        if ("spider".equals(type)) {
            int extraPairs = Math.max(1, (legCount - 2) / 2);
            for (int i = 0; i < extraPairs; i++) {
                float zOff = (i - extraPairs / 2.0f + 0.5f) * 0.25f;
                // Left spider leg
                renderColoredBox(poseStack, vc, packedLight, -0.75f, 0.50f + (i * 0.05f), zOff, -0.15f, 1.20f, zOff + 0.08f, rgb[0], rgb[1], rgb[2], 1.0f);
                // Right spider leg
                renderColoredBox(poseStack, vc, packedLight, 0.15f, 0.50f + (i * 0.05f), zOff, 0.75f, 1.20f, zOff + 0.08f, rgb[0], rgb[1], rgb[2], 1.0f);
            }
        } else if ("centaur".equals(type)) {
            // Rear quadruped body extension
            renderColoredBox(poseStack, vc, packedLight, -0.30f, 0.40f, 0.20f, 0.30f, 0.85f, 1.00f, rgb[0], rgb[1], rgb[2], 1.0f);
            // Rear left leg
            renderColoredBox(poseStack, vc, packedLight, -0.28f, 0.85f, 0.70f, -0.10f, 1.50f, 0.90f, rgb[0], rgb[1], rgb[2], 1.0f);
            // Rear right leg
            renderColoredBox(poseStack, vc, packedLight, 0.10f, 0.85f, 0.70f, 0.28f, 1.50f, 0.90f, rgb[0], rgb[1], rgb[2], 1.0f);
        } else {
            // Generic extra leg pairs
            int extraPairs = Math.max(1, (legCount - 2) / 2);
            for (int i = 0; i < extraPairs; i++) {
                float zOff = (i + 1) * 0.25f;
                renderColoredBox(poseStack, vc, packedLight, -0.35f, 0.60f, zOff, -0.15f, 1.30f, zOff + 0.10f, rgb[0], rgb[1], rgb[2], 1.0f);
                renderColoredBox(poseStack, vc, packedLight, 0.15f, 0.60f, zOff, 0.35f, 1.30f, zOff + 0.10f, rgb[0], rgb[1], rgb[2], 1.0f);
            }
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
