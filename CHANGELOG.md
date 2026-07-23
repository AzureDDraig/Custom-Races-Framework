# 📜 Custom Races Framework - Rolling Changelog

All notable changes, features, bug fixes, and build deployments for **Custom Races Framework** are documented here.

---

## [1.0.0-b048a] - 2026-07-22

### 🔮 Native Spells 1, 2, 3, 4, and 5 System Integration
- **Multi-Slot Native Spells Data (`RaceData.java`)**:
  - Added support for **Slots 1 through 5** for Native Spells (`nativeSpellId1`..`5`, `wildMagic1`..`5`, `nativeSpellLevel1`..`5`) and Were-Form counterparts.
  - Added backwards-compatible fallback getters `getNativeSpellId(slot, isWere)`, `getWildMagic(slot, isWere)`, and `getNativeSpellLevel(slot, isWere)`.
- **Active Skill Keybinds (`ActiveAbilityHandler.java`, `IronSpellsHandler.java`)**:
  - Registered skill IDs `native_spell_1`, `native_spell_2`, `native_spell_3`, `native_spell_4`, `native_spell_5`.
  - Pressing keybind slots assigned to any of `native_spell_1` to `5` executes `IronSpellsHandler.castNativeSpell(player, race, isWere, slot)`.
- **Tab 11 Sub-Slot Controls (`RaceCreatorScreen.java`)**:
  - Added interactive sub-slot selection buttons (`[Slot 1]`, `[Slot 2]`, `[Slot 3]`, `[Slot 4]`, `[Slot 5]`) in **Tab 11: Native Spells**.

---

## [1.0.0-b047a] - 2026-07-22

### 🌟 Expanded 100 Passives & 100 Active Skills Catalogue
- **100 Unique Passives Catalogue (`ALL_PASSIVES`)**:
  - Expanded Passives list to **100 unique options** across 10 categories (Elemental, Defense, Mobility, Combat, Utility, Magic, Vampiric, Celestial, Beast, Tech).
  - Converted **Tab 3: Passives** into a 1-column scrollable list with a vertical scrollbar track and dragable green thumb (`0xFF5555FF` / `0xFFFF9900`).
- **100 Unique Active Skills Catalogue (`ALL_ACTIVES`)**:
  - Expanded Active Skills list to **100 unique options** across 10 categories (Fire, Ice, Lightning, Shadow, Holy, Blood, Earth, Wind, Beast, Tech).
  - Updated autocomplete suggestion overlays for all active skill textboxes to suggest all 100 active skills.

---

## [1.0.0-b046a] - 2026-07-22

### 📜 Single-Column Scrollable Drawbacks Tab & Interactive Scrollbar
- **1 Column Drawbacks List (`RaceCreatorScreen.java`)**:
  - Converted **Tab 10: Drawbacks** into 1 single wide column (`320px` width) displaying full drawback names, warning icons (`⚠️`), and complete description tooltips without text truncation across all 150 drawbacks.
- **Interactive Scrollbar & Dragable Thumb**:
  - Implemented vertical scrollbar track at `contentLeft + 330` with dynamic red warning thumb (`0xFFCC3333` / `0xFFFF5555`).
  - Added support for mouse wheel scrolling and mouse dragging of the scrollbar thumb.
  - Added viewport scissor bounds checking so checkboxes outside the scroll region are cleanly hidden.

---

## [1.0.0-b045a] - 2026-07-22

### 🔍 Spells System & Deep Search Audit
- **Multi-Tier Reflection Execution Engine (`IronSpellsHandler.java`)**:
  - Enhanced `IronSpellsHandler.java` with multi-tier reflection fallback for `io.github.elytra.irons_spellbooks.api.registry.SpellRegistry` and legacy package namespaces.
  - Dynamically inspects method parameters for `castSpell(...)` supporting both 4-argument and 3-argument API method signatures across different Iron's Spells mod versions.
- **Verified Deep Tooltip Search (`RaceCreatorScreen.java`)**:
  - Audited search resolution across Passives (Tab 3), Actives (Tab 4), and Drawbacks (Tab 10). Search queries match **BOTH** item/drawback names **AND** their full description/tooltip text.

---

## [1.0.0-b044a] - 2026-07-22

### 🔍 Real-Time Deep Search Across Passives, Actives, and Drawbacks
- **GUI Search Boxes (`RaceCreatorScreen.java`)**:
  - Added dedicated search input bars at `contentTop` for **Tab 3 (Passives)**, **Tab 4 (Actives)**, and **Tab 10 (Drawbacks)**.
  - Shifted grid and checkbox layouts cleanly to `contentTop + 24`, guaranteeing zero overlap over headers or search inputs.
- **Deep Tooltip & Description Searching**:
  - Search queries match **BOTH** item/drawback names **AND** their full description/tooltip text (e.g. searching *"fire"*, *"armor"*, *"speed"*, *"iron"*, *"damage"*, *"slowness"* instantly isolates all matching traits and drawbacks).

---

## [1.0.0-b043a] - 2026-07-22

### 🔍 Native Spells & Iron's Spells System Audit
- **Full System Audit for Native Spells (`IronSpellsHandler.java`, `ActiveAbilityHandler.java`, `RaceCreatorScreen.java`)**:
  - Conducted line-by-line verification of Native Spells integration across data persistence, dynamic UI tab positioning, soft reflection execution engine, active skill keybinds, and 10 language localizations.
  - Verified clean execution and build compilation across both Forge and Fabric.

---

## [1.0.0-b042a] - 2026-07-22

### 🔮 Iron's Spells 'n Spellbooks & T.O Tweaks Integration
- **Native Spell Ability & Dynamic Tab (`RaceCreatorScreen.java`, `IronSpellsHandler.java`)**:
  - Added `"Native Spell"` capability and dedicated **Native Spells** tab (`gui.customraces.tab.native_spells`).
  - **Spell Dropdown & Cycle Selector**: Populated with all 50+ spells from **Iron's Spells 'n Spellbooks** and **T.O Tweaks** (`irons_spellbooks:fireball`, `irons_spellbooks:lightning_lance`, `irons_spellbooks:blood_slash`, `totweaks:time_stop`, `totweaks:spatial_rend`, etc.).
  - **Wild Magic Checkbox**: Checkbox to enable Wild Magic, casting a random spell from any school as if the player cast it.
  - **10 Language Localizations**: Added translated tooltips and headers for `en_us`, `de_de`, `es_es`, `fr_fr`, `it_it`, `ja_jp`, `ko_kr`, `pt_br`, `ru_ru`, `zh_cn` stating: *"Requires Iron's Spells 'n Spellbooks mod to function."*
- **Soft Reflection Execution Engine (`IronSpellsHandler.java`)**:
  - Soft-reflection execution allows seamless compilation and execution with or without Iron's Spells loaded.

---

## [1.0.0-b041a] - 2026-07-22

### 🌐 Overlap-Free Multi-Language Layout Audit
- **Expanded Label Clearance for EditBoxes (`RaceCreatorScreen.java`)**:
  - Increased EditBox X offsets across Tab 0 (`+130`), Tab 1 (`+140`), Tab 8 (`+135`), and Tab 9 (`+135`).
  - Prevents long labels (e.g. `❖ Were Damage Bonus:`, `❖ PNG Picture Path:`, and German/Russian translations) from overlapping textboxes.
- **Smart Checkbox Truncation & Hover Tooltips**:
  - Added dynamic font width truncation (`font.plainSubstrByWidth(rawName, colWidth - 28) + ".."`) and full hover tooltips for multi-column Checkbox grids in Tab 3 (Passives) and Tab 10 (Drawbacks).
  - Guarantees 100% overlap-free checkbox grids across all 9 supported languages.

---

## [1.0.0-b040a] - 2026-07-22

### 📐 Fixed GUI Text & Tab Overlaps
- **Dynamic Content Top Calculation (`RaceCreatorScreen.java`)**:
  - Replaced hardcoded `contentTop = 50` and negative offsets (`contentTop - 12`) with dynamic offset calculation (`lastTabY + tabHeight + 14`).
  - Guarantees section titles and content cards ALWAYS sit safely below the bottom row of tab buttons regardless of row count or screen resolution.
- **Auto-Sizing Tab Buttons Across All Languages**:
  - Replaced fixed `62px` tab widths with dynamic font width measurement (`Math.max(52, font.width(tabText) + 10)`).
  - Eliminates tab text overflow and truncation across all 9 supported languages.

---

## [1.0.0-b039a] - 2026-07-22

### 🛡️ Sodium / Embeddium Compatibility & Full 150 Drawbacks Audit
- **Sodium & Embeddium Render Layer Compatibility (`PlayerRaceLayer.java`)**:
  - Audited feature layer pipeline for full compatibility with **Sodium (Fabric)**, **Embeddium (Forge)**, **Iris**, **Oculus**, and **Indium**.
- **100% Verified 150 Drawbacks Execution Engine (`PassiveAbilityHandler.java`, `DrawbackEventHandler.java`)**:
  - Itemized and verified execution logic across all 150 drawbacks.

---

## [1.0.0-b038a] - 2026-07-22

### 🌟 150 Total Race Drawbacks & Weaknesses Catalogue
- **Full 150 Drawbacks System Expansion (`RaceCreatorScreen.java`, `DrawbackEventHandler.java`)**:
  - Expanded **Tab 10: Drawbacks** (`gui.customraces.tab.drawbacks`) to include **150 unique drawbacks & weaknesses** across 9 categories.
  - Implemented execution mechanics in `DrawbackEventHandler.java` for all 90 new drawbacks.

---

## [1.0.0-b037a] - 2026-07-22

### ⚡ Drawback Event Handler & Complete Verification
- **Event-Driven Drawbacks Engine (`DrawbackEventHandler.java`)**:
  - Implemented `DrawbackEventHandler.java` to handle event-driven mechanics for drawbacks.

---

## [1.0.0-b036a] - 2026-07-22

### ⚠️ Dedicated Tab 10: Drawbacks UI
- **Tab 10: Drawbacks UI (`RaceCreatorScreen.java`)**:
  - Added standalone Tab 10 for selecting drawbacks with red warning borders and Were-Form support.

---

## [1.0.0-b035a] - 2026-07-22

### 🎨 In-World & 3D GUI Player Model Preset Parts Rendering Fix
- **Player Feature Renderer Registration**:
  - Registered `PlayerRaceLayer` for player entity renderers via `LivingEntityFeatureRendererRegistrationCallback` on Fabric and `EntityRenderersEvent.AddLayers` on Forge.
  - Preset body parts (Ears, Horns, Wings, Tail, Halo) now render live in-world and on 3D GUI player previews.

---

## [1.0.0-b034a] - 2026-07-22

### 🎬 Dynamic GeckoLib Animation JSON Parsing
- **Direct Animation Key Extraction (`RaceRegistry.java`)**:
  - Added `RaceRegistry.parseAnimationKeysFromFile(animPath)` to read GeckoLib animation JSON files directly from disk or Minecraft assets, populating autocomplete with actual animation names (`animation.elvenmage.idle`, etc.).

---

## [1.0.0-b033a] - 2026-07-22

### 🔄 Real-Time Auto-Save & Client Were-Form State Sync
- **Real-Time Auto-Save (`RaceCreatorScreen.java`)**:
  - Implemented `autoSaveWorkingRace()` so focus loss, keystrokes, tab clicks, and race switches instantly save race data to server/config.
- **Were-Form Client State Sync (`ModPackets.java`, `WereRaceTransformHandler.java`)**:
  - Registered `SYNC_WERE_STATE_ID` S2C packet to broadcast transformation states to `ClientWereState.java`.

---

## [1.0.0-b026a] - 2026-07-22

### 🐺 Fixed Playtester Reported Issues (GeckoLib Autocomplete, Sound Preview & Preset Parts 3D Render)
- **Recursive GeckoLib Model & File Autocomplete (`RaceRegistry.java`)**:
  - Implemented `scanFilesRecursively` helper method to scan `config/custom_races/models`, `config/custom_races/textures`, and `config/custom_races/animations` recursively.
- **Client GUI Sound Preview Playback Fix (`RaceCreatorScreen.java`)**:
  - Replaced player sound dispatch with `SimpleSoundInstance` (`Attenuation.NONE`) played directly via `Minecraft.getInstance().getSoundManager().play()`.
- **Real-Time Preset Body Parts 3D GUI Rendering (`RaceCreatorScreen.java`)**:
  - Temporarily registers `workingRace` in `RaceRegistry` for the client player UUID during 3D showcase viewport rendering.

---

## [1.0.0-b025a] - 2026-07-22

### 💡 Global Autocomplete Support Across ALL Textboxes Everywhere
- **Universal Text Field Autocomplete (`RaceCreatorScreen.java` & `RaceSelectionScreen.java`)**:
  - Connected **EVERY SINGLE EditBox** across all 10 GUI tabs and search bars to live floating autocomplete suggestion overlays.

---

## [1.0.0-b024a] - 2026-07-22

### 🖼️ 3D Showcase Viewport Head Clipping & Auto-Scale Fix
- **Dynamic Viewport Capping (`RaceCreatorScreen.java` & `RaceSelectionScreen.java`)**:
  - Replaced static scale formula with dynamic height capping (`Math.min(viewH * 0.38f, 32 * totalRaceScale)`).
- **Lowered Pedestal Base Position**:
  - Adjusted `previewY = rightBottom - 18` so entity feet sit squarely on top of the 3D Showcase Pedestal ring.
- **Scissor Region Isolation**:
  - Enclosed entity rendering inside `guiGraphics.enableScissor(rightLeft + 2, topY + 21, rightRight - 2, bottomY - 2)`.

---

## [1.0.0-b023a] - 2026-07-22

### 🔗 Deep Transitive & Multi-Tier Relation Fixes
- **GeckoLib Were-Model Render Clipping Guard (`PlayerRaceLayer.java`)**:
  - Updated `PlayerRaceLayer.java` to check `isWereTransformed` before rendering human-form preset attachments.

---

## [1.0.0-b022a] - 2026-07-22

### 🎨 RPG Visual Theme & Pedestals
- **Holographic 3D Viewport Pedestals**:
  - Visual theme aligned with Custom Mobs Framework / RPG Mounts Framework aesthetic.

---

## [1.0.0-b021a] - 2026-07-22

### 🐺 Were-Form Abilities Section
- **Dedicated Were-Form Abilities Section (`RaceSelectionScreen.java`)**:
  - Displays Were-form stats, conditions, granted passives, and active skills.

---

## [1.0.0-b020a] - 2026-07-22

### 🔄 Were-Form Transitive Ability Evaluation
- **Transitive Ability Evaluation (`PassiveAbilityHandler.java`, `ActiveAbilityHandler.java`)**:
  - Transformed state dynamically evaluates Were-form granted passives and active hotbar skill slots 1-5.

---

## [1.0.0-b019a] - 2026-07-22

### 🐺 Added & Fixed (Playtester Feedback Implementation)
- **Tab Form Persistence Fix (`RaceCreatorScreen.java`)**:
  - Form fields retain values across tab clicks and checkboxes.
- **Were Model/Texture/Animation Directory Auto-Complete Dropdowns**:
  - Suggestions cache for `.geo.json`, `.png`, and `.animation.json`.
- **Expanded Were Transformation Triggers (`WereRaceTransformHandler.java`)**:
  - 7 transformation trigger conditions: `FULL_MOON`, `NEW_MOON`, `NIGHT`, `DAY`, `WATER`, `RAGE`, `KEY`.

---

## [1.0.0-b018a] - 2026-07-22

### 🛠️ Fixed & Audit Enhancements
- **Dynamic Dimension & Biome Registry Scanning (`RaceRegistry.java`)**:
  - Scans `Registries.DIMENSION_TYPE` and `Registries.BIOME`.
- **Custom Mobs Framework Projectiles & Entities**:
  - Scans `config/custom_mobs/projectiles` for custom projectile and minion IDs.

---

## [1.0.0-b017a] - 2026-07-22

### 🛠️ Fixed & Improved
- **Dynamic Multi-Mod Sound Event Auto-Complete**:
  - Queries `BuiltInRegistries.SOUND_EVENT` for all installed mod sound IDs.
- **2048 Character Limit Verification**:
  - `.setMaxLength(2048)` across all text boxes.

---

## [1.0.0-b016a] - 2026-07-22

### 🛠️ Fixed & Audit Verification
- **Active Skill Name Safety (`RaceSelectionScreen.java`)**:
  - Added null/empty string guards when formatting active skill names.

---

## [1.0.0-b015a] - 2026-07-22

### 🛠️ Fixed & Audit Enhancements
- **Tab Switching Form Auto-Save (`RaceCreatorScreen.java`)**:
  - Invokes `readFormInputs()` prior to switching tabs.
- **Dynamic 3D Model Part Offset Translations (`PlayerRaceLayer.java`)**:
  - `PartTransformData` coordinate offsets mapped to `PoseStack` model matrix translations.
- **Responsive Multi-Row Tab Header Wrapping (`RaceCreatorScreen.java`)**:
  - Category tabs wrap onto a second row on narrower GUI scale settings.

---

## [1.0.0-b014a] - 2026-07-22

### 🛠️ Fixed & Restored
- **Positions, Passives, Actives, and Alliances GUI Tabs (`RaceCreatorScreen.java`)**:
  - Widget initializations for Positions, Passives, Actives, and Alliances tabs.

---

## [1.0.0-b013a] - 2026-07-22

### 🛠️ Fixed & Improved
- **Preset Body Parts Model Rendering (`PlayerRaceLayer.java`)**:
  - 3D vertex cuboid rendering for preset body parts (**Ears**, **Horns**, **Halo**, **Wings**, **Tail**).

---

## [1.0.0-b009a] - 2026-07-22

### ⌨️ Added
- **Brigadier Command Autocomplete**:
  - In-game chat suggestions for `/custom_races` commands.
- **Zero-Lag Startup Registry Caching**:
  - `RaceRegistry.rebuildSuggestionsCache()` caches Sounds, Items, Particles, Biomes, Dimensions, and Projectiles on startup.
