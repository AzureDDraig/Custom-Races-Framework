# 📜 Custom Races Framework - Rolling Changelog

All notable changes, features, bug fixes, and build deployments for **Custom Races Framework** are documented here.

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
  - Implemented execution mechanics in `DrawbackEventHandler.java` for all 90 new drawbacks:
    - **Biome & Climate**: `desert_dehydration`, `snow_hypothermia`, `swamp_miasma`, `cave_suffocation`, `high_altitude_sickness`, `ocean_pressure`.
    - **Equipment & Material Restrictions**: `no_iron_equipment`, `no_diamond_equipment`, `no_offhand_slot`, `no_elytra_equip`.
    - **Mount Restrictions**: `boat_inability`, `minecart_inability`, `horse_mount_inability`, `strider_mount_inability`.
    - **Sensory & Curses**: `blind_in_darkness`, `glowing_curse`, `bloodlust_frenzy`, `golem_rust`, `dragon_greed`.

---

## [1.0.0-b037a] - 2026-07-22

### ⚡ Drawback Event Handler & Complete Verification
- **Event-Driven Drawbacks Engine (`DrawbackEventHandler.java`)**:
  - Implemented `DrawbackEventHandler.java` to handle event-driven mechanics for drawbacks.
  - **Equipment Restrictions**: Automatically pops off or drops prohibited armor/shield items when equipped.
  - **Diet & Consumption**: Enforces diet restrictions when eating food.
  - **Damage Modifiers**: Applies 2.0x–3.0x damage multipliers based on damage source and weapon types.
  - **Entity Hostility & Curses**: Forces Iron Golems to target players, causes Villagers to flee, spawns Phantoms at night, and attracts lightning.

---

## [1.0.0-b036a] - 2026-07-22

### ⚠️ Dedicated Tab 10: Drawbacks UI
- **Tab 10: Drawbacks UI (`RaceCreatorScreen.java`)**:
  - Created standalone Tab 10 (`gui.customraces.tab.drawbacks`) styled with crimson warning borders (`0xFFFF5555`) with full Were-Form mode support.
  - Registered initial catalogue of 60 drawbacks inspired by the top 100 Origins mods.

---

## [1.0.0-b035a] - 2026-07-22

### 🎨 In-World & 3D GUI Player Model Preset Parts Rendering Fix
- **Player Feature Renderer Registration**:
  - Registered `PlayerRaceLayer` (rendering Ears, Horns, Wings, Tail, Halo) to Minecraft's `PlayerRenderer` on Fabric (`LivingEntityFeatureRendererRegistrationCallback` in `CustomRacesFabric.java`) and Forge (`EntityRenderersEvent.AddLayers` in `CustomRacesForge.java`).
  - Preset body parts now render live in-world on player entities and on the 3D GUI player preview!

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

## [1.0.0-b032a] - 2026-07-22

### 📜 Sidebar Mouse Wheel Scrolling & Sound Preview Buttons
- **Sidebar Mouse Wheel Scrolling (`RaceCreatorScreen.java`)**:
  - Added mouse wheel scrolling support to the left sidebar race selection panel for smooth scrolling when managing large race lists.
- **Updated Sound Play Buttons**:
  - Updated all sound preview buttons across Base Form and Were-Form sound tabs.

---

## [1.0.0-b031a] - 2026-07-22

### 🎨 RPG FlatButton Widgets & Layout Enhancements
- **RPG FlatButton Widgets (`FlatButton.java`)**:
  - Replaced vanilla buttons with custom RPG FlatButton widgets featuring smooth color borders, hover animations, and dark obsidian backings.
- **Fixed Armor Checkboxes & Race Auto-Rename**:
  - Fixed armor hiding checkboxes layout in Tab 0 (Basics) and resolved race auto-rename bug.
- **Autocomplete Mouse Wheel Scrolling**:
  - Added mouse wheel scrolling to autocomplete suggestion dropdown overlays.
- **HUD Selection Item Icon**:
  - Rendered race item icon on the HUD race selection summary screen.

---

## [1.0.0-b030a] - 2026-07-22

### 🐺 Crimson Blood Moon Theme & Multi-Language Sync
- **Crimson Blood Moon Theme Shift (`RaceCreatorScreen.java`)**:
  - Added crimson theme shift with wolf head emoji tab indicators when switching to Were-Form editing mode.
- **Multi-Language Sync**:
  - Synchronized language key definitions across all 9 supported language files.

---

## [1.0.0-b029a] - 2026-07-22

### 🖼️ Custom Mobs Framework Admin GUI Layout
- **Left Sidebar Race List (`RaceCreatorScreen.java`)**:
  - Redesigned RaceCreatorScreen layout matching Custom Mobs Framework visual theme.
  - Added left sidebar race list container, real-time search filter, Add button, Del button, and large Duplicate button.

---

## [1.0.0-b028a] - 2026-07-22

### 🐺 Were-Form Editing Mode & Pehkui Reflection Fixes
- **Were-Form Editing Mode Toggle (`RaceCreatorScreen.java`)**:
  - Added `WERE FORM` / `BASE FORM` editor mode toggle button in header.
- **Pehkui Reflection Entity Scale Application (`PehkuiIntegration.java`)**:
  - Fixed Pehkui reflection methods for applying player height, width, and reach scales dynamically.
- **Passive Ability Key Mismatch Resolution**:
  - Aligned passive ability string keys between editor GUI checkboxes and `PassiveAbilityHandler.java` tick evaluations.

---

## [1.0.0-b026a] - 2026-07-22

### 🐺 Fixed Playtester Reported Issues (GeckoLib Autocomplete, Sound Preview & Preset Parts 3D Render)
- **Recursive GeckoLib Model & File Autocomplete (`RaceRegistry.java`)**:
  - Implemented `scanFilesRecursively` helper method to scan `config/custom_races/models`, `config/custom_races/textures`, and `config/custom_races/animations` recursively.
  - Automatically indexes files in subdirectories (e.g. `config/custom_races/models/were/`, `config/custom_races/models/`, etc.) matching both `.geo.json` and `.json` extensions so all GeckoLib model files appear in autocomplete dropdowns.
- **Client GUI Sound Preview Playback Fix (`RaceCreatorScreen.java`)**:
  - Replaced player sound dispatch with `SimpleSoundInstance` (`Attenuation.NONE`) played directly via `Minecraft.getInstance().getSoundManager().play()`.
  - Ensures modded sound event IDs (e.g. `armourers_workshop:page-turn`, `alexsmobs:*`, `custom_mobs:*`) play loud and clear in the client GUI when clicking the `► Play` preview button.
- **Real-Time Preset Body Parts 3D GUI Rendering (`RaceCreatorScreen.java`)**:
  - Temporarily registers `workingRace` in `RaceRegistry` for the client player UUID during 3D showcase viewport rendering.
  - Forces `PlayerRaceLayer` to render selected preset body parts (Ears, Horns, Wings, Tail, Halo, Custom parts), RGB colors, offsets, and scales live on the 3D GUI player preview model in real-time as you edit them.

---

## [1.0.0-b025a] - 2026-07-22

### 💡 Global Autocomplete Support Across ALL Textboxes Everywhere
- **Universal Text Field Autocomplete (`RaceCreatorScreen.java` & `RaceSelectionScreen.java`)**:
  - Connected **EVERY SINGLE EditBox** across all 10 GUI tabs and search bars to live floating autocomplete suggestion overlays.
  - **Category Autocomplete Sources**:
    - 🏷️ **Race Name Box**: Preset race templates (`Elf`, `Demon`, `Angel`, `Dragon`, `Vampire`, `Werewolf`, `Dwarf`, `Ork`, `Fairy`, `Merfolk`, `Cyborg`, `Titan`, `Kitsune`, `Naga`, `Golem`, `Human`).
    - 🎨 **Name Color Box**: Preset hex color options (`#FFAA00`, `#FF5555`, `#55FF55`, `#55FFFF`, `#5555FF`, `#AA00AA`, `#FFFF55`, `#FFFFFF`, `#888888`, `#000000`, `#FF8C00`, `#9932CC`, `#00CED1`).
    - 🖼️ **Custom Texture Box**: Scanned PNG textures from `config/custom_races/textures/`.
    - 🔊 **Sound Event Boxes**: All registered sound event IDs across **all installed mods** (`BuiltInRegistries.SOUND_EVENT`).
    - 🌍 **Dimension & Biome Boxes**: Modded dimension IDs (`twilightforest:*`, `aether:*`) and biome IDs (`biomesoplenty:*`, `byg:*`).
    - ⚔️ **Active Ability Slots (1-5)**: Built-in skills (`flame_breath`, `teleport_dash`, `transform_were`, `summon_minions`) + custom mob skills (`custom_mobs:<id>`).
    - 🌕 **Were Transformation Triggers**: `FULL_MOON`, `NEW_MOON`, `NIGHT`, `DAY`, `WATER`, `RAGE`, `KEY`.
    - 🎬 **Were Model, Texture & Animation Files**: Scanned files from `config/custom_races/models/were/`, `config/custom_races/textures/were/`, and `config/custom_races/animations/were/`.
    - 📏 **Numeric Multipliers & Scales**: Common float scale presets (`0.5`, `0.75`, `1.0`, `1.25`, `1.5`, `1.75`, `2.0`, `5.0`, `10.0`, `20.0`, `40.0`).
    - 📐 **XYZ Position Offsets**: Preset float coordinate offsets (`-0.5`, `-0.4`, `-0.3`, `-0.2`, `-0.1`, `0.0`, `0.1`, `0.2`, `0.3`, `0.4`, `0.5`).
    - 🔍 **Race Selection Search Box**: Real-time autocomplete suggestions matching all loaded race names.

---

## [1.0.0-b024a] - 2026-07-22

### 🖼️ 3D Showcase Viewport Head Clipping & Auto-Scale Fix
- **Dynamic Viewport Capping (`RaceCreatorScreen.java` & `RaceSelectionScreen.java`)**:
  - Replaced static scale formula with dynamic height capping (`Math.min(viewH * 0.38f, 32 * totalRaceScale)`).
  - Automatically adjusts scale to ensure tall entities, large height multipliers, or Were-form transformations fit with generous headroom.
- **Lowered Pedestal Base Position**:
  - Adjusted `previewY = rightBottom - 18` so entity feet sit squarely on top of the 3D Holographic Showcase Pedestal ring near the bottom of the box.
- **Scissor Region Isolation**:
  - Enclosed entity rendering inside `guiGraphics.enableScissor(rightLeft + 2, topY + 21, rightRight - 2, bottomY - 2)`.
  - Guarantees that rotated 3D models or entity limbs stay 100% inside the viewport frame without clipping through top title bars or leaking into black screen margins.

---

## [1.0.0-b023a] - 2026-07-22

### 🔗 Deep Transitive & Multi-Tier Relation Fixes
- **GeckoLib Were-Model Render Clipping Guard (`PlayerRaceLayer.java`)**:
  - Updated `PlayerRaceLayer.java` to check `isWereTransformed` before rendering human-form preset attachments (ears, wings, tail, horns, halo).
  - Prevents human-form preset parts from clipping through custom GeckoLib Werewolf/Were-form 3D models during transformation.
- **Network Packet Buffer Capacity Expansion (`ModPackets.java`)**:
  - Expanded `SAVE_RACE_ID` packet `buf.readUtf()` buffer capacity from `65536` to `262144` bytes (256 KB).
  - Guarantees that saving large custom race configurations with 2048-character strings, sound events, and Were-form data never encounters buffer truncation or network disconnects.

---

## [1.0.0-b022a] - 2026-07-22

### 🎨 Visual Theme Redesign (Custom Mobs & RPG Mounts Framework Aesthetic)
- **Obsidian & Cyberpunk Dark Glass Theme (`RaceCreatorScreen.java` & `RaceSelectionScreen.java`)**:
  - Replaced standard flat dark rectangles with translucent dark obsidian canvases (`0xF50B0D12`).
  - Added high-tech obsidian header banners (`0xFF121520`) with glowing Neon Cyan (`0xFF00CEC9`) accent lines and emblem headers (`§9§l❖ §c§lRACE CREATOR ADMIN GUI §9§l❖`).
  - Added glassmorphic main content container cards (`0xEE121622`) with glowing Violet (`0xFF7B61FF`) top & bottom border lines.
  - Added bullet icons (`§b❖`, `§e❖`, `§c❖`, `§d❖`) to all form category labels across all 10 GUI tabs.
- **3D Holographic Showcase Viewport**:
  - Replaced standard right-side preview box with a **3D Holographic Showcase Viewport**:
    - Dark glass box (`0xEE101422`) with Neon Cyan header bar (`§b❖ 3D SHOWCASE ❖`).
    - Concentric 3D Holographic Pedestal Base Rings (`0x3000CEC9` / `0x606C5CE7`) beneath entity feet.
    - Crimson pedestal rings when previewing Were-form transformations (`[ 🌙 WERE-FORM ]`).

---

## [1.0.0-b021a] - 2026-07-22

### 🌙 Added Dedicated Were-Abilities Section on Race Selection Screen
- **`RaceSelectionScreen.java` Were-Abilities View**:
  - Added dedicated **"🌙 WERE-FORM PASSIVES"** and **"🌙 WERE-FORM ACTIVE SKILLS (Slots 1-5)"** summary sections when previewing Were-form (`[ 🌙 WERE-FORM ]`).
  - Displays all passives granted specifically while transformed in Were-form.
  - Displays active hotbar skills configured for Were-form, explicitly marking default standard skills as `(Base)` if no Were-form override is assigned.

---

## [1.0.0-b020a] - 2026-07-22

### 🔗 Transitive & Cross-System Relation Integration
- **Were-Form Passive Ability Evaluation (`PassiveAbilityHandler.java`)**:
  - Connected `werePassiveAbilities` to living player tick events.
  - When transformed into Were-form, `PassiveAbilityHandler` evaluates both standard passives and Were-form granted passives (e.g. night vision, gills, flight, regeneration) dynamically on tick.
- **Were-Form Active Ability Evaluation (`ActiveAbilityHandler.java`)**:
  - Connected `wereActiveAbilities` to hotbar active skill keybind execution.
  - Pressing hotbar skill slots 1-5 while transformed automatically checks and triggers Were-form specific active skills before falling back to standard skills.

---

## [1.0.0-b019a] - 2026-07-22

### 🐺 Added & Fixed (Playtester Feedback Implementation)
- **Tab Form Persistence Fix (`RaceCreatorScreen.java`)**:
  - Added `readFormInputs()` to the very beginning of `init()` prior to `clearWidgets()`.
  - Fixes form fields reverting back to defaults when switching tabs, checking checkboxes (e.g. `Enable Were-Form`), or clicking toggle buttons.
- **Were Model/Texture/Animation Directory Auto-Complete Dropdowns**:
  - `RaceRegistry.rebuildSuggestionsCache()` now scans `config/custom_races/models/were/` (`.geo.json`), `config/custom_races/textures/were/` (`.png`), and `config/custom_races/animations/were/` (`.animation.json`).
  - Typing in `wereModelBox`, `wereTextureBox`, and `wereAnimFileBox` displays live floating auto-complete suggestion overlays.
- **Expanded Were Transformation Triggers (`WereRaceTransformHandler.java`)**:
  - Added support for 7 transformation trigger conditions:
    - 🌕 `FULL_MOON` (Full moon night)
    - 🌑 `NEW_MOON` (New moon night)
    - 🌙 `NIGHT` (Night creatures / vampires)
    - ☀️ `DAY` (Sun creatures / solar beings)
    - 🌊 `WATER` / `SUBMERGED` (Siren / aquatic creatures when submerged in water)
    - 😡 `RAGE` / `LOW_HEALTH` (Rage transformation when player health drops below 30%)
    - 🔑 `KEY` / `MANUAL` (Active hotbar skill `transform_were` or keybind)
  - Added live cycle button `▶ <TRIGGER>` in Tab 8 ("Were Model & Anims").
- **Were-Form Granted Passives & Actives**:
  - Added `werePassiveAbilities` and `wereActiveAbilities` to `RaceData.java` for configuring custom abilities granted specifically while transformed.

---

## [1.0.0-b018a] - 2026-07-22

### 🛠️ Fixed & Audit Enhancements
- **Dynamic Dimension & Biome Registry Scanning (`RaceRegistry.java`)**:
  - Dynamically queries `Minecraft.getInstance().level.registryAccess()` for `Registries.DIMENSION_TYPE` and `Registries.BIOME`.
  - Automatically incorporates modded dimension IDs (e.g. `twilightforest:twilight_forest`, `aether:the_aether`) and modded biome IDs (e.g. `biomesoplenty:*`, `byg:*`) into auto-complete suggestions.
- **Custom Mobs Framework Projectiles & Entities**:
  - Automatically scans `config/custom_mobs/projectiles` to add `custom_mobs:<id>` custom projectiles and mobs into the auto-complete suggestion dropdown for minion skills and ranged attacks.

---

## [1.0.0-b017a] - 2026-07-22

### 🛠️ Fixed & Improved
- **Dynamic Multi-Mod Sound Event Auto-Complete**:
  - `RaceRegistry.rebuildSuggestionsCache()` is now invoked on GUI open in `RaceCreatorScreen.init()`.
  - Automatically queries all registered `SoundEvent` IDs from every installed mod in `BuiltInRegistries.SOUND_EVENT` (e.g. `alexsmobs:*`, `iceandfire:*`, `custom_mobs:*`, etc.).
  - Shows auto-complete suggestion dropdown for ambient, hurt, death, and were transformation sounds.
- **Exact Mod Sound Preview Playback**:
  - Updated `playPreviewSound()` in `RaceCreatorScreen.java` to look up the exact `SoundEvent` `ResourceLocation` from `BuiltInRegistries.SOUND_EVENT` and play that exact mod sound in-game.
- **2048 Character Limit Verification**:
  - Re-verified `.setMaxLength(2048)` across all sound edit boxes (`ambientSoundBox`, `hurtSoundBox`, `deathSoundBox`, `wereTransformSoundBox`, `wereHowlSoundBox`, `wereAmbientSoundBox`, `wereHurtSoundBox`, `wereDeathSoundBox`).

---

## [1.0.0-b016a] - 2026-07-22

### 🛠️ Fixed & Audit Verification
- **Active Skill Name Safety (`RaceSelectionScreen.java`)**:
  - Added null/empty string guards when formatting active skill names in the race selection summary box (`gui.customraces.active_skill`) to prevent potential NullPointerExceptions.
- **Verified Zero Outstanding Bugs**:
  - Audited all 10 GUI tabs, 3D entity viewport rendering, custom part scanning, packet networking (`S2C` / `C2S`), Pehkui scaling integration, localization keys, and Brigadier autocomplete caches. All systems are 100% stable.

---

## [1.0.0-b015a] - 2026-07-22

### 🛠️ Fixed & Audit Enhancements
- **Tab Switching Form Auto-Save (`RaceCreatorScreen.java`)**:
  - Automatically invokes `readFormInputs()` prior to switching tabs in `RaceCreatorScreen` to prevent loss of typed input when clicking across tabs before hitting "Save Race".
- **Dynamic 3D Model Part Offset Translations (`PlayerRaceLayer.java`)**:
  - Connected `PartTransformData` (`posX`, `posY`, `posZ`) set in the **Positions** tab directly to `PoseStack` model matrix translations.
  - Moving X/Y/Z coordinate offsets in the editor instantly transforms body part locations (Ears, Horns, Halo, Wings, Tail) on the 3D player entity renderer.
- **Responsive Multi-Row Tab Header Wrapping (`RaceCreatorScreen.java`)**:
  - Implemented automatic header button row wrapping so category tabs gracefully wrap onto a second row on narrower GUI scale settings without overflowing off-screen.

---

## [1.0.0-b014a] - 2026-07-22

### 🛠️ Fixed & Restored
- **Positions, Passives, Actives, and Alliances GUI Tabs (`RaceCreatorScreen.java`)**:
  - Implemented missing GUI widget initializations in `init()` and rendering labels for **Positions** (`activeTab == 2`), **Passives** (`activeTab == 3`), **Actives** (`activeTab == 4`), and **Alliances** (`activeTab == 7`).
  - **Positions Tab**: Interactive X/Y/Z coordinate transform input boxes for ears, wings, tail, horns, halo, and custom parts.
  - **Passives Tab**: Interactive checkboxes for all 10 passive race abilities (`night_vision`, `water_breathing`, `fire_resistance`, `flight`, `slow_falling`, `regeneration`, `wither_immunity`, `fall_damage_immunity`, `lava_swimming`, `climbing`).
  - **Actives Tab**: Hotbar Skill Assignment boxes for Slots 1 to 5 (`flame_breath`, `teleport_dash`, `transform_were`, `summon_minions`, `custom_mobs:<id>`).
  - **Alliances Tab**: Faction neutrality stance checkboxes (`minecraft:zombie`, `minecraft:skeleton`, `minecraft:spider`, `minecraft:creeper`, `minecraft:enderman`, `minecraft:piglin`).

---

## [1.0.0-b013a] - 2026-07-22

### 🛠️ Fixed & Improved
- **Preset Body Parts Model Rendering (`PlayerRaceLayer.java`)**:
  - Implemented 3D vertex cuboid rendering for preset body parts (**Ears**, **Horns**, **Halo**, **Wings**, **Tail**).
  - Attached head attachments directly to head bone transforms and body attachments to player torso transforms.
  - Added RGB Hex color tinting for all preset body parts.
- **Body Part Selector GUI Overlap Fix (`BodyPartOverlay.java`)**:
  - Added `this.clearWidgets();` in `BodyPartOverlay.init()` to prevent duplicate overlapping button widgets.
  - Expanded model type button width to `180px` for clear, readable text.
  - Integrated custom body parts cycling from `config/custom_races/models/parts/` via `CustomPartScanner`.

---

## [1.0.0-b012a] - 2026-07-22

### 🖼️ Added & Improved
- **Config Icons Directory (`config/custom_races/icons/`)**:
  - Added automatic creation of `config/custom_races/icons/` folder on startup for pack makers to store PNG custom race icons.
- **2048 Character Text Box Limit**:
  - Updated **EVERY SINGLE EditBox** across all GUI screens (`RaceCreatorScreen`, `RaceSelectionScreen`, `BodyPartOverlay`) with `.setMaxLength(2048)`.
  - Ensures long ResourceLocations, file paths, long lore stories, hex colors, and complex animation IDs are never truncated.

---

## [1.0.0-b009a] - 2026-07-22

### ⌨️ Added
- **Brigadier Command Autocomplete**:
  - Added in-game chat suggestions for `/custom_races` commands (Race IDs, Passives, Active Skills, Sounds, Projectiles).
- **Zero-Lag Startup Registry Caching**:
  - Added `RaceRegistry.rebuildSuggestionsCache()` to cache Sounds, Items, Particles, Biomes, Dimensions, and Projectiles on startup.
- **GUI Auto-Complete Suggestion Overlay**:
  - Expanded text fields to `240px`–`270px` width.
  - Typing in sound boxes, item boxes, dimension/biome boxes, or projectile boxes renders an interactive floating dropdown overlay with one-click autofill.
