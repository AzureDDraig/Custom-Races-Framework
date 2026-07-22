package ddraig.net.customraces.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/**
 * Custom flat dark obsidian RPG button widget with glowing borders matching Custom Mobs Framework theme.
 */
public class FlatButton extends Button {
    private final int borderColor;
    private final int hoverBorderColor;

    public FlatButton(int x, int y, int width, int height, Component message, OnPress onPress, int borderColor, int hoverBorderColor) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.borderColor = borderColor;
        this.hoverBorderColor = hoverBorderColor;
    }

    public FlatButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        this(x, y, width, height, message, onPress, 0xFF00CEC9, 0xFF7B61FF);
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) return;
        boolean isHovered = this.isHoveredOrFocused();
        int bg = isHovered ? 0xFF1F2638 : 0xEE121622;
        int border = !this.active ? 0xFF3D465A : (isHovered ? hoverBorderColor : borderColor);

        // Fill flat background card
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, bg);

        // Draw 1px glowing border
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + 1, border);
        guiGraphics.fill(this.getX(), this.getY() + this.height - 1, this.getX() + this.width, this.getY() + this.height, border);
        guiGraphics.fill(this.getX(), this.getY(), this.getX() + 1, this.getY() + this.height, border);
        guiGraphics.fill(this.getX() + this.width - 1, this.getY(), this.getX() + this.width, this.getY() + this.height, border);

        // Centered Text Label
        int textColor = !this.active ? 0xFF888899 : (isHovered ? 0xFFFFFF : 0xFFDDDDDD);
        guiGraphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(), this.getX() + this.width / 2, this.getY() + (this.height - 8) / 2, textColor);
    }
}
