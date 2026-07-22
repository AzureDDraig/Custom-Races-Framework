package ddraig.net.customraces.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import ddraig.net.customraces.network.ModPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Modern Dark Race Selection Screen matching CMobs Framework aesthetic.
 * Left edge: Scrollable race list container.
 * Center panel: Lore, Playstyle Difficulty Meter (1-10), passives/actives summary, confirm button.
 * Right side: Live rotating 3D Steve model preview.
 */
public class RaceSelectionScreen extends Screen {

    private EditBox searchBox;
    private Button confirmButton;
    private Button wereToggleBtn;
    private boolean previewWereForm = false;
    private String selectedRaceId = "";
    private List<RaceData> filteredRaces = new ArrayList<>();
    private float scrollOffset = 0.0f;
    private boolean isDraggingScroll = false;
    private float modelRotation = 0.0f;

    public RaceSelectionScreen() {
        super(Component.literal("Choose Your Race"));
    }

    @Override
    protected void init() {
        super.init();

        int leftWidth = 140;
        int searchY = 54;

        // Search Input Box
        this.searchBox = new EditBox(this.font, 14, searchY, leftWidth - 26, 18, Component.literal("Search..."));
        this.searchBox.setMaxLength(2048);
        this.searchBox.setHint(Component.literal("Search..."));
        this.searchBox.setResponder(text -> updateFilteredRaces());
        this.searchBox.setTooltip(Tooltip.create(Component.literal("Filter available races by name or trait.")));
        this.addRenderableWidget(this.searchBox);

        updateFilteredRaces();
        if (!filteredRaces.isEmpty() && selectedRaceId.isEmpty()) {
            selectedRaceId = filteredRaces.get(0).id;
        }

        // Confirm Choice Button
        int centerLeft = leftWidth + 20;
        int centerWidth = this.width - leftWidth - 170;
        int confirmY = this.height - 45;

        this.confirmButton = Button.builder(Component.translatable("gui.customraces.button.confirm_choice"), button -> {
            if (!selectedRaceId.isEmpty()) {
                ModPackets.sendSetPlayerRace(selectedRaceId);
                this.onClose();
            }
        }).bounds(centerLeft, confirmY, centerWidth - 100, 24).build();
        this.confirmButton.setTooltip(Tooltip.create(Component.translatable("gui.customraces.tooltip.confirm")));
        this.addRenderableWidget(this.confirmButton);

        // Were-Form Preview Toggle Button
        this.wereToggleBtn = Button.builder(Component.translatable("gui.customraces.button.were_form"), button -> {
            previewWereForm = !previewWereForm;
            updateWereButtonText();
        }).bounds(centerLeft + centerWidth - 95, confirmY, 95, 24).build();
        this.wereToggleBtn.setTooltip(Tooltip.create(Component.translatable("gui.customraces.tooltip.were_toggle")));
        this.addRenderableWidget(this.wereToggleBtn);
        updateWereButtonText();
    }

    private void updateWereButtonText() {
        if (this.wereToggleBtn != null) {
            this.wereToggleBtn.setMessage(previewWereForm ? Component.translatable("gui.customraces.button.normal_form") : Component.translatable("gui.customraces.button.were_form"));
        }
    }

    private void updateFilteredRaces() {
        String query = searchBox != null ? searchBox.getValue().toLowerCase().trim() : "";
        filteredRaces = RaceRegistry.loadedRaces.values().stream()
                .filter(r -> query.isEmpty() || r.name.toLowerCase().contains(query) || r.lore.toLowerCase().contains(query))
                .collect(Collectors.toList());
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Opaque Dark Obsidian Background
        this.renderBackground(guiGraphics);
        guiGraphics.fill(0, 0, this.width, this.height, 0xEE101216);

        // Header Bar
        guiGraphics.fill(0, 0, this.width, 28, 0xFF181B20);
        guiGraphics.fill(0, 27, this.width, 28, 0xFF353A45);
        guiGraphics.drawCenteredString(this.font, Component.translatable("gui.customraces.title.choose_race"), this.width / 2, 8, 0xFFFFFF);

        int leftWidth = 140;
        int topY = 30;
        int bottomY = this.height - 10;

        // 2. Left Edge Scrollable List Box Container
        guiGraphics.fill(10, topY, leftWidth, bottomY, 0xFF14171C);
        guiGraphics.fill(10, topY, leftWidth, topY + 22, 0xFF1C2027);
        guiGraphics.drawString(this.font, "§6§lRaces (" + filteredRaces.size() + ")", 15, topY + 6, 0xFFFFFF);

        // Render Scrollable Race List Box (Below Search Box at Y=76)
        int listTop = topY + 46;
        int itemY = listTop - (int) scrollOffset;
        int itemHeight = 24;

        guiGraphics.enableScissor(10, listTop, leftWidth, bottomY);
        for (RaceData race : filteredRaces) {
            if (itemY + itemHeight >= listTop && itemY <= bottomY) {
                boolean isSelected = race.id.equals(selectedRaceId);
                boolean isHovered = mouseX >= 12 && mouseX <= leftWidth - 12 && mouseY >= itemY && mouseY <= itemY + itemHeight - 2;

                int bgColor = isSelected ? 0xFF2B3A4E : (isHovered ? 0xFF222730 : 0xFF181C22);
                int borderColor = isSelected ? 0xFF4A80C0 : (isHovered ? 0xFF3A4250 : 0xFF20252E);

                guiGraphics.fill(12, itemY, leftWidth - 12, itemY + itemHeight - 2, bgColor);
                guiGraphics.fill(12, itemY, leftWidth - 12, itemY + 1, borderColor);
                guiGraphics.fill(12, itemY + itemHeight - 3, leftWidth - 12, itemY + itemHeight - 2, borderColor);

                String displayName = isSelected ? "§e§l" + race.name : race.name;
                guiGraphics.drawString(this.font, displayName, 18, itemY + 6, 0xFFFFFF);
            }
            itemY += itemHeight;
        }
        guiGraphics.disableScissor();

        // 3. Center Panel: Detailed Info, Difficulty Meter (1-10), Lore, Skills
        int centerLeft = leftWidth + 10;
        int centerWidth = this.width - leftWidth - 160;
        guiGraphics.fill(centerLeft, topY, centerLeft + centerWidth, bottomY, 0xFF14171C);

        RaceData selectedRace = RaceRegistry.getRace(selectedRaceId);
        if (wereToggleBtn != null) {
            wereToggleBtn.visible = selectedRace != null && selectedRace.enableWereRace;
        }

        if (selectedRace != null) {
            // Title Bar
            guiGraphics.fill(centerLeft, topY, centerLeft + centerWidth, topY + 30, 0xFF1E222A);
            int titleColor = parseHexColor(selectedRace.nameColor, 0xFFFFAA00);
            String titleText = (previewWereForm && selectedRace.enableWereRace) ? "§c§l" + selectedRace.name.toUpperCase() + " §4[WERE-FORM]" : "§l" + selectedRace.name.toUpperCase();
            guiGraphics.drawString(this.font, titleText, centerLeft + 12, topY + 10, titleColor);

            // Playstyle Difficulty Meter (1 to 10)
            int diffY = topY + 38;
            guiGraphics.drawString(this.font, "§7Playstyle Difficulty:", centerLeft + 12, diffY, 0xAAAAAA);
            int meterX = centerLeft + 120;
            int meterWidth = 140;
            int meterHeight = 10;

            guiGraphics.fill(meterX, diffY, meterX + meterWidth, diffY + meterHeight, 0xFF0D0E11);
            int diffVal = Math.max(1, Math.min(10, selectedRace.playstyleDifficulty));
            int filledWidth = (int) ((diffVal / 10.0f) * meterWidth);

            int meterColor = diffVal <= 3 ? 0xFF55FF55 : (diffVal <= 6 ? 0xFFFFFF55 : (diffVal <= 8 ? 0xFFFFAA00 : 0xFFFF5555));
            guiGraphics.fill(meterX, diffY, meterX + filledWidth, diffY + meterHeight, meterColor);

            guiGraphics.drawString(this.font, "§l" + diffVal + "/10", meterX + meterWidth + 8, diffY + 1, meterColor);

            // Lore / Were Form Summary Box
            int loreY = topY + 56;
            guiGraphics.fill(centerLeft + 10, loreY, centerLeft + centerWidth - 10, loreY + 50, 0xFF1A1D24);
            if (previewWereForm && selectedRace.enableWereRace) {
                guiGraphics.drawString(this.font, "§c§lWERE-FORM STATS & CONDITION:", centerLeft + 15, loreY + 6, 0xFFFFFF);
                guiGraphics.drawString(this.font, "§7Trigger: §e" + selectedRace.wereTriggerCondition + " §8| §7Health: §a+" + (int)selectedRace.wereHealthBonus + " HP", centerLeft + 15, loreY + 20, 0xCCCCCC);
                guiGraphics.drawString(this.font, "§7Damage Bonus: §c+" + (int)selectedRace.wereDamageBonus + " §8| §7Speed Bonus: §b+" + String.format("%.2f", selectedRace.wereSpeedBonus), centerLeft + 15, loreY + 33, 0xCCCCCC);
            } else {
                guiGraphics.drawString(this.font, "§e§lRACE LORE:", centerLeft + 15, loreY + 6, 0xFFFFFF);
                guiGraphics.drawWordWrap(this.font, Component.literal("§7" + selectedRace.lore), centerLeft + 15, loreY + 20, centerWidth - 30, 0xCCCCCC);
            }

            if (previewWereForm && selectedRace.enableWereRace) {
                // Passives Summary (Were Form)
                int passY = loreY + 58;
                int werePassCount = selectedRace.werePassiveAbilities != null ? selectedRace.werePassiveAbilities.size() : 0;
                guiGraphics.drawString(this.font, "§c§l🌙 WERE-FORM PASSIVES (" + werePassCount + "):", centerLeft + 12, passY, 0xFFFFFF);
                int passItemY = passY + 14;
                if (selectedRace.werePassiveAbilities != null && !selectedRace.werePassiveAbilities.isEmpty()) {
                    for (String passive : selectedRace.werePassiveAbilities) {
                        if (passItemY < passY + 70) {
                            guiGraphics.drawString(this.font, " §8• §c" + passive.replace("_", " "), centerLeft + 18, passItemY, 0xFFDDDD);
                            passItemY += 12;
                        }
                    }
                } else {
                    guiGraphics.drawString(this.font, " §8• §7Retains Standard Race Passives", centerLeft + 18, passItemY, 0xAAAAAA);
                }

                // Actives Summary (Were Form)
                int actY = passY + 78;
                guiGraphics.drawString(this.font, "§c§l🌙 WERE-FORM ACTIVE SKILLS (Slots 1-5):", centerLeft + 12, actY, 0xFFFFFF);
                int actItemY = actY + 14;
                for (int slot = 1; slot <= 5; slot++) {
                    String actName = selectedRace.wereActiveAbilities != null ? selectedRace.wereActiveAbilities.get(slot) : null;
                    if (actName == null || actName.isEmpty() || "none".equalsIgnoreCase(actName)) {
                        actName = selectedRace.activeAbilities.getOrDefault(slot, "None");
                        if (actName == null || actName.isEmpty()) actName = "None";
                        guiGraphics.drawString(this.font, " §8[Slot " + slot + "] §7" + actName.replace("_", " ") + " §8(Base)", centerLeft + 18, actItemY, 0x888888);
                    } else {
                        guiGraphics.drawString(this.font, " §8[Slot " + slot + "] §c" + actName.replace("_", " "), centerLeft + 18, actItemY, 0xFFDDDD);
                    }
                    actItemY += 12;
                }
            } else {
                // Passives Summary (Standard)
                int passY = loreY + 58;
                guiGraphics.drawString(this.font, "§a§lPASSIVE ABILITIES (" + selectedRace.passiveAbilities.size() + "):", centerLeft + 12, passY, 0xFFFFFF);
                int passItemY = passY + 14;
                for (String passive : selectedRace.passiveAbilities) {
                    if (passItemY < passY + 70) {
                        guiGraphics.drawString(this.font, " §8• §f" + passive.replace("_", " "), centerLeft + 18, passItemY, 0xDDDDDD);
                        passItemY += 12;
                    }
                }

                // Actives Summary (Standard)
                int actY = passY + 78;
                guiGraphics.drawString(this.font, "§c§lACTIVE SKILLS (Slots 1-5):", centerLeft + 12, actY, 0xFFFFFF);
                int actItemY = actY + 14;
                for (int slot = 1; slot <= 5; slot++) {
                    String actName = selectedRace.activeAbilities.getOrDefault(slot, "None");
                    if (actName == null || actName.isEmpty()) actName = "None";
                    guiGraphics.drawString(this.font, " §8[Slot " + slot + "] §e" + actName.replace("_", " "), centerLeft + 18, actItemY, 0xDDDDDD);
                    actItemY += 12;
                }
            }
        }

        // 4. Right Side: 3D Rotating Steve Preview Entity
        int rightLeft = centerLeft + centerWidth + 10;
        int rightWidth = this.width - rightLeft - 10;
        guiGraphics.fill(rightLeft, topY, rightLeft + rightWidth, bottomY, 0xFF14171C);
        guiGraphics.fill(rightLeft, topY, rightLeft + rightWidth, topY + 24, 0xFF1E222A);
        String previewHeader = (previewWereForm && selectedRace != null && selectedRace.enableWereRace) ? "§c§lWere Preview" : "§7Preview";
        guiGraphics.drawCenteredString(this.font, previewHeader, rightLeft + rightWidth / 2, topY + 7, 0xFFFFFF);

        if (this.minecraft != null && this.minecraft.player != null) {
            modelRotation += partialTick * 0.8f;
            int previewX = rightLeft + rightWidth / 2;
            int previewY = bottomY - 30;
            int scale = 50;

            if (selectedRace != null) {
                float hScale = (previewWereForm && selectedRace.enableWereRace) ? selectedRace.wereHeightScale : selectedRace.heightScale;
                scale = (int) (50 * hScale * selectedRace.baseScale);
            }

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
        int leftWidth = 140;
        int topY = 30;
        int bottomY = this.height - 10;

        if (mouseX >= 10 && mouseX <= leftWidth && mouseY >= topY + 46 && mouseY <= bottomY) {
            int itemY = topY + 46 - (int) scrollOffset;
            int itemHeight = 24;
            for (RaceData race : filteredRaces) {
                if (mouseY >= itemY && mouseY <= itemY + itemHeight - 2) {
                    this.selectedRaceId = race.id;
                    return true;
                }
                itemY += itemHeight;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        int totalHeight = filteredRaces.size() * 26;
        int viewHeight = this.height - 70;
        scrollOffset = (float) Math.max(0, Math.min(totalHeight - viewHeight, scrollOffset - delta * 15));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    public static int parseHexColor(String hex, int defaultColor) {
        if (hex == null || hex.trim().isEmpty()) return defaultColor;
        try {
            String clean = hex.trim();
            if (clean.startsWith("#")) clean = clean.substring(1);
            if (clean.startsWith("0x") || clean.startsWith("0X")) clean = clean.substring(2);
            return (int) (Long.parseLong(clean, 16) | 0xFF000000);
        } catch (Exception e) {
            return defaultColor;
        }
    }
}
