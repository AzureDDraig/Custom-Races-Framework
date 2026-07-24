# Project: Custom Races Framework Full Implementation

## Architecture
- Common module: Core race definitions (`RaceData.java`), registry & config (`RaceRegistry.java`), first-join event handler (`FirstJoinHandler.java`), body part transforms (`PartTransformData.java`).
- Client module: Render layers (`PlayerRaceLayer.java`, `WereModelRenderer.java`, `CustomRaceModelRenderer.java`), GUI screens (`RaceSelectionScreen.java`, `RaceCreatorScreen.java`), custom model/texture resolution.
- Permissions & Config: `permissionLock` checking in `RaceRegistry`/`RaceData`, `autoOpenSelectionOnJoin` toggle in config JSON.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Exploration & Architecture Analysis | Investigate R1 (texture resolution & assets), R2/R3 (permission lock & auto-open config), R4 (body part presets & matrix stack hygiene) | none | DONE |
| 2 | M2: Were-Form Texture & Rendering Fixes (R1) | Ensure `default_werewolf.png` asset, `"skin"`/`"player"` keywords, clean relative path parsing, fallback to `player.getSkinTextureLocation()` | M1 | DONE |
| 3 | M3: VIP Permission Lock & First-Join Selection Toggle (R2 & R3) | Implement `permissionLock` in `RaceRegistry`/`RaceData`, GUI lock badge/tooltip/disabled button, `autoOpenSelectionOnJoin` config & `FirstJoinHandler` | M1 | DONE |
| 4 | M4: Dynamic Body Part Model Preset Audit & Build Verification (R4) | Audit & verify 6 body part presets (ears, horns, tail, wings, halo, extra legs) for dynamic transforms, matrix isolation, and verify `./gradlew build -x test` | M2, M3 | DONE |

## Interface Contracts
### Were-Form Texture Resolution Contract (R1)
- `wereTexturePath` in `WereModelRenderer`:
  - `null`, empty, `"skin"`, or `"player"` -> resolves to `player.getSkinTextureLocation()`.
  - Relative file paths (e.g. `"textures/were/custom.png"`) -> parsed cleanly into `ResourceLocation("customraces", path)`.
  - Missing asset fallback -> defaults to `player.getSkinTextureLocation()` or `default_werewolf.png` (`assets/customraces/textures/were/default_werewolf.png`).

### Permission Lock & Selection GUI Contract (R2 & R3)
- `RaceData` / `RaceRegistry`:
  - `permissionLock` (String): If non-empty, evaluated via permission API / LuckPerms / player check.
  - `autoOpenSelectionOnJoin` (boolean, default true): Config option controlling `FirstJoinHandler`.
- `RaceSelectionScreen`:
  - Displays `"🔒 VIP / LOCKED"` badge and tooltip `"§cRequires Permission: §e" + permissionLock` for locked races.
  - Select button disabled when locked.

### Body Part Model Preset Contract (R4)
- `PlayerRaceLayer` / `CustomRaceModelRenderer` / `PartTransformData`:
  - 6 Presets: ears, horns, tail, wings, halo, extra legs.
  - Matrix stack push/pop around each part transform to avoid leakage.
  - Position, rotation, tint, and scale applied dynamically per race definition.

## Code Layout
- `common/src/main/java/ddraig/net/customraces/...` or shared directories:
  - `data/RaceData.java`
  - `registry/RaceRegistry.java`
  - `handler/FirstJoinHandler.java`
  - `client/render/PlayerRaceLayer.java`
  - `client/render/WereModelRenderer.java`
  - `client/render/CustomRaceModelRenderer.java`
  - `client/render/part/PartTransformData.java`
  - `client/gui/RaceSelectionScreen.java`
  - `common/src/main/resources/assets/customraces/textures/were/default_werewolf.png`
