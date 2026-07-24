# Execution Plan — Custom Races Framework Full Implementation

## Overview
This plan coordinates the implementation of Were-Form Model & Texture Rendering Fixes (R1), VIP/Permission-Locked Races (R2), Configurable First-Join Selection GUI Toggle (R3), and Dynamic Body Part Model Preset Audit & Verification (R4), concluding with Multi-Platform Build Verification across Fabric and Forge targets.

## Milestones & Execution Strategy

### Milestone 1: Exploration & Architecture Analysis
- **Goal**: Perform comprehensive code search across Fabric, Forge, and Common modules to map texture location resolution, asset paths, permission checking, GUI components, first-join config toggles, and body part presets matrix stack handling.
- **Workers**: 3 Explorers in parallel.
  - Explorer 1: Were-Form Texture & Rendering Fixes (R1) — `WereModelRenderer.java`, `wereTexturePath` parsing, `"skin"`/`"player"` keywords, fallback logic, asset path check for `default_werewolf.png`.
  - Explorer 2: Permission Locks (R2) & First-Join Toggle (R3) — `RaceRegistry.java`, `RaceData.java`, `RaceSelectionScreen.java`, `FirstJoinHandler.java`, config JSON structure.
  - Explorer 3: Dynamic Body Part Model Presets Audit (R4) — `PlayerRaceLayer.java`, `CustomRaceModelRenderer.java`, `PartTransformData.java`, 6 body part presets (ears, horns, tail, wings, halo, extra legs), matrix stack isolation, tinting/scale transforms.

### Milestone 2: Were-Form Model & Texture Rendering Fix (R1)
- **Goal**: Ensure `default_werewolf.png` dark fur texture asset exists, refine `WereModelRenderer.java` to support `"skin"` and `"player"` keywords, parse relative texture file paths, and fall back safely to `player.getSkinTextureLocation()`.
- **Workers**: Worker -> 2 Reviewers + 2 Challengers + 1 Forensic Auditor.

### Milestone 3: VIP Permission Lock & First-Join GUI Toggle (R2 & R3)
- **Goal**: Implement `permissionLock` field and checking in `RaceRegistry.java` & `RaceData.java`, badge/tooltip/disabled button rendering in `RaceSelectionScreen.java`, and `autoOpenSelectionOnJoin` config setting checked by `FirstJoinHandler.java`.
- **Workers**: Worker -> 2 Reviewers + 2 Challengers + 1 Forensic Auditor.

### Milestone 4: Dynamic Body Part Model Preset Audit & Build Verification (R4)
- **Goal**: Audit & verify dynamic rendering of all 6 body part presets (ears, horns, tail, wings, halo, extra legs) without matrix stack leakage or visual corruption, and verify clean compilation via `./gradlew build -x test`.
- **Workers**: Worker -> 2 Reviewers + 2 Challengers + 1 Forensic Auditor.
