# Execution Plan — Were-Race Model Transformation Fixes & Configurable Particle Count

## Overview
This plan coordinates the execution of model transformation fixes, particle count configurability, changelog updates, and multi-platform build verification across Fabric and Forge targets.

## Milestones & Execution Strategy

### Milestone 1: Exploration & Architecture Analysis
- **Goal**: Perform comprehensive code search across Fabric, Forge, and Common modules to map transformation state sync, model rendering layers, Pehkui dimension refresh triggers, particle count fields, and GUI screen components.
- **Workers**: 3 Explorers in parallel.
  - Explorer 1: Were-Race transformation state sync & networking (`ClientWereState`, `WereRaceTransformHandler`, packet handlers).
  - Explorer 2: Model render layers (`PlayerRaceLayer`, `WereModelRenderer`, `CustomRaceModelRenderer`, GeckoLib integration, `wereModelId` fallback).
  - Explorer 3: Pehkui scale bounding box refresh, `RaceData` particle fields (`particleCount`, `wereParticleCount`), and `RaceCreatorScreen` GUI integration.

### Milestone 2: Were-Race Model Transformation Fixes
- **Goal**: Implement client-side tracking sync, model layer swap logic, fallback model handling, and Pehkui dimension refresh on transformation.
- **Workers**: Worker -> 2 Reviewers + 2 Challengers + 1 Forensic Auditor.

### Milestone 3: Configurable Ambient Particle Count Settings
- **Goal**: Implement particle count fields in `RaceData.java` (with codecs & net packets), GUI controls in `RaceCreatorScreen`, and particle scaling in `PlayerRaceLayer.java`.
- **Workers**: Worker -> 2 Reviewers + 2 Challengers + 1 Forensic Auditor.

### Milestone 4: Rolling Changelog & Build Verification
- **Goal**: Update `CHANGELOG.md` without removing existing entries and verify multi-platform build (`./gradlew build -x test`).
- **Workers**: Worker -> 1 Reviewer + 1 Forensic Auditor.
