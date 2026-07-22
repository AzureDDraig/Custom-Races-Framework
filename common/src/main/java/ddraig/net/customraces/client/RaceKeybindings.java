package ddraig.net.customraces.client;

import com.mojang.blaze3d.platform.InputConstants;
import ddraig.net.customraces.network.ModPackets;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * Registers client keybindings for active race abilities 1 through 5.
 */
public class RaceKeybindings {
    public static final String CATEGORY = "key.categories.customraces";

    public static final KeyMapping ABILITY_1 = new KeyMapping("key.customraces.ability_1", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_G, CATEGORY);
    public static final KeyMapping ABILITY_2 = new KeyMapping("key.customraces.ability_2", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_V, CATEGORY);
    public static final KeyMapping ABILITY_3 = new KeyMapping("key.customraces.ability_3", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORY);
    public static final KeyMapping ABILITY_4 = new KeyMapping("key.customraces.ability_4", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_N, CATEGORY);
    public static final KeyMapping ABILITY_5 = new KeyMapping("key.customraces.ability_5", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORY);

    public static void init() {
        KeyMappingRegistry.register(ABILITY_1);
        KeyMappingRegistry.register(ABILITY_2);
        KeyMappingRegistry.register(ABILITY_3);
        KeyMappingRegistry.register(ABILITY_4);
        KeyMappingRegistry.register(ABILITY_5);

        ClientTickEvent.CLIENT_POST.register(client -> {
            if (client.player == null || client.screen != null) return;

            while (ABILITY_1.consumeClick()) ModPackets.sendTriggerAbility(1);
            while (ABILITY_2.consumeClick()) ModPackets.sendTriggerAbility(2);
            while (ABILITY_3.consumeClick()) ModPackets.sendTriggerAbility(3);
            while (ABILITY_4.consumeClick()) ModPackets.sendTriggerAbility(4);
            while (ABILITY_5.consumeClick()) ModPackets.sendTriggerAbility(5);
        });
    }
}
