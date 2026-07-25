package ddraig.net.customraces.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import ddraig.net.customraces.client.render.WereModelRenderer;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderLivingHead(T entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (entity instanceof AbstractClientPlayer player) {
            RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
            LivingEntityRenderer<T, M> renderer = (LivingEntityRenderer<T, M>) (Object) this;
            M model = renderer.getModel();

            if (model instanceof PlayerModel<?> playerModel) {
                if (WereModelRenderer.isWereForm(player, race) && WereModelRenderer.hasCustomModel(race)) {
                    // Suppress base human player model before LivingEntityRenderer renders main mesh
                    WereModelRenderer.setBaseModelVisible(playerModel, false);
                } else {
                    WereModelRenderer.setBaseModelVisible(playerModel, true);
                }
            }
        }
    }
}
