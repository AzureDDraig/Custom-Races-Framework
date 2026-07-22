package ddraig.net.customraces.client.gui;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Race Codex Screen (modeled after BestiaryScreen in CMobs Framework) displaying race lore,
 * Playstyle Difficulty, stats, passive/active descriptions, and live 3D Steve preview.
 */
public class RaceCodexScreen extends Screen {

    private String selectedRaceId = "";
    private List<RaceData> raceList = new ArrayList<>();
    private float scrollOffset = 0.0f;

    public RaceCodexScreen() {
        super(Component.literal("Race Codex"));
    }

    @Override
    protected void init() {
        super.init();
        raceList = new ArrayList<>(RaceRegistry.loadedRaces.values());
        if (!raceList.isEmpty() && selectedRaceId.isEmpty()) {
            selectedRaceId = raceList.get(0).id;
        }

        // Close Button
        Button closeBtn = Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(this.width - 70, 4, 60, 20).build();
        closeBtn.setTooltip(Tooltip.create(Component.literal("Close the Race Codex.")));
        this.addRenderableWidget(closeBtn);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.fill(0, 0, this.width, this.height, 0xEE101216);

        // Header Bar
        guiGraphics.fill(0, 0, this.width, 28, 0xFF181B20);
        guiGraphics.fill(0, 27, this.width, 28, 0xFF353A45);
        guiGraphics.drawCenteredString(this.font, "§lRACE CODEX", this.width / 2, 8, 0xFFFFFF);

        int leftWidth = 130;
        int topY = 30;
        int bottomY = this.height - 10;

        // Left Sidebar: Race List
        guiGraphics.fill(10, topY, leftWidth, bottomY, 0xFF14171C);
        int itemY = topY + 10 - (int) scrollOffset;
        int itemHeight = 24;

        guiGraphics.enableScissor(10, topY + 4, leftWidth, bottomY);
        for (RaceData race : raceList) {
            if (itemY + itemHeight >= topY && itemY <= bottomY) {
                boolean isSelected = race.id.equals(selectedRaceId);
                int bgColor = isSelected ? 0xFF2B3A4E : 0xFF181C22;
                guiGraphics.fill(12, itemY, leftWidth - 12, itemY + itemHeight - 2, bgColor);
                guiGraphics.drawString(this.font, isSelected ? "§e§l" + race.name : race.name, 16, itemY + 6, 0xFFFFFF);
            }
            itemY += itemHeight;
        }
        guiGraphics.disableScissor();

        // Center Info Panel
        int centerLeft = leftWidth + 10;
        int centerWidth = this.width - leftWidth - 160;
        guiGraphics.fill(centerLeft, topY, centerLeft + centerWidth, bottomY, 0xFF14171C);

        RaceData selected = RaceRegistry.getRace(selectedRaceId);
        if (selected != null) {
            guiGraphics.drawString(this.font, "§6§l" + selected.name.toUpperCase(), centerLeft + 12, topY + 10, 0xFFFFFF);
            guiGraphics.drawString(this.font, "§7Difficulty: §e" + selected.playstyleDifficulty + "/10", centerLeft + 12, topY + 25, 0xCCCCCC);
            guiGraphics.drawWordWrap(this.font, Component.literal("§7" + selected.lore), centerLeft + 12, topY + 45, centerWidth - 24, 0xCCCCCC);
        }

        // Right Preview Viewport
        int rightLeft = centerLeft + centerWidth + 10;
        int rightWidth = this.width - rightLeft - 10;
        guiGraphics.fill(rightLeft, topY, rightLeft + rightWidth, bottomY, 0xFF14171C);

        if (this.minecraft != null && this.minecraft.player != null && selected != null) {
            int previewX = rightLeft + rightWidth / 2;
            int previewY = bottomY - 30;
            int scale = (int) (50 * selected.heightScale * selected.baseScale);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics, previewX, previewY, scale,
                    (float)(previewX - mouseX), (float)(previewY - 50 - mouseY),
                    this.minecraft.player
            );
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int leftWidth = 130;
        int topY = 30;
        int bottomY = this.height - 10;

        if (mouseX >= 10 && mouseX <= leftWidth && mouseY >= topY && mouseY <= bottomY) {
            int itemY = topY + 10 - (int) scrollOffset;
            int itemHeight = 24;
            for (RaceData race : raceList) {
                if (mouseY >= itemY && mouseY <= itemY + itemHeight - 2) {
                    this.selectedRaceId = race.id;
                    return true;
                }
                itemY += itemHeight;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
