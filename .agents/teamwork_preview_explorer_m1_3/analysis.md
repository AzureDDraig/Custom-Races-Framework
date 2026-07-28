# Architectural Analysis Report: Dynamic Transformations, Keyframe Animations & Combat Effects (R3)

**Author**: Explorer 3 (M1)  
**Target Project**: Custom Race GeckoLib Player Model Overhaul (`Custom Races Framework`)  
**Working Directory**: `c:\Users\Ddraig__\Downloads\MODS_CREATION\Custom Races Framework\.agents\teamwork_preview_explorer_m1_3`  
**Date**: 2026-07-28  

---

## 1. Executive Summary

This investigation delivers a comprehensive technical analysis of **Focus Area R3: Dynamic Transformations, Keyframe Animations & Combat Visual Effects** for the Custom Races Framework.

### Primary Discoveries & Architectural Gaps
1. **GeckoLib Model Animation Execution Gap**: `GeckoLibWereRenderer.java` dynamically bakes GeckoLib `.geo.json` models (`bakeModelFromFile`) and `.animation.json` files (`bakeAnimationsFromFile`), and renders bone geometry (`renderBoneReflect`). However, **no animation stepping or bone matrix transformation controller currently updates bone position/rotation/scale before rendering**. As a result, GeckoLib models render in a static T-pose / rest pose without responding to player movement, arm swings, or hurt states.
2. **Player State Signal Inventory**: The client rendering pipeline has direct access to `AbstractClientPlayer` state variables: `walkAnimation.speed()`, `walkAnimation.position()`, `swingTime`/`swingProgress`, `hurtTime`, `isCrouching()`, `isVisuallySwimming()`, and `getAbilities().flying`. These signals can be mapped to configured GeckoLib animation triggers (`wereIdleAnim`, `wereWalkAnim`, `wereAttackAnim`, `wereFlyAnim`, `wereSwimAnim`).
3. **Render-Loop Particle Emission Bug**: In `PlayerRaceLayer.java`, ambient particle emission and `ParticleAuraData` loops execute inside `render(...)` under `if (player.tickCount % 3 == 0)`. Because `render(...)` is called every frame (e.g. 144+ FPS), particles spawn multiple times per tick on high-refresh monitors. A tick-guard (`lastParticleSpawnTick`) is required to throttle particle emission strictly to client logic ticks (20 Hz).
4. **Combat Visual Effects & Skin Overrides**: Standard damage red flash overlay (`OverlayTexture.v(true)`) is wired in `GeckoLibWereRenderer.renderCubeReflect` (line 168). Dynamic skin texture overrides are supported in `WereModelRenderer.getValidWereTextureLocation` using `"skin"` and `"player"` keywords with safe fallback to `player.getSkinTextureLocation()`.

---

## 2. Exploration Scope & System Architecture

The investigation examined 10 primary source files across common, client, event, and mixin layers:

| Component / Layer | Primary File | Responsibilities & Current Logic |
|---|---|---|
| **Transformation Handler** | `WereRaceTransformHandler.java` | Checks transformation triggers (Moon phase, night, water, rage, keybind), applies attribute modifiers & Pehkui scales, broadcasts state packets. |
| **Client State Storage** | `ClientWereState.java` | Maintains client-side map `Map<UUID, Boolean> TRANSFORMED_PLAYERS` synced via S2C network packets. |
| **GeckoLib Renderer** | `GeckoLibWereRenderer.java` | Reflection-backed loader for baking `.geo.json` models & `.animation.json` assets; renders bone cubes using Mojang `PoseStack`. |
| **Model Renderer Facade** | `WereModelRenderer.java` | Resolves model/texture/animation paths with fallback logic; handles base player mesh suppression (`setBaseModelVisible`). |
| **Render Pipeline Layer** | `PlayerRaceLayer.java` | Custom player `RenderLayer` rendering GeckoLib transformed model, procedural beast overlays, preset body parts, and ambient particle auras. |
| **Model Visibility Mixin** | `LivingEntityRendererMixin.java` | Injects at `render HEAD` to suppress base human player model (`head`, `body`, `arms`, `legs`) when GeckoLib model is active. |
| **Particle Data Struct** | `ParticleAuraData.java` | Defines particle type, base count, speed, and spread; calculates scaled particle count based on `race.wereParticleCount`. |
| **Race Configuration** | `RaceData.java` | Data model holding animation keys (`wereIdleAnim`, `wereWalkAnim`, `wereAttackAnim`, etc.) and particle settings (`particleCount`, `wereParticleCount`). |

---

## 3. Inspection of Transformation Handlers & Render Pipeline Hooks

### 3.1 Transformation State Lifecycle & Client Sync
The transformation lifecycle flows through three synchronized stages:

```
[Server World Tick / Keybind]
            │
   WereRaceTransformHandler.checkTransformation(ServerPlayer)
            │
            ├─► Apply Attribute Modifiers & Pehkui Scales (Server)
            └─► Broadcast ModPackets.syncWereStateToAll(...)
                        │
                        ▼ (S2C Packet: customraces:sync_were_state)
            ClientWereState.setTransformed(playerUUID, true)
                        │
                        ▼
       [Render Loop: LivingEntityRendererMixin & PlayerRaceLayer]
            │
            ├─► WereModelRenderer.isWereForm(player, race) == true
            ├─► Base Player Mesh Hidden (setBaseModelVisible = false)
            └─► GeckoLibWereRenderer.renderGeckoModel(...)
```

### 3.2 Render Pipeline Hooks Analysis
- **`LivingEntityRendererMixin.java`** (Lines 21-37):
  Injects at `render` method entry point before vanilla `PlayerModel` renders. If transformed and custom model is present:
  ```java
  if (WereModelRenderer.isWereForm(player, race) && WereModelRenderer.isModelAvailable(race)) {
      WereModelRenderer.setBaseModelVisible(playerModel, false);
  }
  ```
- **`PlayerRaceLayer.java`** (Lines 41-75):
  Executes as a feature layer on `PlayerRenderer`. Evaluates `isWereTransformed` and delegates to `WereModelRenderer.renderWereForm(...)`.

---

## 4. GeckoLib Keyframe Animation Controller & State Mapping

### 4.1 Root Cause of Rest-Pose / T-Pose Model Rendering
In `GeckoLibWereRenderer.java`:
- `bakeAnimationsFromFile(animLoc)` loads GeckoLib animation JSON into `BakedAnimations` and stores it in `GeckoLibCache.getBakedAnimations()`.
- However, inside `renderGeckoModel` and `renderBoneReflect` (lines 75-150), bones are queried for their static rest pivots (`getPivotX`, `getPivotY`, `getPivotZ`) and static initial rotations (`getRotX`, `getRotY`, `getRotZ`).
- **Missing Link**: No animation tick evaluator computes the current keyframe time, evaluates bone position/rotation/scale keyframes, or applies animated offsets (`px`, `py`, `pz`, `rx`, `ry`, `rz`) to `bone` before `renderBoneReflect` builds the vertex pose matrix stack.

### 4.2 Player Motion Signals for Animation Driving

To drive keyframe animations dynamically, the client model renderer can inspect the following real-time signals from `AbstractClientPlayer`:

```
                 ┌───────────────────────────────────────────────┐
                 │       AbstractClientPlayer Entity State       │
                 └───────────────────────┬───────────────────────┘
                                         │
       ┌───────────────────┬─────────────┴───────┬───────────────────┐
       ▼                   ▼                     ▼                   ▼
[walkAnimation.speed()] [swingTime/swingProgress] [hurtTime] [isVisuallySwimming()]
[walkAnimation.pos()]                                          [isFallFlying()]
       │                   │                     │                   │
       ▼                   ▼                     ▼                   ▼
  Walk / Run            Attack                Damage Flinch       Swim / Fly
  Animation           Animation               Recoil / Hurt       Animation
```

1. **Idle State**: `walkAnimation.speed() < 0.01f && swingTime == 0 && hurtTime == 0`.
2. **Walk State**: `walkAnimation.speed() >= 0.01f`. Animation play speed scales proportionally to `walkAnimation.speed()`.
3. **Attack State**: `player.swingTime > 0` or `player.attackAnim > 0.0f`. Animation progress tracks `player.getAttackAnim(partialTick)`.
4. **Hurt State**: `player.hurtTime > 0`. Triggers damage recoil animation / bone flinch overlay.
5. **Crouching State**: `player.isCrouching()`. Lowers model torso pivot Y by `-2.0 / 16.0f` and tilts pitch forward.
6. **Swimming / Flying State**: `player.isVisuallySwimming()` or `player.isFallFlying()`. Plays `wereSwimAnim` / `wereFlyAnim`.

### 4.3 Keyframe State Machine Priority Matrix

When multiple states occur simultaneously (e.g. attacking while walking), state priority must resolve animation overrides cleanly:

| Priority | State Condition | Animation Trigger Field | Play Mode | Blending Rule |
|---|---|---|---|---|
| **1 (Highest)** | `player.hurtTime > 0` | Damage recoil / flinch | Single-shot (10 ticks) | Additive bone tilt overlay on upper body |
| **2** | `player.swingTime > 0` | `race.getSafeWereAttackAnim()` | Single-shot (arm swing duration) | Upper-body / arm bone rotation override |
| **3** | `player.isVisuallySwimming()` | `race.wereSwimAnim` | Loop | Full-body swim animation override |
| **4** | `player.isFallFlying() \|\| (flying && isWereFlyingRace)` | `race.wereFlyAnim` | Loop | Full-body flight animation override |
| **5** | `walkAnimation.speed() >= 0.01f` | `race.getSafeWereWalkAnim()` | Loop | Time scaled by limb swing position |
| **6 (Lowest)** | Default / Stationary | `race.getSafeWereIdleAnim()` | Loop | Ambient looping idle time `(tickCount + partialTick) / 20.0f` |

---

## 5. Combat Visual Effects Analysis

### 5.1 Red Hurt Flash Overlay Rendering
- **Mechanism**: Minecraft entity renderers apply damage red tinting via `OverlayTexture`.
- **Current Code in `GeckoLibWereRenderer.java`**:
  ```java
  int overlay = (player != null && player.hurtTime > 0)
      ? OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(true))
      : OverlayTexture.NO_OVERLAY;
  ```
- **Verification**: Passing `overlay` to `.overlayCoords(overlay)` in `VertexConsumer.vertex(...)` correctly invokes Minecraft's entity damage shader, rendering the model with a red tint during `hurtTime > 0` ticks.

### 5.2 Dynamic Skin Texture Overrides
- **Supported Path Keywords in `WereModelRenderer.java`**:
  - `"skin"`, `"player"`, `"player_skin"`, `"skin_texture"`: Binds player's active skin texture via `player.getSkinTextureLocation()`.
  - Custom file paths (e.g. `config/custom_races/textures/were/werewolf_dark.png`): Loaded into OpenGL memory dynamically via `NativeImage` & `DynamicTexture` and cached in `DYNAMIC_TEXTURE_CACHE`.
  - Unresolvable paths: Safe fallback ladder to `DEFAULT_WERE_TEXTURE` (`customraces:textures/were/default_werewolf.png`) or `player.getSkinTextureLocation()`, preventing purple/black missing texture grids.

### 5.3 Ambient Particle Emission Scaling
- **Particle Count Settings in `RaceData.java`**:
  - Base Form Emission: `particleCount` (default: 5, range: 1–100).
  - Were-Form Emission: `wereParticleCount` (default: 10, range: 1–100).
- **Aura Scaling Formula in `ParticleAuraData.java`**:
  ```java
  public int getScaledParticleCount(int raceParticleCount) {
      int effectiveCount = raceParticleCount > 0 ? raceParticleCount : 5;
      return Math.max(1, Math.round(this.count * (effectiveCount / 5.0f)));
  }
  ```
- **Frame-Rate Dependency Bug Identified**:
  In `PlayerRaceLayer.java` line 57 and line 87, particle spawning checks `player.tickCount % 3 == 0` during model rendering. Because `render(...)` fires every frame, high-FPS clients spawn excessive particles per tick.
  - **Remediation**: Use a `lastSpawnedTick` timestamp guard map to ensure particle emission executes at most once per logic tick (20 Hz).

---

## 6. Identified Gaps, Edge Cases & Risk Mitigation

| Risk / Gap ID | Description | Potential Impact | Recommended Mitigation |
|---|---|---|---|
| **GAP-01** | Missing GeckoLib bone keyframe evaluator | GeckoLib models render frozen in T-pose | Implement `GeckoKeyframeEvaluator` class to step animations and compute bone pose transforms per frame. |
| **GAP-02** | Multi-render frame particle duplication | High FPS causes lag & particle density spikes | Add client tick guard `lastParticleTickMap` to restrict spawning to once per 20 Hz tick. |
| **GAP-03** | PoseStack matrix leakage on render exception | Pipeline crash causes player model rendering corruption | Wrap all bone & preset part rendering in strict `try-finally { poseStack.popPose(); }` blocks. |
| **GAP-04** | Dynamic texture cache memory growth | Switching skins/races repeatedly fills memory | Implement `clearCaches()` on world unload or resource reload events. |

---

## 7. Concrete Implementation Recommendations & Code Blueprints

### Blueprint 1: Lightweight GeckoLib Keyframe Evaluator (`GeckoKeyframeEvaluator.java`)

Create a dedicated evaluator in `ddraig.net.customraces.client.geckolib`:

```java
package ddraig.net.customraces.client.geckolib;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;

public class GeckoKeyframeEvaluator {

    public static void applyAnimationTransforms(Object bakedModel, ResourceLocation animLoc, AbstractClientPlayer player, float partialTick) {
        if (bakedModel == null || animLoc == null || player == null) return;
        
        try {
            Class<?> cacheClass = Class.forName("software.bernie.geckolib.cache.GeckoLibCache");
            java.lang.reflect.Method getAnimsMethod = cacheClass.getMethod("getBakedAnimations");
            java.util.Map<?, ?> animMap = (java.util.Map<?, ?>) getAnimsMethod.invoke(null);
            if (animMap == null || !animMap.containsKey(animLoc)) return;

            Object bakedAnimations = animMap.get(animLoc);
            // Evaluate animation time based on player state (walk speed, swing time, tick count)
            float tickTime = player.tickCount + partialTick;
            float walkSpeed = player.walkAnimation.speed();
            boolean isMoving = walkSpeed > 0.01f;
            boolean isAttacking = player.swingTime > 0;
            boolean isHurt = player.hurtTime > 0;

            // Determine active animation key
            String animName = "animation.were.idle";
            if (isHurt) {
                animName = "animation.were.hurt";
            } else if (isAttacking) {
                animName = "animation.were.attack";
            } else if (isMoving) {
                animName = "animation.were.walk";
            }

            // Step bone animations and update bone pos/rot/scale fields via reflection
        } catch (Throwable ignored) {}
    }
}
```

### Blueprint 2: Frame-Rate Throttled Particle Spawning (`PlayerRaceLayer.java`)

Update `PlayerRaceLayer.java` to prevent multi-frame particle duplication:

```java
private static final java.util.Map<java.util.UUID, Long> LAST_PARTICLE_TICKS = new java.util.concurrent.ConcurrentHashMap<>();

private boolean shouldSpawnParticlesThisTick(AbstractClientPlayer player) {
    if (player == null || player.level() == null || !player.level().isClientSide) return false;
    long currentTick = player.level().getGameTime();
    Long lastTick = LAST_PARTICLE_TICKS.get(player.getUUID());
    if (lastTick == null || currentTick != lastTick) {
        LAST_PARTICLE_TICKS.put(player.getUUID(), currentTick);
        return true;
    }
    return false;
}
```

---

## 8. Independent Verification Plan

1. **Multi-Platform Compilation**:
   - Run `./gradlew build -x test` to verify 0 compilation errors across Fabric and Forge.
2. **GeckoLib Model & Keyframe Animation Verification**:
   - Test transformed player model under motion (standing, walking, sprinting, swinging arm, taking damage, swimming).
   - Confirm model bones move smoothly according to keyframe animations without T-pose freezing.
3. **Combat Effects Verification**:
   - Verify red hurt flash overlay renders during damage ticks (`player.hurtTime > 0`).
   - Test `"skin"` and `"player"` texture keywords in `wereTexturePath` to confirm player skin binds cleanly without purple/black checkerboard errors.
4. **Particle Scaling & Performance Verification**:
   - Change `wereParticleCount` from `10` to `50` in race config and observe particle density scaling in-game.
   - Verify particle emission rate remains consistent regardless of frame rate (60 FPS vs 144+ FPS).

---
