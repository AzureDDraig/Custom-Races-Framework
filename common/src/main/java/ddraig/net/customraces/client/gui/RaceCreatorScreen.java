package ddraig.net.customraces.client.gui;

import ddraig.net.customraces.data.MobAllianceData;
import ddraig.net.customraces.data.PartTransformData;
import ddraig.net.customraces.data.ParticleAuraData;
import ddraig.net.customraces.data.RaceData;
import ddraig.net.customraces.data.RaceRegistry;
import ddraig.net.customraces.network.ModPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Creator GUI matching CMobs Framework editor layout with 8 tabs and full tooltips.
 */
public class RaceCreatorScreen extends Screen {

    private int activeTab = 0; // 0: Basics, 1: Model, 2: Positions, 3: Passives, 4: Actives, 5: Sounds, 6: Advanced, 7: Alliances
    private RaceData workingRace;

    // GUI Edit Controls
    private EditBox nameBox;
    private EditBox nameColorBox;
    private EditBox customTextureBox;
    private EditBox loreBox;
    private EditBox difficultyBox;
    private EditBox iconBox;

    private Checkbox hideHelmetBox;
    private Checkbox hideChestplateBox;
    private Checkbox hideLeggingsBox;
    private Checkbox hideBootsBox;

    private EditBox heightScaleBox;
    private EditBox widthScaleBox;
    private EditBox healthBox;
    private EditBox speedBox;

    private EditBox ambientSoundBox;
    private EditBox hurtSoundBox;
    private EditBox deathSoundBox;

    private EditBox spawnDimensionBox;
    private EditBox spawnBiomeBox;
    private Checkbox enableAlliancesBox;

    // Were-Race Controls
    private Checkbox enableWereBox;
    private EditBox wereConditionBox;
    private EditBox wereModelBox;
    private EditBox wereTextureBox;
    private EditBox wereAnimFileBox;
    private EditBox wereIdleAnimBox;
    private EditBox wereWalkAnimBox;
    private EditBox wereAttackAnimBox;
    private EditBox wereTransformSoundBox;
    private EditBox wereHowlSoundBox;
    private EditBox wereAmbientSoundBox;
    private EditBox wereHurtSoundBox;
    private EditBox wereDeathSoundBox;

    // Minion Ability Controls
    private EditBox minionMobTypeBox;
    private EditBox minionCountBox;
    private EditBox minionScaleBox;
    private Checkbox minionIsRangedBox;
    private EditBox minionProjectileBox;

    // Auto-Complete Suggestions Overlay Fields
    private EditBox activeField = null;
    private List<String> activeSuggestions = new ArrayList<>();
    private int suggestionsScrollOffset = 0;
    private boolean showSuggestions = false;

    public RaceCreatorScreen(RaceData race) {
        super(Component.literal("Race Creator Admin"));
        this.workingRace = race != null ? race : new RaceData("new_race_" + System.currentTimeMillis() % 1000, "New Race");
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int topY = 32;
        int tabX = 10;
        int tabWidth = 64;
        int tabHeight = 18;
        int maxTabX = this.width - 150; // Align with right viewport bounds

        // 10 Category Tabs
        String[] tabKeys = {
            "gui.customraces.tab.basics", "gui.customraces.tab.model", "gui.customraces.tab.positions",
            "gui.customraces.tab.passives", "gui.customraces.tab.actives", "gui.customraces.tab.sounds",
            "gui.customraces.tab.advanced", "gui.customraces.tab.alliances", "gui.customraces.tab.were_model",
            "gui.customraces.tab.were_sounds"
        };

        for (int i = 0; i < tabKeys.length; i++) {
            final int index = i;
            if (i == 2 && !"Custom".equalsIgnoreCase(workingRace.modelType)) continue; // Positions hidden if Default
            if (i == 7 && !workingRace.enableAlliances) continue; // Alliances hidden if disabled
            if ((i == 8 || i == 9) && !workingRace.enableWereRace) continue; // Were Model & Sounds hidden if disabled

            if (tabX + tabWidth > maxTabX) {
                tabX = 10;
                topY += tabHeight + 2;
            }

            Button tabBtn = Button.builder(Component.translatable(tabKeys[i]), b -> {
                readFormInputs();
                this.activeTab = index;
                this.init();
            }).bounds(tabX, topY, tabWidth, tabHeight).build();

            tabBtn.setTooltip(Tooltip.create(Component.translatable(tabKeys[i])));
            if (activeTab == i) tabBtn.active = false;
            this.addRenderableWidget(tabBtn);
            tabX += tabWidth + 3;
        }

        // Save & Delete Header Buttons
        Button saveBtn = Button.builder(Component.translatable("gui.customraces.button.save_race"), b -> {
            readFormInputs();
            ModPackets.sendSaveRace(workingRace);
            this.onClose();
        }).bounds(this.width - 160, 4, 75, 20).build();
        saveBtn.setTooltip(Tooltip.create(Component.translatable("gui.customraces.tooltip.save_race")));
        this.addRenderableWidget(saveBtn);

        Button deleteBtn = Button.builder(Component.translatable("gui.customraces.button.delete_race"), b -> {
            ModPackets.sendDeleteRace(workingRace.id);
            this.onClose();
        }).bounds(this.width - 80, 4, 75, 20).build();
        deleteBtn.setTooltip(Tooltip.create(Component.translatable("gui.customraces.tooltip.delete_race")));
        this.addRenderableWidget(deleteBtn);

        // Tab Content Initializer
        int contentLeft = 15;
        int contentTop = 60;

        if (activeTab == 0) { // Basics
            this.nameBox = new EditBox(this.font, contentLeft + 90, contentTop, 160, 18, Component.literal("Name"));
            this.nameBox.setMaxLength(2048);
            this.nameBox.setValue(workingRace.name);
            this.nameBox.setTooltip(Tooltip.create(Component.literal("Display name of the race.")));
            this.addRenderableWidget(this.nameBox);

            this.nameColorBox = new EditBox(this.font, contentLeft + 90, contentTop + 23, 70, 18, Component.literal("Name Color"));
            this.nameColorBox.setMaxLength(2048);
            this.nameColorBox.setValue(workingRace.nameColor);
            this.nameColorBox.setTooltip(Tooltip.create(Component.literal("RGB Hex Color code for race title / nametag (e.g. #FFAA00).")));
            this.addRenderableWidget(this.nameColorBox);

            this.difficultyBox = new EditBox(this.font, contentLeft + 90, contentTop + 46, 60, 18, Component.literal("Difficulty"));
            this.difficultyBox.setMaxLength(2048);
            this.difficultyBox.setValue(String.valueOf(workingRace.playstyleDifficulty));
            this.difficultyBox.setTooltip(Tooltip.create(Component.literal("Playstyle Difficulty rating meter from 1 (Easy) to 10 (Insane).")));
            this.addRenderableWidget(this.difficultyBox);

            this.loreBox = new EditBox(this.font, contentLeft + 90, contentTop + 69, 260, 18, Component.literal("Lore"));
            this.loreBox.setMaxLength(2048);
            this.loreBox.setValue(workingRace.lore);
            this.loreBox.setTooltip(Tooltip.create(Component.literal("Lore and background story description.")));
            this.addRenderableWidget(this.loreBox);

            this.iconBox = new EditBox(this.font, contentLeft + 90, contentTop + 92, 160, 18, Component.literal("Icon Item"));
            this.iconBox.setMaxLength(2048);
            this.iconBox.setValue(workingRace.iconItem);
            this.iconBox.setTooltip(Tooltip.create(Component.literal("ResourceLocation of icon item (e.g. minecraft:player_head).")));
            this.addRenderableWidget(this.iconBox);

            this.customTextureBox = new EditBox(this.font, contentLeft + 90, contentTop + 115, 260, 18, Component.literal("PNG Picture Path"));
            this.customTextureBox.setMaxLength(2048);
            this.customTextureBox.setValue(workingRace.customTexture);
            this.customTextureBox.setTooltip(Tooltip.create(Component.literal("ResourceLocation path to PNG picture (e.g. customraces:textures/gui/races/elf.png).")));
            this.addRenderableWidget(this.customTextureBox);

            // Selective Armor Piece Hiding Checkboxes
            this.hideHelmetBox = new Checkbox(contentLeft, contentTop + 138, 120, 20, Component.literal("Hide Helmet"), workingRace.hideHelmet);
            this.hideHelmetBox.setTooltip(Tooltip.create(Component.literal("Check to hide player helmet armor piece visually.")));
            this.addRenderableWidget(this.hideHelmetBox);

            this.hideChestplateBox = new Checkbox(contentLeft + 130, contentTop + 138, 120, 20, Component.literal("Hide Chestplate"), workingRace.hideChestplate);
            this.hideChestplateBox.setTooltip(Tooltip.create(Component.literal("Check to hide player chestplate armor piece visually.")));
            this.addRenderableWidget(this.hideChestplateBox);

            this.enableWereBox = new Checkbox(contentLeft + 260, contentTop + 138, 140, 20, Component.literal("Enable Were-Form"), workingRace.enableWereRace) {
                @Override
                public void onPress() {
                    super.onPress();
                    workingRace.enableWereRace = this.selected();
                    RaceCreatorScreen.this.init();
                }
            };
            this.enableWereBox.setTooltip(Tooltip.create(Component.literal("Check to enable Were-form capabilities and unlock Were Model & Were Sounds tabs.")));
            this.addRenderableWidget(this.enableWereBox);

            this.hideLeggingsBox = new Checkbox(contentLeft, contentTop + 130, 120, 20, Component.literal("Hide Leggings"), workingRace.hideLeggings);
            this.hideLeggingsBox.setTooltip(Tooltip.create(Component.literal("Check to hide player leggings armor piece visually.")));
            this.addRenderableWidget(this.hideLeggingsBox);

            this.hideBootsBox = new Checkbox(contentLeft + 130, contentTop + 130, 120, 20, Component.literal("Hide Boots"), workingRace.hideBoots);
            this.hideBootsBox.setTooltip(Tooltip.create(Component.literal("Check to hide player boots armor piece visually.")));
            this.addRenderableWidget(this.hideBootsBox);

        } else if (activeTab == 1) { // Model & Animations
            Button modelTypeBtn = Button.builder(Component.literal("Type: " + workingRace.modelType), b -> {
                workingRace.modelType = "Default".equals(workingRace.modelType) ? "Custom" : "Default";
                this.init();
            }).bounds(contentLeft, contentTop, 140, 20).build();
            modelTypeBtn.setTooltip(Tooltip.create(Component.literal("Toggle Model Type between Default (Steve/Alex) and Custom parts model.")));
            this.addRenderableWidget(modelTypeBtn);

            this.heightScaleBox = new EditBox(this.font, contentLeft + 100, contentTop + 30, 60, 18, Component.literal("Height Scale"));
            this.heightScaleBox.setMaxLength(2048);
            this.heightScaleBox.setValue(String.valueOf(workingRace.heightScale));
            this.heightScaleBox.setTooltip(Tooltip.create(Component.literal("Height scale multiplier. Player model scaling Requires Pehkui.")));
            this.addRenderableWidget(this.heightScaleBox);

            this.widthScaleBox = new EditBox(this.font, contentLeft + 100, contentTop + 55, 60, 18, Component.literal("Width Scale"));
            this.widthScaleBox.setMaxLength(2048);
            this.widthScaleBox.setValue(String.valueOf(workingRace.widthScale));
            this.widthScaleBox.setTooltip(Tooltip.create(Component.literal("Width scale multiplier. Player model scaling Requires Pehkui.")));
            this.addRenderableWidget(this.widthScaleBox);

            this.healthBox = new EditBox(this.font, contentLeft + 100, contentTop + 80, 60, 18, Component.literal("Max Health"));
            this.healthBox.setMaxLength(2048);
            this.healthBox.setValue(String.valueOf(workingRace.maxHealth));
            this.healthBox.setTooltip(Tooltip.create(Component.literal("Base Max Health value (Vanilla default is 20.0).")));
            this.addRenderableWidget(this.healthBox);

            this.speedBox = new EditBox(this.font, contentLeft + 100, contentTop + 105, 60, 18, Component.literal("Movement Speed"));
            this.speedBox.setMaxLength(2048);
            this.speedBox.setValue(String.valueOf(workingRace.movementSpeed));
            this.speedBox.setTooltip(Tooltip.create(Component.literal("Base Movement Speed multiplier (Vanilla default is 0.1).")));
            this.addRenderableWidget(this.speedBox);

            // Open Body Parts & Color Picker Overlay
            Button partsBtn = Button.builder(Component.literal("§ePreset Body Parts & Colors"), b -> {
                Minecraft.getInstance().setScreen(new BodyPartOverlay(this, workingRace));
            }).bounds(contentLeft, contentTop + 135, 200, 22).build();
            partsBtn.setTooltip(Tooltip.create(Component.literal("Open Body Part Selector & RGB Color Picker Wheel overlay.")));
            this.addRenderableWidget(partsBtn);

        } else if (activeTab == 2) { // Positions / Part Transforms
            int py = contentTop;
            String[] partKeys = {"ears", "wings", "tail", "horns", "halo", "custom"};
            for (String pKey : partKeys) {
                PartTransformData pt = workingRace.partTransforms.computeIfAbsent(pKey, k -> new PartTransformData());

                EditBox xBox = new EditBox(this.font, contentLeft + 110, py, 45, 18, Component.literal(pKey + " X"));
                xBox.setMaxLength(2048);
                xBox.setValue(String.valueOf(pt.posX));
                xBox.setResponder(val -> { try { pt.posX = Float.parseFloat(val); } catch (Exception ignored) {} });
                this.addRenderableWidget(xBox);

                EditBox yBox = new EditBox(this.font, contentLeft + 160, py, 45, 18, Component.literal(pKey + " Y"));
                yBox.setMaxLength(2048);
                yBox.setValue(String.valueOf(pt.posY));
                yBox.setResponder(val -> { try { pt.posY = Float.parseFloat(val); } catch (Exception ignored) {} });
                this.addRenderableWidget(yBox);

                EditBox zBox = new EditBox(this.font, contentLeft + 210, py, 45, 18, Component.literal(pKey + " Z"));
                zBox.setMaxLength(2048);
                zBox.setValue(String.valueOf(pt.posZ));
                zBox.setResponder(val -> { try { pt.posZ = Float.parseFloat(val); } catch (Exception ignored) {} });
                this.addRenderableWidget(zBox);

                py += 24;
            }

        } else if (activeTab == 3) { // Passives
            int py = contentTop;
            String[] allPassives = {
                "night_vision", "water_breathing", "fire_resistance", "flight", "slow_falling",
                "regeneration", "wither_immunity", "fall_damage_immunity", "lava_swimming", "climbing"
            };

            for (String passive : allPassives) {
                boolean active = workingRace.passiveAbilities.contains(passive);
                Checkbox pBox = new Checkbox(contentLeft, py, 180, 20, Component.literal(passive.replace("_", " ").toUpperCase()), active) {
                    @Override
                    public void onPress() {
                        super.onPress();
                        if (this.selected()) {
                            if (!workingRace.passiveAbilities.contains(passive)) workingRace.passiveAbilities.add(passive);
                        } else {
                            workingRace.passiveAbilities.remove(passive);
                        }
                    }
                };
                pBox.setTooltip(Tooltip.create(Component.literal("Toggle passive ability: " + passive)));
                this.addRenderableWidget(pBox);
                py += 21;
            }

        } else if (activeTab == 4) { // Actives
            int py = contentTop;
            for (int slot = 1; slot <= 5; slot++) {
                final int currentSlot = slot;
                String currentSkill = workingRace.activeAbilities.getOrDefault(slot, "none");

                EditBox slotBox = new EditBox(this.font, contentLeft + 60, py, 200, 18, Component.literal("Slot " + slot));
                slotBox.setMaxLength(2048);
                slotBox.setValue(currentSkill);
                slotBox.setTooltip(Tooltip.create(Component.literal("Skill ID (e.g. flame_breath, teleport_dash, transform_were, summon_minions).")));
                slotBox.setResponder(val -> workingRace.activeAbilities.put(currentSlot, val));
                this.addRenderableWidget(slotBox);

                py += 24;
            }

        } else if (activeTab == 7) { // Alliances
            int py = contentTop;
            String[] factions = {"minecraft:zombie", "minecraft:skeleton", "minecraft:spider", "minecraft:creeper", "minecraft:enderman", "minecraft:piglin"};
            for (String mobId : factions) {
                boolean isNeutral = workingRace.alliances.stream().anyMatch(a -> mobId.equalsIgnoreCase(a.mobId));
                Checkbox aBox = new Checkbox(contentLeft, py, 180, 20, Component.literal(mobId.substring(mobId.indexOf(':') + 1).toUpperCase() + " Neutrality"), isNeutral) {
                    @Override
                    public void onPress() {
                        super.onPress();
                        if (this.selected()) {
                            if (workingRace.alliances.stream().noneMatch(a -> mobId.equalsIgnoreCase(a.mobId))) {
                                workingRace.alliances.add(new MobAllianceData(mobId, "neutral"));
                            }
                        } else {
                            workingRace.alliances.removeIf(a -> mobId.equalsIgnoreCase(a.mobId));
                        }
                    }
                };
                aBox.setTooltip(Tooltip.create(Component.literal("Toggle neutrality stance for " + mobId)));
                this.addRenderableWidget(aBox);
                py += 22;
            }

        } else if (activeTab == 5) { // Sounds & FX
            this.ambientSoundBox = new EditBox(this.font, contentLeft + 110, contentTop, 200, 18, Component.literal("Ambient Sound"));
            this.ambientSoundBox.setMaxLength(2048);
            this.ambientSoundBox.setValue(workingRace.ambientSound);
            this.ambientSoundBox.setTooltip(Tooltip.create(Component.literal("Sound event ID for ambient idle sounds.")));
            this.addRenderableWidget(this.ambientSoundBox);

            Button playAmbBtn = Button.builder(Component.literal("▶ Play"), b -> {
                playPreviewSound(this.ambientSoundBox.getValue());
            }).bounds(contentLeft + 315, contentTop, 50, 18).build();
            playAmbBtn.setTooltip(Tooltip.create(Component.literal("Preview sound playback.")));
            this.addRenderableWidget(playAmbBtn);

            this.hurtSoundBox = new EditBox(this.font, contentLeft + 110, contentTop + 30, 200, 18, Component.literal("Hurt Sound"));
            this.hurtSoundBox.setMaxLength(2048);
            this.hurtSoundBox.setValue(workingRace.hurtSound);
            this.hurtSoundBox.setTooltip(Tooltip.create(Component.literal("Sound event ID when receiving damage.")));
            this.addRenderableWidget(this.hurtSoundBox);

            Button playHurtBtn = Button.builder(Component.literal("▶ Play"), b -> {
                playPreviewSound(this.hurtSoundBox.getValue());
            }).bounds(contentLeft + 315, contentTop + 30, 50, 18).build();
            playHurtBtn.setTooltip(Tooltip.create(Component.literal("Preview sound playback.")));
            this.addRenderableWidget(playHurtBtn);

            this.deathSoundBox = new EditBox(this.font, contentLeft + 110, contentTop + 60, 200, 18, Component.literal("Death Sound"));
            this.deathSoundBox.setMaxLength(2048);
            this.deathSoundBox.setValue(workingRace.deathSound);
            this.deathSoundBox.setTooltip(Tooltip.create(Component.literal("Sound event ID when dying.")));
            this.addRenderableWidget(this.deathSoundBox);

            Button playDeathBtn = Button.builder(Component.literal("▶ Play"), b -> {
                playPreviewSound(this.deathSoundBox.getValue());
            }).bounds(contentLeft + 315, contentTop + 60, 50, 18).build();
            playDeathBtn.setTooltip(Tooltip.create(Component.literal("Preview sound playback.")));
            this.addRenderableWidget(playDeathBtn);

        } else if (activeTab == 6) { // Advanced Features
            this.spawnDimensionBox = new EditBox(this.font, contentLeft + 120, contentTop, 180, 18, Component.literal("Spawn Dimension"));
            this.spawnDimensionBox.setMaxLength(2048);
            this.spawnDimensionBox.setValue(workingRace.spawnDimension);
            this.spawnDimensionBox.setTooltip(Tooltip.create(Component.literal("Dimension ID for custom race spawn point (e.g. minecraft:the_nether).")));
            this.addRenderableWidget(this.spawnDimensionBox);

            this.spawnBiomeBox = new EditBox(this.font, contentLeft + 120, contentTop + 30, 180, 18, Component.literal("Spawn Biome"));
            this.spawnBiomeBox.setMaxLength(2048);
            this.spawnBiomeBox.setValue(workingRace.spawnBiome);
            this.spawnBiomeBox.setTooltip(Tooltip.create(Component.literal("Biome ID for custom race spawn point (e.g. minecraft:ocean).")));
            this.addRenderableWidget(this.spawnBiomeBox);

            this.enableAlliancesBox = new Checkbox(contentLeft, contentTop + 65, 180, 20, Component.literal("Enable Custom Alliances"), workingRace.enableAlliances);
            this.enableAlliancesBox.setTooltip(Tooltip.create(Component.literal("Enables the Alliances tab for setting mob neutrality stances.")));
            this.addRenderableWidget(this.enableAlliancesBox);

            // Minion Ability Settings
            this.minionMobTypeBox = new EditBox(this.font, contentLeft + 120, contentTop + 95, 200, 18, Component.literal("Minion Mob Type"));
            this.minionMobTypeBox.setMaxLength(2048);
            this.minionMobTypeBox.setValue(workingRace.minionMobType);
            this.minionMobTypeBox.setTooltip(Tooltip.create(Component.literal("Mob Entity ID to summon (e.g. minecraft:zombie or custom_mobs:<id>).")));
            this.addRenderableWidget(this.minionMobTypeBox);

            this.minionCountBox = new EditBox(this.font, contentLeft + 120, contentTop + 118, 60, 18, Component.literal("Minion Count"));
            this.minionCountBox.setMaxLength(2048);
            this.minionCountBox.setValue(String.valueOf(workingRace.minionCount));
            this.minionCountBox.setTooltip(Tooltip.create(Component.literal("Number of minions to summon (1 to 10).")));
            this.addRenderableWidget(this.minionCountBox);

            this.minionScaleBox = new EditBox(this.font, contentLeft + 260, contentTop + 118, 60, 18, Component.literal("Minion Scale"));
            this.minionScaleBox.setMaxLength(2048);
            this.minionScaleBox.setValue(String.valueOf(workingRace.minionScale));
            this.minionScaleBox.setTooltip(Tooltip.create(Component.literal("Minion size scaling multiplier (0.5 to 5.0).")));
            this.addRenderableWidget(this.minionScaleBox);

            this.minionIsRangedBox = new Checkbox(contentLeft, contentTop + 141, 120, 20, Component.literal("Minion Ranged"), workingRace.minionIsRanged);
            this.minionIsRangedBox.setTooltip(Tooltip.create(Component.literal("Check if minion shoots ranged projectiles instead of melee.")));
            this.addRenderableWidget(this.minionIsRangedBox);

            this.minionProjectileBox = new EditBox(this.font, contentLeft + 250, contentTop + 142, 160, 18, Component.literal("Minion Projectile"));
            this.minionProjectileBox.setMaxLength(2048);
            this.minionProjectileBox.setValue(workingRace.minionProjectile);
            this.minionProjectileBox.setTooltip(Tooltip.create(Component.literal("Projectile Entity ID if minion is ranged (e.g. minecraft:arrow).")));
            this.addRenderableWidget(this.minionProjectileBox);
        } else if (activeTab == 8) { // Were Model & Anims
            this.enableWereBox = new Checkbox(contentLeft, contentTop, 180, 20, Component.literal("Enable Were-Form"), workingRace.enableWereRace);
            this.enableWereBox.setTooltip(Tooltip.create(Component.literal("Toggle Were-form transformation capabilities for this race.")));
            this.addRenderableWidget(this.enableWereBox);

            this.wereConditionBox = new EditBox(this.font, contentLeft + 120, contentTop + 24, 120, 18, Component.literal("Trigger Condition"));
            this.wereConditionBox.setMaxLength(2048);
            this.wereConditionBox.setValue(workingRace.wereTriggerCondition);
            this.wereConditionBox.setTooltip(Tooltip.create(Component.literal("FULL_MOON, NEW_MOON, NIGHT, or MANUAL.")));
            this.addRenderableWidget(this.wereConditionBox);

            this.wereModelBox = new EditBox(this.font, contentLeft + 120, contentTop + 46, 240, 18, Component.literal("Were Model Geo JSON"));
            this.wereModelBox.setMaxLength(2048);
            this.wereModelBox.setValue(workingRace.wereModelPath);
            this.wereModelBox.setTooltip(Tooltip.create(Component.literal("GeckoLib Were model file (e.g. customraces:models/were/werewolf.geo.json).")));
            this.addRenderableWidget(this.wereModelBox);

            this.wereTextureBox = new EditBox(this.font, contentLeft + 120, contentTop + 68, 240, 18, Component.literal("Were Texture PNG"));
            this.wereTextureBox.setMaxLength(2048);
            this.wereTextureBox.setValue(workingRace.wereTexturePath);
            this.wereTextureBox.setTooltip(Tooltip.create(Component.literal("Were-form PNG texture (e.g. customraces:textures/were/werewolf.png).")));
            this.addRenderableWidget(this.wereTextureBox);

            this.wereAnimFileBox = new EditBox(this.font, contentLeft + 120, contentTop + 90, 240, 18, Component.literal("Were Animation JSON"));
            this.wereAnimFileBox.setMaxLength(2048);
            this.wereAnimFileBox.setValue(workingRace.wereAnimationPath);
            this.wereAnimFileBox.setTooltip(Tooltip.create(Component.literal("GeckoLib Were animation file (e.g. customraces:animations/were/werewolf.animation.json).")));
            this.addRenderableWidget(this.wereAnimFileBox);

            this.wereIdleAnimBox = new EditBox(this.font, contentLeft + 120, contentTop + 112, 140, 18, Component.literal("Idle Animation"));
            this.wereIdleAnimBox.setMaxLength(2048);
            this.wereIdleAnimBox.setValue(workingRace.wereIdleAnim);
            this.addRenderableWidget(this.wereIdleAnimBox);

            this.wereWalkAnimBox = new EditBox(this.font, contentLeft + 120, contentTop + 134, 140, 18, Component.literal("Walk Animation"));
            this.wereWalkAnimBox.setMaxLength(2048);
            this.wereWalkAnimBox.setValue(workingRace.wereWalkAnim);
            this.addRenderableWidget(this.wereWalkAnimBox);

            this.wereAttackAnimBox = new EditBox(this.font, contentLeft + 120, contentTop + 156, 140, 18, Component.literal("Attack Animation"));
            this.wereAttackAnimBox.setMaxLength(2048);
            this.wereAttackAnimBox.setValue(workingRace.wereAttackAnim);
            this.addRenderableWidget(this.wereAttackAnimBox);

        } else if (activeTab == 9) { // Were Sounds
            this.wereTransformSoundBox = new EditBox(this.font, contentLeft + 120, contentTop, 200, 18, Component.literal("Transform Sound"));
            this.wereTransformSoundBox.setMaxLength(2048);
            this.wereTransformSoundBox.setValue(workingRace.wereTransformSound);
            this.addRenderableWidget(this.wereTransformSoundBox);

            Button pTr = Button.builder(Component.literal("▶ Play"), b -> playPreviewSound(this.wereTransformSoundBox.getValue())).bounds(contentLeft + 325, contentTop, 50, 18).build();
            this.addRenderableWidget(pTr);

            this.wereHowlSoundBox = new EditBox(this.font, contentLeft + 120, contentTop + 25, 200, 18, Component.literal("Howl Sound"));
            this.wereHowlSoundBox.setMaxLength(2048);
            this.wereHowlSoundBox.setValue(workingRace.wereHowlSound);
            this.addRenderableWidget(this.wereHowlSoundBox);

            Button pHw = Button.builder(Component.literal("▶ Play"), b -> playPreviewSound(this.wereHowlSoundBox.getValue())).bounds(contentLeft + 325, contentTop + 25, 50, 18).build();
            this.addRenderableWidget(pHw);

            this.wereAmbientSoundBox = new EditBox(this.font, contentLeft + 120, contentTop + 50, 200, 18, Component.literal("Ambient Sound"));
            this.wereAmbientSoundBox.setMaxLength(2048);
            this.wereAmbientSoundBox.setValue(workingRace.wereAmbientSound);
            this.addRenderableWidget(this.wereAmbientSoundBox);

            Button pAm = Button.builder(Component.literal("▶ Play"), b -> playPreviewSound(this.wereAmbientSoundBox.getValue())).bounds(contentLeft + 325, contentTop + 50, 50, 18).build();
            this.addRenderableWidget(pAm);

            this.wereHurtSoundBox = new EditBox(this.font, contentLeft + 120, contentTop + 75, 200, 18, Component.literal("Hurt Sound"));
            this.wereHurtSoundBox.setMaxLength(2048);
            this.wereHurtSoundBox.setValue(workingRace.wereHurtSound);
            this.addRenderableWidget(this.wereHurtSoundBox);

            Button pHr = Button.builder(Component.literal("▶ Play"), b -> playPreviewSound(this.wereHurtSoundBox.getValue())).bounds(contentLeft + 325, contentTop + 75, 50, 18).build();
            this.addRenderableWidget(pHr);

            this.wereDeathSoundBox = new EditBox(this.font, contentLeft + 120, contentTop + 100, 200, 18, Component.literal("Death Sound"));
            this.wereDeathSoundBox.setMaxLength(2048);
            this.wereDeathSoundBox.setValue(workingRace.wereDeathSound);
            this.addRenderableWidget(this.wereDeathSoundBox);

            Button pDt = Button.builder(Component.literal("▶ Play"), b -> playPreviewSound(this.wereDeathSoundBox.getValue())).bounds(contentLeft + 325, contentTop + 100, 50, 18).build();
            this.addRenderableWidget(pDt);
        }
    }

    private void readFormInputs() {
        if (nameBox != null) workingRace.name = nameBox.getValue();
        if (nameColorBox != null) workingRace.nameColor = nameColorBox.getValue();
        if (customTextureBox != null) workingRace.customTexture = customTextureBox.getValue();
        if (loreBox != null) workingRace.lore = loreBox.getValue();
        if (iconBox != null) workingRace.iconItem = iconBox.getValue();
        if (difficultyBox != null) {
            try { workingRace.playstyleDifficulty = Integer.parseInt(difficultyBox.getValue()); } catch (Exception ignored) {}
        }
        if (hideHelmetBox != null) workingRace.hideHelmet = hideHelmetBox.selected();
        if (hideChestplateBox != null) workingRace.hideChestplate = hideChestplateBox.selected();
        if (hideLeggingsBox != null) workingRace.hideLeggings = hideLeggingsBox.selected();
        if (hideBootsBox != null) workingRace.hideBoots = hideBootsBox.selected();

        if (heightScaleBox != null) {
            try { workingRace.heightScale = Float.parseFloat(heightScaleBox.getValue()); } catch (Exception ignored) {}
        }
        if (widthScaleBox != null) {
            try { workingRace.widthScale = Float.parseFloat(widthScaleBox.getValue()); } catch (Exception ignored) {}
        }
        if (healthBox != null) {
            try { workingRace.maxHealth = Float.parseFloat(healthBox.getValue()); } catch (Exception ignored) {}
        }
        if (speedBox != null) {
            try { workingRace.movementSpeed = Float.parseFloat(speedBox.getValue()); } catch (Exception ignored) {}
        }
        if (ambientSoundBox != null) workingRace.ambientSound = ambientSoundBox.getValue();
        if (hurtSoundBox != null) workingRace.hurtSound = hurtSoundBox.getValue();
        if (deathSoundBox != null) workingRace.deathSound = deathSoundBox.getValue();
        if (spawnDimensionBox != null) workingRace.spawnDimension = spawnDimensionBox.getValue();
        if (spawnBiomeBox != null) workingRace.spawnBiome = spawnBiomeBox.getValue();
        if (enableAlliancesBox != null) workingRace.enableAlliances = enableAlliancesBox.selected();
        if (minionMobTypeBox != null) workingRace.minionMobType = minionMobTypeBox.getValue();
        if (minionCountBox != null) {
            try { workingRace.minionCount = Integer.parseInt(minionCountBox.getValue()); } catch (Exception ignored) {}
        }
        if (minionScaleBox != null) {
            try { workingRace.minionScale = Float.parseFloat(minionScaleBox.getValue()); } catch (Exception ignored) {}
        }
        if (minionIsRangedBox != null) workingRace.minionIsRanged = minionIsRangedBox.selected();
        if (minionProjectileBox != null) workingRace.minionProjectile = minionProjectileBox.getValue();

        if (enableWereBox != null) workingRace.enableWereRace = enableWereBox.selected();
        if (wereConditionBox != null) workingRace.wereTriggerCondition = wereConditionBox.getValue();
        if (wereModelBox != null) workingRace.wereModelPath = wereModelBox.getValue();
        if (wereTextureBox != null) workingRace.wereTexturePath = wereTextureBox.getValue();
        if (wereAnimFileBox != null) workingRace.wereAnimationPath = wereAnimFileBox.getValue();
        if (wereIdleAnimBox != null) workingRace.wereIdleAnim = wereIdleAnimBox.getValue();
        if (wereWalkAnimBox != null) workingRace.wereWalkAnim = wereWalkAnimBox.getValue();
        if (wereAttackAnimBox != null) workingRace.wereAttackAnim = wereAttackAnimBox.getValue();

        if (wereTransformSoundBox != null) workingRace.wereTransformSound = wereTransformSoundBox.getValue();
        if (wereHowlSoundBox != null) workingRace.wereHowlSound = wereHowlSoundBox.getValue();
        if (wereAmbientSoundBox != null) workingRace.wereAmbientSound = wereAmbientSoundBox.getValue();
        if (wereHurtSoundBox != null) workingRace.wereHurtSound = wereHurtSoundBox.getValue();
        if (wereDeathSoundBox != null) workingRace.wereDeathSound = wereDeathSoundBox.getValue();
    }

    private void playPreviewSound(String soundId) {
        if (soundId == null || soundId.isEmpty() || Minecraft.getInstance().player == null) return;
        Minecraft.getInstance().player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0f, 1.0f);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.fill(0, 0, this.width, this.height, 0xEE101216);

        // Header Bar
        guiGraphics.fill(0, 0, this.width, 28, 0xFF181B20);
        guiGraphics.fill(0, 27, this.width, 28, 0xFF353A45);
        guiGraphics.drawString(this.font, "§lRACE CREATOR ADMIN GUI", 12, 9, 0xFFFFFF);

        // Form Field Labels
        int contentLeft = 15;
        int contentTop = 60;

        if (activeTab == 0) {
            guiGraphics.drawString(this.font, "Race Name:", contentLeft, contentTop + 4, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Name Color:", contentLeft, contentTop + 27, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Difficulty (1-10):", contentLeft, contentTop + 50, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Lore Description:", contentLeft, contentTop + 73, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Icon Item ID:", contentLeft, contentTop + 96, 0xCCCCCC);
            guiGraphics.drawString(this.font, "PNG Picture Path:", contentLeft, contentTop + 119, 0xCCCCCC);
        } else if (activeTab == 1) {
            guiGraphics.drawString(this.font, "Height Scale:", contentLeft, contentTop + 34, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Width Scale:", contentLeft, contentTop + 59, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Max Health:", contentLeft, contentTop + 84, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Move Speed:", contentLeft, contentTop + 109, 0xCCCCCC);
        } else if (activeTab == 2) {
            guiGraphics.drawString(this.font, "§ePart Scale & Offset (X, Y, Z):", contentLeft, contentTop - 12, 0xFFFFFF);
            String[] partKeys = {"Ears", "Wings", "Tail", "Horns", "Halo", "Custom"};
            int py = contentTop;
            for (String pKey : partKeys) {
                guiGraphics.drawString(this.font, pKey + ":", contentLeft, py + 4, 0xCCCCCC);
                py += 24;
            }
        } else if (activeTab == 3) {
            guiGraphics.drawString(this.font, "§aToggle Passive Race Abilities:", contentLeft, contentTop - 12, 0xFFFFFF);
        } else if (activeTab == 4) {
            guiGraphics.drawString(this.font, "§cAssign Active Skills (Slots 1-5):", contentLeft, contentTop - 12, 0xFFFFFF);
            int py = contentTop;
            for (int slot = 1; slot <= 5; slot++) {
                guiGraphics.drawString(this.font, "Slot " + slot + ":", contentLeft, py + 4, 0xCCCCCC);
                py += 24;
            }
        } else if (activeTab == 7) {
            guiGraphics.drawString(this.font, "§bMob Faction Neutrality Stances:", contentLeft, contentTop - 12, 0xFFFFFF);
        } else if (activeTab == 5) {
            guiGraphics.drawString(this.font, "Ambient Sound:", contentLeft, contentTop + 4, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Hurt Sound:", contentLeft, contentTop + 34, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Death Sound:", contentLeft, contentTop + 64, 0xCCCCCC);
        } else if (activeTab == 6) {
            guiGraphics.drawString(this.font, "Spawn Dimension:", contentLeft, contentTop + 4, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Spawn Biome:", contentLeft, contentTop + 34, 0xCCCCCC);
        } else if (activeTab == 8) {
            guiGraphics.drawString(this.font, "Trigger Moon Phase:", contentLeft, contentTop + 28, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Were Geo Model:", contentLeft, contentTop + 50, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Were Texture PNG:", contentLeft, contentTop + 72, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Were Anim JSON:", contentLeft, contentTop + 94, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Idle Animation:", contentLeft, contentTop + 116, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Walk Animation:", contentLeft, contentTop + 138, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Attack Animation:", contentLeft, contentTop + 160, 0xCCCCCC);
        } else if (activeTab == 9) {
            guiGraphics.drawString(this.font, "Transform Sound:", contentLeft, contentTop + 4, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Howl Sound:", contentLeft, contentTop + 29, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Ambient Sound:", contentLeft, contentTop + 54, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Hurt Sound:", contentLeft, contentTop + 79, 0xCCCCCC);
            guiGraphics.drawString(this.font, "Death Sound:", contentLeft, contentTop + 104, 0xCCCCCC);
        }

        // Right Panel: 3D Live Entity Preview
        int rightLeft = this.width - 150;
        guiGraphics.fill(rightLeft, 32, this.width - 10, this.height - 10, 0xFF14171C);
        guiGraphics.drawCenteredString(this.font, "§7Preview Viewport", rightLeft + 70, 38, 0xFFFFFF);

        if (this.minecraft != null && this.minecraft.player != null) {
            int previewX = rightLeft + 70;
            int previewY = this.height - 40;
            int scale = (int) (45 * workingRace.heightScale * workingRace.baseScale);
            InventoryScreen.renderEntityInInventoryFollowsMouse(
                    guiGraphics, previewX, previewY, scale,
                    (float)(previewX - mouseX), (float)(previewY - 45 - mouseY),
                    this.minecraft.player
            );
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 5. Render Floating Auto-Complete Suggestion Dropdown Box
        updateCurrentSuggestions();
        if (showSuggestions && !activeSuggestions.isEmpty() && activeField != null) {
            int dropX = activeField.getX();
            int dropY = activeField.getY() + activeField.getHeight() + 2;
            int dropW = Math.max(activeField.getWidth(), 200);
            int rowH = 14;
            int maxVisible = 6;
            int visible = Math.min(maxVisible, activeSuggestions.size());
            int dropH = visible * rowH + 4;

            guiGraphics.fill(dropX, dropY, dropX + dropW, dropY + dropH, 0xFE12151B);
            guiGraphics.fill(dropX, dropY, dropX + dropW, dropY + 1, 0xFF4A80C0);
            guiGraphics.fill(dropX, dropY + dropH - 1, dropX + dropW, dropY + dropH, 0xFF4A80C0);

            for (int i = 0; i < visible; i++) {
                int idx = i + suggestionsScrollOffset;
                if (idx >= activeSuggestions.size()) break;
                String sugg = activeSuggestions.get(idx);
                int itemY = dropY + 2 + i * rowH;

                boolean isHover = mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= itemY && mouseY <= itemY + rowH;
                if (isHover) {
                    guiGraphics.fill(dropX + 2, itemY, dropX + dropW - 2, itemY + rowH, 0xFF2A364F);
                }

                guiGraphics.drawString(this.font, "§e" + sugg, dropX + 6, itemY + 3, 0xFFFFFF);
            }
        }
    }

    private void updateCurrentSuggestions() {
        showSuggestions = false;
        activeSuggestions.clear();
        activeField = null;

        for (net.minecraft.client.gui.components.events.GuiEventListener child : this.children()) {
            if (child instanceof EditBox box && box.isFocused()) {
                String val = box.getValue().toLowerCase().trim();
                List<String> source = null;

                if (box == ambientSoundBox || box == hurtSoundBox || box == deathSoundBox || box == wereTransformSoundBox || box == wereHowlSoundBox || box == wereAmbientSoundBox || box == wereHurtSoundBox || box == wereDeathSoundBox) {
                    source = RaceRegistry.CACHED_SOUNDS;
                } else if (box == iconBox) {
                    source = RaceRegistry.CACHED_ITEMS;
                } else if (box == spawnDimensionBox) {
                    source = RaceRegistry.CACHED_DIMENSIONS;
                } else if (box == spawnBiomeBox) {
                    source = RaceRegistry.CACHED_BIOMES;
                } else if (box == minionMobTypeBox || box == minionProjectileBox) {
                    source = RaceRegistry.CACHED_PROJECTILES;
                }

                if (source != null && !source.isEmpty()) {
                    activeField = box;
                    final String query = val;
                    activeSuggestions = source.stream()
                            .filter(s -> query.isEmpty() || s.toLowerCase().contains(query))
                            .limit(20)
                            .collect(Collectors.toList());
                    showSuggestions = !activeSuggestions.isEmpty();
                }
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (showSuggestions && !activeSuggestions.isEmpty() && activeField != null) {
            int dropX = activeField.getX();
            int dropY = activeField.getY() + activeField.getHeight() + 2;
            int dropW = Math.max(activeField.getWidth(), 200);
            int rowH = 14;
            int maxVisible = 6;
            int visible = Math.min(maxVisible, activeSuggestions.size());
            int dropH = visible * rowH + 4;

            if (mouseX >= dropX && mouseX <= dropX + dropW && mouseY >= dropY && mouseY <= dropY + dropH) {
                int row = (int) ((mouseY - dropY - 2) / rowH);
                int idx = row + suggestionsScrollOffset;
                if (idx >= 0 && idx < activeSuggestions.size()) {
                    activeField.setValue(activeSuggestions.get(idx));
                    showSuggestions = false;
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }
}
