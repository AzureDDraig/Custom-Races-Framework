# Project: Custom Race GeckoLib Player Model Overhaul

## Architecture
- Common module: Core race definitions (`RaceData.java`), registry & config (`RaceRegistry.java`), transformation state (`ClientWereState.java`, `WereRaceTransformHandler.java`).
- Client module: Render layers (`PlayerRaceLayer.java`, `WereModelRenderer.java`, `CustomRaceModelRenderer.java`), GeckoLib asset resolution (`GeckoAssetResolver.java`), GeckoLib entity/model renderer (`GeckoLibWereRenderer.java`).
- Fallback & Guardrails: Model suppression guardrails for base player cuboid mesh (`head`, `hat`, `body`, `arms`, `legs`, `jacket`, `sleeves`, `pants`, `cloak`, `ear`) in `LivingEntityRendererMixin` and `WereModelRenderer`; fail-safe fallback to base model + procedural features on asset load failure/missing model to prevent player invisibility. Invisibility effect handling (`player.isInvisible()`).
- Animations & Combat Effects: GeckoLib keyframe controller (idle, walk, attack, hurt, fly, swim), red hurt tint overlay, dynamic skin texture overrides, and particle aura effects with 20 Hz tick guards.

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | M1: Exploration & Architecture Analysis | Analyze GeckoLib player model override, dual asset loading (disk config vs resource pack), base player model suppression, and animation/combat effect hooks | none | DONE |
| 2 | M2: GeckoLib Model Override & Dual Asset Resolution (R1) | Implement `GeckoAssetResolver` (dual path loading for disk & resource packs), `netHeadYaw`/`headPitch` head bone rotations in `GeckoLibWereRenderer`, and Pehkui double-scaling fix in `PlayerRaceLayer` | M1 | DONE |
| 3 | M3: Base Human Player Model Suppression Guardrails (R2) | Extend `setBaseModelVisible()` for `cloak` & `ear`, verify model integrity before suppression, implement fail-safe fallback to base model + procedural features, and handle player invisibility/spectator status | M1, M2 | DONE |
| 4 | M4: Dynamic Animations, Combat Effects & Multi-Platform Build Verification (R3) | Implement keyframe animation controller (idle/walk/attack/hurt/fly/swim), red hurt flash, dynamic skin texture overrides, 20 Hz particle aura, and multi-platform build (`./gradlew build -x test`) | M2, M3 | DONE |

## Interface Contracts
### GeckoLib Asset Resolution & Rendering Contract (R1)
- `GeckoAssetResolver` supports dual path loading and path normalization:
  - Disk config paths: `config/custom_races/models/`, `config/custom_races/textures/`, `config/custom_races/animations/`
  - Mod resource pack paths: `assets/customraces/geo/`, `assets/customraces/textures/`, `assets/customraces/animations/`
- Render GeckoLib model at full scale aligned to entity feet and player rotation yaw/pitch. Pass `netHeadYaw` and `headPitch` to head bones (`head`, `bipedHead`, `head_bone`, `headbone`).
- Pehkui scale coordination: avoid double-scaling by checking `!PehkuiIntegration.isPehkuiLoaded()` before applying layer scale.

### Base Player Model Suppression & Fallback Guardrails Contract (R2)
- Mesh Suppression: When player is transformed AND valid custom GeckoLib model exists, suppress default player model parts (`head`, `hat`, `body`, `right_arm`, `left_arm`, `right_leg`, `left_leg`, clothing overlays `jacket`, `right_sleeve`, `left_sleeve`, `right_pants`, `left_pants`, `cloak`, `ear`).
- Fail-Safe Fallback: If GeckoLib model is missing, invalid, or fails to parse/load, DO NOT suppress base model. Render base player model + procedural features (ears/tail/snout) so players are NEVER invisible.
- Status Effects: Check `player.isInvisible()` to render translucency / translucent buffer or suppress model appropriately during potion invisibility.

### Dynamic Animations & Combat Effects Contract (R3)
- Keyframe Animation Controller: Map player state (`speed`, `swingTime`, `hurtTime`, `isVisuallySwimming`, `flying`) to GeckoLib animation triggers (`wereIdleAnim`, `wereWalkAnim`, `wereAttackAnim`, `wereFlyAnim`, `wereSwimAnim`).
- Combat Visuals: Apply hurt red flash overlay during damage tick, dynamic skin texture binding when configured, and scale particle aura emission rate with 20 Hz tick guards during transformed state.

## Code Layout
- `common/src/main/java/ddraig/net/customraces/...`:
  - `data/RaceData.java`
  - `registry/RaceRegistry.java`
  - `handler/WereRaceTransformHandler.java`
  - `client/render/PlayerRaceLayer.java`
  - `client/render/WereModelRenderer.java`
  - `client/render/GeckoLibWereRenderer.java`
  - `client/render/GeckoAssetResolver.java`
  - `mixin/LivingEntityRendererMixin.java`
