# Project: Were-Race Model Transformation Fixes & Configurable Particle Count

## Architecture
- Common module: Core race data models (`RaceData.java`), transformation handlers, particle emission logic, net serialization/codecs.
- Client module: Render layers (`PlayerRaceLayer`, `WereModelRenderer`, `CustomRaceModelRenderer`), GUI screens (`RaceCreatorScreen`), GeckoLib render integration, client state tracking (`ClientWereState`).
- Cross-platform networking: Client-to-server and server-to-client tracking packets for transformation state and race configuration sync.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Exploration & Architecture Analysis | Search and analyze transformation state sync, model render layers, fallback logic, scale refresh, particle fields, and GUI screen | none | DONE |
| 2 | M2: Were-Race Custom Model Transformation Rendering Fixes | Implement tracking client sync, model layer swap logic, fallback model handling, Pehkui dimension refresh | M1 | IN_PROGRESS |
| 3 | M3: Configurable Ambient Particle Count Settings | Add particle count fields to `RaceData`, GUI controls in `RaceCreatorScreen`, dynamic particle emission in `PlayerRaceLayer` | M1 | PLANNED |
| 4 | M4: Rolling Changelog & Multi-Platform Build Verification | Update `CHANGELOG.md`, verify multi-platform build with `./gradlew build -x test` | M2, M3 | PLANNED |

## Interface Contracts
### Transformation Sync Contract
- `WereRaceTransformHandler` / `ClientWereState`: Client state must be updated for all tracking players when transformation occurs.
- Render layers inspect `ClientWereState.isTransformed(player)` / `isWereForm` to render `WereModelRenderer` / `CustomRaceModelRenderer` instead of player model.
- Dimension refresh: `player.refreshDimensions()` invoked upon transform toggle & Pehkui scale application.

### Particle Configuration Contract
- `RaceData`: `particleCount` (int, default 5), `wereParticleCount` (int, default 10). Serialized in NBT, JSON/Codec, and Network Packets.
- `PlayerRaceLayer`: Spawns ambient particles scaled by `particleCount` (or `wereParticleCount` when transformed).
- `RaceCreatorScreen`: Provides GUI input fields/sliders for modifying particle counts when creating/editing races.

## Code Layout
- `common/src/main/java/com/customraces/...` or fabric/forge shared directories:
  - `data/RaceData.java`
  - `client/render/PlayerRaceLayer.java`
  - `client/render/WereModelRenderer.java`
  - `client/render/CustomRaceModelRenderer.java`
  - `client/gui/RaceCreatorScreen.java`
  - `client/state/ClientWereState.java`
  - `handler/WereRaceTransformHandler.java`
