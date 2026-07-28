# Execution Plan — Custom Race GeckoLib Player Model Overhaul

## Objective
Implement full custom player model rendering using GeckoLib for transformed races in Custom Races Framework, with base human player model suppression when transformed and robust fallback guardrails ensuring zero player invisibility. Support smooth keyframe animations, hurt flash overlays, dynamic textures, and particle aura effects. Verify multi-platform build (`./gradlew build -x test`).

## Milestones

### Milestone 1: Exploration & Architecture Analysis
- **Goal**: Analyze current GeckoLib integration (`WereModelRenderer`, `PlayerRaceLayer`, `CustomRaceModelRenderer`, `RaceData`, state syncing), asset loading paths, player model rendering pipelines on Fabric/Forge, and suppression/fallback mechanisms.
- **Workers**: 3 Explorers in parallel.
- **Deliverables**: Comprehensive exploration reports in `.agents/teamwork_preview_explorer_m1_gecko_*`.

### Milestone 2: GeckoLib Model Override & Asset Resolution (R1)
- **Goal**: Support dual path asset resolution (`config/custom_races/models/`, `textures/`, `animations/` vs `assets/customraces/`). Implement precise 3D model rendering aligned to player entity feet and yaw/pitch rotation.
- **Workers**: Worker -> 2 Reviewers -> 2 Challengers -> Forensic Auditor.

### Milestone 3: Base Human Player Model Suppression Guardrails (R2)
- **Goal**: Implement suppression of standard human player cuboid mesh parts (`head`, `body`, `arms`, `legs`, clothing overlays) when transformed with a valid custom GeckoLib model. Implement graceful fallback to standard player model + procedural features if GeckoLib model fails to load or is unassigned (NO player invisibility).
- **Workers**: Worker -> 2 Reviewers -> 2 Challengers -> Forensic Auditor.

### Milestone 4: Dynamic Animations, Combat Effects & Multi-Platform Build Verification (R3 & Acceptance)
- **Goal**: Implement GeckoLib animation state controller (idle, walk, attack, hurt), hurt red flash overlay rendering, dynamic skin texture overrides, and particle aura emission integration. Verify `./gradlew build -x test` builds cleanly with 0 errors across Fabric and Forge targets.
- **Workers**: Worker -> 2 Reviewers -> 2 Challengers -> Forensic Auditor.

## Integrity & Verification Gate
- Every implementation milestone requires passing:
  1. `./gradlew build -x test` clean build across Fabric and Forge.
  2. Peer Code Reviews (2 Reviewers, PASS).
  3. Empirical & Stress Testing (2 Challengers, PASS).
  4. Forensic Integrity Audit (`teamwork_preview_auditor`, CLEAN).
