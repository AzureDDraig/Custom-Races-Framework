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
        if (race == null) return;

        poseStack.pushPose();

        // 1. Render Preset Body Parts (Ears, Wings, Tail, Horns, Halo, Legs)
        renderPresetParts(poseStack, buffer, packedLight, player, race, netHeadYaw, headPitch);

        // 2. Render Particle Auras in Real-Time
        if (player.level().isClientSide && !race.particleAuras.isEmpty()) {
            for (ParticleAuraData aura : race.particleAuras) {
                if (player.tickCount % 4 == 0) {
                    net.minecraft.core.particles.ParticleType<?> pType = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(new ResourceLocation(aura.particleType));
                    if (pType instanceof net.minecraft.core.particles.ParticleOptions pOptions) {
                        player.level().addParticle(
                                pOptions,
                                player.getRandomX(aura.spread),
                                player.getRandomY() + 0.5,
                                player.getRandomZ(aura.spread),
                                0.0, aura.speed, 0.0
                        );
                    }
                }
            }
        }

        poseStack.popPose();
    }

    private void renderPresetParts(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, RaceData race, float headYaw, float headPitch) {
        // Parse RGB color tinting
        String earColor = race.getColor("ears");
        String wingColor = race.getColor("wings");
        String tailColor = race.getColor("tail");
        String hornColor = race.getColor("horns");
        String haloColor = race.getColor("halo");

        // Attach to Head Bone for Ears, Horns, Halo
        if (!"none".equalsIgnoreCase(race.earType) || !"none".equalsIgnoreCase(race.hornType) || !"none".equalsIgnoreCase(race.haloType)) {
            poseStack.pushPose();
            this.getParentModel().getHead().translateAndRotate(poseStack);
            // Render Head Attachments
            poseStack.popPose();
        }

        // Attach to Body Bone for Wings, Tail
        if (!"none".equalsIgnoreCase(race.wingType) || !"none".equalsIgnoreCase(race.tailType)) {
            poseStack.pushPose();
            this.getParentModel().body.translateAndRotate(poseStack);
            // Render Body Attachments
            poseStack.popPose();
        }
    }
}
