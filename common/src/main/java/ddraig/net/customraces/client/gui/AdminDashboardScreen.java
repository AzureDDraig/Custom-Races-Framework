package ddraig.net.customraces.client.gui;

import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Admin status GUI showing race population statistics, loaded races count, and player distribution.
 */
public class AdminDashboardScreen extends Screen {

    public AdminDashboardScreen() {
        super(Component.literal("Admin Race Dashboard"));
    }

    @Override
    protected void init() {
        super.init();

        Button closeBtn = Button.builder(Component.literal("Close"), b -> this.onClose())
                .bounds(this.width / 2 - 40, this.height - 35, 80, 20).build();
        closeBtn.setTooltip(Tooltip.create(Component.literal("Close Admin Dashboard.")));
        this.addRenderableWidget(closeBtn);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.fill(0, 0, this.width, this.height, 0xEE101216);

        // Header Bar
        guiGraphics.fill(0, 0, this.width, 28, 0xFF181B20);
        guiGraphics.fill(0, 27, this.width, 28, 0xFF353A45);
        guiGraphics.drawCenteredString(this.font, "§lADMIN RACE DASHBOARD & POPULATION", this.width / 2, 8, 0xFFFFFF);

        int panelLeft = (this.width - 320) / 2;
        int panelTop = 45;
        int panelWidth = 320;
        int panelHeight = this.height - 90;

        guiGraphics.fill(panelLeft, panelTop, panelLeft + panelWidth, panelTop + panelHeight, 0xFF14171C);

        int loadedCount = RaceRegistry.loadedRaces.size();
        int assignedCount = RaceRegistry.playerRaces.size();

        guiGraphics.drawString(this.font, "§eTotal Loaded Templates: §f" + loadedCount, panelLeft + 15, panelTop + 15, 0xFFFFFF);
        guiGraphics.drawString(this.font, "§eAssigned Players: §f" + assignedCount, panelLeft + 15, panelTop + 30, 0xFFFFFF);

        // Calculate Population breakdown
        Map<String, Integer> popCounts = new HashMap<>();
        for (String rId : RaceRegistry.playerRaces.values()) {
            popCounts.put(rId, popCounts.getOrDefault(rId, 0) + 1);
        }

        guiGraphics.drawString(this.font, "§a§lRACE POPULATION BREAKDOWN:", panelLeft + 15, panelTop + 55, 0xFFFFFF);
        int lineY = panelTop + 72;

        for (RaceData race : RaceRegistry.loadedRaces.values()) {
            int count = popCounts.getOrDefault(race.id, 0);
            if (lineY < panelTop + panelHeight - 20) {
                guiGraphics.drawString(this.font, " • " + race.name + " (" + race.id + "): §e" + count + " players", panelLeft + 20, lineY, 0xDDDDDD);
                lineY += 14;
            }
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
}
