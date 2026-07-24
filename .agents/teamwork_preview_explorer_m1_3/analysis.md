# Analysis Report: Particle Configuration & GUI Explorer

## Executive Summary
This report provides a comprehensive, read-only architectural investigation into adding customizable ambient particle count settings (`particleCount` default 5, `wereParticleCount` default 10) to the **Custom Races Framework**. It maps data structure serialization in `RaceData.java`, GUI input fields and binding in `RaceCreatorScreen.java`, dynamic particle emission scaling in `PlayerRaceLayer.java` and `ParticleAuraData`, changelog formatting in `CHANGELOG.md`, and multi-platform build verification across Fabric and Forge.

---

## 1. `RaceData.java` Inspection & Serialization Analysis

### Exact File Location
`common/src/main/java/ddraig/net/customraces/data/RaceData.java`

### Field Additions & Defaults
To support configurable ambient particle counts per race across human base form and transformed Were-form, `RaceData` requires two new integer fields:
```java
// Particle Count Configurations
public int particleCount = 5;      // Base form ambient particle emission rate (Default: 5)
public int wereParticleCount = 10;  // Were-form ambient particle emission rate (Default: 10)
```

### Constructor & Fallback Initialization (`initDefaults`)
- **Lines 244-252**: Constructors `public RaceData()` and `public RaceData(String id, String name)` call `initDefaults()`.
- **Lines 254-279**: In `public void initDefaults()`, backward-compatibility guards must be added to handle legacy JSON deserialization where missing fields evaluate to `0`:
  ```java
  if (particleCount <= 0) particleCount = 5;
  if (wereParticleCount <= 0) wereParticleCount = 10;
  ```

### Serialization & Network Packet Architecture
- **NBT / Mojang Codecs**: The project does **not** rely on NBT tags or Mojang `Codec` objects for race configuration storage.
- **Gson JSON Storage (`RaceRegistry.java`)**: Race configurations are saved to `config/custom_races/races.json` via GSON (`GSON.fromJson` / `GSON.toJson`). Because Gson reflects public class fields automatically, adding `particleCount` and `wereParticleCount` to `RaceData` auto-persists them to JSON disk files.
- **Network Sync (`ModPackets.java`)**:
  - `sendSaveRace(RaceData race)` (lines 176-180): Serializes `RaceData` using `GSON.toJson(race)` and sends packet `SAVE_RACE_ID` (C2S).
  - `syncRacesToAll(MinecraftServer server)` (lines 154-174): Serializes `RaceRegistry.loadedRaces` map into `racesJson` and sends packet `SYNC_RACES_ID` (S2C) to all connected clients.
  - No custom network buffer codec changes are required since Gson handles the extra integer properties natively within the JSON payload.

---

## 2. `RaceCreatorScreen.java` GUI Controls & Data Binding Analysis

### Exact File Location
`common/src/main/java/ddraig/net/customraces/client/gui/RaceCreatorScreen.java`

### GUI Field Declarations
- **Lines 49-53 / 75-81**: Add `EditBox` field declarations:
  ```java
  private EditBox particleCountBox;
  private EditBox wereParticleCountBox;
  ```

### GUI Lifecycle Method Binding Map

1. **Field Reset (`resetFormFields()`, lines 360-371)**:
   Reset references to prevent memory leaks or stale state:
   ```java
   particleCountBox = null;
   wereParticleCountBox = null;
   ```

2. **Widget Initialization (`init()`, lines 616-698)**:
   - **Were-Form Mode (`editingWereForm && workingRace.enableWereRace`)**:
     Initialize `wereParticleCountBox` under Tab 1 (Model & Animations) at line offset ~155:
     ```java
     this.wereParticleCountBox = new EditBox(this.font, contentLeft + 140, contentTop + 155, 60, 18, Component.literal("Were Particle Count"));
     this.wereParticleCountBox.setMaxLength(2048);
     this.wereParticleCountBox.setValue(String.valueOf(workingRace.wereParticleCount));
     this.wereParticleCountBox.setTooltip(Tooltip.create(Component.literal("Ambient particle count per tick emission rate in Were-form (Default: 10).")));
     this.addRenderableWidget(this.wereParticleCountBox);
     ```
   - **Base Form Mode (`!editingWereForm`)**:
     Initialize `particleCountBox` under Tab 1 (Model & Animations) at line offset ~180:
     ```java
     this.particleCountBox = new EditBox(this.font, contentLeft + 140, contentTop + 180, 60, 18, Component.literal("Particle Count"));
     this.particleCountBox.setMaxLength(2048);
     this.particleCountBox.setValue(String.valueOf(workingRace.particleCount));
     this.particleCountBox.setTooltip(Tooltip.create(Component.literal("Ambient particle count per tick emission rate in base form (Default: 5).")));
     this.addRenderableWidget(this.particleCountBox);
     ```

3. **Form Input Reading (`readFormInputs()`, lines 1225-1308)**:
   Read integer values from textboxes during saves or tab changes:
   ```java
   if (editingWereForm) {
       if (wereParticleCountBox != null) {
           try { workingRace.wereParticleCount = Integer.parseInt(wereParticleCountBox.getValue()); } catch (Exception ignored) {}
       }
   } else {
       if (particleCountBox != null) {
           try { workingRace.particleCount = Integer.parseInt(particleCountBox.getValue()); } catch (Exception ignored) {}
       }
   }
   ```

4. **Race Duplication (`duplicateRace()`, lines 1335-1400)**:
   Copy particle counts when cloning a race template:
   ```java
   copy.particleCount = source.particleCount;
   copy.wereParticleCount = source.wereParticleCount;
   ```

---

## 3. Dynamic Particle Emission Scaling Analysis (`PlayerRaceLayer.java` & `ParticleAuraData`)

### Exact File Locations
- `common/src/main/java/ddraig/net/customraces/data/ParticleAuraData.java`
- `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`

### Current Implementation State
- `ParticleAuraData.java` defines individual aura properties: `particleType` (String), `count` (float), `speed` (float), `spread` (float).
- `PlayerRaceLayer.java`:
  - **Lines 52-67**: Hardcodes `player.tickCount % 3 == 0` spawning 1 `LARGE_SMOKE` and 1 `FLAME` / `SOUL_FIRE_FLAME` particle when transformed in Were-form.
  - **Lines 74-89**: Hardcodes `player.tickCount % 4 == 0` spawning 1 particle per `ParticleAuraData` layer regardless of configured race particle density.

### Proposed Dynamic Scaling Mechanics
1. **Determine Active Particle Density**:
   ```java
   int effectiveParticleCount = isWereTransformed ?
       (race.wereParticleCount > 0 ? race.wereParticleCount : 10) :
       (race.particleCount > 0 ? race.particleCount : 5);
   ```

2. **Dynamic Were-Form Ambient Smoke/Flame Spawning**:
   Replace the single particle spawn inside `player.tickCount % 3 == 0` with a scaled emission loop:
   ```java
   int smokeLoops = Math.max(1, effectiveParticleCount / 2);
   for (int i = 0; i < smokeLoops; i++) {
       player.level().addParticle(
               net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
               player.getRandomX(0.6), player.getRandomY(), player.getRandomZ(0.6),
               0.0, 0.05, 0.0
       );
       player.level().addParticle(
               race.isWereFlyingRace ? net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME : net.minecraft.core.particles.ParticleTypes.FLAME,
               player.getRandomX(0.4), player.getRandomY(), player.getRandomZ(0.4),
               0.0, 0.02, 0.0
       );
   }
   ```

3. **Dynamic `ParticleAuraData` Scaling**:
   Scale the number of particles spawned per aura by `effectiveParticleCount`:
   ```java
   int auraParticlesToSpawn = Math.max(1, Math.round(aura.count * (effectiveParticleCount / 5.0f)));
   for (int i = 0; i < auraParticlesToSpawn; i++) {
       player.level().addParticle(
               pOptions,
               player.getRandomX(aura.spread),
               player.getRandomY() + 0.5,
               player.getRandomZ(aura.spread),
               0.0, aura.speed, 0.0
       );
   }
   ```

---

## 4. Multi-Platform Build Verification & Changelog Formatting

### CHANGELOG.md Structure
- **Location**: `CHANGELOG.md`
- **Format Standard**:
  - Version block: `## [1.0.0-bXXXX] - YYYY-MM-DD`
  - Category headers: `### 🔮 Category Title`
  - Detailed nested bullet points referencing specific class names and line changes.
- **Rule**: Rolling log must append new release entries at the top of the file without deleting historical entries.

### Multi-Platform Gradle Build Setup
- **Root `build.gradle`**: Configures Architectury Loom for multi-module subprojects (`common`, `fabric`, `forge`).
- **Verification Command**:
  ```bash
  ./gradlew build -x test
  ```
- **Execution Target**: Generates cross-platform mod JAR artifacts under `fabric/build/libs/` and `forge/build/libs/` with zero compilation errors.
