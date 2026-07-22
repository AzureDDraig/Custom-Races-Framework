# 📜 Custom Races Framework - Rolling Changelog

All notable changes, features, bug fixes, and build deployments for **Custom Races Framework** are documented here.

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
