# Original User Request

## Initial Request — 2026-07-23T19:03:48Z

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

## Follow-up — 2026-07-24T18:50:02Z

# Teamwork Project — Custom Races Framework Full Implementation

Working directory: c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework
Integrity mode: development

## Requirements

### R1. Were-Form Model & Texture Rendering Fix
- Ensure default_werewolf.png dark fur texture asset exists in assets/customraces/textures/were/default_werewolf.png.
- Refine WereModelRenderer.java texture location resolution:
  - Support "skin" and "player" keywords in wereTexturePath to bind player skin textures directly.
  - Parse relative texture file strings cleanly.
  - Fall back safely to player.getSkinTextureLocation() if custom texture is unresolvable, preventing missing purple/black checkerboard textures.

### R2. VIP / Permission-Locked Races (permissionLock)
- Implement permissionLock checking in RaceRegistry.java and RaceData.java.
- If a race has a non-empty permissionLock string, evaluate if player possesses permission node before allowing selection.
- Render "🔒 VIP / LOCKED" badge in RaceSelectionScreen.java with tooltip "§cRequires Permission: §e" + permissionLock. Disable select button for locked races.

### R3. Configurable First-Join Selection GUI Toggle (autoOpenSelectionOnJoin)
- Implement autoOpenSelectionOnJoin (boolean, default: true) in RaceRegistry.java and config JSON.
- Check setting in FirstJoinHandler.java on player join: automatically open RaceSelectionScreen only if enabled.

### R4. Dynamic Body Part Model Preset Audit & Verification
- Audit PlayerRaceLayer.java, CustomRaceModelRenderer.java, and PartTransformData.java for all 6 body part presets (ears, horns, tail, wings, halo, extra legs).
- Verify position, rotation, color tint, and scale transforms render dynamically and cleanly per race without matrix stack leakage or visual corruption.

## Acceptance Criteria

### Verification & Build Integrity
- [ ] ./gradlew build -x test builds cleanly with 0 errors across Fabric and Forge.
- [ ] Were-form transformation renders clean dark werewolf texture without purple/black missing texture grid.
- [ ] Permission-locked VIP races render lock badge & disabled selection button for unauthorized players.
- [ ] autoOpenSelectionOnJoin configuration option functions as intended on first join.
- [ ] All 6 body part attachments apply dynamically per race definition.

