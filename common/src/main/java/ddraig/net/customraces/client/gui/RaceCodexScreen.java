package ddraig.net.customraces.client.gui;

import ddraig.net.customraces.data.PassiveAbilityDescriptions;
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
 * Playstyle Difficulty, stats, passive/active descriptions with rich tooltips, drawbacks,
 * restricted items/diets, and live 3D Steve preview.
 */
public class RaceCodexScreen extends Screen {

    private String selectedRaceId = "";
    private List<RaceData> raceList = new ArrayList<>();
    private float scrollOffset = 0.0f;
    private Component hoveredTooltip = null;

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
        hoveredTooltip = null;
        this.renderBackground(guiGraphics);
        guiGraphics.fill(0, 0, this.width, this.height, 0xEE101216);

        // Header Bar
        guiGraphics.fill(0, 0, this.width, 28, 0xFF181B20);
        guiGraphics.fill(0, 27, this.width, 28, 0xFF353A45);
        guiGraphics.drawCenteredString(this.font, "§l📜 RACE CODEX & ENCYCLOPEDIA", this.width / 2, 8, 0xFFFFFF);

        int leftWidth = 140;
        int topY = 32;
        int bottomY = this.height - 10;

        // Left Sidebar: Race List
        guiGraphics.fill(10, topY, leftWidth, bottomY, 0xFF14171C);
        guiGraphics.fill(10, topY, leftWidth, topY + 16, 0xFF1C222C);
        guiGraphics.drawString(this.font, "§7SELECT RACE:", 16, topY + 4, 0xAAAAAA);

        int itemY = topY + 20 - (int) scrollOffset;
        int itemHeight = 22;

        guiGraphics.enableScissor(10, topY + 18, leftWidth, bottomY);
        for (RaceData race : raceList) {
            if (itemY + itemHeight >= topY && itemY <= bottomY) {
                boolean isSelected = race.id.equals(selectedRaceId);
                int bgColor = isSelected ? 0xFF2B3A4E : 0xFF181C22;
                guiGraphics.fill(12, itemY, leftWidth - 4, itemY + itemHeight - 2, bgColor);
                guiGraphics.drawString(this.font, isSelected ? "§e§l" + race.name : race.name, 16, itemY + 5, 0xFFFFFF);
            }
            itemY += itemHeight;
        }
        guiGraphics.disableScissor();

        // Center Info Panel
        int centerLeft = leftWidth + 10;
        int centerWidth = this.width - leftWidth - 170;
        guiGraphics.fill(centerLeft, topY, centerLeft + centerWidth, bottomY, 0xFF14171C);

        RaceData selected = RaceRegistry.getRace(selectedRaceId);
        if (selected != null) {
            int cy = topY + 8;
            guiGraphics.drawString(this.font, "§6§l" + selected.name.toUpperCase() + " §7(ID: " + selected.id + ")", centerLeft + 12, cy, 0xFFFFFF);
            cy += 14;
            guiGraphics.drawString(this.font, "§7Difficulty: §e" + selected.playstyleDifficulty + "/10  §7| Base Scale: §b" + selected.baseScale + "x  §7| Height: §b" + selected.heightScale + "x", centerLeft + 12, cy, 0xCCCCCC);
            cy += 14;
            guiGraphics.drawString(this.font, "§7Base Stats: §c❤ " + (int)selected.maxHealth + " HP  §9🛡 " + (int)selected.armor + " Armor  §e⚔ " + String.format("%.1f", selected.attackDamage) + " ATK  §a⚡ " + String.format("%.2f", selected.movementSpeed) + " SPD", centerLeft + 12, cy, 0xCCCCCC);
            cy += 16;
            guiGraphics.drawWordWrap(this.font, Component.literal("§7" + selected.lore), centerLeft + 12, cy, centerWidth - 24, 0xDDDDDD);
            cy += 28;

            // Passives Section
            guiGraphics.drawString(this.font, "§a§l🛡 PASSIVE ABILITIES (" + selected.passiveAbilities.size() + "):", centerLeft + 12, cy, 0xFFFFFF);
            cy += 12;
            for (String p : selected.passiveAbilities) {
                if (cy > bottomY - 60) break;
                boolean isHover = mouseX >= centerLeft + 12 && mouseX <= centerLeft + centerWidth - 12 && mouseY >= cy && mouseY < cy + 11;
                PassiveAbilityDescriptions.AbilityInfo info = PassiveAbilityDescriptions.get(p);
                String label = info != null ? " §8• §a" + info.displayName() + " §7(" + info.category() + ")" : " §8• §a" + p;
                guiGraphics.drawString(this.font, label, centerLeft + 14, cy, isHover ? 0xFFFFFF : 0x55FF55);
                if (isHover && info != null) {
                    hoveredTooltip = Component.literal("§a§l" + info.displayName() + " §7[" + info.category() + "]\n§f" + info.description() + (info.stats().isEmpty() ? "" : "\n§a" + info.stats()));
                }
                cy += 11;
            }
            cy += 4;

            // Actives Section
            if (cy < bottomY - 50) {
                guiGraphics.drawString(this.font, "§c§l⚔ ACTIVE ABILITIES (Slots 1-5):", centerLeft + 12, cy, 0xFFFFFF);
                cy += 12;
                for (int slot = 1; slot <= 5; slot++) {
                    if (cy > bottomY - 30) break;
                    String act = selected.activeAbilities.getOrDefault(slot, "none");
                    int cd = selected.getAbilityCooldown(slot, false);
                    guiGraphics.drawString(this.font, " §8[Slot " + slot + "] §e" + act.replace("_", " ") + " §8(" + cd + "s cd)", centerLeft + 14, cy, 0xEEEEEE);
                    cy += 11;
                }
            }

            // Drawbacks & Restrictions
            if (cy < bottomY - 40 && selected.drawbacks != null && !selected.drawbacks.isEmpty()) {
                cy += 4;
                guiGraphics.drawString(this.font, "§e§l⚠ DRAWBACKS & RESTRICTIONS:", centerLeft + 12, cy, 0xFFFFFF);
                cy += 12;
                for (String d : selected.drawbacks) {
                    if (cy > bottomY - 15) break;
                    boolean isHover = mouseX >= centerLeft + 12 && mouseX <= centerLeft + centerWidth - 12 && mouseY >= cy && mouseY < cy + 11;
                    PassiveAbilityDescriptions.AbilityInfo dInfo = PassiveAbilityDescriptions.get(d);
                    String dLabel = dInfo != null ? " §8• §e" + dInfo.displayName() : " §8• §e" + d;
                    guiGraphics.drawString(this.font, dLabel, centerLeft + 14, cy, isHover ? 0xFFFFFF : 0xFFFF55);
                    if (isHover && dInfo != null) {
                        hoveredTooltip = Component.literal("§e§l" + dInfo.displayName() + "\n§f" + dInfo.description());
                    }
                    cy += 11;
                }
            }
        }

        // Right Preview Viewport
        int rightLeft = centerLeft + centerWidth + 10;
        int rightWidth = this.width - rightLeft - 10;
        guiGraphics.fill(rightLeft, topY, rightLeft + rightWidth, bottomY, 0xFF14171C);
        guiGraphics.drawCenteredString(this.font, "§73D PREVIEW", rightLeft + rightWidth / 2, topY + 6, 0x888888);

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

        if (hoveredTooltip != null) {
            guiGraphics.renderTooltip(this.font, hoveredTooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int leftWidth = 140;
        int topY = 32;
        int bottomY = this.height - 10;

        if (mouseX >= 10 && mouseX <= leftWidth && mouseY >= topY + 18 && mouseY <= bottomY) {
            int itemY = topY + 20 - (int) scrollOffset;
            int itemHeight = 22;
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
