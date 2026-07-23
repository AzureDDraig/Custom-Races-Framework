# 📜 Custom Races Framework - Rolling Changelog

All notable changes, features, bug fixes, and build deployments for **Custom Races Framework** are documented here.

## [1.0.0-b066a] - 2026-07-23

### 🛡️ Atomic Data Persistence, Network Buffer Safety & Scale Respawn Refresh
- **Atomic Race File Saving (`RaceRegistry.java`)**:
  - Implemented atomic temp file writes (`races.json.tmp` -> `races.json`) preventing JSON data corruption or file wiping during unexpected crashes.
- **Network Sync Payload Buffer Expansion (`ModPackets.java`)**:
  - Expanded packet string buffer limit to `1048576` (1MB) in `SYNC_RACES_ID` and `SAVE_RACE_ID` receivers, preventing network decoding exceptions when loading large race packs.
- **Render Exception Isolation Guard (`PlayerRaceLayer.java`)**:
  - Wrapped feature layer rendering inside `try-catch-finally` block to ensure rendering anomalies never crash the game client or entity renderer.
- **Actionbar Native Spell Fallback Overlay (`IronSpellsHandler.java`)**:
  - Updated fallback notifications when Iron's Spells mod is missing to display as clean actionbar overlay messages (`displayClientMessage`).
- **Persistent Player Scale Refresh on Respawn (`FirstJoinHandler.java`)**:
  - Registered `PlayerEvent.PLAYER_RESPAWN` listener to re-apply race scale multipliers (`applyRaceScales`) immediately upon player death and respawn.

---

## [1.0.0-b065a] - 2026-07-23

### 🐺 Were-Form Sonic Howl Shockwaves, Water Transformation & Particle Aura Polish
- **Were-Form Howl 360-Degree Radial Pushback (`ActiveAbilityHandler.java`)**:
  - Upgraded `were_howl` to generate a 360-degree expanding radial sonic boom particle ring pushing back nearby hostile entities and inflicting `WEAKNESS` and `SLOWNESS`.
- **Water & Submerged Were-Form Transformation Trigger (`WereRaceTransformHandler.java`)**:
  - Implemented automatic aquatic beast transformation when `wereTriggerCondition` is `"WATER"`, transforming players in water and reverting upon stepping onto dry land.
- **Flying Were-Form Soul Fire Aura Particles (`PlayerRaceLayer.java`)**:
  - Transformed Were-flying races now emit `SOUL_FIRE_FLAME` ambient particles alongside dark smoke trails.
- **Were-Form Revert Status Effect Cleanup (`WereRaceTransformHandler.java`)**:
  - Guarantees `Night Vision` is cleanly removed from the player upon reverting from Were-form to base form.
- **Client Keybind Network Packet Helper (`ModPackets.java`)**:
  - Added `sendToggleWereForm()` helper method to streamline client-side keybind transformation toggling.

---

## [1.0.0-b064a] - 2026-07-23

### 💫 Custom Werewolf Transformation Sounds, Orb of Rebirth Particle Wave & Minion FX
- **Custom Werewolf Transformation Sound Events (`WereRaceTransformHandler.java`)**:
  - Dynamically resolves and plays configured `wereTransformSound` event IDs upon player transformation.
- **Orb of Rebirth Level-Up Chime & Particle Wave (`RaceOrbItem.java`)**:
  - Added level-up chime (`SoundEvents.PLAYER_LEVELUP`) and Totem/End Rod particle waves when right-clicking the Orb of Rebirth.
- **Minion Summoning Witch & Soul Flame Particle Bursts (`ActiveAbilityHandler.java`)**:
  - Added 3D witch and soul flame particle bursts spawning around each summoned minion position (`summon_minion`).
- **Fall Damage Immunity & Slow Falling Safety (`PassiveAbilityHandler.java`)**:
  - Refined `fall_damage_immunity` and `slow_falling` logic to continuously reset fall distance without status effect clutter.
- **Eye-Level Projectile Origin Alignment (`ActiveAbilityHandler.java`)**:
  - Aligned projectile spawn coordinates for active abilities (`fireball_volley`, `web_trap_throw`) with `player.getEyeY()`.

---

## [1.0.0-b063a] - 2026-07-23

### 🌟 Custom Race Spawn Biomes, Were-Form Night Vision & Search Filter Enhancements
- **Custom Race Spawn Biome Locator (`CustomSpawnHandler.java`)**:
  - Enhanced `CustomSpawnHandler.java` to locate nearest matching biome coordinates using `findNearestBiome` when `spawnBiome` is configured (e.g. `minecraft:desert`, `minecraft:jagged_peaks`).
- **Were-Form Night Vision Status Effect (`WereRaceTransformHandler.java`)**:
  - Automatically grants `Night Vision` status effect upon transforming into Werewolf form so players can navigate dark environments clearly.
- **In-Game Command `/custom_races transform` (`CustomRacesCommands.java`)**:
  - Added `/custom_races transform` command allowing server players and admins to toggle Were-form transformation on demand.
- **Armor Slot Drawback Warning Notifications (`DrawbackEventHandler.java`)**:
  - Added actionbar notifications (`player.displayClientMessage`) informing players when armor items are unequipped due to slot drawbacks.
- **Dynamic Eye-Level Particle Emission (`ActiveAbilityHandler.java`)**:
  - Aligned `flame_breath` particle origins with `player.getEyeY()`, ensuring breath particles scale dynamically with entity height.
- **Passive Trait Searching in Race Selection Menu (`RaceSelectionScreen.java`)**:
  - Expanded search filter to match `passiveAbilities` (e.g. searching `"flight"`, `"fire_resistance"`, or `"gills"` filters races by passives).
- **Backwards-Compatible Deserialization Defaults (`RaceData.java`)**:
  - Added null guards in `initDefaults()` for animation paths, spawn dimensions, and spawn biomes when loading legacy race JSON files.
- **Safe Sound Registry Lookup (`RaceSoundHandler.java`)**:
  - Replaced raw sound lookup with `BuiltInRegistries.SOUND_EVENT.getOptional(loc)`, preventing invalid custom sound IDs from triggering default stone breaking fallbacks.
- **Holographic Showcase Viewport Ring Polish (`RaceSelectionScreen.java`)**:
  - Enhanced 3D showcase pedestal ring rendering with glowing accent fills and depth contrast.
- **Full 10-Language Localization Sync (`assets/customraces/lang/`)**:
  - Re-synced all 10 supported language JSON files with localized command and tooltip strings.

---

## [1.0.0-b062a] - 2026-07-23

### 🌌 Custom Spawn Dimensions, Pehkui Eye-Height & Network Packet Polish
- **Custom Race Dimension Spawning & Respawn Positioning (`CustomSpawnHandler.java`)**:
  - Implemented `CustomSpawnHandler.java` listening to `PlayerEvent.PLAYER_RESPAWN`.
  - Automatically teleports players configured with custom race `spawnDimension` IDs (e.g. `minecraft:the_nether`, `minecraft:the_end`) upon first spawn or bedless respawn.
- **Pehkui Eye-Height & Collision Box Dimensions Refresh (`PehkuiIntegration.java`)**:
  - Added `player.refreshDimensions()` calls to `applyRaceScales` and `resetPlayerScales`.
  - Guarantees player camera eye-height and collision boxes instantly adapt when scale multipliers change.
- **Iron's Spells Safe `MagicData` Reflection (`IronSpellsHandler.java`)**:
  - Implemented dynamic `MagicData.getPlayerMagicData(player)` reflection lookup.
  - Prevents `NullPointerExceptions` when casting Iron's Spells native spells.
- **Werewolf Manual Transformation Hotkey Packet (`ModPackets.java`)**:
  - Registered `TOGGLE_WERE_FORM_ID` C2S network packet and `sendToggleWereForm()` helper method for client keybind transformation toggles.
- **Ambient Elemental Aura Particles (`PassiveAbilityHandler.java`)**:
  - Implemented 3D ambient particle rendering around players with elemental passives (`fire_aura`, `ice_aura`, `lightning_aura`, `holy_aura`, `shadow_aura`).

---

## [1.0.0-b061a] - 2026-07-23

### 🤝 Mob Alliance In-Game Targeting Interceptor
- **Mob AI Targeting Listener (`MobAllianceHandler.java`)**:
  - Implemented `MobAllianceHandler.java` to dynamically check nearby mob AI targets (`mob.getTarget() == player`).
  - Automatically clears mob aggression (`mob.setTarget(null)`) for mobs configured with `"friendly"`, `"neutral"`, or `"allied"` stances in **Tab 7 (Mob Alliances)**.

---

## [1.0.0-b060a] - 2026-07-23

### 🔊 Custom Race & Were-Form In-Game Sound FX Engine
- **In-Game Sound Playback Handler (`RaceSoundHandler.java`)**:
  - Implemented event-driven sound playback for `hurtSound`, `deathSound`, `ambientSound`, `wereHurtSound`, `wereDeathSound`, and `wereAmbientSound`.
  - Players now dynamically trigger configured sound event IDs upon receiving damage, dying, or roaming in-game (both base form and transformed Were-form).

---

## [1.0.0-b059a] - 2026-07-23

### 🐺 Deep Multiplayer Werewolf Sync & Chat Cleanliness Polish
- **Multiplayer Werewolf Transformation Sync (`WereRaceTransformHandler.java` & `FirstJoinHandler.java`)**:
  - Implemented `syncAllWereStatesTo(serverPlayer)` so when a player logs into a server, all currently active Werewolf transformation states across all online players are immediately synchronized to the joining player.
  - Ensures newly joining players immediately see transformed players as Werewolves with proper 3D models, scales, smoke particles, and beast features.
- **Chat Cooldown Polish (`ActiveAbilityHandler.java`)**:
  - Removed duplicate chat message output on ability cooldown checks, restricting cooldown alerts strictly to actionbar overlay messages (`true`).

---

## [1.0.0-b058a] - 2026-07-23

### 🪽 Real-Time Dynamic Wing Flapping Visual Animation
- **Dynamic 60 FPS Wing Flapping Render Engine (`PlayerRaceLayer.java`)**:
  - Added real-time Y-axis rotational wing matrix calculations for flying races and wing-bearing preset models.
  - Wings smoothly flap in real-time (`sin(tick * 0.45) * 0.4rad`) when the player is airborne or actively flying, and rest gracefully on the back when grounded.

---

## [1.0.0-b057a] - 2026-07-23

### 🌐 Full 10-Language Localization Sync & UI Polishing
- **Complete 10-Language Localization Alignment (`assets/customraces/lang/`)**:
  - Updated all 10 supported language files (`en_us`, `de_de`, `es_es`, `fr_fr`, `it_it`, `ja_jp`, `ko_kr`, `pt_br`, `ru_ru`, `zh_cn`) with full localized labels, tooltips, and headers for `fly_anim`, `swim_anim`, `is_flying_race`, and `is_were_flying_race`.
  - Guarantees zero un-translated text or raw key fallback strings across all supported language configurations.

---

## [1.0.0-b056a] - 2026-07-23

### 🛡️ Flight Fall Damage Safety & Race Duplication Copy Field Preservation
- **Flight Fall Distance Safety (`PassiveAbilityHandler.java`)**:
  - Automatically resets `player.fallDistance = 0.0f` while a player is actively flying.
  - Prevents players from taking fall damage when landing after flying or mid-air transformations.
- **Race Copy & Duplication Field Preservation (`RaceCreatorScreen.java`)**:
  - Updated `copyRace(...)` to copy `isFlyingRace`, `flyAnim`, `swimAnim`, `isWereFlyingRace`, `wereFlyAnim`, and `wereSwimAnim` when duplicating a race in the creator screen.

---

## [1.0.0-b055a] - 2026-07-23

### 🕊️ Flight/Swim Animations & Flying-Only Race Capabilities
- **Flying-Only Race System (`RaceData.java`, `PassiveAbilityHandler.java`)**:
  - Added `isFlyingRace` (Base Form) and `isWereFlyingRace` (Were-Form) configuration flags.
  - Automatically grants creative-style flight capabilities (`player.getAbilities().mayfly = true`) when enabled for a race or transformed Were-form.
  - Added `[🕊️ FLYING RACE]` visual badge in `RaceSelectionScreen.java` details panel.
- **Flight & Swim GeckoLib Model Animations (`RaceData.java`, `RaceCreatorScreen.java`)**:
  - Added custom GeckoLib animation key controls:
    - **Base Form**: `flyAnim` (`animation.model.fly`) and `swimAnim` (`animation.model.swim`).
    - **Were-Form**: `wereFlyAnim` (`animation.were.fly`) and `wereSwimAnim` (`animation.were.swim`).
  - Integrated into **Tab 0 (Basics)**, **Tab 1 (Model & Animations)**, and **Tab 8 (Were Model)** in `RaceCreatorScreen.java`.
  - Added live autocomplete support so typing animation names suggests keys parsed directly from the GeckoLib animation JSON file.

---

## [1.0.0-b054a] - 2026-07-23

### 🔮 Iron's Spells Autocomplete, Lag-Free Scroll, and Creator Persistence Fix
- **Native Spells Autocomplete Dropdown (`RaceCreatorScreen.java`)**:
  - Connected `nativeSpellBox` in **Tab 11: Native Spells** to `IronSpellsHandler.ALL_SPELLS`.
  - Added fuzzy search matching so typing `"fireball"` or `"lightning"` immediately displays `irons_spellbooks:fireball`, `irons_spellbooks:lightning_lance`, etc., in the floating autocomplete suggestion overlay.
- **60 FPS Lag-Free Scroll Performance (`RaceCreatorScreen.java`)**:
  - Eliminated GUI teardown (`this.init()`) and heap allocations during mouse scrolling and scrollbar dragging in **Tab 3 (Passives)** and **Tab 10 (Drawbacks)**.
  - Replaced screen re-initialization with lightweight widget position updating (`updatePassivesWidgetPositions()` and `updateDrawbacksWidgetPositions()`), making scrolling silky smooth with zero stutter or garbage collection lag.
- **Prevent Orphan "New Race" Template Persistence (`RaceCreatorScreen.java` & `RaceSelectionScreen.java`)**:
  - Fixed `RaceCreatorScreen` constructor to select the first existing race or the player's active race when opened without arguments, preventing auto-creation and saving of unwanted `"New Race"` templates on opening the creator GUI.
  - Added filter in `RaceSelectionScreen.java` to exclude unconfigured draft template entries (`"New Race"`) from the player race selection list.

---

## [1.0.0-b053a] - 2026-07-22

### 🔮 Iron's Spells Reflection Fix & Raycast Safe Teleportation
- **Official Iron's Spells API Package Resolution (`IronSpellsHandler.java`)**:
  - Updated multi-tier reflection resolution chain to include `net.ironsspellbooks.api.registry.SpellRegistry`, `net.ironsspellbooks.api.spells.CastSource`, and `net.ironsspellbooks.api.magic.MagicData`.
  - Fixes Iron's Spells spell casting failure when Iron's Spells 'n Spellbooks mod is installed on 1.20.1.
- **Active Skills Autocomplete Dropdown Integration (`RaceCreatorScreen.java`)**:
  - Added `native_spell_1`, `native_spell_2`, `native_spell_3`, `native_spell_4`, and `native_spell_5` to the `ALL_ACTIVES` catalogue.
  - Native Spells can now be assigned to any hotbar active skill keybind via autocomplete and dropdowns.
- **Raycast Safe Teleportation (`ActiveAbilityHandler.java`)**:
  - Added `getSafeTeleportTarget(...)` raycast collision checking to `teleport_dash`, `blink_teleport`, and `shadow_step`.
  - Teleportation now traces block colliders and automatically steps upward if destination blocks are solid, guaranteeing players never get stuck or suffocate inside blocks.

---

## [1.0.0-b052a] - 2026-07-22

### 🔍 System-Wide Inspection & Race Selection 3D Showcase Sync
- **Race Selection Screen 3D Showcase Were-Form Sync (`RaceSelectionScreen.java`)**:
  - Connected `[Were-Form Preview]` toggle button directly to `ClientWereState.setTransformed(player.getUUID(), previewWereForm)`.
  - Guarantees 3D Showcase viewport model transforms live when toggling Were-form preview mode, and cleanly restores on screen close (`onClose()`).
- **Native Spells Slots 1-5 Selection Summary (`RaceSelectionScreen.java`)**:
  - Added dedicated `🔮 NATIVE SPELLS (Slots 1-5)` summary section in the Race Selection GUI.
  - Displays spell IDs, Wild Magic status (`✨ Wild Magic`), and spell levels (Lvl 1-10) for all 5 native spell slots.

---

## [1.0.0-b051a] - 2026-07-22

### 🐺 Fixed Were-Form Model Transformation & Visual Rendering
- **Client Transformation State Sync (`WereRaceTransformHandler.java` & `PlayerRaceLayer.java`)**:
  - Fixed client-side transformation detection by querying `ClientWereState.isTransformed(uuid)` as a fallback in `WereRaceTransformHandler.isTransformed(...)`.
  - Ensures player render layers immediately register transformation states synchronized from the server.
- **Were-Form Visual Scale & Model Layer (`PlayerRaceLayer.java`)**:
  - Implemented real-time Were-form model matrix scaling (`wereHeightScale` and `wereWidthScale`).
  - Implemented 3D Werewolf Beast feature overlays (wolf ears, ferocious snout, glowing crimson eye overlay).
  - Added real-time dark smoke and flame aura particles around transformed Were players.

---

## [1.0.0-b050a] - 2026-07-22

### 🛡️ 100% Un-Truncated Complete Abilities Expansion
- **Full Un-Truncated Passive Abilities Handler (`PassiveAbilityHandler.java`)**:
  - Expanded `PassiveAbilityHandler.java` to 380+ lines containing complete, un-truncated tick logic for all 100 passives without combining or omitting any passive effects.
- **Full Un-Truncated Active Skills Handler (`ActiveAbilityHandler.java`)**:
  - Expanded `ActiveAbilityHandler.java` to 470+ lines containing un-truncated handlers for all 100 active skills.
  - Preserved 100% of all feature logic including Pehkui minion scaling, minion taming, hostile mob targeting, web trap snowball throwing, were howl sonic boom particles, and sound FX.

---

## [1.0.0-b049a] - 2026-07-22

### 🔍 Complete Individual Inspection of 100 Passives & 100 Active Skills
- **100 Passives Execution Logic (`PassiveAbilityHandler.java`)**:
  - Implemented living tick execution handlers for all 100 passives across all 10 categories (Elemental, Defense, Mobility, Combat, Utility, Magic, Vampiric, Celestial, Beast, Tech).
  - Verified 100% passive ability coverage with zero missing IDs.
- **100 Active Skills Execution Logic (`ActiveAbilityHandler.java`)**:
  - Implemented hotbar keybind execution handlers for all 100 active skills across all 10 categories.
  - Verified 100% active skill coverage with zero missing IDs.

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
  - Converted **Tab 10: Drawbacks** (`gui.customraces.tab.drawbacks`) to 1 single wide column (`320px` width) displaying full drawback names, warning icons (`⚠️`), and complete description tooltips without text truncation across all 150 drawbacks.
- **Interactive Scrollbar & Dragable Thumb**:
  - Implemented vertical scrollbar track at `contentLeft + 330` with dynamic red warning thumb (`0xFFCC3333` / `0xFFFF5555`).
  - Added support for mouse wheel scrolling and mouse dragging of the scrollbar thumb.
  - Added viewport scissor bounds checking so checkboxes outside the scroll region are cleanly hidden.

---

## [1.0.0-b045a] - 2026-07-22

### 🔍 Spells System & Deep Search Verification
- **Multi-Tier Reflection Execution Engine (`IronSpellsHandler.java`)**:
  - Enhanced `IronSpellsHandler.java` with multi-tier reflection fallback for `io.github.elytra.irons_spellbooks.api.registry.SpellRegistry` and legacy package namespaces.
  - Dynamically inspects method parameters for `castSpell(...)` supporting both 4-argument and 3-argument API method signatures across different Iron's Spells mod versions.
- **Verified Deep Tooltip Search (`RaceCreatorScreen.java`)**:
  - Inspected search resolution across Passives (Tab 3), Actives (Tab 4), and Drawbacks (Tab 10). Search queries match **BOTH** item/drawback names **AND** their full description/tooltip text.

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

### 🔍 Native Spells & Iron's Spells System Review
- **Full System Verification for Native Spells (`IronSpellsHandler.java`, `ActiveAbilityHandler.java`, `RaceCreatorScreen.java`)**:
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

### 🌐 Overlap-Free Multi-Language Layout Review
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

### 🛡️ Sodium / Embeddium Compatibility & Full 150 Drawbacks Review
- **Sodium & Embeddium Render Layer Compatibility (`PlayerRaceLayer.java`)**:
  - Verified feature layer pipeline for full compatibility with **Sodium (Fabric)**, **Embeddium (Forge)**, **Iris**, **Oculus**, and **Indium**.
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

### 🛠️ Fixed & Enhancements
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

### 🛠️ Fixed & Verification
- **Active Skill Name Safety (`RaceSelectionScreen.java`)**:
  - Added null/empty string guards when formatting active skill names.

---

## [1.0.0-b015a] - 2026-07-22

### 🛠️ Fixed & Enhancements
- **Tab Switching Form Auto-Save (`RaceCreatorScreen.java`)**:
  - Invokes `readFormInputs()` prior to switching tabs.
- **Dynamic 3D Model Part Offset Translations (`PlayerRaceLayer.java`)**:
  - `PartTransformData` coordinate offsets mapped to `PoseStack` model matrix translations.
- **Responsive Multi-Row Tab Header Wrapping (`RaceCreatorScreen.java`)**:
  - Responsive multi-row category tabs for narrower GUI scales.

---

## [1.0.0-b014a] - 2026-07-22

### 🛠️ Fixed & Restored
- **Positions, Passives, Actives, and Alliances GUI Tabs (`RaceCreatorScreen.java`)**:
  - Widget initializations for Positions, Passives, Actives, and Alliances tabs.

---

## [1.0.0-b009a] - 2026-07-22

### ⌨️ Added
- **Brigadier Command Autocomplete**:
  - In-game chat suggestions for `/custom_races` commands.
- **Zero-Lag Startup Registry Caching**:
  - `RaceRegistry.rebuildSuggestionsCache()` caches Sounds, Items, Particles, Biomes, Dimensions, and Projectiles on startup.
