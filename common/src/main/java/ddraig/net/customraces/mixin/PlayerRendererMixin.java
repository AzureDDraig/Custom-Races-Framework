package ddraig.net.customraces.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import ddraig.net.customraces.client.render.WereModelRenderer;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    @Inject(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"))
    private void onRenderPlayerHead(AbstractClientPlayer player, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (player == null) return;
        RaceData race = RaceRegistry.getPlayerRace(player.getUUID());
        PlayerRenderer renderer = (PlayerRenderer) (Object) this;
        PlayerModel<AbstractClientPlayer> model = renderer.getModel();

        if (WereModelRenderer.isWereForm(player, race)) {
            if (WereModelRenderer.hasCustomModel(race)) {
                // Hide human player model BEFORE main body renders
                WereModelRenderer.setBaseModelVisible(model, false);
            } else {
                WereModelRenderer.setBaseModelVisible(model, true);
            }
        } else {
            WereModelRenderer.setBaseModelVisible(model, true);
        }
    }
}
