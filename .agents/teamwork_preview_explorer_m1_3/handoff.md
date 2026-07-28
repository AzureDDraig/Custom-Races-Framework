# Handoff Report: Dynamic Transformations, Keyframe Animations & Combat Effects (R3)

**Role**: Explorer 3 (M1)  
**Target Path**: `.agents/teamwork_preview_explorer_m1_3/handoff.md`  
**Focus Area**: R3 - Dynamic Transformations, Keyframe Animations & Combat Effects  

---

## 1. Observation

1. **GeckoLib Model Loader & Renderer**:
   - File: `common/src/main/java/ddraig/net/customraces/client/render/GeckoLibWereRenderer.java`
   - Lines 41–73 (`renderGeckoModel`): Bakes and retrieves model assets via reflection (`GeckoLibCache.getBakedModels()`).
   - Lines 75–150 (`renderBoneReflect`): Reads static bone positions (`getPosX`, `getPosY`, `getPosZ`), rotations (`getRotX`, `getRotY`, `getRotZ`), and scales (`getScaleX`, `getScaleY`, `getScaleZ`).
   - Lines 302–355 (`bakeAnimationsFromFile`): Bakes `.animation.json` assets into `GeckoLibCache.getBakedAnimations()`.
   - **Observation**: `renderBoneReflect` renders bones using initial static pivots without updating bone position/rotation/scale against baked keyframe track channels per frame.

2. **Player Movement & Combat Signals**:
   - File: `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`
   - Lines 29–30: Receives `AbstractClientPlayer player`, `limbSwing`, `limbSwingAmount`, `partialTick`, `ageInTicks`, `netHeadYaw`, `headPitch`.
   - `AbstractClientPlayer` exposes `walkAnimation.speed()`, `swingTime`, `hurtTime`, `isCrouching()`, `isVisuallySwimming()`, and `getAbilities().flying`.

3. **Combat Effects & Red Hurt Flash**:
   - File: `common/src/main/java/ddraig/net/customraces/client/render/GeckoLibWereRenderer.java`
   - Line 168: `int overlay = (player != null && player.hurtTime > 0) ? OverlayTexture.pack(OverlayTexture.u(0.0F), OverlayTexture.v(true)) : OverlayTexture.NO_OVERLAY;`
   - Line 226: Passes `overlay` to `vc.vertex(...).overlayCoords(overlay)`.

4. **Dynamic Skin Texture Overrides**:
   - File: `common/src/main/java/ddraig/net/customraces/client/render/WereModelRenderer.java`
   - Lines 104–112: Intercepts `"skin"`, `"player"`, `"player_skin"`, `"skin_texture"` keywords to return `player.getSkinTextureLocation()`.
   - Lines 147–188: Loads dynamic disk textures from `config/custom_races/textures/` via `NativeImage` & `DynamicTexture` with safe fallback to default textures.

5. **Particle Emission & Scaling**:
   - File: `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java`
   - Lines 57 & 87: Spawns ambient smoke and `ParticleAuraData` particles inside `render(...)` using `player.tickCount % 3 == 0` and `player.tickCount % 4 == 0`.
   - File: `common/src/main/java/ddraig/net/customraces/data/ParticleAuraData.java`
   - Line 21–24 (`getScaledParticleCount`): Scales aura count proportionally: `Math.max(1, Math.round(this.count * (effectiveCount / 5.0f)))`.

---

## 2. Logic Chain

1. **From Observation 1**: `GeckoLibWereRenderer.java` bakes animations (`bakeAnimationsFromFile`) into `GeckoLibCache.getBakedAnimations()`, but `renderBoneReflect` queries static rest bone fields (`getRotX`, etc.) without applying keyframe pose updates.
   - **Step Reasoning**: Because bone pose values are never updated per tick/frame, GeckoLib models render in static T-pose / rest pose.
   - **Step Conclusion**: A keyframe animation evaluator (`GeckoKeyframeEvaluator`) must step animations based on player movement/combat state and update bone rotation/translation matrices before rendering.

2. **From Observation 2**: `AbstractClientPlayer` provides real-time state signals (`walkAnimation.speed()`, `swingTime`, `hurtTime`, `isVisuallySwimming()`, `flying`).
   - **Step Reasoning**: Comparing these signals against configured animation strings in `RaceData` (`wereIdleAnim`, `wereWalkAnim`, `wereAttackAnim`, `wereFlyAnim`, `wereSwimAnim`) yields a clear priority hierarchy: Hurt > Attack > Swim/Fly > Walk > Idle.
   - **Step Conclusion**: GeckoLib models can be dynamically animated matching player movement without requiring complex entity class hierarchies.

3. **From Observation 3 & 4**: Red hurt flash overlay is correctly wired via `OverlayTexture.v(true)` in `renderCubeReflect`, and dynamic skin textures resolve cleanly via `"skin"`/`"player"` keywords with safe fallbacks.
   - **Step Reasoning**: The existing hurt overlay and texture binding implementations are robust and prevent purple/black checkerboard texture errors.

4. **From Observation 5**: Particle emission in `PlayerRaceLayer.render(...)` checks `player.tickCount % 3 == 0` during rendering.
   - **Step Reasoning**: Since `render(...)` runs once per frame (e.g. 144 Hz) rather than once per tick (20 Hz), high refresh rates cause excessive particle spawning per tick.
   - **Step Conclusion**: A client game-time guard (`lastParticleTickMap`) must restrict particle emission to once per 20 Hz tick.

---

## 3. Caveats

- **No Source Code Implementation Performed**: As an Explorer agent, all investigation findings, blueprints, and recommendations are read-only analysis. Source files were not modified.
- **GeckoLib Reflection Compatibility**: Keyframe evaluation relies on GeckoLib reflection calls (`software.bernie.geckolib.cache.GeckoLibCache`). Any major GeckoLib major-version API breaking changes should be monitored during build testing.

---

## 4. Conclusion

Focus Area R3 (Dynamic Transformations, Keyframe Animations & Combat Effects) is architecturally sound in its state and rendering contracts, but requires two key enhancements for M4 implementation:
1. **GeckoLib Keyframe Animation Evaluator (`GeckoKeyframeEvaluator`)**: Implement keyframe channel evaluation to drive bone transformations dynamically from player state (idle, walk, attack, hurt, swim, fly).
2. **Frame-Rate Throttled Particle Spawning**: Add a 20 Hz client tick guard in `PlayerRaceLayer` to ensure ambient particle aura density remains constant across high-refresh monitors.

---

## 5. Verification Method

To independently verify these conclusions and recommendations:

1. **Compilation Check**:
   ```bash
   ./gradlew build -x test
   ```
   *Expected Result*: Clean build with 0 compilation errors across Fabric and Forge targets.

2. **File Inspection**:
   - Inspect `common/src/main/java/ddraig/net/customraces/client/render/GeckoLibWereRenderer.java` (lines 75–150) to confirm static bone matrix rendering.
   - Inspect `common/src/main/java/ddraig/net/customraces/client/render/PlayerRaceLayer.java` (lines 57–102) to verify particle spawning in render loop.

3. **Invalidation Conditions**:
   - If GeckoLib model bones animate without keyframe stepping, conclusion 1 is invalidated.
   - If particle emission density is frame-rate invariant without a tick guard, conclusion 2 is invalidated.
