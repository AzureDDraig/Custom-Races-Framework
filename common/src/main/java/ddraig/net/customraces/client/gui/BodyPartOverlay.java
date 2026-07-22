package ddraig.net.customraces.client.gui;

import ddraig.net.customraces.data.RaceData;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Dedicated Body Part Selector Popup Window with high Z-order solid opaque dark backdrop
 * featuring preset part selection on the left and an RGB Color Picker Wheel/Sliders on the right.
 */
public class BodyPartOverlay extends Screen {

    private final Screen parentScreen;
    private final RaceData workingRace;
    private String selectedPartKey = "ears"; // ears, wings, tail, horns, halo, legs, custom

    private EditBox hexColorBox;
    private int rVal = 255;
    private int gVal = 255;
    private int bVal = 255;

    public BodyPartOverlay(Screen parentScreen, RaceData workingRace) {
        super(Component.literal("Preset Body Parts & RGB Color Picker"));
        this.parentScreen = parentScreen;
        this.workingRace = workingRace;
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int popWidth = 420;
        int popHeight = 240;
        int popX = (this.width - popWidth) / 2;
        int popY = (this.height - popHeight) / 2;

        // Part Selector Buttons on Left Side
        String[] parts = {"ears", "wings", "tail", "horns", "halo", "legs", "custom"};
        int pY = popY + 40;
        for (String p : parts) {
            final String partKey = p;
            Button pBtn = Button.builder(Component.literal(p.toUpperCase()), b -> {
                this.selectedPartKey = partKey;
                parseCurrentHexColor();
                this.init();
            }).bounds(popX + 15, pY, 70, 18).build();

            pBtn.setTooltip(Tooltip.create(Component.literal("Select " + p + " to configure preset model and color.")));
            if (selectedPartKey.equalsIgnoreCase(p)) pBtn.active = false;
            this.addRenderableWidget(pBtn);
            pY += 22;
        }

        // Preset Part Type Switcher
        int optionX = popX + 100;
        int optionY = popY + 40;

        Button nextTypeBtn = Button.builder(Component.literal("Type: " + getPartType(selectedPartKey)), b -> {
            cyclePartType(selectedPartKey);
            this.init();
        }).bounds(optionX, optionY, 180, 20).build();
        nextTypeBtn.setTooltip(Tooltip.create(Component.literal("Cycle through available preset part models.")));
        this.addRenderableWidget(nextTypeBtn);

        // RGB Color Hex Input
        this.hexColorBox = new EditBox(this.font, optionX + 60, optionY + 35, 70, 18, Component.literal("Hex Color"));
        this.hexColorBox.setMaxLength(2048);
        this.hexColorBox.setValue(workingRace.getColor(selectedPartKey));
        this.hexColorBox.setResponder(hex -> {
            if (hex.startsWith("#") && hex.length() == 7) {
                workingRace.setColor(selectedPartKey, hex);
            }
        });
        this.hexColorBox.setTooltip(Tooltip.create(Component.literal("Hex color code for part tinting (e.g. #FF0000).")));
        this.addRenderableWidget(this.hexColorBox);

        // Quick RGB Preset Buttons
        int rgbY = optionY + 65;
        String[] presetColors = {"#FFFFFF", "#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF", "#808080"};
        int colX = optionX;
        for (String cHex : presetColors) {
            Button colBtn = Button.builder(Component.literal(""), b -> {
                workingRace.setColor(selectedPartKey, cHex);
                if (hexColorBox != null) hexColorBox.setValue(cHex);
            }).bounds(colX, rgbY, 14, 14).build();
            colBtn.setTooltip(Tooltip.create(Component.literal("Color Preset: " + cHex)));
            this.addRenderableWidget(colBtn);
            colX += 17;
        }

        // Close Overlay Button
        Button closeBtn = Button.builder(Component.literal("§lDONE"), b -> {
            this.onClose();
        }).bounds(popX + popWidth - 90, popY + popHeight - 30, 80, 20).build();
        closeBtn.setTooltip(Tooltip.create(Component.literal("Save body part selections and return to creator.")));
        this.addRenderableWidget(closeBtn);

        parseCurrentHexColor();
    }

    private String getPartType(String key) {
        return switch (key) {
            case "ears" -> workingRace.earType;
            case "wings" -> workingRace.wingType;
            case "tail" -> workingRace.tailType;
            case "horns" -> workingRace.hornType;
            case "halo" -> workingRace.haloType;
            case "legs" -> workingRace.legType + " (" + workingRace.legCount + " legs)";
            default -> workingRace.customPartId;
        };
    }

    private void cyclePartType(String key) {
        switch (key) {
            case "ears" -> workingRace.earType = cycle(workingRace.earType, new String[]{"none", "dog", "cat", "dragon", "bunny"});
            case "wings" -> workingRace.wingType = cycle(workingRace.wingType, new String[]{"none", "dragon", "feathered"});
            case "tail" -> workingRace.tailType = cycle(workingRace.tailType, new String[]{"none", "dragon", "dog", "cat", "camel", "fish"});
            case "horns" -> workingRace.hornType = cycle(workingRace.hornType, new String[]{"none", "demon", "ram", "dragon", "unicorn"});
            case "halo" -> workingRace.haloType = cycle(workingRace.haloType, new String[]{"none", "angel", "demon", "flower"});
            case "legs" -> {
                if ("human".equals(workingRace.legType)) {
                    workingRace.legType = "spider"; workingRace.legCount = 8;
                } else if ("spider".equals(workingRace.legType)) {
                    workingRace.legType = "centaur"; workingRace.legCount = 4;
                } else {
                    workingRace.legType = "human"; workingRace.legCount = 2;
                }
            }
            default -> {
                java.util.List<String> customList = new java.util.ArrayList<>(ddraig.net.customraces.data.CustomPartScanner.getDiscoveredCustomParts());
                if (customList.isEmpty()) customList.add("none");
                if (!customList.contains("none")) customList.add(0, "none");
                workingRace.customPartId = cycle(workingRace.customPartId, customList.toArray(new String[0]));
            }
        }
    }

    private String cycle(String current, String[] options) {
        for (int i = 0; i < options.length; i++) {
            if (options[i].equalsIgnoreCase(current)) {
                return options[(i + 1) % options.length];
            }
        }
        return options[0];
    }

    private void parseCurrentHexColor() {
        String hex = workingRace.getColor(selectedPartKey);
        try {
            if (hex.startsWith("#") && hex.length() == 7) {
                rVal = Integer.parseInt(hex.substring(1, 3), 16);
                gVal = Integer.parseInt(hex.substring(3, 5), 16);
                bVal = Integer.parseInt(hex.substring(5, 7), 16);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // 1. Dark Backdrop to Prevent UI Bleed-Through
        this.renderBackground(guiGraphics);
        guiGraphics.fill(0, 0, this.width, this.height, 0xD0000000);

        int popWidth = 420;
        int popHeight = 240;
        int popX = (this.width - popWidth) / 2;
        int popY = (this.height - popHeight) / 2;

        // 2. High Z-Order Opaque Popup Container
        guiGraphics.fill(popX, popY, popX + popWidth, popY + popHeight, 0xFF181B20);
        guiGraphics.fill(popX, popY, popX + popWidth, popY + 28, 0xFF242830);
        guiGraphics.fill(popX, popY + 27, popX + popWidth, popY + 28, 0xFF3D4452);
        guiGraphics.drawString(this.font, "§lPRESET BODY PARTS & RGB COLOR PICKER", popX + 12, popY + 9, 0xFFFFFF);

        int optionX = popX + 100;
        int optionY = popY + 40;
        guiGraphics.drawString(this.font, "Color Hex:", optionX, optionY + 39, 0xCCCCCC);

        // Preview Selected Color Swatch
        String hex = workingRace.getColor(selectedPartKey);
        int colorInt = 0xFFFFFFFF;
        try {
            if (hex.startsWith("#") && hex.length() == 7) {
                colorInt = 0xFF000000 | Integer.parseInt(hex.substring(1), 16);
            }
        } catch (Exception ignored) {}

        guiGraphics.fill(optionX + 140, optionY + 35, optionX + 180, optionY + 53, colorInt);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
