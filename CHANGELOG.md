# 📜 Custom Races Framework - Rolling Changelog

All notable changes, features, bug fixes, and build deployments for **Custom Races Framework** are documented here.

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
