package ddraig.net.customraces.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import ddraig.net.customraces.data.RaceData;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Handles general custom race model rendering and model part visibility management.
 * Checks transformation state and resolves resource locations with fallback handling.
 */
public class CustomRaceModelRenderer {
    private static final Set<String> LOGGED_WARNINGS = new HashSet<>();

    public static boolean isTransformed(UUID uuid) {
        return WereModelRenderer.isTransformed(uuid);
    }

    public static boolean isWereForm(AbstractClientPlayer player, RaceData race) {
        return WereModelRenderer.isWereForm(player, race);
    }

    public static ResourceLocation resolveModelLocation(String rawPath, ResourceLocation defaultFallback) {
        if (rawPath == null || rawPath.trim().isEmpty() || "none".equalsIgnoreCase(rawPath.trim())) {
            return defaultFallback;
        }
        String cleanPath = rawPath.trim();
        ResourceLocation loc = ResourceLocation.tryParse(cleanPath);
        if (loc == null) {
            if (LOGGED_WARNINGS.add(cleanPath)) {
                System.err.println("[CustomRaces] Could not resolve custom model path '" + cleanPath + "', using fallback: " + defaultFallback);
            }
            return defaultFallback;
        }
        return loc;
    }

    public static void updateModelPartVisibility(PlayerModel<AbstractClientPlayer> parentModel, AbstractClientPlayer player, RaceData race) {
        if (parentModel == null) return;
        boolean inWereForm = isWereForm(player, race);
        boolean hasCustomModel = inWereForm && WereModelRenderer.hasCustomModel(race);

        if (hasCustomModel) {
            WereModelRenderer.setBaseModelVisible(parentModel, false);
        } else {
            WereModelRenderer.setBaseModelVisible(parentModel, true);
        }
    }

    public static void renderCustomRaceModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player, PlayerModel<AbstractClientPlayer> parentModel, RaceData race, float netHeadYaw, float headPitch) {
        updateModelPartVisibility(parentModel, player, race);
        if (isWereForm(player, race)) {
            WereModelRenderer.renderWereForm(poseStack, buffer, packedLight, player, parentModel, race, netHeadYaw, headPitch);
        }
    }
}
