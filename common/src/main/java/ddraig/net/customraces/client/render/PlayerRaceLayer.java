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

    private static final java.util.Map<java.util.UUID, Integer> LAST_PARTICLE_TICKS = new java.util.concurrent.ConcurrentHashMap<>();

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

            boolean isInvisible = player.isInvisible() || player.isSpectator();
            if (isInvisible) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                net.minecraft.client.player.LocalPlayer clientPlayer = mc != null ? mc.player : null;
                if (clientPlayer != null && player.isInvisibleTo(clientPlayer)) {
                    // Completely invisible to viewing player: skip custom layer rendering & particles
                    return;
                }
            }

            // 20 Hz Tick Check Guard for Particle Emission (spawns particles once per 20 Hz tick across framerates)
            boolean canEmitTickParticle = false;
            if (!isInvisible && player.level().isClientSide) {
                if (LAST_PARTICLE_TICKS.size() > 1000) LAST_PARTICLE_TICKS.clear();
                Integer lastTick = LAST_PARTICLE_TICKS.get(player.getUUID());
                if (lastTick == null || lastTick != player.tickCount) {
                    LAST_PARTICLE_TICKS.put(player.getUUID(), player.tickCount);
                    canEmitTickParticle = true;
                }
            }

            boolean isWereTransformed = WereModelRenderer.isWereForm(player, race);
            int effectiveParticleCount = isWereTransformed ? race.getWereParticleCount() : race.getParticleCount();
            float hScale = isWereTransformed ? (race.wereHeightScale > 0 ? race.wereHeightScale : 1.3f) : 1.0f;
            float wScale = isWereTransformed ? (race.wereWidthScale > 0 ? race.wereWidthScale : 1.3f) : 1.0f;
            float scaleFactor = Math.max(wScale, hScale);

            if (isWereTransformed) {
                // Apply Were-Form Visual Scale Transformation (guarded against Pehkui double-scaling)
                if (!ddraig.net.customraces.integration.PehkuiIntegration.isPehkuiLoaded()) {
                    poseStack.scale(wScale, hScale, wScale);
                }

                // Render custom Were model or fallback procedural beast parts
                boolean customRendered = WereModelRenderer.renderWereForm(poseStack, buffer, packedLight, player, this.getParentModel(), race, netHeadYaw, headPitch);
                if (!customRendered) {
                    renderWereBeastParts(poseStack, buffer, packedLight, player, race, netHeadYaw, headPitch);
                }

                // Render Real-Time Dark Were-Form Smoke Particles (Scaled by player scale during transformed state, guarded by 20 Hz tick check)
                if (canEmitTickParticle && player.tickCount % 3 == 0) {
                    int smokeLoops = Math.max(1, Math.round(effectiveParticleCount / 2.0f));
                    for (int i = 0; i < smokeLoops; i++) {
                        player.level().addParticle(
                                net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                                player.getRandomX(0.6 * wScale),
                                player.getRandomY(),
                                player.getRandomZ(0.6 * wScale),
                                0.0, 0.05 * scaleFactor, 0.0
                        );
                        player.level().addParticle(
                                race.isWereFlyingRace ? net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME : net.minecraft.core.particles.ParticleTypes.FLAME,
                                player.getRandomX(0.4 * wScale),
                                player.getRandomY(),
                                player.getRandomZ(0.4 * wScale),
                                0.0, 0.02 * scaleFactor, 0.0
                        );
                    }
                }
            } else {
                // Ensure base player model mesh is visible in human form
                WereModelRenderer.setBaseModelVisible(this.getParentModel(), true);

                // Render Base Race Preset Body Parts (Ears, Wings, Tail, Horns, Halo, Legs)
                renderPresetParts(poseStack, buffer, packedLight, player, race, netHeadYaw, headPitch);
            }

            // Render Particle Auras in Real-Time (Scaled by player scale during transformed state, guarded by 20 Hz tick check)
            if (canEmitTickParticle && race.particleAuras != null && !race.particleAuras.isEmpty()) {
                for (ParticleAuraData aura : race.particleAuras) {
                    if (aura == null || !aura.matchesForm(isWereTransformed)) continue;
                    if (player.tickCount % 4 == 0) {
                        ResourceLocation pLoc = ResourceLocation.tryParse(aura.getValidParticleType());
                        if (pLoc != null) {
                            net.minecraft.core.particles.ParticleType<?> pType = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(pLoc);
                            if (pType instanceof net.minecraft.core.particles.ParticleOptions pOptions) {
                                int countToSpawn = aura.getScaledParticleCount(effectiveParticleCount);
                                float auraSpread = aura.getSafeSpread() * wScale;
                                float auraSpeed = aura.getSafeSpeed() * scaleFactor;
                                String placement = aura.getValidPlacement();

                                double baseX = player.getX() + (aura.offsetX * wScale);
                                double baseZ = player.getZ() + (aura.offsetZ * wScale);
                                double baseY;

                                switch (placement) {
                                    case "head":
                                        baseY = player.getY() + (player.getEyeHeight() * 0.95) + (aura.offsetY * hScale);
                                        break;
                                    case "eyes":
                                        baseY = player.getY() + player.getEyeHeight() + (aura.offsetY * hScale);
                                        break;
                                    case "feet":
                                        baseY = player.getY() + 0.1 + (aura.offsetY * hScale);
                                        break;
                                    case "hands":
                                        baseY = player.getY() + (0.75 * hScale) + (aura.offsetY * hScale);
                                        break;
                                    case "ambient":
                                        baseY = player.getY() + (player.level().random.nextDouble() * 2.0 * hScale) + (aura.offsetY * hScale);
                                        break;
                                    case "body":
                                    default:
                                        baseY = player.getY() + (0.5 * hScale) + (aura.offsetY * hScale);
                                        break;
                                }

                                for (int i = 0; i < countToSpawn; i++) {
                                    double px = baseX + (player.level().random.nextGaussian() * auraSpread * 0.5);
                                    double pz = baseZ + (player.level().random.nextGaussian() * auraSpread * 0.5);
                                    double py = baseY + (player.level().random.nextGaussian() * 0.1);
                                    player.level().addParticle(
                                            pOptions,
                                            px, py, pz,
                                            0.0, auraSpeed, 0.0
                                    );
                                }
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
        if (WereModelRenderer.isFirstPerson(player)) {
            return;
        }
        boolean isInvisible = player.isInvisible() || player.isSpectator();
        RenderType renderType = isInvisible ? RenderType.entityTranslucent(WHITE_TEXTURE) : RenderType.entityCutoutNoCull(WHITE_TEXTURE);
        VertexConsumer vc = buffer.getBuffer(renderType);
        float alpha = isInvisible ? 0.15f : 1.0f;

        poseStack.pushPose();
        try {
            this.getParentModel().getHead().translateAndRotate(poseStack);

            // Werewolf ears (Crimson & Dark Fur)
            renderColoredBox(poseStack, vc, packedLight, -0.40f, -0.75f, -0.05f, -0.25f, -0.45f, 0.05f, 0.2f, 0.05f, 0.05f, alpha);
            renderColoredBox(poseStack, vc, packedLight, 0.25f, -0.75f, -0.05f, 0.40f, -0.45f, 0.05f, 0.2f, 0.05f, 0.05f, alpha);

            // Werewolf snout
            renderColoredBox(poseStack, vc, packedLight, -0.15f, -0.25f, -0.55f, 0.15f, -0.05f, -0.25f, 0.15f, 0.04f, 0.04f, alpha);

            // Glowing Crimson Eyes Overlay
            renderColoredBox(poseStack, vc, packedLight, -0.25f, -0.42f, -0.32f, -0.08f, -0.30f, -0.28f, 1.0f, 0.1f, 0.1f, alpha);
            renderColoredBox(poseStack, vc, packedLight, 0.08f, -0.42f, -0.32f, 0.25f, -0.30f, -0.28f, 1.0f, 0.1f, 0.1f, alpha);
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
        boolean isInvisible = player.isInvisible() || player.isSpectator();
        RenderType renderType = isInvisible ? RenderType.entityTranslucent(WHITE_TEXTURE) : RenderType.entityCutoutNoCull(WHITE_TEXTURE);
        VertexConsumer vc = buffer.getBuffer(renderType);
        float baseAlpha = isInvisible ? 0.15f : 1.0f;

        boolean isFirstPerson = WereModelRenderer.isFirstPerson(player);

        // 1. Head Attachments (Ears, Horns, Halo) - Suppressed in first person to prevent camera clipping
        if (!isFirstPerson && (!"none".equalsIgnoreCase(race.earType) || !"none".equalsIgnoreCase(race.hornType) || !"none".equalsIgnoreCase(race.haloType))) {
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
                        renderEarsGeometry(poseStack, vc, packedLight, race.earType, rgb, baseAlpha);
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
                        renderHornsGeometry(poseStack, vc, packedLight, race.hornType, rgb, baseAlpha);
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
                        renderHaloGeometry(poseStack, vc, packedLight, race.haloType, rgb, baseAlpha);
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
                        renderWingGeometry(poseStack, vc, packedLight, race.wingType, rgb, true, baseAlpha);
                    } finally {
                        poseStack.popPose();
                    }

                    // Right Wing Panel
                    poseStack.pushPose();
                    try {
                        applyPartTransforms(poseStack, pt);
                        poseStack.mulPose(com.mojang.math.Axis.YP.rotation(-flapAngle));
                        renderWingGeometry(poseStack, vc, packedLight, race.wingType, rgb, false, baseAlpha);
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
                        renderTailGeometry(poseStack, vc, packedLight, race.tailType, rgb, baseAlpha);
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
                        renderExtraLegsGeometry(poseStack, vc, packedLight, race.legType, race.legCount, rgb, baseAlpha);
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
                        renderColoredBox(poseStack, vc, packedLight, -0.20f, -0.20f, -0.20f, 0.20f, 0.20f, 0.20f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
                    } finally {
                        poseStack.popPose();
                    }
                }
            } finally {
                poseStack.popPose();
            }
        }
    }

    private void renderEarsGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String earType, float[] rgb, float baseAlpha) {
        String type = earType.toLowerCase();
        if ("dog".equals(type)) {
            // Dog: Floppy/slanted ears
            renderColoredBox(poseStack, vc, packedLight, -0.42f, -0.50f, -0.05f, -0.25f, -0.10f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.25f, -0.50f, -0.05f, 0.42f, -0.10f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else if ("cat".equals(type)) {
            // Cat: Pointed ears with inner ear contrast
            renderColoredBox(poseStack, vc, packedLight, -0.35f, -0.75f, -0.05f, -0.20f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.20f, -0.75f, -0.05f, 0.35f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            // Inner ear accents (slightly lighter)
            renderColoredBox(poseStack, vc, packedLight, -0.32f, -0.70f, -0.06f, -0.23f, -0.45f, -0.05f, Math.min(1.0f, rgb[0] + 0.2f), Math.min(1.0f, rgb[1] + 0.2f), Math.min(1.0f, rgb[2] + 0.2f), 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.23f, -0.70f, -0.06f, 0.32f, -0.45f, -0.05f, Math.min(1.0f, rgb[0] + 0.2f), Math.min(1.0f, rgb[1] + 0.2f), Math.min(1.0f, rgb[2] + 0.2f), 1.0f * baseAlpha);
        } else if ("dragon".equals(type)) {
            // Dragon: Webbed fin-like flared ears
            renderColoredBox(poseStack, vc, packedLight, -0.55f, -0.50f, -0.05f, -0.25f, -0.38f, 0.05f, rgb[0], rgb[1], rgb[2], 0.95f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.25f, -0.50f, -0.05f, 0.55f, -0.38f, 0.05f, rgb[0], rgb[1], rgb[2], 0.95f * baseAlpha);
        } else if ("bunny".equals(type)) {
            // Bunny: Long upright rabbit ears
            renderColoredBox(poseStack, vc, packedLight, -0.30f, -1.10f, -0.05f, -0.18f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.18f, -1.10f, -0.05f, 0.30f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else {
            // Standard/Default Ear Cuboids
            renderColoredBox(poseStack, vc, packedLight, -0.35f, -0.65f, -0.05f, -0.22f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.22f, -0.65f, -0.05f, 0.35f, -0.40f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        }
    }

    private void renderHornsGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String hornType, float[] rgb, float baseAlpha) {
        String type = hornType.toLowerCase();
        if ("demon".equals(type)) {
            // Demon: Steep curved backward horns
            renderColoredBox(poseStack, vc, packedLight, -0.22f, -0.90f, -0.15f, -0.12f, -0.50f, -0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.12f, -0.90f, -0.15f, 0.22f, -0.50f, -0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else if ("ram".equals(type)) {
            // Ram: Wide curling horns wrapping outward around head sides
            renderColoredBox(poseStack, vc, packedLight, -0.45f, -0.60f, -0.20f, -0.18f, -0.40f, 0.10f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.18f, -0.60f, -0.20f, 0.45f, -0.40f, 0.10f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else if ("dragon".equals(type)) {
            // Dragon: Swept-back multi-pronged spiky horns
            renderColoredBox(poseStack, vc, packedLight, -0.20f, -0.80f, 0.00f, -0.10f, -0.50f, 0.30f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.10f, -0.80f, 0.00f, 0.20f, -0.50f, 0.30f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else if ("unicorn".equals(type)) {
            // Unicorn: Single center forehead horn
            renderColoredBox(poseStack, vc, packedLight, -0.05f, -0.95f, -0.30f, 0.05f, -0.45f, -0.18f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else {
            // Standard Horn Cuboids
            renderColoredBox(poseStack, vc, packedLight, -0.20f, -0.70f, -0.15f, -0.12f, -0.50f, -0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.12f, -0.70f, -0.15f, 0.20f, -0.50f, -0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        }
    }

    private void renderHaloGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String haloType, float[] rgb, float baseAlpha) {
        String type = haloType.toLowerCase();
        if ("angel".equals(type)) {
            // Angel: Floating luminous ring
            renderColoredBox(poseStack, vc, packedLight, -0.35f, -0.80f, -0.35f, 0.35f, -0.76f, 0.35f, rgb[0], rgb[1], rgb[2], 0.95f * baseAlpha);
        } else if ("flower".equals(type)) {
            // Flower: Halo ring surrounded by 4 floral petal accents
            renderColoredBox(poseStack, vc, packedLight, -0.32f, -0.78f, -0.32f, 0.32f, -0.74f, 0.32f, rgb[0], rgb[1], rgb[2], 0.9f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, -0.38f, -0.80f, -0.05f, -0.30f, -0.72f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.30f, -0.80f, -0.05f, 0.38f, -0.72f, 0.05f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, -0.05f, -0.80f, -0.38f, 0.05f, -0.72f, -0.30f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, -0.05f, -0.80f, 0.30f, 0.05f, -0.72f, 0.38f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else if ("demon".equals(type)) {
            // Demon: Dark spiky ring halo
            renderColoredBox(poseStack, vc, packedLight, -0.30f, -0.75f, -0.30f, 0.30f, -0.71f, 0.30f, rgb[0], rgb[1], rgb[2], 0.9f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, -0.28f, -0.82f, -0.28f, -0.24f, -0.75f, -0.24f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, 0.24f, -0.82f, -0.28f, 0.28f, -0.75f, -0.24f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else {
            // Standard Halo Ring
            renderColoredBox(poseStack, vc, packedLight, -0.30f, -0.75f, -0.30f, 0.30f, -0.71f, 0.30f, rgb[0], rgb[1], rgb[2], 0.9f * baseAlpha);
        }
    }

    private void renderWingGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String wingType, float[] rgb, boolean isLeft, float baseAlpha) {
        String type = wingType.toLowerCase();
        if ("feathered".equals(type)) {
            if (isLeft) {
                renderColoredBox(poseStack, vc, packedLight, -0.85f, -0.10f, 0.15f, -0.15f, 0.70f, 0.20f, rgb[0], rgb[1], rgb[2], 0.95f * baseAlpha);
                renderColoredBox(poseStack, vc, packedLight, -0.95f, 0.20f, 0.16f, -0.25f, 0.95f, 0.21f, rgb[0] * 0.9f, rgb[1] * 0.9f, rgb[2] * 0.9f, 0.95f * baseAlpha);
            } else {
                renderColoredBox(poseStack, vc, packedLight, 0.15f, -0.10f, 0.15f, 0.85f, 0.70f, 0.20f, rgb[0], rgb[1], rgb[2], 0.95f * baseAlpha);
                renderColoredBox(poseStack, vc, packedLight, 0.25f, 0.20f, 0.16f, 0.95f, 0.95f, 0.21f, rgb[0] * 0.9f, rgb[1] * 0.9f, rgb[2] * 0.9f, 0.95f * baseAlpha);
            }
        } else if ("dragon".equals(type)) {
            if (isLeft) {
                renderColoredBox(poseStack, vc, packedLight, -0.90f, 0.0f, 0.15f, -0.15f, 0.85f, 0.20f, rgb[0], rgb[1], rgb[2], 0.90f * baseAlpha);
                renderColoredBox(poseStack, vc, packedLight, -0.88f, -0.05f, 0.14f, -0.17f, 0.10f, 0.21f, rgb[0] * 0.7f, rgb[1] * 0.7f, rgb[2] * 0.7f, 1.0f * baseAlpha);
            } else {
                renderColoredBox(poseStack, vc, packedLight, 0.15f, 0.0f, 0.15f, 0.90f, 0.85f, 0.20f, rgb[0], rgb[1], rgb[2], 0.90f * baseAlpha);
                renderColoredBox(poseStack, vc, packedLight, 0.17f, -0.05f, 0.14f, 0.88f, 0.10f, 0.21f, rgb[0] * 0.7f, rgb[1] * 0.7f, rgb[2] * 0.7f, 1.0f * baseAlpha);
            }
        } else {
            if (isLeft) {
                renderColoredBox(poseStack, vc, packedLight, -0.85f, 0.0f, 0.15f, -0.15f, 0.80f, 0.20f, rgb[0], rgb[1], rgb[2], 0.95f * baseAlpha);
            } else {
                renderColoredBox(poseStack, vc, packedLight, 0.15f, 0.0f, 0.15f, 0.85f, 0.80f, 0.20f, rgb[0], rgb[1], rgb[2], 0.95f * baseAlpha);
            }
        }
    }

    private void renderTailGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String tailType, float[] rgb, float baseAlpha) {
        String type = tailType.toLowerCase();
        if ("dog".equals(type)) {
            // Dog: Bushy upward tail
            renderColoredBox(poseStack, vc, packedLight, -0.08f, 0.50f, 0.15f, 0.08f, 1.05f, 0.45f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else if ("cat".equals(type)) {
            // Cat: Slender long tail box
            renderColoredBox(poseStack, vc, packedLight, -0.04f, 0.60f, 0.15f, 0.04f, 1.35f, 0.35f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else if ("camel".equals(type)) {
            // Camel: Hump / tail tuft
            renderColoredBox(poseStack, vc, packedLight, -0.15f, 0.20f, 0.15f, 0.15f, 0.60f, 0.45f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else if ("fish".equals(type)) {
            // Fish: Tail body with caudal fin span
            renderColoredBox(poseStack, vc, packedLight, -0.04f, 0.65f, 0.15f, 0.04f, 1.20f, 0.50f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            renderColoredBox(poseStack, vc, packedLight, -0.25f, 1.00f, 0.45f, 0.25f, 1.30f, 0.52f, rgb[0], rgb[1], rgb[2], 0.95f * baseAlpha);
        } else if ("dragon".equals(type)) {
            // Dragon: Thick spiky tail
            renderColoredBox(poseStack, vc, packedLight, -0.10f, 0.60f, 0.15f, 0.10f, 1.30f, 0.70f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else {
            // Standard rear vertical tail
            renderColoredBox(poseStack, vc, packedLight, -0.06f, 0.65f, 0.15f, 0.06f, 1.25f, 0.65f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        }
    }

    private void renderExtraLegsGeometry(PoseStack poseStack, VertexConsumer vc, int packedLight, String legType, int legCount, float[] rgb, float baseAlpha) {
        String type = legType != null ? legType.toLowerCase() : "human";
        if ("spider".equals(type)) {
            int extraPairs = Math.max(1, (legCount - 2) / 2);
            for (int i = 0; i < extraPairs; i++) {
                float zOff = (i - extraPairs / 2.0f + 0.5f) * 0.25f;
                // Left spider leg
                renderColoredBox(poseStack, vc, packedLight, -0.75f, 0.50f + (i * 0.05f), zOff, -0.15f, 1.20f, zOff + 0.08f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
                // Right spider leg
                renderColoredBox(poseStack, vc, packedLight, 0.15f, 0.50f + (i * 0.05f), zOff, 0.75f, 1.20f, zOff + 0.08f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            }
        } else if ("centaur".equals(type)) {
            // Rear quadruped body extension
            renderColoredBox(poseStack, vc, packedLight, -0.30f, 0.40f, 0.20f, 0.30f, 0.85f, 1.00f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            // Rear left leg
            renderColoredBox(poseStack, vc, packedLight, -0.28f, 0.85f, 0.70f, -0.10f, 1.50f, 0.90f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
            // Rear right leg
            renderColoredBox(poseStack, vc, packedLight, 0.10f, 0.85f, 0.70f, 0.28f, 1.50f, 0.90f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
        } else {
            // Generic extra leg pairs
            int extraPairs = Math.max(1, (legCount - 2) / 2);
            for (int i = 0; i < extraPairs; i++) {
                float zOff = (i + 1) * 0.25f;
                renderColoredBox(poseStack, vc, packedLight, -0.35f, 0.60f, zOff, -0.15f, 1.30f, zOff + 0.10f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
                renderColoredBox(poseStack, vc, packedLight, 0.15f, 0.60f, zOff, 0.35f, 1.30f, zOff + 0.10f, rgb[0], rgb[1], rgb[2], 1.0f * baseAlpha);
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
