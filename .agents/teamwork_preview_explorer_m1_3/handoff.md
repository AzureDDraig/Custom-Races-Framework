# Handoff Report: Particle Configuration & GUI Explorer

## 1. Observation
- **`RaceData.java` (`common/src/main/java/ddraig/net/customraces/data/RaceData.java`)**:
  - Contains fields for pehkui scales, base stats, armor hiding, body parts, particle auras (`List<ParticleAuraData>`), and Were-race settings (lines 11-120).
  - Currently missing explicit fields for `particleCount` and `wereParticleCount`.
  - Serializes via Gson (`RaceRegistry.java` lines 301-337) to `config/custom_races/races.json`.
  - `ModPackets.java` (lines 154-180) transmits serialized `RaceData` via `GSON.toJson(race)` over `SYNC_RACES_ID` (S2C) and `SAVE_RACE_ID` (C2S).

- **`RaceCreatorScreen.java` (`common/src/main/java/ddraig/net/customraces/client/gui/RaceCreatorScreen.java`)**:
  - Manages GUI edit controls (lines 36-86), form resetting (`resetFormFields()`, lines 360-371), auto-saving (`autoSaveWorkingRace()`, lines 353-358), tab switching (lines 487-523), widget rendering (`init()`, lines 379-1224), form parsing (`readFormInputs()`, lines 1225-1308), and race duplication (`duplicateRace()`, lines 1335-1400).

- **`PlayerRaceLayer.java` (`common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`) & `ParticleAuraData.java` (`common/src/main/java/ddraig/net/customraces/data/ParticleAuraData.java`)**:
  - Spawns Were-form smoke and flame particles on client-side when `player.tickCount % 3 == 0` (lines 52-67).
  - Spawns `ParticleAuraData` particles when `player.tickCount % 4 == 0` (lines 74-89).
  - `ParticleAuraData.java` contains `particleType`, `count`, `speed`, and `spread`.

- **`CHANGELOG.md` (`CHANGELOG.md`) & Gradle Build Setup**:
  - Rolling changelog uses `## [1.0.0-bXXXX] - YYYY-MM-DD` header format with detailed category sections (`### Title`).
  - `build.gradle` compiles multi-module project via `./gradlew build -x test`.

## 2. Logic Chain
1. **Adding Particle Count Fields**:
   Adding `public int particleCount = 5;` and `public int wereParticleCount = 10;` to `RaceData.java` automatically integrates into Gson serialization (`races.json`) and network sync (`ModPackets.java`) without needing custom byte buffer encoders.
2. **GUI Input Binding**:
   Adding `particleCountBox` and `wereParticleCountBox` to `RaceCreatorScreen.java` inside `init()`, `resetFormFields()`, `readFormInputs()`, and `duplicateRace()` connects user UI controls directly to race data fields.
3. **Dynamic Emission Rate Scaling**:
   In `PlayerRaceLayer.java`, calculating `effectiveParticleCount = isWereTransformed ? (race.wereParticleCount > 0 ? race.wereParticleCount : 10) : (race.particleCount > 0 ? race.particleCount : 5)` and iterating loops `for (int i = 0; i < loops; i++)` scales ambient smoke and particle aura density dynamically based on configured particle counts.
4. **Verification & Changelog**:
   Updating `CHANGELOG.md` without removing past entries preserves version history, and running `./gradlew build -x test` verifies cross-platform Fabric and Forge compilation.

## 3. Caveats
- No source code files outside of `.agents/teamwork_preview_explorer_m1_3/` were modified during this read-only investigation.
- Default fallback values (5 for base, 10 for Were-form) must be enforced in `RaceData.initDefaults()` so legacy JSON files without particle count keys deserialize smoothly.

## 4. Conclusion
The implementation plan for particle count configuration and GUI integration is fully mapped, verified, and ready for worker implementation in subsequent milestones.

## 5. Verification Method
- Codebase inspection verified via `view_file` and `grep_search`.
- Build verification command for implementation phase: `./gradlew build -x test`.
