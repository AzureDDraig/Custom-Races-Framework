# BRIEFING — 2026-07-23T19:14:35Z

## Mission
Implement Configurable Ambient Particle Count Settings for human and were forms (`particleCount` and `wereParticleCount`) across Data Model, GUI, and Particle Emission logic.

## 🔒 My Identity
- Archetype: implementer, qa, specialist
- Roles: implementer, qa, specialist
- Working directory: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_worker_m3
- Original parent: b28d3adc-2ae5-4650-a72a-7258580882b0
- Milestone: Milestone 3 - Configurable Ambient Particle Count Settings

## 🔒 Key Constraints
- CODE_ONLY network mode: no external HTTP/downloads.
- BACKUP FOLDER READ-ONLY: never write to BACKUP directory.
- Minimal change principle: no unrelated refactoring.
- Genuine implementation: no hardcoding or dummy implementations.

## Current Parent
- Conversation ID: b28d3adc-2ae5-4650-a72a-7258580882b0
- Updated: 2026-07-23T19:14:35Z

## Task Summary
- **What to build**: Add `particleCount` (default: 5) and `wereParticleCount` (default: 10) to `RaceData.java`, NBT, Codec/Gson, Packet, getters/setters. Add GUI controls in `RaceCreatorScreen.java`. Dynamic scaling of particle emission in `PlayerRaceLayer.java` / `ParticleAuraData.java`.
- **Success criteria**: Gradle build succeeds for both Fabric and Forge targets; particle count fields fully serialized, editable in GUI, and scaling active during rendering/particle tick.
- **Interface contracts**: PROJECT.md & Explorer 3 Analysis Report

## Change Tracker
- **Files modified**:
  - `common/src/main/java/ddraig/net/customraces/data/RaceData.java`: Added `particleCount` and `wereParticleCount` fields, fallback defaults in `initDefaults()`, getters/setters, and `toNBT`/`fromNBT` serialization methods.
  - `common/src/main/java/ddraig/net/customraces/data/ParticleAuraData.java`: Added `getScaledParticleCount(int raceParticleCount)` for dynamic aura particle emission scaling.
  - `common/src/main/java/ddraig/net/customraces/client/gui/RaceCreatorScreen.java`: Added `particleCountBox` and `wereParticleCountBox` widgets, field resets, tab 1 widget binding, input reading, race duplicating, and text label rendering.
  - `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`: Scaled Were-form ambient smoke/flame particle spawning loops and particle aura emission by `effectiveParticleCount`.
  - `common/src/test/java/ddraig/net/customraces/client/render/M3ParticleConfigVerificationTest.java`: Added empirical test suite verifying defaults, getters/setters, NBT cycle, aura scaling logic, and edge cases.
- **Build status**: PASS (Fabric and Forge targets build successfully with zero errors).
- **Pending issues**: None

## Quality Status
- **Build/test result**: PASS (`./gradlew build -x test` succeeded).
- **Lint status**: Clean
- **Tests added/modified**: `M3ParticleConfigVerificationTest.java` added.

## Loaded Skills
- None

## Key Decisions Made
- `RaceData` particle counts default to 5 (human) and 10 (Were form).
- Fallback logic in `initDefaults()` ensures zero or negative values fall back to defaults for legacy JSON payloads.
- `ParticleAuraData` calculates emission density relative to base count 5.
- Were-form ambient smoke/flame loops scale dynamically with `wereParticleCount`.

## Artifact Index
- `.agents/teamwork_preview_worker_m3/ORIGINAL_REQUEST.md` — Original request record
- `.agents/teamwork_preview_worker_m3/BRIEFING.md` — Agent briefing & state
- `.agents/teamwork_preview_worker_m3/handoff.md` — Handoff report
