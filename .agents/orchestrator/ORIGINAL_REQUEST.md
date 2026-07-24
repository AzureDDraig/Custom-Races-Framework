# Original User Request

## 2026-07-23T19:03:54Z

# Teamwork Project Prompt — Were-Race Model Transformation Fixes & Configurable Particle Count

Working directory: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework
Integrity mode: development

## Requirements

### R1. Were-Race Custom Model Transformation Rendering Fixes
Analyze and implement fixes for every potential reason why a transformed Were-race player retains their default player model instead of rendering their defined custom Were model:
- Ensure client-side transformation state (`ClientWereState` / `WereRaceTransformHandler.isTransformed`) correctly syncs to all tracking clients.
- Verify GeckoLib and custom model render layers (`PlayerRaceLayer`, `WereModelRenderer`, `CustomRaceModelRenderer`) check `isWereForm` and properly swap/override the player entity model rendering.
- Fix any fallback logic where `wereModelId` is null, empty, or unmapped, ensuring it defaults gracefully to a valid model or custom GeckoLib asset.
- Ensure Pehkui height/width scale updates (`wereHeightScale`, `wereWidthScale`) re-trigger bounding box refresh (`player.refreshDimensions()`) on transformation state changes.

### R2. Configurable Ambient Particle Count Settings
Add customizable particle count fields to `RaceData.java`, `RaceCreatorScreen` (GUI fields/sliders), and rendering layers (`PlayerRaceLayer` / `ParticleAuraData`):
- Add `particleCount` (default: 5) and `wereParticleCount` (default: 10) fields to `RaceData.java`.
- Connect particle spawning logic in `PlayerRaceLayer.java` to scale emission rates dynamically based on the race's configured particle count.
- Add input/slider controls in `RaceCreatorScreen` for particle count configuration.

### R3. Rolling Changelog & Multi-Platform Build Verification
- Update `CHANGELOG.md` with detailed release notes without removing any existing changelog entries.
- Verify full multi-platform compilation with `./gradlew build -x test`.

## Acceptance Criteria

### Execution & Verification
- [ ] `./gradlew build -x test` completes with 0 errors across Fabric and Forge targets.
- [ ] Were-form transformation successfully swaps player rendering from default model to custom defined Were-form model.
- [ ] Ambient particle count is fully configurable per-race and properly controls particle density in-game.
- [ ] Rolling changelog in `CHANGELOG.md` is preserved and updated with new additions.
