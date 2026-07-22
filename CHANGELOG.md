# 📜 Custom Races Framework - Rolling Changelog

All notable changes, features, bug fixes, and build deployments for **Custom Races Framework** are documented here.

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

## [1.0.0-b011a] - 2026-07-22

### 🔮 Added
- **Summon Minion(s) Active Ability (`summon_minions`)**:
  - Added configurable minion active skill supporting vanilla mobs (`minecraft:zombie`, `minecraft:skeleton`, etc.) and **Custom Mobs Framework** entities (`custom_mobs:<mob_id>`).
  - Configurable **Minion Mob Type** (`minionMobType`), **Minion Count** (`minionCount`, 1 to 10), **Minion Size Scale** (`minionScale`, Pehkui integration), **Combat Mode** (`minionIsRanged`), and **Minion Projectile ID** (`minionProjectile`).
  - Circular spawn formation around player with magic sound effects (`SoundEvents.EVOKER_PREPARE_SUMMON`) and purple/smoke particles (`POOF` & `WITCH`).
  - Auto-tames tamable mobs to player and targets nearby hostile entities.
  - Added Minion controls to Admin Creator GUI **Tab 6 ("Advanced")** with live auto-complete suggestion overlays.

---

## [1.0.0-b010a] - 2026-07-22

### 🌐 Added
- **Full 9-Language Localization & Translation Key System**:
  - Converted all GUI text strings, headers, buttons, tooltips, form labels, tabs, and chat messages to `Component.translatable(...)` keys.
  - Added translation files matching Custom Mobs Framework:
    - 🇺🇸 `en_us.json` (English - US)
    - 🇩🇪 `de_de.json` (German)
    - 🇪🇸 `es_es.json` (Spanish)
    - 🇫🇷 `fr_fr.json` (French)
    - 🇯🇵 `ja_jp.json` (Japanese)
    - 🇰🇷 `ko_kr.json` (Korean)
    - 🇧🇷 `pt_br.json` (Portuguese - Brazil)
    - 🇷🇺 `ru_ru.json` (Russian)
    - 🇨🇳 `zh_cn.json` (Chinese - Simplified)

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

---

## [1.0.0-b008a] - 2026-07-22

### 💄 Improved
- **Dynamic Were-Race Tab Visibility**:
  - **"Were Model"** (Tab 8) and **"Were Sounds"** (Tab 9) are now hidden by default on standard races to prevent GUI clutter.
  - Checking `Enable Were-Form` in the **"Basics"** tab dynamically unlocks and displays the Were-race tabs.

---

## [1.0.0-b007a] - 2026-07-22

### 🌙 Added
- **Race Selection Screen Were-Form Preview Toggle**:
  - Added interactive `[ 🌙 WERE-FORM ]` / `[ 👤 NORMAL FORM ]` toggle button on the Race Selection GUI for races with `enableWereRace` enabled.
  - Real-time 3D player entity model rescaling (`wereHeightScale * baseScale`) and Were-form stats / moon phase trigger condition display.

---

## [1.0.0-b006a] - 2026-07-22

### 🐺 Added
- **Were-Race Transformation System**:
  - Support for Moon Phase triggers (`FULL_MOON`, `NEW_MOON`, `NIGHT`, `MANUAL`) with custom GeckoLib models, textures, animations, stats, and sound events.
  - Expanded Admin Creator GUI to 10 tabs ("Were Model & Anims", "Were Sounds").
- **Custom Mobs Framework Projectiles**:
  - Integrated projectile shooting for `custom_mobs:<projectile_id>` active skills.

---

## [1.0.0-b005a] - 2026-07-22

### 🛠️ Fixed & Improved
- **GUI & Network Fixes**:
  - Connected `/custom_races admin editor` to `OPEN_CREATOR_ID` S2C packet.
  - Fixed search box scissor region overlap on Race Selection Screen.
  - Updated Pehkui scale application (`setScale` + `setTargetScale`) for instant entity scaling without relogging.
  - Added PNG picture path field (`customTexture`) and RGB Hex color picker field (`nameColor`) to `RaceData.java`.
